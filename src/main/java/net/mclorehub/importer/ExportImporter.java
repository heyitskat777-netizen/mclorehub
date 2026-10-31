package net.mclorehub.importer;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.zip.*;
import static net.mclorehub.importer.JsonSupport.*;

/** Offline conversion only. Candidates never activate a pack or certify current lore. */
public final class ExportImporter {
    public static final String VERSION = "1";
    public record Limits(int inputBytes, int entryBytes, long expandedBytes, int entries, int records) {
        public Limits {
            if (inputBytes < 1 || entryBytes < 1 || expandedBytes < 1 || entries < 1 || records < 1
                    || inputBytes == Integer.MAX_VALUE || entryBytes == Integer.MAX_VALUE)
                throw new IllegalArgumentException("Limits must be positive and bounded");
        }
        public static Limits defaults() { return new Limits(32 << 20, 32 << 20, 64L << 20, 32, 10_000); }
    }
    public record Result(JsonObject pack, JsonObject report) {}
    private final Limits limits;
    public ExportImporter() { this(Limits.defaults()); }
    public ExportImporter(Limits limits) { this.limits = limits; }

    public Result importFile(Path path) throws IOException {
        byte[] input;
        try (InputStream in = Files.newInputStream(path)) { input = bounded(in, limits.inputBytes()); }
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        byte[] json;
        if (name.endsWith(".zip")) json = archive(input);
        else if (name.endsWith(".json")) json = input;
        else throw new ImportException("Expected a .zip or .json export");
        JsonObject export = parse(json);
        JsonObject metadata = object(export.get("metadata"), "metadata");
        JsonArray records = array(export.get("records"), "records");
        if (records.isEmpty() || records.size() > limits.records() || integer(metadata, "record_count") != records.size())
            throw new ImportException("Record count outside limits or different from metadata.record_count");
        String snapshot = optional(metadata, "updated_utc");
        if (snapshot == null) snapshot = text(metadata, "generated_utc");
        try { OffsetDateTime.parse(snapshot); }
        catch (DateTimeParseException e) { throw new ImportException("Invalid source snapshot date", e); }
        JsonArray documents = new JsonArray(); Set<String> topics = new HashSet<>(), posts = new HashSet<>();
        int postCount = 0, artifactCount = 0;
        for (JsonElement item : records) {
            JsonObject raw = object(item, "record");
            String topic = identifier(raw, "topic_id"), title = text(raw, "title"), body = text(raw, "content_text");
            if (!topics.add(topic)) throw new ImportException("Duplicate topic ID: " + topic);
            String url = text(raw, "url"); forumUrl(url);
            String digest = hash(body);
            if (!digest.equals(sha(raw, "content_sha256"))) throw new ImportException("Content hash mismatch: " + topic);
            String error = optional(raw, "scrape_error");
            if (error != null && !error.isBlank()) throw new ImportException("Source retrieval error: " + topic);
            strings(raw, "tags"); strings(raw, "search_aliases");
            JsonArray gaps = new JsonArray(); gaps.add("current_status_requires_review"); gaps.add("original_visual_formatting_unavailable");
            if (raw.has("thread_posts")) {
                for (JsonElement postItem : array(raw.get("thread_posts"), "thread_posts")) {
                    JsonObject post = object(postItem, "thread post"); String id = identifier(post, "post_id");
                    if (!posts.add(id)) throw new ImportException("Duplicate post ID: " + id);
                    if (string(post, "author").isBlank()) gaps.add("unattributed_thread_post:" + id);
                    string(post, "content_text"); forumUrl(text(post, "url")); postCount++;
                }
            } else gaps.add("reply_amendments_and_continuations_not_captured");
            if(raw.has("coverage_notes"))for(String note:strings(raw,"coverage_notes"))gaps.add(note);
            if (raw.has("artifact_type")) artifactCount++;
            JsonObject document = new JsonObject();
            document.addProperty("id", "lotc:topic:" + topic); document.addProperty("title", title);
            String recordSnapshot=optional(raw,"source_snapshot_at");if(recordSnapshot==null)recordSnapshot=snapshot;
            try{if(OffsetDateTime.parse(recordSnapshot).toInstant().isAfter(OffsetDateTime.parse(snapshot).toInstant()))throw new ImportException("Record snapshot is newer than export");}
            catch(DateTimeParseException e){throw new ImportException("Invalid per-record snapshot date",e);}
            document.addProperty("source_url", url); document.addProperty("source_snapshot_at", recordSnapshot);
            document.addProperty("revision_sha256", digest); document.addProperty("formatting_fidelity", "plain_text");
            JsonObject status = new JsonObject(); status.addProperty("value", "unknown"); status.addProperty("requires_review", true);
            status.addProperty("reason", "Snapshot import is not a current acceptance audit"); document.add("status", status);
            JsonObject block = new JsonObject(); block.addProperty("id", "body"); block.addProperty("type", "plain_text"); block.addProperty("text", body);
            JsonArray blocks = new JsonArray(); blocks.add(block); document.add("blocks", blocks);
            document.add("coverage_gaps", gaps); document.add("source_record", raw.deepCopy()); documents.add(document);
        }
        JsonObject provenance = export.deepCopy(); provenance.remove("records");
        JsonObject source = new JsonObject(); source.addProperty("input_sha256", hash(input)); source.addProperty("json_sha256", hash(json));
        source.addProperty("snapshot_at", snapshot); source.add("export_provenance", provenance);
        JsonObject pack = new JsonObject(); pack.addProperty("schema_version", 1); pack.addProperty("importer_version", VERSION);
        pack.addProperty("revision_id", "sha256:" + hash(json)); pack.addProperty("kind", "review_candidate"); pack.addProperty("snapshot_scope", "partial");
        pack.add("source", source); pack.add("documents", documents);
        JsonObject report = new JsonObject(); report.addProperty("schema_version", 1); report.addProperty("imported_at", Instant.now().toString());
        report.add("revision_id", pack.get("revision_id")); report.addProperty("input_sha256", hash(input));
        report.addProperty("documents", documents.size()); report.addProperty("artifact_documents", artifactCount);
        report.addProperty("thread_posts", postCount); report.addProperty("verified_content_hashes", documents.size());
        report.addProperty("accepted_current", 0); report.addProperty("requires_review", documents.size());
        JsonArray warnings = new JsonArray(); warnings.add("All statuses require review; plain text cannot restore original visual formatting");
        warnings.add("Partial snapshot: missing records are not deletions; candidate does not activate a pack"); report.add("warnings", warnings);
        return new Result(pack, report);
    }
    private byte[] archive(byte[] input) throws IOException {
        Set<String> names = new HashSet<>(); byte[] json = null; int count = 0; long expanded = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(input))) {
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
                if (++count > limits.entries()) throw new ImportException("Too many ZIP entries");
                String name = entry.getName();
                if (name.isEmpty() || name.startsWith("/") || name.contains("\\") || name.matches("^[A-Za-z]:.*"))
                    throw new ImportException("Unsafe ZIP path");
                for (String part : name.split("/"))
                    if (part.isEmpty() || part.equals(".") || part.equals("..")) throw new ImportException("Unsafe ZIP path");
                if (!names.add(name)) throw new ImportException("Duplicate ZIP entry");
                byte[] bytes = bounded(zip, (int) Math.min(limits.entryBytes(), limits.expandedBytes() - expanded));
                expanded += bytes.length;
                if (name.equals("lotc_lore.json") && !entry.isDirectory()) json = bytes;
                zip.closeEntry();
            }
        } catch (IllegalArgumentException e) { throw new ImportException("Invalid ZIP", e); }
        if (json == null) throw new ImportException("ZIP must contain root lotc_lore.json");
        return json;
    }
}
