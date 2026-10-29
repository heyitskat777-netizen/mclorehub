package net.mclorehub.updates;

import net.mclorehub.catalogue.LoreCatalogue;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class UpdateStoreTest {
    @TempDir Path root;LoreCatalogue base;UpdateStore store;
    @BeforeEach void setup()throws Exception{base=UpdateFixtures.install(root,true);store=new UpdateStore(root);}
    @Test void stagedSnapshotIsInvisibleUntilPointerActivation()throws Exception{var before=store.load();var d=base.documents().getFirst();var stage=store.stage(before,d.fingerprint(),UpdateFixtures.snapshot(d,"Changed main"),true);
        assertEquals(d.body(),store.apply(base,store.load()).catalogue().documents().getFirst().body());assertFalse(Files.exists(root.resolve("updates/active.json")));
        store.activate(stage);assertEquals("Changed main",store.apply(base,store.load()).catalogue().documents().getFirst().body());}
    @Test void rollbackChangesOnlySelectedTopicAndPreservesOtherUpdates()throws Exception{var a=base.documents().getFirst();var b=base.documents().getLast();
        var state=store.activate(store.stage(store.load(),a.fingerprint(),UpdateFixtures.snapshot(a,"A first"),true));
        state=store.activate(store.stage(state,a.fingerprint(),UpdateFixtures.snapshot(a,"A second"),true));
        state=store.activate(store.stage(state,b.fingerprint(),UpdateFixtures.snapshot(b,"B updated"),true));
        state=store.activate(store.rollback(state,a.id()));var read=store.apply(base,state).catalogue();assertEquals("A first",read.documents().getFirst().body());assertEquals("B updated",read.documents().getLast().body());}
    @Test void unchangedChecksDoNotLosePreviousContentVersion()throws Exception{var d=base.documents().getFirst();var state=store.activate(store.stage(store.load(),d.fingerprint(),UpdateFixtures.snapshot(d,"Changed"),true));
        state=store.activate(store.stage(state,d.fingerprint(),UpdateFixtures.snapshot(d,"Changed"),false));state=store.activate(store.rollback(state,d.id()));
        assertEquals(d.body(),store.apply(base,state).catalogue().documents().getFirst().body());}
    @Test void badSnapshotHashFailsWithoutChangingPointer()throws Exception{var d=base.documents().getFirst();var state=store.activate(store.stage(store.load(),d.fingerprint(),UpdateFixtures.snapshot(d,"Changed"),true));byte[] pointer=Files.readAllBytes(root.resolve("updates/active.json"));
        Files.writeString(root.resolve("updates/snapshots/"+state.entries().get(d.id()).active()+".json"),"corrupt");assertThrows(java.io.IOException.class,()->store.apply(base,state));assertArrayEquals(pointer,Files.readAllBytes(root.resolve("updates/active.json")));}
    @Test void anotherBaselineDoesNotInheritOldPageOverride()throws Exception{var d=base.documents().getFirst();var state=store.stage(store.load(),"f".repeat(64),UpdateFixtures.snapshot(d,"Changed"),true);
        var read=store.apply(base,state);assertEquals(1,read.ignored());assertEquals(d.body(),read.catalogue().documents().getFirst().body());assertEquals(1,state.entries().size());}
    @Test void externallyEditedPointerIsNotOverwritten()throws Exception{var d=base.documents().getFirst();var state=store.activate(store.stage(store.load(),d.fingerprint(),UpdateFixtures.snapshot(d,"Changed"),true));
        var next=store.stage(state,d.fingerprint(),UpdateFixtures.snapshot(d,"Second update"),true);Files.writeString(root.resolve("updates/active.json"),"outside edit");
        assertThrows(java.io.IOException.class,()->store.activate(next));assertEquals("outside edit",Files.readString(root.resolve("updates/active.json")));}
    @Test void pointerCannotEscapeSnapshotDirectory()throws Exception{Files.createDirectories(root.resolve("updates"));Files.writeString(root.resolve("updates/active.json"),
        "{\"schema_version\":1,\"topics\":{\"lotc:topic:123\":{\"base_fingerprint\":\""+"a".repeat(64)+"\",\"active\":\"../../personal/state.json\",\"previous\":\"\"}}}");assertThrows(java.io.IOException.class,store::load);}
    @Test void refreshedUntrustedTextDoesNotRenewOldAcceptanceWithoutEvidence()throws Exception{var raw=UpdateFixtures.install(root.resolve("other"),false);var d=raw.documents().getFirst();var updated=UpdateFixtures.snapshot(d,"New main").apply(d);
        assertEquals(LoreCatalogue.Status.UNKNOWN,updated.status());assertFalse(LoreCatalogue.isCurrent(updated));}
    @Test void observedShelvingMovesTrustedReferenceOutOfCurrent()throws Exception{var d=base.documents().getFirst();var s=UpdateFixtures.snapshot(d,"New main");
        var shelved=new TopicSnapshot(s.documentId(),s.url(),s.title(),s.author(),s.body(),s.posts(),s.pages(),s.fetchedAt(),java.util.Optional.of(LoreCatalogue.Status.SHELVED),s.absentPosts());
        assertEquals(LoreCatalogue.Status.SHELVED,shelved.apply(d).status());assertFalse(LoreCatalogue.isCurrent(shelved.apply(d)));}
}
