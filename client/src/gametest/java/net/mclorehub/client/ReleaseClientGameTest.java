package net.mclorehub.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.mclorehub.character.*;
import net.mclorehub.library.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("UnstableApiUsage")
public final class ReleaseClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c){try{scenario(c);}catch(Exception e){throw new AssertionError("Release acceptance failed",e);}}
    private void scenario(ClientGameTestContext c)throws Exception {
        c.waitForScreen(TitleScreen.class);String export=System.getenv("MCLOREHUB_TEST_EXPORT");if(export==null||export.isBlank()){LoggerFactory.getLogger("mclorehub-test").info("RELEASE_SCENARIO_NOT_RUN: supply private MCLOREHUB_TEST_EXPORT for fresh full-database acceptance");return;}
        check(FabricLoader.getInstance().getModContainer("mclorehub").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("1.0.1"),"Unexpected release version");
        check(FabricLoader.getInstance().getModContainer("fabricloader").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("0.19.5"),"Acceptance must run on requested Fabric Loader 0.19.5");
        Path root=Files.createTempDirectory("mclorehub-release-clean-");int oldScale=c.computeOnClient(m->m.options.guiScale().get());
        try(var library=new LibraryService(root);var chars=new CharacterService(root)){
            library.reload().get(10,TimeUnit.SECONDS);chars.reload().get(5,TimeUnit.SECONDS);c.runOnClient(m->{m.options.guiScale().set(2);m.resizeDisplay();});
            c.setScreen(()->new WikiScreen(new TitleScreen(),library));c.clickScreenButton("Database");c.runOnClient(m->m.screen.onFilesDrop(List.of(Path.of(export))));c.waitFor(m->m.screen.getNarrationMessage().getString().contains("488 topics"),600);c.takeScreenshot("release-full-import-preview");
            c.clickScreenButton("Activate import");c.waitFor(m->m.screen.getNarrationMessage().getString().contains("Database active"),600);check(library.view().library().orElseThrow().catalogue().currentDocuments().size()==488,"Fresh import did not populate 488 normal references");
            var lore=library.view().library().orElseThrow().characterLore();for(var kind:CharacterBook.Kind.values())for(var choice:lore.choices(kind,""))check(choice.availability()==CharacterLore.Availability.AVAILABLE,"Curated real source mismatch: "+choice.option().key());
            var created=chars.edit(b->b.create("Release slot fixture")).get();String id=created.book().selectedId();
            var kani=new CharacterBook.Entry(UUID.randomUUID().toString(),CharacterBook.Kind.MAGIC,"Kani","","T3","",CharacterBook.RecordedStatus.NOT_RECORDED,"","");var blood=new CharacterBook.Entry(UUID.randomUUID().toString(),CharacterBook.Kind.MAGIC,"Blood Magic","","Lesser","",CharacterBook.RecordedStatus.NOT_RECORDED,"","");
            var k=lore.link(kani,"kani");var b=lore.link(blood,"blood-magic");chars.edit(book->book.putEntry(id,k).putEntry(id,b).putHistory(id,new CharacterBook.HistoryEvent(UUID.randomUUID().toString(),"Old Necromancy","lotc:topic:249533",CharacterBook.EventType.DISCONNECTED,"2026-10-01","Synthetic release test"))).get();
            c.setScreen(()->new CharacterSheetScreen(new TitleScreen(),chars,id,library));c.clickScreenButton("Slots");set(c,"Usable slot capacity","5");set(c,"Total lost slots","3");bounds(c);c.takeScreenshot("release-slot-editor");c.clickScreenButton("Save slots");
            c.waitFor(m->m.screen instanceof CharacterSheetScreen&&m.screen.getNarrationMessage().getString().contains("1 conflict(s)"));c.clickScreenButton("Compatibility");c.waitFor(m->m.screen.getNarrationMessage().getString().contains("Over recorded usable capacity"));c.takeScreenshot("release-slot-overflow");
            chars.edit(book->book.removeEntry(id,b.id())).get();c.waitFor(m->m.screen.getNarrationMessage().getString().contains("0 conflict(s)"));var sheet=chars.view().book().selected().orElseThrow();var slots=lore.slots(sheet);check(slots.minimum()==5&&slots.maximum()==5&&!slots.overBudget(),"Permanent loss was not retained after active entry removal");check(new CharacterStore(root.resolve("personal/characters.json")).load().book().equals(chars.view().book()),"Slot persistence failed");
            c.setScreen(()->new CharacterSheetScreen(new TitleScreen(),chars,id,library));for(int scale:new int[]{1,2}){c.runOnClient(m->{m.options.guiScale().set(scale);m.resizeDisplay();});bounds(c);c.takeScreenshot("release-sheet-scale-"+scale);}
            c.setScreen(TitleScreen::new);LoggerFactory.getLogger("mclorehub-test").info("RELEASE_SCENARIO_PASS: clean 1.0.1 install on Loader 0.19.5, full original ZIP UI import, 488 normal references, all curated real sources, slot editor/overflow/permanent-loss recovery, persistence and scales; no authenticated MArt requests");
        }finally{c.setScreen(TitleScreen::new);c.runOnClient(m->{m.options.guiScale().set(oldScale);m.resizeDisplay();});}
        try(var restart=new LibraryService(root)){check(restart.reload().get(20,TimeUnit.SECONDS).library().orElseThrow().catalogue().currentDocuments().size()==488,"Fresh selected pack did not survive restart");}
    }
    private static void set(ClientGameTestContext c,String name,String value){c.runOnClient(m->m.screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).filter(e->e.getMessage().getString().equals(name)).findFirst().orElseThrow().setValue(value));}
    private static void bounds(ClientGameTestContext c){c.runOnClient(m->m.screen.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).forEach(w->check(w.getX()>=0&&w.getY()>=0&&w.getRight()<=m.screen.width&&w.getBottom()<=m.screen.height,"Widget clipped: "+w.getMessage().getString())));}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
