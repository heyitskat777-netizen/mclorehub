package net.mclorehub;

import com.google.gson.*;
import net.mclorehub.importer.ExportImporter;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import static net.mclorehub.importer.JsonSupport.*;

/** Synthetic fixtures only: titles, quotes, and approval examples are not server lore. */
public final class TestData {
    public static JsonObject export() {
        JsonObject raw = new JsonObject();
        raw.addProperty("topic_id", "123"); raw.addProperty("title", "[✓] Synthetic entry — not server canon");
        raw.addProperty("url", "https://www.lordofthecraft.net/forums/topic/123-synthetic/");
        raw.addProperty("author", "Test author"); raw.addProperty("family", "magic"); raw.addProperty("category", "synthetic");
        raw.addProperty("content_text", "First paragraph.\n\nSecond paragraph: Æther, 薄明.\nNo invented rules.");
        rehash(raw); raw.add("scrape_error", JsonNull.INSTANCE); raw.add("future_nullable", JsonNull.INSTANCE);
        raw.addProperty("audit_role", "player-use candidate; requires source review");
        raw.addProperty("status_note", "Accepted at snapshot; not a current audit");
        raw.addProperty("verification", "Synthetic fixture");
        raw.add("tags", new JsonArray()); JsonArray aliases = new JsonArray(); aliases.add("Synthetic alias"); raw.add("search_aliases", aliases);
        JsonArray records = new JsonArray(); records.add(raw);
        JsonObject metadata = new JsonObject(); metadata.addProperty("record_count", 1);
        metadata.addProperty("generated_utc", "2026-09-20T08:22:55Z"); metadata.addProperty("updated_utc", "2026-09-21T02:12:38Z");
        JsonObject export = new JsonObject(); export.add("metadata", metadata); export.add("records", records);
        export.addProperty("audit", "Synthetic fixture, incomplete coverage"); return export;
    }
    public static JsonObject record(JsonObject export) { return export.getAsJsonArray("records").get(0).getAsJsonObject(); }
    public static void rehash(JsonObject raw) { raw.addProperty("content_sha256", hash(raw.get("content_text").getAsString())); }
    public static JsonObject post(String id) {
        JsonObject post = new JsonObject(); post.addProperty("post_id", id); post.addProperty("author", "");
        post.addProperty("content_text", "Discussion is not automatically a ruling");
        post.addProperty("url", "https://www.lordofthecraft.net/forums/topic/123-synthetic/#comment-" + id);
        JsonArray links = new JsonArray(); links.add("https://example.org/reference"); post.add("links", links);
        JsonArray images = new JsonArray(); images.add("https://example.org/image.png"); post.add("images", images); return post;
    }
    public static Path write(Path path, JsonElement value) throws IOException { Files.writeString(path, JSON.toJson(value)); return path; }
    public static JsonObject candidate(Path directory, JsonObject export) throws IOException {
        return new ExportImporter().importFile(write(directory.resolve("source.json"), export)).pack();
    }
    public static byte[] zip(Map<String, byte[]> entries) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            for (var entry : entries.entrySet()) {
                out.putNextEntry(new ZipEntry(entry.getKey())); out.write(entry.getValue()); out.closeEntry();
            }
        }
        return bytes.toByteArray();
    }
}
