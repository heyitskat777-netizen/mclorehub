package net.mclorehub.updates;

import net.mclorehub.library.*;
import net.mclorehub.character.CharacterService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class RefreshServiceTest {
    @TempDir Path root;
    @Test void refreshPersistsAcrossRestartAndLeavesBaseFavoritesCharactersUnchanged()throws Exception{
        UpdateFixtures.install(root,true);Path pack=root.resolve("packs/active/pack.json");byte[] original=Files.readAllBytes(pack),personal,characters;
        try(var service=new LibraryService(root,d->UpdateFixtures.snapshot(d,"New searchable starlight"));var chars=new CharacterService(root)){
            service.reload().get(5,TimeUnit.SECONDS);chars.reload().get(5,TimeUnit.SECONDS);chars.edit(b->b.create("Test character")).get(5,TimeUnit.SECONDS);
            service.toggleFavorite("lotc:topic:123").get(5,TimeUnit.SECONDS);personal=Files.readAllBytes(root.resolve("personal/state.json"));characters=Files.readAllBytes(root.resolve("personal/characters.json"));
            assertTrue(service.refresh("lotc:topic:123").get(10,TimeUnit.SECONDS).changed());assertTrue(service.canRollback("lotc:topic:123"));
            assertEquals(1,service.search(new LoreSearch.Query("starlight","",false,false,java.util.Set.of(),0,10)).get(5,TimeUnit.SECONDS).total());
        }
        try(var service=new LibraryService(root)){var loaded=service.reload().get(5,TimeUnit.SECONDS);assertEquals("New searchable starlight",loaded.library().orElseThrow().catalogue().documents().getFirst().body());
            assertEquals(2,loaded.library().orElseThrow().catalogue().currentDocuments().size());service.rollback("lotc:topic:123").get(5,TimeUnit.SECONDS);}
        assertArrayEquals(original,Files.readAllBytes(pack));assertArrayEquals(personal,Files.readAllBytes(root.resolve("personal/state.json")));assertArrayEquals(characters,Files.readAllBytes(root.resolve("personal/characters.json")));
    }
    @Test void failedRefreshNeverCreatesActivePointerOrChangesLibrary()throws Exception{UpdateFixtures.install(root,true);
        try(var service=new LibraryService(root,d->{throw new java.io.IOException("HTTP 403");})){var loaded=service.reload().get(5,TimeUnit.SECONDS).library();
            assertThrows(ExecutionException.class,()->service.refresh("lotc:topic:123").get(5,TimeUnit.SECONDS));assertEquals(loaded,service.view().library());assertFalse(Files.exists(root.resolve("updates/active.json")));}}
    @Test void cancelDoesNotBlockPersonalWritesOrPersistPartialContent()throws Exception{UpdateFixtures.install(root,true);CountDownLatch started=new CountDownLatch(1);
        try(var service=new LibraryService(root,d->{started.countDown();Thread.sleep(30000);return UpdateFixtures.snapshot(d,"Cancelled content");})){
            service.reload().get(5,TimeUnit.SECONDS);var pending=service.refresh("lotc:topic:123");assertTrue(started.await(2,TimeUnit.SECONDS));
            assertTrue(service.toggleFavorite("lotc:topic:123").get(2,TimeUnit.SECONDS).personal().favorites().contains("lotc:topic:123"));
            assertThrows(ExecutionException.class,()->service.rollback("lotc:topic:123").get(2,TimeUnit.SECONDS));
            assertEquals(LibraryService.RefreshPhase.FETCHING,service.refreshStatus().phase(),"Rejected Undo must not hide active refresh/cancel controls");
            service.cancelRefresh();assertThrows(CancellationException.class,pending::join);assertFalse(Files.exists(root.resolve("updates/active.json")));}}
    @Test void invalidFetchedSnapshotNeverBecomesActive()throws Exception{UpdateFixtures.install(root,true);
        try(var service=new LibraryService(root,d->new TopicSnapshot(d.id(),UpdateFixtures.URL.replace("123-","999-"),d.title(),"Author","Body",java.util.List.of(),java.util.List.of(d.url()),java.time.Instant.now(),java.util.Optional.empty(),java.util.List.of()))){
            service.reload().get(5,TimeUnit.SECONDS);assertThrows(ExecutionException.class,()->service.refresh("lotc:topic:123").get(5,TimeUnit.SECONDS));assertFalse(Files.exists(root.resolve("updates/active.json")));}}
    @Test void reloadDuringFetchDoesNotApplyUpdateToChangedLocalDocument()throws Exception{UpdateFixtures.install(root,true);CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1);
        try(var service=new LibraryService(root,d->{started.countDown();if(!release.await(5,TimeUnit.SECONDS))throw new java.io.IOException("Timeout");return UpdateFixtures.snapshot(d,"Changed");})){
            service.reload().get(5,TimeUnit.SECONDS);var task=service.refresh("lotc:topic:123");assertTrue(started.await(2,TimeUnit.SECONDS));
            var json=net.mclorehub.importer.JsonSupport.read(root.resolve("packs/active/pack.json"),64<<20);
            var doc=json.getAsJsonArray("documents").get(0).getAsJsonObject();var raw=doc.getAsJsonObject("source_record");raw.addProperty("status_note","Changed outside game");
            Files.writeString(root.resolve("packs/active/pack.json"),net.mclorehub.importer.JsonSupport.JSON.toJson(json));service.reload().get(5,TimeUnit.SECONDS);release.countDown();
            assertThrows(ExecutionException.class,()->task.get(5,TimeUnit.SECONDS));assertFalse(Files.exists(root.resolve("updates/active.json")));}}
}
