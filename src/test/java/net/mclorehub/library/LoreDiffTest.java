package net.mclorehub.library;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import net.mclorehub.catalogue.LoreCatalogue.Document;
import static org.junit.jupiter.api.Assertions.*;

class LoreDiffTest {
    @TempDir Path temp;
    Document body(Document d,String body){return new Document(d.id(),d.title(),d.url(),d.author(),d.exportSnapshotAt(),body,d.revision(),d.fingerprint(),d.family(),d.category(),d.aliases(),d.role(),d.status(),d.reviewState(),d.decision(),d.posts(),d.coverageGaps(),d.trust());}
    @Test void showsMainAddedRemovedAndUnicodeWithoutReplies()throws Exception {var d=LibraryFixtures.catalogue(temp).documents().getFirst();var r=LoreDiff.compare(body(d,"a\nold 😀\nz"),body(d,"a\nnew 薄明\nz"));assertTrue(r.text().contains("- old 😀"));assertTrue(r.text().contains("+ new 薄明"));assertEquals(1,r.added());assertEquals(1,r.removed());}
    @Test void identicalMainReportsNoChanges()throws Exception {var d=LibraryFixtures.catalogue(temp).documents().getFirst();assertTrue(LoreDiff.compare(d,d).text().contains("No main lore changes"));}
    @Test void hugeDiffHasBoundedOutputAndExplicitAbbreviation()throws Exception {var d=LibraryFixtures.catalogue(temp).documents().getFirst();var r=LoreDiff.compare(body(d,"old\n".repeat(10000)),body(d,"new\n".repeat(10000)));assertTrue(r.abbreviated());assertTrue(r.text().length()<49000);}
    @Test void oneGiantUnicodeLineIsBoundedAndRetainsValidSurrogates()throws Exception {var d=LibraryFixtures.catalogue(temp).documents().getFirst();var r=LoreDiff.compare(body(d,"😀".repeat(10000)),body(d,"薄明".repeat(10000)));assertTrue(r.abbreviated());assertTrue(r.text().length()<12000);for(int i=0;i<r.text().length();i++)if(Character.isHighSurrogate(r.text().charAt(i)))assertTrue(i+1<r.text().length()&&Character.isLowSurrogate(r.text().charAt(++i)));}
    @Test void fullVersionsRemainAvailableAfterAbbreviatedDiff()throws Exception{var d=LibraryFixtures.catalogue(temp).documents().getFirst();String before="old\n".repeat(10000),after="new\n".repeat(10000);var r=ReaderContent.comparison(body(d,before),body(d,after));assertEquals(before,r.sections().stream().filter(s->s.blockId().equals("previous")).map(ReaderContent.Section::text).collect(java.util.stream.Collectors.joining()));assertEquals(after,r.sections().stream().filter(s->s.blockId().equals("current")).map(ReaderContent.Section::text).collect(java.util.stream.Collectors.joining()));assertTrue(r.sections().stream().allMatch(s->s.text().length()<=6000));}
}
