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
import static net.mclorehub.character.CharacterLore.*;

@SuppressWarnings("UnstableApiUsage")
public final class ExpandedLoreClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context){try{scenario(context);}catch(Exception e){throw new AssertionError("Expanded lore scenario failed",e);}}
    private void scenario(ClientGameTestContext c)throws Exception {
        c.waitForScreen(TitleScreen.class);String pack=System.getenv("MCLOREHUB_TEST_PACK"),trust=System.getenv("MCLOREHUB_TEST_TRUST");
        if(pack==null||trust==null||pack.isBlank()||trust.isBlank()){
            LoggerFactory.getLogger("mclorehub-test").info("EXPANDED_LORE_SCENARIO_NOT_RUN: supply private MCLOREHUB_TEST_PACK and MCLOREHUB_TEST_TRUST");return;
        }
        Path root=Files.createTempDirectory("mclorehub-expanded-lore-ui-");Files.createDirectories(root.resolve("packs/active"));
        Files.copy(Path.of(pack),root.resolve("packs/active/pack.json"));Files.copy(Path.of(trust),root.resolve("packs/active/trust.json"));
        TopicFetcher transport=d->new TopicSnapshot(d.id(),d.url(),d.title(),d.author().orElse(""),d.body()+"\nSynthetic test-only revision.",d.posts(),List.of(d.url()),Instant.now(),Optional.empty(),List.of());
        int scale=c.computeOnClient(m->m.options.guiScale().get());
        try(var library=new LibraryService(root,transport);var characters=new CharacterService(root)){
            check(library.reload().get(20,TimeUnit.SECONDS).phase()==LibraryService.Phase.READY,"Library failed to load");
            var lore=library.view().library().orElseThrow().characterLore();int options=0;
            for(var kind:CharacterBook.Kind.values())for(var choice:lore.choices(kind,"")){options++;check(choice.availability()==Availability.AVAILABLE,"Real source/evidence mismatch: "+choice.option().key());}
            check(options==72&&lore.rules().size()==178,"Expanded manifest scope differs");
            var picked=new java.util.concurrent.atomic.AtomicReference<CharacterLore.Choice>();
            c.setScreen(()->new CharacterLorePickerScreen(new TitleScreen(),library,CharacterBook.Kind.CA,picked::set));
            for(char ch:"Klones".toCharArray()){c.getInput().typeChars(String.valueOf(ch));c.waitTicks(3);}
            checkButton(c,"Klone");c.takeScreenshot("polish-klone-ca-picker");c.clickScreenButton("Klone");
            check(picked.get()!=null&&picked.get().option().sourceId().equals("lotc:topic:198584"),"Klone CA did not link Tawkin");
            c.setScreen(()->new ReaderScreen(new TitleScreen(),library,lore.document("lotc:topic:198584").orElseThrow(),false,false));
            c.waitFor(m->m.screen.getNarrationMessage().getString().contains("Foreword"));c.takeScreenshot("polish-tawkin-centered");
            characters.reload().get(5,TimeUnit.SECONDS);characters.edit(b->b.create("Expanded lore fixture")).get(5,TimeUnit.SECONDS);String id=characters.view().book().selectedId();
            c.runOnClient(m->{m.options.guiScale().set(2);m.resizeDisplay();});
            c.setScreen(()->new CharacterSheetScreen(new TitleScreen(),characters,id,library));
            c.clickScreenButton("Add magic");c.clickScreenButton("Choose lore");c.clickScreenButton("Next");
            checkButton(c,"Bardmancy");bounds(c);c.takeScreenshot("phase9-expanded-picker-page-2");c.clickScreenButton("Previous");
            set(c,"Search lore options","voidal");c.waitTicks(2);
            c.runOnClient(m->check(m.screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).anyMatch(b->b.active&&b.getMessage().getString().equals("Air Evocation")),"Voidal category not searchable"));
            c.takeScreenshot("phase9-voidal-picker");set(c,"Search lore options","Blood");c.clickScreenButton("Blood Magic");c.clickScreenButton("Save entry");c.waitFor(m->m.screen instanceof CharacterSheetScreen);
            for(String[] row:new String[][]{{"magic","Cleric","Clericism"},{"magic","Air","Air Evocation"},{"CA","Sprite","Sprite"},{"FA","Herblore","Druidic Herblore"},{"CA","Klones","Klone"}}){
                c.clickScreenButton("Add "+row[0]);c.clickScreenButton("Choose lore");set(c,"Search lore options",row[1]);c.clickScreenButton(row[2]);c.clickScreenButton("Save entry");c.waitFor(m->m.screen instanceof CharacterSheetScreen);
            }
            c.waitFor(m->m.screen.getNarrationMessage().getString().contains("3 conflict(s)"));
            byte[] personal=Files.readAllBytes(root.resolve("personal/characters.json")),base=Files.readAllBytes(root.resolve("packs/active/pack.json"));
            c.clickScreenButton("Compatibility");c.waitFor(m->m.screen.getNarrationMessage().getString().contains("3 conflict(s)"));bounds(c);c.takeScreenshot("phase9-expanded-conflicts");
            var report=library.assess(characters.view().book().selected().orElseThrow()).get(5,TimeUnit.SECONDS);
            int conditional=-1;for(int i=0;i<report.findings().size();i++)if(report.findings().get(i).message().contains("creature-specific provision"))conditional=i;
            check(conditional>=0,"Sprite Herblore provision missing");for(int i=0;i<conditional;i++)c.clickScreenButton("Next");
            c.waitFor(m->m.screen.getNarrationMessage().getString().contains("creature-specific provision"));c.takeScreenshot("phase9-conditional-provision");
            check(!c.computeOnClient(m->m.screen.getNarrationMessage().getString()).contains("EXCLUSION"),"Conditional provision rendered as exclusion");
            library.refresh("lotc:topic:242890").get(10,TimeUnit.SECONDS);
            var stale=library.assess(characters.view().book().selected().orElseThrow()).get(5,TimeUnit.SECONDS);
            check(stale.findings().stream().noneMatch(f->f.message().contains("creature-specific provision")),"Stale exception remained active");
            check(stale.findings().stream().filter(f->f.outcome()==Outcome.EXCLUSION).count()==2,"Unrelated Cleric checks did not survive source change");
            library.rollback("lotc:topic:242890").get(10,TimeUnit.SECONDS);
            check(library.assess(characters.view().book().selected().orElseThrow()).get(5,TimeUnit.SECONDS).findings().stream().anyMatch(f->f.message().contains("creature-specific provision")),"Undo failed to restore conditional evidence");
            check(Arrays.equals(personal,Files.readAllBytes(root.resolve("personal/characters.json"))),"Source updates changed characters");check(Arrays.equals(base,Files.readAllBytes(root.resolve("packs/active/pack.json"))),"Source updates changed base");
            c.setScreen(TitleScreen::new);check(new CharacterStore(root.resolve("personal/characters.json")).load().book().selected().orElseThrow().entries().size()==6,"Expanded links did not persist");
            LoggerFactory.getLogger("mclorehub-test").info("EXPANDED_LORE_SCENARIO_PASS: 72 choices/67 real sources/178 rules, pagination/category search, new magic/CA/FA links, three exclusions, conditional provision, granular source invalidation/Undo, persistence");
        }finally{c.setScreen(TitleScreen::new);c.runOnClient(m->{m.options.guiScale().set(scale);m.resizeDisplay();});}
    }
    private static void set(ClientGameTestContext c,String label,String value){c.runOnClient(m->m.screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).filter(e->e.getMessage().getString().equals(label)).findFirst().orElseThrow().setValue(value));}
    private static void checkButton(ClientGameTestContext c,String text){c.runOnClient(m->check(m.screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).anyMatch(b->b.active&&b.getMessage().getString().equals(text)),"Expected picker choice: "+text));}
    private static void bounds(ClientGameTestContext c){c.runOnClient(m->m.screen.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).forEach(w->check(w.getX()>=0&&w.getY()>=0&&w.getRight()<=m.screen.width&&w.getBottom()<=m.screen.height,"Widget clipped: "+w.getMessage().getString())));}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
