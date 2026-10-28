package net.mclorehub.importer;

import com.google.gson.*;
import net.mclorehub.TestData;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static net.mclorehub.TestData.*;
import static net.mclorehub.importer.JsonSupport.*;

class ExportImporterTest {
    @TempDir Path temp;
    @Test void preservesSourceNullsUnicodeAndProvenanceWithoutApproving() throws Exception {
        JsonObject source = export(); JsonObject pack = candidate(temp, source);
        JsonObject doc = pack.getAsJsonArray("documents").get(0).getAsJsonObject();
        assertEquals(record(source), doc.get("source_record"));
        assertEquals("lotc:topic:123", doc.get("id").getAsString());
        assertEquals(record(source).get("content_text"), doc.getAsJsonArray("blocks").get(0).getAsJsonObject().get("text"));
        assertEquals("unknown", doc.getAsJsonObject("status").get("value").getAsString());
        assertEquals("partial", pack.get("snapshot_scope").getAsString());
        assertEquals(source.get("audit"), pack.getAsJsonObject("source").getAsJsonObject("export_provenance").get("audit"));
    }
    @Test void retainsUnattributedPostsAndMediaLinksAndFlagsTheGap() throws Exception {
        JsonObject source = export(); JsonArray posts = new JsonArray(); posts.add(post("456"));
        record(source).add("thread_posts", posts); record(source).addProperty("artifact_type", "MArt");
        JsonObject pack = candidate(temp, source); JsonObject doc = pack.getAsJsonArray("documents").get(0).getAsJsonObject();
        assertEquals(posts, doc.getAsJsonObject("source_record").get("thread_posts"));
        assertTrue(doc.getAsJsonArray("coverage_gaps").asList().stream().anyMatch(v -> v.getAsString().equals("unattributed_thread_post:456")));
    }
    @Test void zipAndJsonProduceStableLogicalRevisionsWithoutChangingInput() throws Exception {
        Path input = write(temp.resolve("source.json"), export()); byte[] before = Files.readAllBytes(input);
        Path archive = temp.resolve("source.zip"); Files.write(archive, zip(Map.of("lotc_lore.json", before)));
        var importer = new ExportImporter(); var a = importer.importFile(input); var b = importer.importFile(input); var c = importer.importFile(archive);
        assertEquals(a.pack(), b.pack()); assertEquals(a.pack().get("revision_id"), c.pack().get("revision_id"));
        assertEquals(a.pack().get("documents"), c.pack().get("documents")); assertArrayEquals(before, Files.readAllBytes(input));
    }
    static Stream<Consumer<JsonObject>> invalid() {
        return Stream.of(
                e -> record(e).remove("title"), e -> record(e).addProperty("title", " "),
                e -> record(e).addProperty("topic_id", "../123"), e -> record(e).addProperty("topic_id", "0123"),
                e -> record(e).addProperty("topic_id", 123), e -> record(e).addProperty("content_sha256", "0".repeat(64)),
                e -> record(e).addProperty("content_text", "Changed without a new hash"),
                e -> record(e).addProperty("scrape_error", "Incomplete retrieval"),
                e -> record(e).addProperty("url", "http://www.lordofthecraft.net/forums/topic/123/"),
                e -> record(e).addProperty("url", "https://evil.example/forums/topic/123/"),
                e -> record(e).addProperty("url", "https://user@www.lordofthecraft.net/forums/topic/123/"),
                e -> record(e).addProperty("url", "https://www.lordofthecraft.net:8080/forums/topic/123/"),
                e -> record(e).addProperty("tags", "not an array"), e -> record(e).getAsJsonArray("search_aliases").add(42),
                e -> e.getAsJsonObject("metadata").addProperty("record_count", 2),
                e -> e.getAsJsonObject("metadata").addProperty("record_count", 1.5),
                e -> e.getAsJsonObject("metadata").addProperty("record_count", "1"),
                e -> e.getAsJsonObject("metadata").addProperty("updated_utc", "2026-09-21"),
                e -> e.getAsJsonArray("records").remove(0));
    }
    @ParameterizedTest @MethodSource("invalid") void rejectsInvalidExports(Consumer<JsonObject> mutate) throws Exception {
        JsonObject e = export(); mutate.accept(e); Path input = write(temp.resolve("source.json"), e);
        assertThrows(ImportException.class, () -> new ExportImporter().importFile(input));
    }
    @Test void rejectsDuplicateTopicIds() throws Exception {
        JsonObject e = export(); e.getAsJsonArray("records").add(record(e).deepCopy()); e.getAsJsonObject("metadata").addProperty("record_count", 2);
        Path input = write(temp.resolve("source.json"), e); assertThrows(ImportException.class, () -> new ExportImporter().importFile(input));
    }
    @Test void rejectsDuplicatePostIds() throws Exception {
        JsonObject e = export(); JsonArray posts = new JsonArray(); posts.add(post("456")); posts.add(post("456")); record(e).add("thread_posts", posts);
        Path input = write(temp.resolve("source.json"), e); assertThrows(ImportException.class, () -> new ExportImporter().importFile(input));
    }
    @ParameterizedTest @ValueSource(strings={"../outside", "/absolute", "C:/windows", "dir/../../outside", "dir\\outside"})
    void rejectsUnsafeZipEntriesEvenIfUnused(String name) throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>(); entries.put("lotc_lore.json", JSON.toJson(export()).getBytes(StandardCharsets.UTF_8)); entries.put(name, new byte[0]);
        Path input = temp.resolve("unsafe.zip"); Files.write(input, zip(entries)); assertThrows(ImportException.class, () -> new ExportImporter().importFile(input));
    }
    @Test void rejectsMissingRootJson() throws Exception {
        Path input = temp.resolve("missing.zip"); Files.write(input, zip(Map.of("nested/lotc_lore.json", new byte[0])));
        assertThrows(ImportException.class, () -> new ExportImporter().importFile(input));
    }
    @Test void limitsCompressedAndExpandedInput() throws Exception {
        Path input = temp.resolve("oversize.zip"); Files.write(input, zip(Map.of("lotc_lore.json", new byte[4096])));
        assertThrows(ImportException.class, () -> new ExportImporter(new ExportImporter.Limits(8192, 128, 8192, 4, 10)).importFile(input));
        assertThrows(ImportException.class, () -> new ExportImporter(new ExportImporter.Limits(1, 8192, 8192, 4, 10)).importFile(input));
    }
    @Test void limitsTotalExpandedSizeAndEntryCount() throws Exception {
        Path input = temp.resolve("budget.zip"); Files.write(input, zip(Map.of("a", new byte[80], "b", new byte[80])));
        assertThrows(ImportException.class, () -> new ExportImporter(new ExportImporter.Limits(8192, 100, 100, 4, 10)).importFile(input));
        assertThrows(ImportException.class, () -> new ExportImporter(new ExportImporter.Limits(8192, 100, 1000, 1, 10)).importFile(input));
    }
    @ParameterizedTest @ValueSource(strings={"{\"x\":1,\"x\":2}", "{} {}", "{unquoted:1}", "[1]", "{\"n\":NaN}"})
    void rejectsMalformedOrAmbiguousJson(String value) {
        assertThrows(ImportException.class, () -> parse(value.getBytes(StandardCharsets.UTF_8)));
    }
    @Test void rejectsInvalidUtf8AndDeepNesting() {
        assertThrows(ImportException.class, () -> parse(new byte[]{'{','"',(byte)0xc3,'"',':','1','}'}));
        assertThrows(ImportException.class, () -> parse(("[".repeat(1000)+"0"+"]".repeat(1000)).getBytes(StandardCharsets.UTF_8)));
    }
}
