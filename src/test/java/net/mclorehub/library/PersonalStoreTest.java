package net.mclorehub.library;

import net.mclorehub.importer.ImportException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PersonalStoreTest {
    @TempDir Path temp;
    PersonalStore store;Path file;
    @BeforeEach void setup(){file=temp.resolve("personal/state.json");store=new PersonalStore(file);}
    @Test void missingStateIsEmptyWithoutCreatingFiles()throws Exception{assertEquals(PersonalState.empty(),store.load().state());assertFalse(Files.exists(file));}
    @Test void roundTripRetainsMissingFavoritesAndBlockOffsets()throws Exception{
        var state=PersonalState.empty().toggle("lotc:topic:999").remember(new PersonalState.ReadingPosition("lotc:topic:123","a".repeat(64),"post:456",1234,true));
        store.save(state,Optional.empty());assertEquals(state,store.load().state());
    }
    @Test void previousGoodStateIsBackedUp()throws Exception{var first=store.save(PersonalState.empty().toggle("lotc:topic:123"),Optional.empty());
        byte[] before=Files.readAllBytes(file);store.save(first.state().toggle("lotc:topic:456"),first.fileHash());
        assertArrayEquals(before,Files.readAllBytes(file.resolveSibling("state.previous.json")));
        try(var files=Files.list(file.getParent())){assertEquals(2,files.count());}}
    @Test void externalEditIsNotOverwritten()throws Exception{var loaded=store.save(PersonalState.empty(),Optional.empty());Files.writeString(file,"external edit");
        assertThrows(ImportException.class,()->store.save(loaded.state().toggle("lotc:topic:123"),loaded.fileHash()));assertEquals("external edit",Files.readString(file));}
    @Test void newlyCreatedExternalFileIsNotOverwritten()throws Exception{Files.createDirectories(file.getParent());Files.writeString(file,"new external file");
        assertThrows(ImportException.class,()->store.save(PersonalState.empty(),Optional.empty()));assertEquals("new external file",Files.readString(file));}
    @Test void corruptOrFutureSchemaIsNotSilentlyReset()throws Exception{Files.createDirectories(file.getParent());
        for(String invalid:List.of("not json","{\"schema_version\":2,\"favorites\":[]}","{\"schema_version\":1,\"favorites\":[\"lotc:topic:1\",\"lotc:topic:1\"]}")){
            Files.writeString(file,invalid);assertThrows(ImportException.class,store::load);assertEquals(invalid,Files.readString(file));}}
    @Test void symlinkStateIsRefusedWithoutChangingItsTarget()throws Exception{Files.createDirectories(file.getParent());Path target=Files.writeString(temp.resolve("original"),"private data");
        Files.createSymbolicLink(file,target);assertThrows(ImportException.class,store::load);assertThrows(ImportException.class,()->store.save(PersonalState.empty(),Optional.empty()));assertEquals("private data",Files.readString(target));}
    @Test void invalidReadingReferencesAndOffsetsFail(){assertThrows(IllegalArgumentException.class,()->new PersonalState.ReadingPosition("../../secret","a".repeat(64),"body",0,false));
        assertThrows(IllegalArgumentException.class,()->new PersonalState.ReadingPosition("lotc:topic:1","bad","body",0,false));
        assertThrows(IllegalArgumentException.class,()->new PersonalState.ReadingPosition("lotc:topic:1","a".repeat(64),"body",-1,false));}
    @Test void immutableFavoritesAndToggle(){var state=PersonalState.empty().toggle("lotc:topic:1");assertThrows(UnsupportedOperationException.class,()->state.favorites().clear());
        assertEquals(PersonalState.empty(),state.toggle("lotc:topic:1"));}
}
