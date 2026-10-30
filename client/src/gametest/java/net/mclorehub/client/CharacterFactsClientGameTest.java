package net.mclorehub.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.mclorehub.character.*;
import net.mclorehub.library.*;
import net.mclorehub.updates.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.TitleScreen;
import org.slf4j.LoggerFactory;
import com.google.gson.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static net.mclorehub.importer.JsonSupport.*;

@SuppressWarnings("UnstableApiUsage")
public final class CharacterFactsClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c){try{scenario(c);}catch(Exception e){throw new AssertionError("Character facts scenario failed",e);}}
    private void scenario(ClientGameTestContext c)throws Exception {
        c.waitForScreen(TitleScreen.class);String pack=System.getenv("MCLOREHUB_TEST_PACK"),trust=System.getenv("MCLOREHUB_TEST_TRUST");
        if(pack==null||trust==null||pack.isBlank()||trust.isBlank()){LoggerFactory.getLogger("mclorehub-test").info("CHARACTER_FACTS_SCENARIO_NOT_RUN: supply private pack/trust for source-backed native checks");return;}
        Path root=Files.createTempDirectory("mclorehub-facts-ui-");Files.createDirectories(root.resolve("packs/active"));Files.copy(Path.of(pack),root.resolve("packs/active/pack.json"));Files.copy(Path.of(trust),root.resolve("packs/active/trust.json"));
        var book=CharacterBook.empty().create("Legacy facts fixture");String id=book.selectedId();var legacy=new JsonObject();legacy.addProperty("schema_version",1);legacy.addProperty("selected_id",id);var sheets=JSON.toJsonTree(book.sheets()).getAsJsonArray();for(var s:sheets){s.getAsJsonObject().remove("facts");s.getAsJsonObject().remove("history");}legacy.add("sheets",sheets);
        Path file=root.resolve("personal/characters.json");Files.createDirectories(file.getParent());Files.writeString(file,JSON.toJson(legacy));byte[] before=Files.readAllBytes(file);
        TopicFetcher fetcher=d->new TopicSnapshot(d.id(),d.url(),d.title(),d.author().orElse(""),d.body()+"\nSynthetic history-source update.",d.posts(),List.of(d.url()),Instant.now(),Optional.empty(),List.of());
        int scale=c.computeOnClient(m->m.options.guiScale().get());
        try(var library=new LibraryService(root,fetcher);var characters=new CharacterService(root)){
            library.reload().get(20,TimeUnit.SECONDS);characters.reload().get(5,TimeUnit.SECONDS);check(Arrays.equals(before,Files.readAllBytes(file)),"Legacy load rewrote file");
            c.runOnClient(m->{m.options.guiScale().set(2);m.resizeDisplay();});c.setScreen(()->new CharacterSheetScreen(new TitleScreen(),characters,id,library));
            c.clickScreenButton("Known facts");c.clickScreenButton("Soul: unknown");c.clickScreenButton("Soul: greater");c.clickScreenButton("Can breathe: unknown");c.clickScreenButton("Has lungs: unknown");c.clickScreenButton("Has lungs: yes");
            bounds(c);c.takeScreenshot("phase10-known-facts");c.clickScreenButton("Next");c.clickScreenButton("Corrupted / modified Body portion: unknown");c.clickScreenButton("Klone body: unknown");c.clickScreenButton("Save facts");c.waitFor(m->m.screen instanceof CharacterSheetScreen);
            check(read(file,1<<20).get("schema_version").getAsInt()==2,"Did not migrate");check(Arrays.equals(before,Files.readAllBytes(file.resolveSibling("characters.previous.json"))),"Legacy backup changed");
            c.clickScreenButton("Add magic");c.clickScreenButton("Choose lore");set(c,"Search lore options","Kani");c.clickScreenButton("Kani");c.clickScreenButton("Save entry");c.waitFor(m->m.screen instanceof CharacterSheetScreen&&m.screen.getNarrationMessage().getString().contains("1 conflict(s)"));
            c.clickScreenButton("Compatibility");c.waitFor(m->m.screen.getNarrationMessage().getString().contains("requires lungs"));c.takeScreenshot("phase10-eligibility-conflict");c.clickScreenButton("Back");
            c.clickScreenButton("Known facts");c.clickScreenButton("Has lungs: no");c.clickScreenButton("Has lungs: unknown");c.clickScreenButton("Save facts");c.waitFor(m->m.screen instanceof CharacterSheetScreen&&m.screen.getNarrationMessage().getString().contains("0 conflict(s)"));
            c.clickScreenButton("History");c.clickScreenButton("Record event");c.clickScreenButton("Choose lore");set(c,"Search lore options","Necromancy");c.clickScreenButton("Necromancy");c.clickScreenButton("Event: acquired");
            set(c,"Event date","2026-02-30");c.clickScreenButton("Save event");check(characters.view().book().selected().orElseThrow().history().isEmpty(),"Invalid date saved");
            set(c,"Event date","2026-10-01");set(c,"Event notes","Synthetic old disconnection; current facts unchanged");bounds(c);c.takeScreenshot("phase10-history-entry");c.clickScreenButton("Save event");c.waitFor(m->m.screen instanceof CharacterHistoryScreen);
            c.takeScreenshot("phase10-history-list");c.clickScreenButton("Back");
            var saved=characters.view().book();check(saved.selected().orElseThrow().entries().size()==1&&saved.selected().orElseThrow().history().size()==1,"History replaced active entry");check(saved.selected().orElseThrow().facts().templarSeverances()==CharacterFacts.Severances.UNKNOWN,"History inferred current facts");
            byte[] personal=Files.readAllBytes(file);c.clickScreenButton("Compatibility");var report=library.assess(saved.selected().orElseThrow()).get(5,TimeUnit.SECONDS);int history=-1;for(int i=0;i<report.findings().size();i++)if(report.findings().get(i).message().contains("Past Necromancy disconnection"))history=i;check(history>=0,"Lost-slot guidance missing");c.waitTicks(3);for(int i=0;i<history;i++)c.clickScreenButton("Next");
            c.waitFor(m->m.screen.getNarrationMessage().getString().contains("dedicated slots are permanently lost"));c.takeScreenshot("phase10-history-guidance");
            library.refresh("lotc:topic:249533").get(10,TimeUnit.SECONDS);c.waitFor(m->m.screen.getNarrationMessage().getString().contains("History source changed"));c.takeScreenshot("phase10-history-source-review");library.rollback("lotc:topic:249533").get(10,TimeUnit.SECONDS);
            check(Arrays.equals(personal,Files.readAllBytes(file)),"Lore update/Undo changed facts/history");check(new CharacterStore(file).load().book().equals(saved),"Facts/history lost on reload");
            c.setScreen(()->new CharacterFactsScreen(new TitleScreen(),characters,saved.selected().orElseThrow()));for(int guiScale:new int[]{1,2}){c.runOnClient(m->{m.options.guiScale().set(guiScale);m.resizeDisplay();});bounds(c);c.takeScreenshot("phase10-facts-scale-"+guiScale);}
            LoggerFactory.getLogger("mclorehub-test").info("CHARACTER_FACTS_SCENARIO_PASS: exact legacy backup/migration, facts editor, Kani Klone exception with lungs failure/recovery, history/date validation, disconnected Necromancy without active entry, source suspension/Undo, persistence and scales");
        }finally{c.setScreen(TitleScreen::new);c.runOnClient(m->{m.options.guiScale().set(scale);m.resizeDisplay();});}
    }
    private static void set(ClientGameTestContext c,String label,String value){c.runOnClient(m->m.screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).filter(e->e.getMessage().getString().equals(label)).findFirst().orElseThrow().setValue(value));}
    private static void bounds(ClientGameTestContext c){c.runOnClient(m->m.screen.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).forEach(w->check(w.getX()>=0&&w.getY()>=0&&w.getRight()<=m.screen.width&&w.getBottom()<=m.screen.height,"Widget clipped: "+w.getMessage().getString())));}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
