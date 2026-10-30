package net.mclorehub.character;

import com.google.gson.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static net.mclorehub.importer.JsonSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class CharacterMigrationTest {
    @TempDir Path temp;
    CharacterBook book;CharacterStore store;Path file;
    @BeforeEach void setup(){book=CharacterBook.empty().create("Before — 薄明");file=temp.resolve("characters.json");store=new CharacterStore(file);}
    CharacterBook.HistoryEvent event(String id){return new CharacterBook.HistoryEvent(id,"Lost practice","lotc:topic:249533",CharacterBook.EventType.DISCONNECTED,"2026-10-01","Personal notes");}
    void writeLegacy()throws Exception{
        var root=new JsonObject();root.addProperty("schema_version",1);root.addProperty("selected_id",book.selectedId());var sheets=JSON.toJsonTree(book.sheets()).getAsJsonArray();
        for(var s:sheets){s.getAsJsonObject().remove("facts");s.getAsJsonObject().remove("history");}root.add("sheets",sheets);Files.writeString(file,JSON.toJson(root));
    }
    @Test void legacyReadDoesNotWriteOrInventFacts()throws Exception{
        writeLegacy();byte[] before=Files.readAllBytes(file);var loaded=store.load();assertEquals(book,loaded.book());assertEquals(CharacterFacts.unknown(),loaded.book().selected().orElseThrow().facts());assertTrue(loaded.book().selected().orElseThrow().history().isEmpty());assertArrayEquals(before,Files.readAllBytes(file));
    }
    @Test void firstEditMigratesAndPreservesExactLegacyBackup()throws Exception{
        writeLegacy();byte[] before=Files.readAllBytes(file);var loaded=store.load();var edited=loaded.book().facts(book.selectedId(),CharacterFacts.unknown().set(CharacterFacts.Field.SOUL,"LESSER"));store.save(edited,loaded.hash());
        assertEquals(2,read(file,1<<20).get("schema_version").getAsInt());assertArrayEquals(before,Files.readAllBytes(file.resolveSibling("characters.previous.json")));assertEquals(book,new CharacterStore(file.resolveSibling("characters.previous.json")).load().book());assertEquals(edited,store.load().book());
    }
    @Test void factsAndHistorySurviveAllOtherBookEdits()throws Exception{
        var e=event(UUID.randomUUID().toString());var facts=CharacterFacts.unknown().set(CharacterFacts.Field.KLONE,"YES");String id=book.selectedId();book=book.facts(id,facts).putHistory(id,e).rename(id,"After","Notes").archive(id,true).archive(id,false).select(id);
        var entry=new CharacterBook.Entry(UUID.randomUUID().toString(),CharacterBook.Kind.CA,"Test","","","",CharacterBook.RecordedStatus.PENDING,"","");book=book.putEntry(id,entry).removeEntry(id,entry.id());
        store.save(book,Optional.empty());var s=store.load().book().selected().orElseThrow();assertEquals(facts,s.facts());assertEquals(List.of(e),s.history());assertEquals("After",s.name());
    }
    @Test void historyUpdatesDoNotInferFactsAndReplaceById(){
        var e=event(UUID.randomUUID().toString());book=book.putHistory(book.selectedId(),e).putHistory(book.selectedId(),e);assertEquals(1,book.selected().orElseThrow().history().size());assertEquals(CharacterFacts.unknown(),book.selected().orElseThrow().facts());
    }
    @Test void deletingHistoryHasRecoverableBackup()throws Exception{
        var e=event(UUID.randomUUID().toString());book=book.putHistory(book.selectedId(),e);var saved=store.save(book,Optional.empty());store.save(book.removeHistory(book.selectedId(),e.id()),saved.hash());assertTrue(store.load().book().selected().orElseThrow().history().isEmpty());assertEquals(List.of(e),new CharacterStore(file.resolveSibling("characters.previous.json")).load().book().selected().orElseThrow().history());
    }
    @Test void duplicateEventIdsAcrossSheetsAndEntriesAreRejected(){
        var e=event(UUID.randomUUID().toString());book=book.putHistory(book.selectedId(),e);var two=book.create("Second");assertThrows(IllegalArgumentException.class,()->two.putHistory(two.selectedId(),e));
        assertThrows(IllegalArgumentException.class,()->book.putHistory(book.selectedId(),event(book.selectedId())));
    }
    @Test void invalidEnumsMissingAndUnknownVersionTwoFieldsRetainOriginal()throws Exception{
        store.save(book,Optional.empty());var good=read(file,1<<20);
        for(String variation:List.of("null","enum","missing","extra")){
            var bad=good.deepCopy();var facts=bad.getAsJsonArray("sheets").get(0).getAsJsonObject().getAsJsonObject("facts");
            switch(variation){case "null"->facts.add("soul",JsonNull.INSTANCE);case "enum"->facts.addProperty("soul","small");case "missing"->facts.remove("lungs");case "extra"->facts.addProperty("future",true);}
            Files.writeString(file,JSON.toJson(bad));byte[] before=Files.readAllBytes(file);assertThrows(java.io.IOException.class,store::load);assertArrayEquals(before,Files.readAllBytes(file));
        }
    }
    @Test void invalidDatesIdsAndUnknownFactsAreRejected(){
        for(String date:List.of("2026-02-30","2026-2-03","tomorrow"))assertThrows(IllegalArgumentException.class,()->new CharacterBook.HistoryEvent(UUID.randomUUID().toString(),"Test","",CharacterBook.EventType.OTHER,date,""));
        assertThrows(IllegalArgumentException.class,()->CharacterFacts.unknown().set(CharacterFacts.Field.SOUL,"INVALID"));assertThrows(IllegalArgumentException.class,()->new CharacterBook.HistoryEvent("invalid","Test","",CharacterBook.EventType.OTHER,"",""));
    }
    @Test void completeFactsAndUnlinkedUnicodeHistoryRoundTrip()throws Exception{
        var f=new CharacterFacts(CharacterFacts.Soul.GREATER,CharacterFacts.Truth.YES,CharacterFacts.Truth.NO,CharacterFacts.Truth.YES,CharacterFacts.Truth.NO,CharacterFacts.Truth.YES,CharacterFacts.Truth.NO,CharacterFacts.Truth.YES,CharacterFacts.Severances.THREE_OR_MORE);
        var h=new CharacterBook.HistoryEvent(UUID.randomUUID().toString(),"Unlinked 薄明","",CharacterBook.EventType.OTHER,"","No sourced inference");book=book.facts(book.selectedId(),f).putHistory(book.selectedId(),h);store.save(book,Optional.empty());assertEquals(book,store.load().book());assertThrows(UnsupportedOperationException.class,()->book.selected().orElseThrow().history().clear());
    }
}
