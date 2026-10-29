package net.mclorehub.catalogue;

import com.google.gson.*;
import net.mclorehub.importer.ImportException;
import java.io.IOException;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import static net.mclorehub.importer.JsonSupport.*;

/** Immutable catalogue projection with separate, revision-bound local review decisions. */
public final class LoreCatalogue {
    public enum Status { UNKNOWN, ACCEPTED_CURRENT, LEGACY, SHELVED, SUPERSEDED, PENDING }
    public enum ReviewState { UNREVIEWED, APPLIED, STALE }
    public enum Role {
        PLAYER_OPTION_CANDIDATE, NAVIGATION_HUB, CORE_AND_HUB, BACKGROUND_CONTEXT,
        DISCONNECTION_NOTICE, CHARACTER_ARTIFACT, MAGICAL_ARTIFACT, UNCLASSIFIED
    }
    public record Evidence(String url, String passage) {}
    public record ThreadPost(String id, String author, String url, String text) {}
    public record Decision(String id, String revision, String fingerprint, Status status,
                           String reviewer, Instant reviewedAt, List<Evidence> evidence) {
        public Decision { evidence = List.copyOf(evidence); }
    }
    public record Document(String id, String title, String url, Optional<String> author, Instant exportSnapshotAt,
                           String body, String revision,
                           String fingerprint, String family, String category, List<String> aliases,
                           Role role, Status status, ReviewState reviewState, Optional<Decision> decision,
                           List<ThreadPost> posts, List<String> coverageGaps) {
        public Document {
            aliases = List.copyOf(aliases); posts = List.copyOf(posts); coverageGaps = List.copyOf(coverageGaps);
        }
    }
    private final String revision;
    private final List<Document> documents;
    private final List<String> orphanedReviewIds;

    private LoreCatalogue(String revision, List<Document> documents, List<String> orphans) {
        this.revision = revision; this.documents = List.copyOf(documents); this.orphanedReviewIds = List.copyOf(orphans);
    }
    public List<Document> documents() { return documents; }
    public List<Document> currentDocuments() {
        return documents.stream().filter(d -> d.status() == Status.ACCEPTED_CURRENT && d.reviewState() == ReviewState.APPLIED).toList();
    }
    public List<Document> reviewQueue() {
        return documents.stream().filter(d -> d.reviewState() != ReviewState.APPLIED || d.status() == Status.UNKNOWN).toList();
    }
    public List<String> orphanedReviewIds() { return orphanedReviewIds; }

    public static LoreCatalogue load(Path packPath, Path reviewPath) throws IOException {
        return load(packPath, reviewPath, Clock.systemUTC());
    }
    public static LoreCatalogue load(Path packPath, Path reviewPath, Clock clock) throws IOException {
        JsonObject pack = read(packPath, 64 << 20);
        if (integer(pack, "schema_version") != 1 || !text(pack, "importer_version").equals("1")
                || !text(pack, "kind").equals("review_candidate") || !text(pack, "snapshot_scope").equals("partial"))
            throw new ImportException("Unsupported candidate pack contract");
        JsonObject source = object(pack.get("source"), "source");
        sha(source, "input_sha256"); String jsonHash = sha(source, "json_sha256");
        String revision = text(pack, "revision_id");
        if (!revision.equals("sha256:" + jsonHash)) throw new ImportException("Pack revision does not match source hash");
        String snapshot = text(source, "snapshot_at"); Instant snapshotAt = instant(snapshot);
        JsonObject provenance = object(source.get("export_provenance"), "export_provenance");
        JsonObject metadata = object(provenance.get("metadata"), "metadata");
        JsonArray entries = array(pack.get("documents"), "documents");
        if (entries.isEmpty() || entries.size() > 10_000 || integer(metadata, "record_count") != entries.size())
            throw new ImportException("Invalid candidate document count");
        Map<String, Decision> decisions = reviewPath == null ? Map.of() : readDecisions(reviewPath, clock);
        Set<String> ids = new HashSet<>(), posts = new HashSet<>(); List<Document> documents = new ArrayList<>();
        for (JsonElement entry : entries) {
            JsonObject doc = object(entry, "document");
            JsonObject raw = object(doc.get("source_record"), "source_record");
            String id = "lotc:topic:" + identifier(raw, "topic_id");
            if (!text(doc, "id").equals(id) || !ids.add(id)) throw new ImportException("Mismatched or duplicate document ID");
            String title = text(raw, "title"), url = text(raw, "url"), body = text(raw, "content_text"); forumUrl(url);
            String digest = sha(doc, "revision_sha256");
            if (!digest.equals(hash(body)) || !digest.equals(sha(raw, "content_sha256"))
                    || !title.equals(text(doc, "title")) || !url.equals(text(doc, "source_url")))
                throw new ImportException("Document/source content mismatch: " + id);
            if (!snapshot.equals(text(doc, "source_snapshot_at")) || !text(doc, "formatting_fidelity").equals("plain_text"))
                throw new ImportException("Unsupported source snapshot/formatting");
            JsonObject status = object(doc.get("status"), "status");
            JsonElement requires = status.get("requires_review");
            if (!text(status, "value").equals("unknown") || requires == null || !requires.isJsonPrimitive()
                    || !requires.getAsJsonPrimitive().isBoolean() || !requires.getAsBoolean())
                throw new ImportException("Candidate statuses cannot certify acceptance; use separate reviews");
            text(status, "reason"); strings(doc, "coverage_gaps"); strings(raw, "tags");
            JsonArray blocks = array(doc.get("blocks"), "blocks");
            if (blocks.size() != 1) throw new ImportException("Expected one plain-text block");
            JsonObject block = object(blocks.get(0), "block");
            if (!text(block, "id").equals("body") || !text(block, "type").equals("plain_text") || !body.equals(text(block, "text")))
                throw new ImportException("Reader text does not match source");
            String error = optional(raw, "scrape_error");
            if (error != null && !error.isBlank()) throw new ImportException("Source retrieval error: " + id);
            List<ThreadPost> thread = new ArrayList<>();
            if (raw.has("thread_posts")) {
                for (JsonElement postEntry : array(raw.get("thread_posts"), "thread_posts")) {
                    JsonObject post = object(postEntry, "post"); String postId = identifier(post, "post_id");
                    if (!posts.add(postId)) throw new ImportException("Duplicate post ID");
                    string(post, "author"); string(post, "content_text"); forumUrl(text(post, "url"));
                    thread.add(new ThreadPost(postId, string(post, "author"), text(post, "url"), string(post, "content_text")));
                }
            }
            String fingerprint = fingerprint(raw);
            Decision decision = decisions.get(id); ReviewState state = ReviewState.UNREVIEWED; Status effective = Status.UNKNOWN;
            if (decision != null) {
                state = digest.equals(decision.revision()) && fingerprint.equals(decision.fingerprint()) ? ReviewState.APPLIED : ReviewState.STALE;
                if (state == ReviewState.APPLIED) effective = decision.status();
            }
            Optional<String> author = Optional.ofNullable(optional(raw, "author")).filter(s -> !s.isBlank());
            documents.add(new Document(id, title, url, author, snapshotAt, body, digest, fingerprint, text(raw, "family"), text(raw, "category"),
                    strings(raw, "search_aliases"), role(raw), effective, state, Optional.ofNullable(decision),
                    thread, strings(doc, "coverage_gaps")));
        }
        return new LoreCatalogue(revision, documents, decisions.keySet().stream().filter(id -> !ids.contains(id)).sorted().toList());
    }

    private static Role role(JsonObject raw) throws ImportException {
        String audit = optional(raw, "audit_role");
        if ("approved_artifact_record".equals(audit)) {
            return switch (Objects.toString(optional(raw, "artifact_type"), "")) {
                case "CArt" -> Role.CHARACTER_ARTIFACT;
                case "MArt" -> Role.MAGICAL_ARTIFACT;
                default -> Role.UNCLASSIFIED;
            };
        }
        return switch (Objects.toString(audit, "")) {
            case "player-use candidate; requires source review" -> Role.PLAYER_OPTION_CANDIDATE;
            case "navigation hub; not a separate magic" -> Role.NAVIGATION_HUB;
            case "core rules and navigation hub" -> Role.CORE_AND_HUB;
            case "background/context; not a player option" -> Role.BACKGROUND_CONTEXT;
            case "disconnection notice; not available Paladin magic" -> Role.DISCONNECTION_NOTICE;
            default -> Role.UNCLASSIFIED;
        };
    }

    private static Map<String, Decision> readDecisions(Path path, Clock clock) throws IOException {
        JsonObject manifest = read(path, 8 << 20);
        if (integer(manifest, "schema_version") != 1) throw new ImportException("Unsupported review schema");
        JsonArray list = array(manifest.get("decisions"), "decisions");
        if (list.size() > 10_000) throw new ImportException("Too many review decisions");
        Map<String, Decision> result = new LinkedHashMap<>();
        for (JsonElement item : list) {
            JsonObject entry = object(item, "decision"); String id = text(entry, "document_id");
            if (!id.matches("lotc:topic:[1-9][0-9]{0,17}")) throw new ImportException("Invalid review document ID");
            Status status;
            try {
                String value = text(entry, "status");
                if (!value.equals(value.toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("Status must be lower case");
                status = Status.valueOf(value.toUpperCase(Locale.ROOT));
            }
            catch (IllegalArgumentException e) { throw new ImportException("Unknown review status", e); }
            Instant reviewedAt = instant(text(entry, "reviewed_at"));
            if (reviewedAt.isAfter(clock.instant())) throw new ImportException("Review date is in the future");
            List<Evidence> evidence = new ArrayList<>();
            for (JsonElement evidenceItem : array(entry.get("evidence"), "evidence")) {
                JsonObject citation = object(evidenceItem, "citation");
                String url = text(citation, "source_url"); forumUrl(url);
                evidence.add(new Evidence(url, text(citation, "passage")));
            }
            if (evidence.isEmpty()) throw new ImportException("Review must retain source evidence");
            Decision decision = new Decision(id, sha(entry, "revision_sha256"), sha(entry, "review_fingerprint"), status,
                    text(entry, "reviewer"), reviewedAt, evidence);
            if (result.putIfAbsent(id, decision) != null) throw new ImportException("Duplicate review decision: " + id);
        }
        return Collections.unmodifiableMap(result);
    }
    private static Instant instant(String text) throws ImportException {
        try { return OffsetDateTime.parse(text).toInstant(); }
        catch (DateTimeParseException e) { throw new ImportException("Date must be ISO-8601 with an offset", e); }
    }
    private static JsonObject summary(Document document) {
        JsonObject item = new JsonObject(); item.addProperty("id", document.id()); item.addProperty("title", document.title());
        document.author().ifPresent(author -> item.addProperty("source_author", author));
        item.addProperty("export_snapshot_at", document.exportSnapshotAt().toString());
        item.addProperty("source_url", document.url()); item.addProperty("revision_sha256", document.revision());
        item.addProperty("review_fingerprint", document.fingerprint()); item.addProperty("family", document.family());
        item.addProperty("category", document.category()); item.add("aliases", JSON.toJsonTree(document.aliases()));
        item.addProperty("role", document.role().name().toLowerCase(Locale.ROOT));
        item.addProperty("status", document.status().name().toLowerCase(Locale.ROOT));
        item.addProperty("review_state", document.reviewState().name().toLowerCase(Locale.ROOT));
        document.decision().ifPresent(d -> {
            item.addProperty("reviewer", d.reviewer()); item.addProperty("reviewed_at", d.reviewedAt().toString());
            JsonArray evidence = new JsonArray();
            for (Evidence citation : d.evidence()) {
                JsonObject e = new JsonObject(); e.addProperty("source_url", citation.url()); e.addProperty("passage", citation.passage()); evidence.add(e);
            }
            item.add("review_evidence", evidence);
        });
        return item;
    }
    public JsonObject report() {
        JsonObject report = new JsonObject(); report.addProperty("schema_version", 1); report.addProperty("pack_revision_id", revision);
        report.addProperty("document_count", documents.size()); report.addProperty("current_count", currentDocuments().size());
        report.addProperty("needs_review_count", reviewQueue().size());
        report.addProperty("stale_review_count", documents.stream().filter(d -> d.reviewState() == ReviewState.STALE).count());
        JsonObject roles = new JsonObject(), statuses = new JsonObject();
        for (Role role : Role.values()) roles.addProperty(role.name().toLowerCase(Locale.ROOT), documents.stream().filter(d -> d.role() == role).count());
        for (Status status : Status.values()) statuses.addProperty(status.name().toLowerCase(Locale.ROOT), documents.stream().filter(d -> d.status() == status).count());
        report.add("role_counts", roles); report.add("status_counts", statuses);
        report.add("orphaned_review_ids", JSON.toJsonTree(orphanedReviewIds));
        JsonArray current = new JsonArray(), queue = new JsonArray(), other = new JsonArray();
        for (Document doc : documents) {
            JsonObject summary = summary(doc);
            if (doc.status() == Status.ACCEPTED_CURRENT && doc.reviewState() == ReviewState.APPLIED) current.add(summary);
            else if (doc.reviewState() != ReviewState.APPLIED || doc.status() == Status.UNKNOWN) queue.add(summary);
            else other.add(summary);
        }
        report.add("current_documents", current); report.add("review_queue", queue); report.add("other_reviewed_documents", other);
        return report;
    }
}
