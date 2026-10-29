package net.mclorehub.library;

import net.mclorehub.catalogue.LoreCatalogue.Document;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ReaderContentTest {
    @TempDir Path temp;
    Document doc;
    @BeforeEach void setup()throws Exception{doc=LibraryFixtures.catalogue(temp).documents().getLast();}
    Document withBody(String body){return new Document(doc.id(),doc.title(),doc.url(),doc.author(),doc.exportSnapshotAt(),body,doc.revision(),doc.fingerprint(),doc.family(),
            doc.category(),doc.aliases(),doc.role(),doc.status(),doc.reviewState(),doc.decision(),doc.posts(),doc.coverageGaps(),doc.trust());}
    @Test void chunkingPreservesEveryCharacterAndStableReplyBlocks(){String text="x".repeat(5999)+"😀\n\n"+"  paragraph\n".repeat(2000);var content=ReaderContent.document(withBody(text));
        assertEquals(text,content.sections().stream().filter(s->s.blockId().equals("body")).map(ReaderContent.Section::text).reduce("",String::concat));
        for(var s:content.sections()){assertTrue(s.text().length()<=6000);if(!s.text().isEmpty())assertFalse(Character.isHighSurrogate(s.text().charAt(s.text().length()-1)));}
        assertTrue(content.sections().stream().allMatch(s->s.blockId().equals("body")));var archive=ReaderContent.discussion(doc);
        assertTrue(archive.sections().getLast().heading().contains("Unknown author"));assertEquals("post:456",archive.sections().getLast().blockId());}
    @Test void oldDiscussionResumeResetsSafelyInMainReader(){var content=ReaderContent.document(doc);var p=new PersonalState.ReadingPosition(doc.id(),doc.fingerprint(),"post:456",10,true);
        assertEquals(new ReaderContent.Resume(0,0,true),content.resume(doc,Optional.of(p)));}
    @Test void selectsCorrectLongDocumentSection(){var content=ReaderContent.document(withBody("x".repeat(15000)));var p=new PersonalState.ReadingPosition(doc.id(),doc.fingerprint(),"body",6500,false);
        assertEquals(1,content.resume(doc,Optional.of(p)).section());assertEquals(6500,content.resume(doc,Optional.of(p)).offset());}
    @Test void changedSourceOrMissingBlockResetsSafely(){var content=ReaderContent.document(doc);
        for(var p:List.of(new PersonalState.ReadingPosition(doc.id(),"f".repeat(64),"body",20,false),new PersonalState.ReadingPosition(doc.id(),doc.fingerprint(),"post:999",10,true))){
            assertEquals(new ReaderContent.Resume(0,0,true),content.resume(doc,Optional.of(p)));}}
    @Test void absentOrDifferentDocumentStateStartsAtBeginning(){var content=ReaderContent.document(doc);assertEquals(new ReaderContent.Resume(0,0,false),content.resume(doc,Optional.empty()));
        assertEquals(new ReaderContent.Resume(0,0,false),content.resume(doc,Optional.of(new PersonalState.ReadingPosition("lotc:topic:999",doc.fingerprint(),"body",999,false))));}
    @Test void oversizedOffsetClampsToSourceEnd(){var content=ReaderContent.document(doc);assertEquals(doc.body().length(),content.resume(doc,Optional.of(
            new PersonalState.ReadingPosition(doc.id(),doc.fingerprint(),"body",100000,false))).offset());}
    @Test void detailsExposeReviewEvidenceAndFormattingGaps(){String text=ReaderContent.details(doc).sections().stream().map(ReaderContent.Section::text).reduce("",String::concat);
        assertTrue(text.contains("Synthetic test evidence only"));assertTrue(text.contains(doc.url()));assertTrue(text.contains(doc.exportSnapshotAt().toString()));
        assertTrue(text.contains("not automatically verified"));assertTrue(text.contains("original colors"));}
}
