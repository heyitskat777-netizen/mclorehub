package net.mclorehub.importer;

import net.mclorehub.catalogue.CatalogueCommand;
import net.mclorehub.updates.UpdateCommand;
import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import static net.mclorehub.importer.JsonSupport.*;

public final class ImportCommand {
    private ImportCommand() {}
    public static void main(String[] args) { System.exit(run(args, System.out, System.err)); }
    public static int run(String[] args, PrintStream out, PrintStream err) {
        if(args.length>0&&java.util.Set.of("trust","refresh","undo").contains(args[0]))return UpdateCommand.run(args[0],Arrays.copyOfRange(args,1,args.length),out,err);
        if (args.length > 0 && args[0].equals("catalogue"))
            return CatalogueCommand.run(Arrays.copyOfRange(args, 1, args.length), out, err);
        if (args.length == 1 && args[0].equals("--help")) {
            out.println("Usage: mclorehub-importer --input <export.zip|export.json> --output <new-directory>");
            out.println("       mclorehub-importer catalogue --pack <pack.json> [--reviews <reviews.json>] --output <new-report.json>");
            out.println("       mclorehub-importer trust --pack <pack.json> --output <new-trust.json>");
            out.println("       mclorehub-importer refresh|undo --library <config/mclorehub> --document <lotc:topic:id>");
            return 0;
        }
        if (args.length != 4 || !args[0].equals("--input") || !args[2].equals("--output")) {
            err.println("Usage: --input <export.zip|export.json> --output <new-directory>"); return 2;
        }
        Path staging = null;
        try {
            Path target = Path.of(args[3]).toAbsolutePath().normalize();
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Output already exists");
            ExportImporter.Result result = new ExportImporter().importFile(Path.of(args[1]));
            Files.createDirectories(target.getParent()); staging = Files.createTempDirectory(target.getParent(), ".mclorehub-import-");
            writeNew(staging.resolve("pack.json"), result.pack()); writeNew(staging.resolve("report.json"), result.report());
            Files.move(staging, target); staging = null;
            out.println(JSON.toJson(result.report())); out.println("Candidate written; not activated. Install matching user-confirmation trust or explicit reviews."); return 0;
        } catch (IOException | IllegalArgumentException e) { err.println("Import failed: " + e.getMessage()); return 1; }
        finally {
            if (staging != null) {
                try { Files.deleteIfExists(staging.resolve("pack.json")); Files.deleteIfExists(staging.resolve("report.json")); Files.deleteIfExists(staging); }
                catch (IOException e) { err.println("Could not remove staging directory: " + staging); }
            }
        }
    }
}
