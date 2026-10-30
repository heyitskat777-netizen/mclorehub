package net.mclorehub.character;

import net.mclorehub.library.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class CharacterStoreTest {
    @TempDir Path temp;Path file;CharacterStore store;
    @BeforeEach void setup(){file=temp.resolve("personal/characters.json");store=new CharacterStore(file);}
    CharacterBook.Entry entry(CharacterBook.Kind kind){return new CharacterBook.Entry(UUID.randomUUID().toString(),kind,"Synthetic "+kind,
            kind==CharacterBook.Kind.MAGIC?"branch":"",kind==CharacterBook.Kind.MAGIC?"T2":"","https://www.lordofthecraft.net/forums/topic/123-synthetic/",
            CharacterBook.RecordedStatus.APPROVED,"Recorded by user, not actual approval","lotc:topic:123");}
    @Test void blankBookDoesNotCreateFiles()throws Exception{assertEquals(CharacterBook.empty(),store.load().book());assertFalse(Files.exists(file));}
    @Test void roundTripMultipleCharactersAllEntryTypesAndLinks()throws Exception{var book=CharacterBook.empty().create("First — 薄明");String first=book.selectedId();
        for(var kind:CharacterBook.Kind.values())book=book.putEntry(first,entry(kind));book=book.create("Second");store.save(book,Optional.empty());
        assertEquals(book,store.load().book());assertEquals(3,store.load().book().sheets().getFirst().entries().size());}
    @Test void archiveAndRestoreRetainEntriesAndClearSelection(){var book=CharacterBook.empty().create("Test");String id=book.selectedId();book=book.putEntry(id,entry(CharacterBook.Kind.MAGIC));
        var archived=book.archive(id,true);assertEquals("",archived.selectedId());assertEquals(1,archived.sheets().getFirst().entries().size());
        assertFalse(archived.archive(id,false).select(id).selected().orElseThrow().archived());}
    @Test void renamePreservesStableIdsAndOtherSheets(){var first=CharacterBook.empty().create("One");String id=first.selectedId();var two=first.create("Two");
        var renamed=two.rename(id,"Renamed","Notes");assertEquals(id,renamed.sheets().getFirst().id());assertEquals("Two",renamed.selected().orElseThrow().name());}
    @Test void entryEditsReplaceByStableIdWithoutDuplicating(){var book=CharacterBook.empty().create("One");var e=entry(CharacterBook.Kind.CA);String id=book.selectedId();
        book=book.putEntry(id,e).putEntry(id,e);assertEquals(1,book.selected().orElseThrow().entries().size());assertTrue(book.removeEntry(id,e.id()).selected().orElseThrow().entries().isEmpty());}
    @Test void deletedEntryRecoverableFromPreviousFile()throws Exception{var book=CharacterBook.empty().create("Test");var e=entry(CharacterBook.Kind.FA);book=book.putEntry(book.selectedId(),e);
        var first=store.save(book,Optional.empty());store.save(book.removeEntry(book.selectedId(),e.id()),first.hash());
        assertEquals(1,new CharacterStore(file.resolveSibling("characters.previous.json")).load().book().selected().orElseThrow().entries().size());}
    @Test void corruptFutureUnknownAndDuplicateFieldsAreNeverReset()throws Exception{Files.createDirectories(file.getParent());
        for(String invalid:List.of("bad json","{\"schema_version\":4,\"selected_id\":\"\",\"sheets\":[]}","{\"schema_version\":1,\"selected_id\":\"\",\"sheets\":[],\"future\":true}",
                "{\"schema_version\":1,\"schema_version\":1,\"selected_id\":\"\",\"sheets\":[]}")){
            Files.writeString(file,invalid);assertThrows(java.io.IOException.class,store::load);assertEquals(invalid,Files.readString(file));}}
    @Test void externalEditsAreNotOverwritten()throws Exception{var loaded=store.save(CharacterBook.empty(),Optional.empty());Files.writeString(file,"external");
        assertThrows(java.io.IOException.class,()->store.save(loaded.book().create("Test"),loaded.hash()));assertEquals("external",Files.readString(file));}
    @Test void unsafeLinksApplicationTierAndIdsAreRejected(){assertThrows(IllegalArgumentException.class,()->new CharacterBook.Entry(UUID.randomUUID().toString(),CharacterBook.Kind.CA,"Test","branch","", "",CharacterBook.RecordedStatus.NOT_RECORDED,"",""));
        assertThrows(IllegalArgumentException.class,()->new CharacterBook.Entry(UUID.randomUUID().toString(),CharacterBook.Kind.FA,"Test","","","https://example.com/",CharacterBook.RecordedStatus.APPROVED,"",""));
        assertThrows(IllegalArgumentException.class,()->CharacterBook.empty().select("missing"));assertThrows(IllegalArgumentException.class,()->CharacterBook.empty().create(" "));}
    @Test void duplicateIdsAndMutatingCollectionsAreRejected(){var book=CharacterBook.empty().create("Test");var sheet=book.sheets().getFirst();
        assertThrows(IllegalArgumentException.class,()->new CharacterBook(List.of(sheet,sheet),""));assertThrows(UnsupportedOperationException.class,()->book.sheets().clear());}
    @Test void servicePersistsSelectionAndInvalidDataIsReadOnly()throws Exception{try(var service=new CharacterService(temp)){
        service.reload().get(5,TimeUnit.SECONDS);var saved=service.edit(b->b.create("First")).get(5,TimeUnit.SECONDS);assertEquals("First",saved.book().selected().orElseThrow().name());}
        try(var restarted=new CharacterService(temp)){assertEquals("First",restarted.reload().get(5,TimeUnit.SECONDS).book().selected().orElseThrow().name());Files.writeString(file,"broken");
            assertFalse(restarted.reload().get(5,TimeUnit.SECONDS).writable());restarted.edit(b->b.create("Lost")).get(5,TimeUnit.SECONDS);assertEquals("broken",Files.readString(file));}}
    @Test void charactersAndFavoritesUseIndependentFiles()throws Exception{try(var chars=new CharacterService(temp);var library=new LibraryService(temp)){
        chars.reload().get(5,TimeUnit.SECONDS);library.reload().get(5,TimeUnit.SECONDS);chars.edit(b->b.create("First")).get(5,TimeUnit.SECONDS);
        byte[] before=Files.readAllBytes(file);library.toggleFavorite("lotc:topic:123").get(5,TimeUnit.SECONDS);assertArrayEquals(before,Files.readAllBytes(file));
        Files.writeString(file,"broken chars");chars.reload().get(5,TimeUnit.SECONDS);assertTrue(library.toggleFavorite("lotc:topic:456").get(5,TimeUnit.SECONDS).writable());}}
}
