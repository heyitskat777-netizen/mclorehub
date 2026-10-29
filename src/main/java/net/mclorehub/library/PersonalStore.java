package net.mclorehub.library;

import com.google.gson.*;
import net.mclorehub.importer.ImportException;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import static net.mclorehub.importer.JsonSupport.*;

/** Atomic personal-state writes with previous-good backup and external-edit detection. */
public final class PersonalStore {
    public record Loaded(PersonalState state, Optional<String> fileHash) {}
    private final Path file;
    public PersonalStore(Path file) { this.file = file; }
    public Loaded load() throws IOException {
        byte[] bytes = existing();
        if (bytes == null) return new Loaded(PersonalState.empty(), Optional.empty());
        JsonObject root = parse(bytes);
        if (integer(root, "schema_version") != 1) throw new ImportException("Unsupported personal-state schema; original retained");
        if(!Set.of("schema_version","favorites","reading").containsAll(root.keySet()))
            throw new ImportException("Unknown personal-state fields; original retained");
        try {
            List<String> ids = strings(root, "favorites");
            if (new HashSet<>(ids).size() != ids.size()) throw new ImportException("Duplicate favorite");
            Optional<PersonalState.ReadingPosition> reading = Optional.empty();
            if (root.has("reading") && !root.get("reading").isJsonNull()) {
                JsonObject r = object(root.get("reading"), "reading"); JsonElement audit = r.get("audit");
                if(!Set.of("document_id","fingerprint","block_id","offset","audit").containsAll(r.keySet()))
                    throw new ImportException("Unknown reading-state fields; original retained");
                if (audit == null || !audit.isJsonPrimitive() || !audit.getAsJsonPrimitive().isBoolean())
                    throw new ImportException("Reading audit scope must be boolean");
                reading = Optional.of(new PersonalState.ReadingPosition(text(r,"document_id"), sha(r,"fingerprint"),
                        text(r,"block_id"), integer(r,"offset"), audit.getAsBoolean()));
            }
            return new Loaded(new PersonalState(new HashSet<>(ids), reading), Optional.of(hash(bytes)));
        } catch (IllegalArgumentException e) { throw new ImportException("Invalid personal state; original retained", e); }
    }
    private byte[] existing() throws IOException {
        if (Files.isSymbolicLink(file)) throw new ImportException("Personal state must not be a symbolic link");
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) return null;
        try (var input = Files.newInputStream(file)) { return bounded(input, 1 << 20); }
    }
    public Loaded save(PersonalState state, Optional<String> expectedHash) throws IOException {
        byte[] before = existing();
        if (!Objects.equals(expectedHash, before == null ? Optional.empty() : Optional.of(hash(before))))
            throw new ImportException("Personal state changed outside the mod; reload before saving");
        JsonObject json = new JsonObject(); json.addProperty("schema_version",1);
        json.add("favorites", JSON.toJsonTree(state.favorites())); json.add("reading", JsonNull.INSTANCE);
        state.reading().ifPresent(p -> {
            JsonObject r = new JsonObject(); r.addProperty("document_id",p.documentId()); r.addProperty("fingerprint",p.fingerprint());
            r.addProperty("block_id",p.blockId()); r.addProperty("offset",p.offset()); r.addProperty("audit",p.audit()); json.add("reading",r);
        });
        Files.createDirectories(file.getParent()); Path staged = Files.createTempFile(file.getParent(), ".state-", ".tmp");
        Path backup = null;
        try {
            String serialized=JSON.toJson(json) + "\n";Files.writeString(staged,serialized);
            try(var channel=java.nio.channels.FileChannel.open(staged,StandardOpenOption.WRITE)){channel.force(true);}
            if (before != null) {
                backup = Files.createTempFile(file.getParent(), ".previous-", ".tmp"); Files.write(backup,before);
                Files.move(backup, file.resolveSibling("state.previous.json"), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            // Fail rather than silently fall back to a non-atomic replace.
            Files.move(staged,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            return new Loaded(state,Optional.of(hash(serialized)));
        } finally { Files.deleteIfExists(staged); if (backup != null) Files.deleteIfExists(backup); }
    }
}
