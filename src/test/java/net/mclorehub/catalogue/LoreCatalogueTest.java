package net.mclorehub.catalogue;

import com.google.gson.*;
import net.mclorehub.importer.ImportException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import static net.mclorehub.TestData.*;
import static net.mclorehub.importer.JsonSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class LoreCatalogueTest {
    @TempDir Path temp;
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-28T12:00:00Z"), ZoneOffset.UTC);
    Path packPath, reviewsPath;
    JsonObject pack, decision, reviews;
    @BeforeEach void setup() throws Exception {
        pack = candidate(temp, export()); packPath = write(temp.resolve("pack.json"), pack);
        LoreCatalogue.Document doc = LoreCatalogue.load(packPath, null, CLOCK).documents().getFirst();
        decision = new JsonObject(); decision.addProperty("document_id", doc.id()); decision.addProperty("revision_sha256", doc.revision());
        decision.addProperty("review_fingerprint", doc.fingerprint()); decision.addProperty("status", "accepted_current");
        decision.addProperty("reviewer", "Synthetic reviewer"); decision.addProperty("reviewed_at", "2026-10-20T10:00:00Z");
        JsonObject citation = new JsonObject(); citation.addProperty("source_url", doc.url()); citation.addProperty("passage", "Synthetic approval evidence; not actual lore");
        JsonArray citations = new JsonArray(); citations.add(citation); decision.add("evidence", citations);
        reviews = new JsonObject(); reviews.addProperty("schema_version", 1); JsonArray decisions = new JsonArray(); decisions.add(decision); reviews.add("decisions", decisions);
        reviewsPath = temp.resolve("reviews.json");
    }
    LoreCatalogue load() throws Exception { write(reviewsPath, reviews); return LoreCatalogue.load(packPath, reviewsPath, CLOCK); }
    @Test void importsNeverInferCurrentFromCheckmarksOrTags() throws Exception {
        var catalogue = LoreCatalogue.load(packPath, null, CLOCK);
        assertTrue(catalogue.currentDocuments().isEmpty()); assertEquals(1, catalogue.reviewQueue().size());
        assertEquals(List.of("Synthetic alias"), catalogue.documents().getFirst().aliases());
        assertEquals(Optional.of("Test author"), catalogue.documents().getFirst().author());
        assertEquals(Instant.parse("2026-09-21T02:12:38Z"), catalogue.documents().getFirst().exportSnapshotAt());
        assertEquals(LoreCatalogue.Role.PLAYER_OPTION_CANDIDATE, catalogue.documents().getFirst().role());
    }
    @Test void missingAttributionRemainsUnknownInsteadOfInventingAnAuthor() throws Exception {
        JsonObject e = export(); record(e).add("author", JsonNull.INSTANCE); write(packPath, candidate(temp, e));
        var c = LoreCatalogue.load(packPath, null, CLOCK);
        assertTrue(c.documents().getFirst().author().isEmpty());
        assertFalse(c.report().getAsJsonArray("review_queue").get(0).getAsJsonObject().has("source_author"));
    }
    @Test void appliesBoundLocalReviewAndRetainsEvidenceAndTime() throws Exception {
        byte[] before = Files.readAllBytes(packPath); var catalogue = load(); var doc = catalogue.currentDocuments().getFirst();
        assertEquals(LoreCatalogue.ReviewState.APPLIED, doc.reviewState()); assertTrue(catalogue.reviewQueue().isEmpty());
        assertEquals("Synthetic reviewer", doc.decision().orElseThrow().reviewer());
        assertEquals("Synthetic approval evidence; not actual lore", doc.decision().orElseThrow().evidence().getFirst().passage());
        assertArrayEquals(before, Files.readAllBytes(packPath));
    }
    @ParameterizedTest @ValueSource(strings={"legacy","shelved","superseded","pending","unknown"})
    void otherStatusesStayOutsideCurrentResults(String status) throws Exception {
        decision.addProperty("status", status); var c = load(); assertTrue(c.currentDocuments().isEmpty());
        assertEquals(LoreCatalogue.Status.valueOf(status.toUpperCase(Locale.ROOT)), c.documents().getFirst().status());
    }
    @Test void changedBodyInvalidatesEarlierReview() throws Exception {
        JsonObject e = export(); record(e).addProperty("content_text", "New source revision"); rehash(record(e));
        write(packPath, candidate(temp, e)); var c = load();
        assertTrue(c.currentDocuments().isEmpty()); assertEquals(LoreCatalogue.ReviewState.STALE, c.documents().getFirst().reviewState());
        assertTrue(c.documents().getFirst().decision().isPresent());
    }
    @Test void changedStatusEvidenceInvalidatesReviewEvenWithTheSameBodyHash() throws Exception {
        String old = decision.get("revision_sha256").getAsString();
        JsonObject e = export(); record(e).addProperty("status_note", "New shelving evidence"); write(packPath, candidate(temp, e));
        var c = load(); assertEquals(old, c.documents().getFirst().revision()); assertEquals(0, c.currentDocuments().size());
        assertEquals(1, c.report().get("stale_review_count").getAsInt());
    }
    @Test void addedReplyEvidenceAlsoInvalidatesReview() throws Exception {
        JsonObject e = export(); JsonArray posts = new JsonArray(); posts.add(post("456")); record(e).add("thread_posts", posts);
        write(packPath, candidate(temp, e)); assertEquals(LoreCatalogue.ReviewState.STALE, load().documents().getFirst().reviewState());
    }
    @Test void objectKeyReorderingDoesNotInvalidateEquivalentSource() throws Exception {
        JsonObject e = export(), raw = record(e), reversed = new JsonObject();
        List<String> keys = new ArrayList<>(raw.keySet()); Collections.reverse(keys); keys.forEach(k -> reversed.add(k, raw.get(k)));
        e.getAsJsonArray("records").set(0, reversed); write(packPath, candidate(temp, e)); assertEquals(1, load().currentDocuments().size());
    }
    @Test void orphanedReviewIsReportedAndNotAppliedToAnotherDocument() throws Exception {
        decision.addProperty("document_id", "lotc:topic:999"); var c = load();
        assertTrue(c.currentDocuments().isEmpty()); assertEquals(List.of("lotc:topic:999"), c.orphanedReviewIds());
        assertEquals(1, c.reviewQueue().size());
    }
    static Stream<Consumer<JsonObject>> badDecisions() {
        return Stream.of(d -> d.addProperty("status", "invented"), d -> d.addProperty("status", "ACCEPTED_CURRENT"), d -> d.addProperty("reviewer", " "),
                d -> d.addProperty("document_id", "../../source"), d -> d.addProperty("revision_sha256", "bad"),
                d -> d.addProperty("review_fingerprint", "bad"), d -> d.addProperty("reviewed_at", "2030-01-01T00:00:00Z"),
                d -> d.addProperty("reviewed_at", "2026-10-20"), d -> d.add("evidence", new JsonArray()),
                d -> d.getAsJsonArray("evidence").get(0).getAsJsonObject().addProperty("passage", " "),
                d -> d.getAsJsonArray("evidence").get(0).getAsJsonObject().addProperty("source_url", "https://evil.example/forums/topic/123/"));
    }
    @ParameterizedTest @MethodSource("badDecisions") void invalidReviewNeverProducesCurrentResults(Consumer<JsonObject> mutate) throws Exception {
        mutate.accept(decision); write(reviewsPath, reviews); assertThrows(ImportException.class, () -> LoreCatalogue.load(packPath, reviewsPath, CLOCK));
    }
    @Test void duplicateDecisionsAndUnknownReviewVersionAreRejected() throws Exception {
        reviews.getAsJsonArray("decisions").add(decision.deepCopy()); write(reviewsPath, reviews);
        assertThrows(ImportException.class, () -> LoreCatalogue.load(packPath, reviewsPath, CLOCK));
        reviews.getAsJsonArray("decisions").remove(1); reviews.addProperty("schema_version", 2); write(reviewsPath, reviews);
        assertThrows(ImportException.class, () -> LoreCatalogue.load(packPath, reviewsPath, CLOCK));
    }
    @Test void fingerprintMismatchIsStaleNotSilentlyApproved() throws Exception {
        decision.addProperty("review_fingerprint", "0".repeat(64)); var c = load();
        assertEquals(LoreCatalogue.ReviewState.STALE, c.documents().getFirst().reviewState()); assertTrue(c.currentDocuments().isEmpty());
    }
    static Stream<Consumer<JsonObject>> badPacks() {
        return Stream.of(p -> p.addProperty("schema_version", 2), p -> p.addProperty("kind", "active"),
                p -> p.getAsJsonArray("documents").get(0).getAsJsonObject().getAsJsonObject("status").addProperty("value", "accepted_current"),
                p -> p.getAsJsonArray("documents").get(0).getAsJsonObject().addProperty("title", "Forged title"),
                p -> p.getAsJsonArray("documents").get(0).getAsJsonObject().getAsJsonArray("blocks").get(0).getAsJsonObject().addProperty("text", "Forged text"),
                p -> p.getAsJsonArray("documents").get(0).getAsJsonObject().addProperty("id", "lotc:topic:999"),
                p -> p.addProperty("revision_id", "sha256:" + "0".repeat(64)));
    }
    @ParameterizedTest @MethodSource("badPacks") void rejectsTamperedOrUnsupportedCandidates(Consumer<JsonObject> mutate) throws Exception {
        mutate.accept(pack); write(packPath, pack); assertThrows(ImportException.class, () -> LoreCatalogue.load(packPath, null, CLOCK));
    }
    @ParameterizedTest @CsvSource({
        "navigation hub; not a separate magic,NAVIGATION_HUB", "background/context; not a player option,BACKGROUND_CONTEXT",
        "disconnection notice; not available Paladin magic,DISCONNECTION_NOTICE", "core rules and navigation hub,CORE_AND_HUB",
        "unrecognized source role,UNCLASSIFIED"})
    void classifiesFromAuditEvidenceWithoutTreatingHubsAsMagics(String audit, String expected) throws Exception {
        JsonObject e = export(); record(e).addProperty("audit_role", audit); write(packPath, candidate(temp, e));
        var c = LoreCatalogue.load(packPath, null, CLOCK); assertEquals(LoreCatalogue.Role.valueOf(expected), c.documents().getFirst().role());
        assertTrue(c.currentDocuments().isEmpty());
    }
    @ParameterizedTest @CsvSource({"CArt,CHARACTER_ARTIFACT", "MArt,MAGICAL_ARTIFACT"})
    void artifactCategoriesAreNotCaFaApplications(String artifact, String expected) throws Exception {
        JsonObject e = export(); record(e).addProperty("audit_role", "approved_artifact_record"); record(e).addProperty("artifact_type", artifact);
        write(packPath, candidate(temp, e)); var c = LoreCatalogue.load(packPath, null, CLOCK);
        assertEquals(LoreCatalogue.Role.valueOf(expected), c.documents().getFirst().role()); assertTrue(c.currentDocuments().isEmpty());
    }
    @Test void publicCollectionsCannotMutateCatalogueOrReviewEvidence() throws Exception {
        var c = load(); var doc = c.documents().getFirst();
        assertThrows(UnsupportedOperationException.class, () -> c.documents().clear());
        assertThrows(UnsupportedOperationException.class, () -> c.currentDocuments().clear());
        assertThrows(UnsupportedOperationException.class, () -> doc.aliases().add("mutation"));
        assertThrows(UnsupportedOperationException.class, () -> doc.decision().orElseThrow().evidence().clear());
    }
}
