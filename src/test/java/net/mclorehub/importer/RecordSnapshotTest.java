package net.mclorehub.importer;

import net.mclorehub.catalogue.LoreCatalogue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static net.mclorehub.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

class RecordSnapshotTest {
    @TempDir Path temp;
    @Test void retainedRestrictedSourceKeepsItsActualOldSnapshot()throws Exception{
        var source=export();record(source).addProperty("source_snapshot_at","2026-09-20T00:00:00Z");var pack=write(temp.resolve("pack.json"),candidate(temp,source));assertEquals("2026-09-20T00:00:00Z",LoreCatalogue.load(pack,null).documents().getFirst().exportSnapshotAt().toString());
    }
    @Test void recordCannotClaimCaptureNewerThanItsExport(){var source=export();record(source).addProperty("source_snapshot_at","2026-10-01T00:00:00Z");assertThrows(java.io.IOException.class,()->candidate(temp,source));}
    @Test void retainedRestrictedWarningIsVisibleUntilAnActualMainPostCheck()throws Exception{
        var source=export();var notes=new com.google.gson.JsonArray();notes.add("retained_restricted_snapshot_not_reverified");record(source).add("coverage_notes",notes);var doc=LoreCatalogue.load(write(temp.resolve("pack.json"),candidate(temp,source)),null).documents().getFirst();assertTrue(doc.coverageGaps().contains("retained_restricted_snapshot_not_reverified"));
        assertTrue(net.mclorehub.library.ReaderContent.details(doc).sections().getFirst().text().contains("retained restricted snapshot not reverified"));
        var refreshed=new net.mclorehub.updates.TopicSnapshot(doc.id(),doc.url(),doc.title(),"Author",doc.body(),doc.posts(),java.util.List.of(doc.url()),java.time.Instant.now(),java.util.Optional.empty(),java.util.List.of()).apply(doc);assertFalse(refreshed.coverageGaps().contains("retained_restricted_snapshot_not_reverified"));
    }
}
