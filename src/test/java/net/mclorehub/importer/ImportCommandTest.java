package net.mclorehub.importer;

import com.google.gson.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import static net.mclorehub.TestData.*;
import static net.mclorehub.importer.JsonSupport.*;

class ImportCommandTest {
    @TempDir Path temp;
    private int run(String... args) { return ImportCommand.run(args, new PrintStream(new ByteArrayOutputStream()), new PrintStream(new ByteArrayOutputStream())); }
    @Test void serializesCompleteCandidateIncludingExplicitNulls() throws Exception {
        JsonObject source = export(); Path input = write(temp.resolve("source.json"), source); Path out = temp.resolve("candidate");
        byte[] before = Files.readAllBytes(input);
        assertEquals(0, run("--input", input.toString(), "--output", out.toString()));
        JsonObject pack = read(out.resolve("pack.json"), 1 << 20);
        assertEquals(record(source), pack.getAsJsonArray("documents").get(0).getAsJsonObject().get("source_record"));
        assertTrue(Files.exists(out.resolve("report.json"))); assertArrayEquals(before, Files.readAllBytes(input));
        try (var paths = Files.list(temp)) { assertFalse(paths.anyMatch(p -> p.getFileName().toString().startsWith(".mclorehub-import-"))); }
    }
    @Test void preservesExistingOutputAndPersonalData() throws Exception {
        Path input = write(temp.resolve("source.json"), export()); Path out = Files.createDirectory(temp.resolve("candidate"));
        Files.writeString(out.resolve("characters.json"), "personal sentinel");
        assertEquals(1, run("--input", input.toString(), "--output", out.toString()));
        assertEquals("personal sentinel", Files.readString(out.resolve("characters.json"))); assertFalse(Files.exists(out.resolve("pack.json")));
    }
    @Test void failedValidationCreatesNoOutput() throws Exception {
        Path input = temp.resolve("invalid.json"); Files.writeString(input, "{}"); Path out = temp.resolve("candidate");
        assertEquals(1, run("--input", input.toString(), "--output", out.toString())); assertFalse(Files.exists(out));
    }
    @Test void refusesInputAsOutputAndDanglingSymlinks() throws Exception {
        Path input = write(temp.resolve("source.json"), export()); byte[] before = Files.readAllBytes(input);
        assertEquals(1, run("--input", input.toString(), "--output", input.toString())); assertArrayEquals(before, Files.readAllBytes(input));
        Path out = temp.resolve("candidate"); Files.createSymbolicLink(out, temp.resolve("missing"));
        assertEquals(1, run("--input", input.toString(), "--output", out.toString())); assertTrue(Files.isSymbolicLink(out));
    }
    @Test void helpAndUsageExitCodesDiffer() { assertEquals(0, run("--help")); assertEquals(2, run()); assertEquals(2, run("--input", "missing")); }
}
