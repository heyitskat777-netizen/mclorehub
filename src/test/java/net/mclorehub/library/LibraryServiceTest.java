package net.mclorehub.library;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class LibraryServiceTest {
    @TempDir Path temp;
    @Test void emptyLibraryLoadsAndCreatesNoSourceFiles()throws Exception{try(var s=new LibraryService(temp)){
        assertEquals(LibraryService.Phase.LOADING,s.view().phase());assertEquals(LibraryService.Phase.EMPTY,s.reload().get(5,TimeUnit.SECONDS).phase());
        assertFalse(Files.exists(temp.resolve("packs")));}}
    void install()throws Exception{LibraryFixtures.catalogue(temp.resolve("fixture"));Path active=temp.resolve("packs/active");Files.createDirectories(active);
        Files.copy(temp.resolve("fixture/pack.json"),active.resolve("pack.json"));Files.copy(temp.resolve("fixture/reviews.json"),active.resolve("reviews.json"));}
    @Test void loadedLibrarySearchesOffCallerAndPersistsFavoritesAcrossServices()throws Exception{install();try(var s=new LibraryService(temp)){
        var loaded=s.reload().get(5,TimeUnit.SECONDS);assertEquals(4,loaded.library().orElseThrow().catalogue().documents().size());
        assertEquals(3,s.search(new LoreSearch.Query("","",false,false,Set.of(),0,10)).get(5,TimeUnit.SECONDS).total());
        assertTrue(s.toggleFavorite("lotc:topic:104").get(5,TimeUnit.SECONDS).personal().favorites().contains("lotc:topic:104"));}
        try(var s=new LibraryService(temp)){assertTrue(s.reload().get(5,TimeUnit.SECONDS).personal().favorites().contains("lotc:topic:104"));}}
    @Test void invalidPackRetainsLastValidatedLibrary()throws Exception{install();try(var s=new LibraryService(temp)){
        var old=s.reload().get(5,TimeUnit.SECONDS).library().orElseThrow();Files.writeString(temp.resolve("packs/active/pack.json"),"broken");
        var failed=s.reload().get(5,TimeUnit.SECONDS);assertEquals(LibraryService.Phase.ERROR,failed.phase());assertSame(old,failed.library().orElseThrow());}}
    @Test void corruptPersonalFileDoesNotBlockLoreAndIsNeverOverwritten()throws Exception{install();Path state=temp.resolve("personal/state.json");Files.createDirectories(state.getParent());Files.writeString(state,"broken personal data");
        try(var s=new LibraryService(temp)){var view=s.reload().get(5,TimeUnit.SECONDS);assertEquals(LibraryService.Phase.READY,view.phase());assertFalse(view.writable());
            s.toggleFavorite("lotc:topic:104").get(5,TimeUnit.SECONDS);assertEquals("broken personal data",Files.readString(state));}}
    @Test void missingPackRetainsPreviousLibraryAndPersonalReferences()throws Exception{install();try(var s=new LibraryService(temp)){
        s.reload().get(5,TimeUnit.SECONDS);s.toggleFavorite("lotc:topic:104").get(5,TimeUnit.SECONDS);Files.delete(temp.resolve("packs/active/pack.json"));
        var view=s.reload().get(5,TimeUnit.SECONDS);assertEquals(LibraryService.Phase.ERROR,view.phase());assertEquals(4,view.library().orElseThrow().catalogue().documents().size());
        assertEquals(Set.of("lotc:topic:104"),view.personal().favorites());}}
    @Test void externalEditsDisableSavingUntilExplicitReload()throws Exception{try(var s=new LibraryService(temp)){
        s.reload().get(5,TimeUnit.SECONDS);s.toggleFavorite("lotc:topic:104").get(5,TimeUnit.SECONDS);Path file=temp.resolve("personal/state.json");
        String external="{\"schema_version\":1,\"favorites\":[\"lotc:topic:999\"],\"reading\":null}";Files.writeString(file,external);
        assertFalse(s.toggleFavorite("lotc:topic:103").get(5,TimeUnit.SECONDS).writable());assertEquals(external,Files.readString(file));
        assertEquals(Set.of("lotc:topic:999"),s.reload().get(5,TimeUnit.SECONDS).personal().favorites());}}
}
