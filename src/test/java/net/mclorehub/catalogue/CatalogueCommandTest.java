package net.mclorehub.catalogue;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import static net.mclorehub.TestData.*;
import static net.mclorehub.importer.JsonSupport.*;

class CatalogueCommandTest {
    @TempDir Path temp;
    int run(String... args) { return CatalogueCommand.run(args, new PrintStream(new ByteArrayOutputStream()), new PrintStream(new ByteArrayOutputStream())); }
    @Test void writesReviewQueueWithNoCurrentDocumentsAndNoSourceChanges() throws Exception {
        Path pack = write(temp.resolve("pack.json"), candidate(temp, export())); byte[] before = Files.readAllBytes(pack);
        Path out = temp.resolve("catalogue.json"); assertEquals(0, run("--pack", pack.toString(), "--output", out.toString()));
        var report = read(out, 1 << 20); assertEquals(0, report.get("current_count").getAsInt()); assertEquals(1, report.getAsJsonArray("review_queue").size());
        assertArrayEquals(before, Files.readAllBytes(pack));
    }
    @Test void rejectsExistingTargetsAndLeavesSentinelsAlone() throws Exception {
        Path pack = write(temp.resolve("pack.json"), candidate(temp, export())); Path out = temp.resolve("personal.json"); Files.writeString(out, "sentinel");
        assertEquals(1, run("--pack", pack.toString(), "--output", out.toString())); assertEquals("sentinel", Files.readString(out));
    }
    @Test void reviewOptionProducesOnlyAnExplicitlyReviewedCurrentResult() throws Exception {
        Path pack = write(temp.resolve("pack.json"), candidate(temp, export()));
        var doc = LoreCatalogue.load(pack, null).documents().getFirst();
        var manifest = new com.google.gson.JsonObject(); manifest.addProperty("schema_version", 1);
        var decision = new com.google.gson.JsonObject();
        decision.addProperty("document_id", doc.id()); decision.addProperty("revision_sha256", doc.revision());
        decision.addProperty("review_fingerprint", doc.fingerprint()); decision.addProperty("status", "accepted_current");
        decision.addProperty("reviewer", "Synthetic reviewer"); decision.addProperty("reviewed_at", "2026-10-20T10:00:00Z");
        var citation = new com.google.gson.JsonObject(); citation.addProperty("source_url", doc.url()); citation.addProperty("passage", "Synthetic evidence only");
        var citations = new com.google.gson.JsonArray(); citations.add(citation); decision.add("evidence", citations);
        var decisions = new com.google.gson.JsonArray(); decisions.add(decision); manifest.add("decisions", decisions);
        Path reviews = write(temp.resolve("reviews.json"), manifest); byte[] before = Files.readAllBytes(reviews);
        Path out = temp.resolve("current.json");
        assertEquals(0, run("--pack", pack.toString(), "--reviews", reviews.toString(), "--output", out.toString()));
        assertEquals(1, read(out, 1 << 20).get("current_count").getAsInt()); assertArrayEquals(before, Files.readAllBytes(reviews));
        try (var paths = Files.list(temp)) { assertFalse(paths.anyMatch(p -> p.getFileName().toString().startsWith(".mclorehub-catalogue-"))); }
    }
    @Test void invalidPackOrMissingReviewsCreateNoReport() throws Exception {
        Path pack = temp.resolve("bad.json"); Files.writeString(pack, "{}"); Path out = temp.resolve("out.json");
        assertEquals(1, run("--pack", pack.toString(), "--output", out.toString())); assertFalse(Files.exists(out));
        write(pack, candidate(temp, export()));
        assertEquals(1, run("--pack", pack.toString(), "--reviews", temp.resolve("missing.json").toString(), "--output", out.toString()));
        assertFalse(Files.exists(out));
    }
    @Test void malformedOrRepeatedOptionsAreUsageErrors() {
        assertEquals(2, run()); assertEquals(2, run("--pack", "a")); assertEquals(2, run("--pack", "a", "--pack", "b", "--output", "out"));
        assertEquals(2, run("--invented", "x", "--output", "out"));
    }
}
