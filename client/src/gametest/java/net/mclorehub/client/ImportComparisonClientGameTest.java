package net.mclorehub.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.mclorehub.library.*;
import net.mclorehub.catalogue.LoreCatalogue;
import net.mclorehub.character.*;
import net.mclorehub.updates.*;
import net.minecraft.client.gui.screens.TitleScreen;
import com.google.gson.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.slf4j.LoggerFactory;
import static net.mclorehub.importer.JsonSupport.*;

@SuppressWarnings("UnstableApiUsage")
public final class ImportComparisonClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c){try{scenario(c);}catch(Exception e){throw new AssertionError("Import/comparison scenario failed",e);}}
    private void scenario(ClientGameTestContext c)throws Exception {
        c.waitForScreen(TitleScreen.class);Path root=Files.createTempDirectory("mclorehub-import-ui-");LibraryClientGameTest.fixture(root);Path pack=root.resolve("packs/active/pack.json");
        writeNew(pack.resolveSibling("trust.json"),BaselineTrust.manifest(LoreCatalogue.load(pack,null),Instant.now()));
        var original=read(pack,64<<20);var source=new JsonObject();var metadata=new JsonObject();metadata.addProperty("record_count",3);metadata.addProperty("generated_utc","2026-09-21T00:00:00Z");source.add("metadata",metadata);var records=new JsonArray();
        for(var value:original.getAsJsonArray("documents")){var raw=value.getAsJsonObject().getAsJsonObject("source_record").deepCopy();if(raw.get("topic_id").getAsString().equals("123")){raw.addProperty("content_text","Imported replacement MAIN lore.");raw.addProperty("content_sha256",hash("Imported replacement MAIN lore."));}records.add(raw);}source.add("records",records);Path incoming=root.resolve("replacement.json");writeNew(incoming,source);
        TopicFetcher transport=d->new TopicSnapshot(d.id(),d.url(),d.title(),d.author().orElse(""),"Fetched comparison MAIN lore.",d.posts(),List.of(d.url()),Instant.now(),Optional.empty(),List.of());
        try(var library=new LibraryService(root,transport);var chars=new CharacterService(root)){
            library.reload().get(10,TimeUnit.SECONDS);chars.reload().get();chars.edit(b->b.create("Preserved fixture")).get();library.toggleFavorite("lotc:topic:123").get();byte[] personal=Files.readAllBytes(root.resolve("personal/state.json")),characters=Files.readAllBytes(root.resolve("personal/characters.json")),base=Files.readAllBytes(pack);
            c.setScreen(()->new WikiScreen(new TitleScreen(),library));c.clickScreenButton("Database");c.runOnClient(m->m.screen.onFilesDrop(List.of(incoming)));c.waitFor(m->m.screen.getNarrationMessage().getString().contains("3 topics"));c.takeScreenshot("completion-import-preview");
            var before=library.view().library().orElseThrow().catalogue().documents().getFirst();c.clickScreenButton("Changed: "+before.title());findText(c,"+ Imported replacement MAIN lore.");c.takeScreenshot("completion-import-diff");c.clickScreenButton("Back");c.clickScreenButton("Activate import");c.waitFor(m->m.screen.getNarrationMessage().getString().contains("Database active"));
            check(library.view().library().orElseThrow().catalogue().documents().getFirst().body().equals("Imported replacement MAIN lore."),"Import did not activate");
            c.clickScreenButton("Undo database import");c.waitFor(m->library.view().library().orElseThrow().catalogue().documents().getFirst().body().equals(before.body()));
            check(Arrays.equals(personal,Files.readAllBytes(root.resolve("personal/state.json"))),"Import changed favorites");check(Arrays.equals(characters,Files.readAllBytes(root.resolve("personal/characters.json"))),"Import changed characters");check(Arrays.equals(base,Files.readAllBytes(pack)),"Import rewrote legacy pack");
            library.refresh("lotc:topic:123").get(10,TimeUnit.SECONDS);var after=library.view().library().orElseThrow().catalogue().documents().getFirst();c.setScreen(()->new ReaderScreen(new TitleScreen(),library,after,false,true));c.clickScreenButton("Compare changes");findText(c,"+ Fetched comparison MAIN lore.");c.takeScreenshot("completion-topic-diff");c.setScreen(TitleScreen::new);
        }
        try(var reopened=new LibraryService(root)){check(reopened.reload().get().library().orElseThrow().catalogue().documents().getFirst().body().equals("Fetched comparison MAIN lore."),"Selection and topic update did not survive restart");}
        LoggerFactory.getLogger("mclorehub-test").info("IMPORT_COMPARISON_SCENARIO_PASS: dropped export, preview/readable diff, activation/Undo, personal/base retention, topic comparison and restart");
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void findText(ClientGameTestContext c,String text){c.waitFor(m->m.screen instanceof ReportScreen&&m.screen.getNarrationMessage().getString().contains("previous saved text"));for(int i=0;i<20;i++){if(c.computeOnClient(m->m.screen.getNarrationMessage().getString()).contains(text))return;c.clickScreenButton("Next");}throw new AssertionError("Diff text missing: "+text);}
}
