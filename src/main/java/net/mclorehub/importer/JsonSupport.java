package net.mclorehub.importer;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.*;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Shared strict data handling. Source text and unknown fields are never executed. */
public final class JsonSupport {
    public static final Gson JSON = new GsonBuilder().setPrettyPrinting().serializeNulls()
            .disableHtmlEscaping().create();
    private JsonSupport() {}

    public static JsonObject read(Path path, int limit) throws IOException {
        try (InputStream stream = Files.newInputStream(path)) { return parse(bounded(stream, limit)); }
    }
    public static byte[] bounded(InputStream stream, int limit) throws IOException {
        byte[] bytes = stream.readNBytes(limit + 1);
        if (bytes.length > limit) throw new ImportException("Input exceeds size limit");
        return bytes;
    }
    public static JsonObject parse(byte[] bytes) throws ImportException {
        try {
            String text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            try (JsonReader reader = new JsonReader(new StringReader(text))) {
                reader.setStrictness(Strictness.STRICT);
                JsonElement value = readValue(reader, 0);
                if (!value.isJsonObject() || reader.peek() != JsonToken.END_DOCUMENT)
                    throw new ImportException("Expected exactly one JSON object");
                return value.getAsJsonObject();
            }
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            if (e instanceof ImportException invalid) throw invalid;
            throw new ImportException("Invalid UTF-8/JSON: " + e.getMessage(), e);
        }
    }
    private static JsonElement readValue(JsonReader reader, int depth) throws IOException {
        if (depth > 64) throw new ImportException("JSON nesting exceeds 64 levels");
        return switch (reader.peek()) {
            case BEGIN_OBJECT -> {
                JsonObject object = new JsonObject(); reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (object.has(name)) throw new ImportException("Duplicate JSON key at " + reader.getPath());
                    object.add(name, readValue(reader, depth + 1));
                }
                reader.endObject(); yield object;
            }
            case BEGIN_ARRAY -> {
                JsonArray array = new JsonArray(); reader.beginArray();
                while (reader.hasNext()) array.add(readValue(reader, depth + 1));
                reader.endArray(); yield array;
            }
            case STRING -> new JsonPrimitive(reader.nextString());
            case NUMBER -> new JsonPrimitive(new BigDecimal(reader.nextString()));
            case BOOLEAN -> new JsonPrimitive(reader.nextBoolean());
            case NULL -> { reader.nextNull(); yield JsonNull.INSTANCE; }
            default -> throw new ImportException("Unexpected JSON token at " + reader.getPath());
        };
    }
    public static JsonObject object(JsonElement value, String field) throws ImportException {
        if (value == null || !value.isJsonObject()) throw new ImportException(field + " must be an object");
        return value.getAsJsonObject();
    }
    public static JsonArray array(JsonElement value, String field) throws ImportException {
        if (value == null || !value.isJsonArray()) throw new ImportException(field + " must be an array");
        return value.getAsJsonArray();
    }
    public static String string(JsonObject object, String key) throws ImportException {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
            throw new ImportException(key + " must be a string");
        return value.getAsString();
    }
    public static String text(JsonObject object, String key) throws ImportException {
        String value = string(object, key);
        if (value.isBlank()) throw new ImportException(key + " must not be blank");
        return value;
    }
    public static String optional(JsonObject object, String key) throws ImportException {
        return !object.has(key) || object.get(key).isJsonNull() ? null : string(object, key);
    }
    public static int integer(JsonObject object, String key) throws ImportException {
        try {
            JsonElement value = object.get(key);
            if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
                throw new ImportException(key + " must be an integer");
            return value.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException | NumberFormatException e) { throw new ImportException(key + " must be an integer", e); }
    }
    public static List<String> strings(JsonObject object, String key) throws ImportException {
        List<String> result = new ArrayList<>();
        for (JsonElement value : array(object.get(key), key)) {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
                throw new ImportException(key + " must contain strings");
            result.add(value.getAsString());
        }
        return List.copyOf(result);
    }
    public static String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    public static String hash(String text) { return hash(text.getBytes(StandardCharsets.UTF_8)); }
    public static String sha(JsonObject object, String key) throws ImportException {
        String value = text(object, key);
        if (!value.matches("[a-f0-9]{64}")) throw new ImportException(key + " must be SHA-256");
        return value;
    }
    public static String identifier(JsonObject object, String key) throws ImportException {
        String value = text(object, key);
        if (!value.matches("[1-9][0-9]{0,17}")) throw new ImportException("Invalid " + key);
        return value;
    }
    public static void forumUrl(String value) throws ImportException {
        try {
            URI uri = URI.create(value);
            if (!"https".equals(uri.getScheme()) || uri.getUserInfo() != null || uri.getPort() != -1
                    || !("www.lordofthecraft.net".equals(uri.getHost()) || "lordofthecraft.net".equals(uri.getHost()))
                    || uri.getPath() == null || !uri.getPath().startsWith("/forums/topic/"))
                throw new ImportException("Expected an official HTTPS forum topic URL");
        } catch (IllegalArgumentException e) { throw new ImportException("Invalid forum URL", e); }
    }
    /** Stable across object-key ordering, retaining array order and explicit nulls. */
    public static String fingerprint(JsonElement value) { return hash(JSON.toJson(sorted(value))); }
    private static JsonElement sorted(JsonElement value) {
        if (value.isJsonObject()) {
            JsonObject result = new JsonObject();
            new TreeSet<>(value.getAsJsonObject().keySet()).forEach(key -> result.add(key, sorted(value.getAsJsonObject().get(key))));
            return result;
        }
        if (value.isJsonArray()) {
            JsonArray result = new JsonArray(); value.getAsJsonArray().forEach(item -> result.add(sorted(item))); return result;
        }
        return value.deepCopy();
    }
    public static void writeNew(Path path, JsonElement value) throws IOException {
        // CREATE_NEW also refuses symlinks and existing files; failure never overwrites user data.
        try (Writer out = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            JSON.toJson(value, out); out.write('\n');
        }
    }
}
