package net.mclorehub.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.mclorehub.character.*;
import net.mclorehub.library.*;
import net.mclorehub.updates.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.TitleScreen;
import org.slf4j.LoggerFactory;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("UnstableApiUsage")
public final class CharacterLoreClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context){try{scenario(context);}catch(Exception e){throw new AssertionError("Character lore scenario failed",e);}}
    private void scenario(ClientGameTestContext c)throws Exception {
        c.waitForScreen(TitleScreen.class);Path root=Files.createTempDirectory("mclorehub-character-lore-ui-");
        String pack=System.getenv("MCLOREHUB_TEST_PACK"),trust=System.getenv("MCLOREHUB_TEST_TRUST");
        boolean real=pack!=null&&trust!=null&&!pack.isBlank()&&!trust.isBlank();
        if(real){Files.createDirectories(root.resolve("packs/active"));Files.copy(Path.of(pack),root.resolve("packs/active/pack.json"));Files.copy(Path.of(trust),root.resolve("packs/active/trust.json"));}
        else LibraryClientGameTest.fixture(root);
        AtomicBoolean mutate=new AtomicBoolean(false);
        var transport=(TopicFetcher)d->new TopicSnapshot(d.id(),d.url(),d.title(),d.author().orElse(""),d.body()+(mutate.get()?"\nSynthetic test update; not live lore.":""),d.posts(),List.of(d.url()),Instant.now(),Optional.empty(),List.of());
        int scale=c.computeOnClient(m->m.options.guiScale().get());
        try(var library=new LibraryService(root,transport);var characters=new CharacterService(root)){
            var loaded=library.reload().get(20,TimeUnit.SECONDS);check(loaded.phase()==LibraryService.Phase.READY,"Library unavailable: "+loaded.message());
            characters.reload().get(5,TimeUnit.SECONDS);characters.edit(b->b.create("Lore picker fixture")).get(5,TimeUnit.SECONDS);String id=characters.view().book().selectedId();
            c.runOnClient(m->{m.options.guiScale().set(2);m.resizeDisplay();});
            c.setScreen(()->new CharacterSheetScreen(new TitleScreen(),characters,id,library));
            c.clickScreenButton("Add magic");c.clickScreenButton("Choose lore");
            if(!real){
                c.runOnClient(m->check(m.screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).noneMatch(b->b.active&&b.getMessage().getString().equals("Necromancy")),"Missing curated source became selectable"));
                c.setScreen(TitleScreen::new);LoggerFactory.getLogger("mclorehub-test").info("CHARACTER_LORE_MISSING_SOURCE_PASS; real curated-corpus scenario not run (set MCLOREHUB_TEST_PACK and MCLOREHUB_TEST_TRUST)");return;
            }
            check(library.view().library().orElseThrow().characterLore().choices(CharacterBook.Kind.MAGIC,"").stream().allMatch(o->o.availability()==CharacterLore.Availability.AVAILABLE),"Real main-post sources/revisions/evidence did not match");
            library.refresh("lotc:topic:249533").get(10,TimeUnit.SECONDS);
            check(library.view().library().orElseThrow().characterLore().availability("lotc:topic:249533")==CharacterLore.Availability.AVAILABLE,"No-change check invalidated a main-post rule");mutate.set(true);
            c.runOnClient(m->{var box=m.screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow();m.screen.setFocused(box);box.setFocused(true);});
            c.getInput().typeChars("Necro");c.waitTicks(3);
            c.runOnClient(m->check(m.screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow().getValue().equals("Necro"),"Typing lost query/focus during picker rebuild"));
            bounds(c);c.takeScreenshot("phase8-lore-picker");c.clickScreenButton("Necromancy");
            set(c,"Entry name","Custom necromancer label");set(c,"Tier","T2");set(c,"Entry notes","Keep this private note");c.clickScreenButton("Save entry");c.waitFor(m->m.screen instanceof CharacterSheetScreen);
            for(String[] row:new String[][]{{"magic","Templar","Radiant Templar"},{"FA","Alchemy","Alchemy"},{"CA","Tree","Tree Lord"}}){
                c.clickScreenButton("Add "+row[0]);c.clickScreenButton("Choose lore");set(c,"Search lore options",row[1]);c.clickScreenButton(row[2]);c.clickScreenButton("Save entry");c.waitFor(m->m.screen instanceof CharacterSheetScreen);
            }
            var entries=characters.view().book().selected().orElseThrow().entries();check(entries.size()==4&&entries.stream().allMatch(e->!e.loreDocumentId().isEmpty()),"Curated links not saved");
            byte[] personal=Files.readAllBytes(root.resolve("personal/characters.json")),base=Files.readAllBytes(root.resolve("packs/active/pack.json"));
            c.waitFor(m->m.screen.getNarrationMessage().getString().contains("1 conflict(s)"));c.takeScreenshot("phase8-sheet-summary");
            c.clickScreenButton("Compatibility");c.waitFor(m->m.screen.getNarrationMessage().getString().contains("1 conflict(s)"));bounds(c);c.takeScreenshot("phase8-compatibility");
            c.clickScreenButton("Read source");c.waitFor(m->m.screen instanceof ReaderScreen);c.clickScreenButton("screen.mclorehub.refresh_topic");
            c.waitFor(m->library.view().library().orElseThrow().characterLore().availability("lotc:topic:249533")==CharacterLore.Availability.NEEDS_REVIEW);
            c.clickScreenButton("gui.back");c.waitFor(m->m.screen instanceof CompatibilityScreen&&m.screen.getNarrationMessage().getString().contains("0 conflict(s), 1 need review"));c.takeScreenshot("phase8-source-needs-review");
            library.rollback("lotc:topic:249533").get(10,TimeUnit.SECONDS);c.waitFor(m->m.screen.getNarrationMessage().getString().contains("1 conflict(s)"));
            check(Arrays.equals(personal,Files.readAllBytes(root.resolve("personal/characters.json"))),"Source update/Undo changed characters");check(Arrays.equals(base,Files.readAllBytes(root.resolve("packs/active/pack.json"))),"Source update changed original pack");
            for(int guiScale:new int[]{1,2}){c.runOnClient(m->{m.options.guiScale().set(guiScale);m.resizeDisplay();});bounds(c);c.takeScreenshot("phase8-checks-scale-"+guiScale);}
            c.setScreen(TitleScreen::new);
            var persisted=new CharacterStore(root.resolve("personal/characters.json")).load().book().selected().orElseThrow().entries().getFirst();
            check(persisted.name().equals("Custom necromancer label")&&persisted.loreDocumentId().equals("lotc:topic:249533")&&persisted.tier().equals("T2")&&persisted.notes().equals("Keep this private note"),"Custom fields/reference did not persist");
            LoggerFactory.getLogger("mclorehub-test").info("CHARACTER_LORE_SCENARIO_PASS: real curated sources, picker and MAGIC/CA/FA links, cited conflict, source update suspension, Undo, scales and unchanged character/base data");
        }finally{c.setScreen(TitleScreen::new);c.runOnClient(m->{m.options.guiScale().set(scale);m.resizeDisplay();});}
    }
    private static void set(ClientGameTestContext c,String label,String value){c.runOnClient(m->m.screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).filter(e->e.getMessage().getString().equals(label)).findFirst().orElseThrow().setValue(value));}
    private static void bounds(ClientGameTestContext c){c.runOnClient(m->m.screen.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).forEach(w->check(w.getX()>=0&&w.getY()>=0&&w.getRight()<=m.screen.width&&w.getBottom()<=m.screen.height,"Widget clipped: "+w.getMessage().getString())));}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
