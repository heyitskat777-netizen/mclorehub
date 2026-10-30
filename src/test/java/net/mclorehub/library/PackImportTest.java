package net.mclorehub.library;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static net.mclorehub.TestData.*;
import static net.mclorehub.importer.JsonSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class PackImportTest {
    @TempDir Path temp;
    Path zip(Path path,Map<String,byte[]> entries)throws Exception{return Files.write(path,net.mclorehub.TestData.zip(entries));}
    Path exportFile(String name,String body)throws Exception {var source=export();record(source).addProperty("content_text",body);rehash(record(source));return write(temp.resolve(name),source);}
    @Test void importIntoEmptyTrustPreviewActivationRestartAndUndo()throws Exception {
        Path first=exportFile("first.json","First main");Path second=exportFile("second.json","Second main");Path root=temp.resolve("library");
        try(var service=new LibraryService(root)){service.reload().get();var preview=service.prepareImport(first,true).get();assertEquals(1,preview.current());assertEquals(LibraryService.Phase.EMPTY,service.view().phase());service.activateImport(preview.token()).get();assertEquals("First main",service.view().library().orElseThrow().catalogue().documents().getFirst().body());
            service.toggleFavorite("lotc:topic:123").get();byte[] personal=Files.readAllBytes(root.resolve("personal/state.json"));var next=service.prepareImport(second,true).get();assertTrue(service.importComparison(next.token(),"lotc:topic:123").get().sections().getFirst().text().contains("+ Second main"));service.activateImport(next.token()).get();assertArrayEquals(personal,Files.readAllBytes(root.resolve("personal/state.json")));assertTrue(service.canUndoImport());service.undoImport().get();assertEquals("First main",service.view().library().orElseThrow().catalogue().documents().getFirst().body());}
        try(var service=new LibraryService(root)){assertEquals("First main",service.reload().get().library().orElseThrow().catalogue().documents().getFirst().body());}
    }
    @Test void unconfirmedImportDoesNotInventCurrentStatus()throws Exception {try(var s=new LibraryService(temp.resolve("lib"))){s.reload().get();var p=s.prepareImport(exportFile("candidate.json","Text"),false).get();assertEquals(0,p.current());s.activateImport(p.token()).get();assertEquals(0,s.view().library().orElseThrow().catalogue().currentDocuments().size());}}
    @Test void invalidAndStalePreviewCannotReplaceWorkingData()throws Exception {try(var s=new LibraryService(temp.resolve("lib"))){s.reload().get();var p=s.prepareImport(exportFile("valid.json","Valid"),true).get();s.activateImport(p.token()).get();var before=s.view().library();Path bad=Files.writeString(temp.resolve("bad.json"),"bad");assertThrows(ExecutionException.class,()->s.prepareImport(bad,true).get());assertEquals(before,s.view().library());var next=s.prepareImport(exportFile("new.json","New"),true).get();s.reload().get();assertThrows(ExecutionException.class,()->s.activateImport(next.token()).get());assertEquals("Valid",s.view().library().orElseThrow().catalogue().documents().getFirst().body());}}
    @Test void externalSelectionEditsAreRetained()throws Exception {Path root=temp.resolve("lib");try(var s=new LibraryService(root)){s.reload().get();var p=s.prepareImport(exportFile("valid.json","Valid"),true).get();s.activateImport(p.token()).get();var next=s.prepareImport(exportFile("new.json","New"),true).get();Path pointer=root.resolve("packs/selection.json");Files.writeString(pointer,"external");assertThrows(ExecutionException.class,()->s.activateImport(next.token()).get());assertEquals("external",Files.readString(pointer));}}
    @Test void immutablePackCorruptionRetainsLoadedLibrary()throws Exception {Path root=temp.resolve("lib");try(var s=new LibraryService(root)){s.reload().get();var p=s.prepareImport(exportFile("one.json","Good"),true).get();s.activateImport(p.token()).get();var loaded=s.view().library();var store=new PackStore(root);Files.writeString(store.selected(store.load()),"corrupt");assertEquals(LibraryService.Phase.ERROR,s.reload().get().phase());assertEquals(loaded,s.view().library());}}
    @Test void originalZipAndNormalizedPackRoundTrip()throws Exception {var source=export();Path raw=zip(temp.resolve("raw.zip"),Map.of("lotc_lore.json",JSON.toJson(source).getBytes(java.nio.charset.StandardCharsets.UTF_8)));var input=PackImport.readInput(raw,temp.resolve("scratch"));Path normalized=zip(temp.resolve("pack.zip"),Map.of("pack.json",JSON.toJson(input.pack()).getBytes(java.nio.charset.StandardCharsets.UTF_8)));assertEquals(input.pack(),PackImport.readInput(normalized,temp.resolve("scratch")).pack());}
    @Test void pathTraversalArchiveNeverActivates()throws Exception {Path bad=zip(temp.resolve("bad.zip"),Map.of("../pack.json","{}".getBytes()));assertThrows(java.io.IOException.class,()->PackImport.readInput(bad,temp.resolve("scratch")));assertFalse(Files.exists(temp.resolve("pack.json")));}
    @Test void corruptActiveRevisionCanUndoWithoutDeletingItsBytes()throws Exception {Path root=temp.resolve("lib");try(var s=new LibraryService(root)){s.reload().get();var a=s.prepareImport(exportFile("a.json","A"),true).get();s.activateImport(a.token()).get();var b=s.prepareImport(exportFile("b.json","B"),true).get();s.activateImport(b.token()).get();var store=new PackStore(root);Path damaged=store.selected(store.load());Files.writeString(damaged,"damaged");s.undoImport().get();assertEquals("A",s.view().library().orElseThrow().catalogue().documents().getFirst().body());assertEquals("damaged",Files.readString(damaged));assertFalse(s.canUndoImport());}}
}
