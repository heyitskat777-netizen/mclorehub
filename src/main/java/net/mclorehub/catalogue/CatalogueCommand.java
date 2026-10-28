package net.mclorehub.catalogue;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import static net.mclorehub.importer.JsonSupport.*;

/** Developer review report; never writes an active pack or the supplied reviews. */
public final class CatalogueCommand {
    private CatalogueCommand() {}
    public static int run(String[] args, PrintStream out, PrintStream err) {
        Map<String, String> options = new LinkedHashMap<>();
        if (args.length % 2 != 0) { err.println("Expected --pack <path> [--reviews <path>] --output <new-file>"); return 2; }
        for (int i = 0; i < args.length; i += 2) {
            if (!Set.of("--pack", "--reviews", "--output").contains(args[i]) || options.putIfAbsent(args[i], args[i + 1]) != null) {
                err.println("Unknown or repeated option"); return 2;
            }
        }
        if (!options.containsKey("--pack") || !options.containsKey("--output")) { err.println("--pack and --output are required"); return 2; }
        Path staging = null;
        try {
            Path output = Path.of(options.get("--output")).toAbsolutePath().normalize();
            if (Files.exists(output, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Output already exists");
            LoreCatalogue catalogue = LoreCatalogue.load(Path.of(options.get("--pack")),
                    options.containsKey("--reviews") ? Path.of(options.get("--reviews")) : null);
            staging = Files.createTempDirectory(output.getParent(), ".mclorehub-catalogue-");
            Path stagedReport = staging.resolve("report.json");
            writeNew(stagedReport, catalogue.report());
            Files.move(stagedReport, output);
            out.println("Catalogue: " + catalogue.documents().size() + " documents; " + catalogue.currentDocuments().size()
                    + " reviewed current; " + catalogue.reviewQueue().size() + " awaiting review");
            return 0;
        } catch (IOException | IllegalArgumentException e) { err.println("Catalogue failed: " + e.getMessage()); return 1; }
        finally {
            if (staging != null) {
                try { Files.deleteIfExists(staging.resolve("report.json")); Files.deleteIfExists(staging); }
                catch (IOException e) { err.println("Could not remove report staging directory: " + staging); }
            }
        }
    }
}
