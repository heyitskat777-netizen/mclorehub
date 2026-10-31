package net.mclorehub.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.mclorehub.catalogue.LoreCatalogue;
import net.mclorehub.character.*;
import net.mclorehub.library.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.TitleScreen;
import org.slf4j.LoggerFactory;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Private rebuilt corpus stays outside source and CI. No forum requests are made. */
@SuppressWarnings("UnstableApiUsage")
public final class RebuiltDatabaseClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c){try{scenario(c);}catch(Exception e){throw new AssertionError("Rebuilt database failed",e);}}
    private void scenario(ClientGameTestContext c)throws Exception{
        c.waitForScreen(TitleScreen.class);String input=System.getenv("MCLOREHUB_TEST_REBUILT");if(input==null||input.isBlank()){LoggerFactory.getLogger("mclorehub-test").info("REBUILT_DATABASE_NOT_RUN: supply private MCLOREHUB_TEST_REBUILT import ZIP");return;}
        Path root=Files.createTempDirectory("mclorehub-rebuilt-database-");int scale=c.computeOnClient(m->m.options.guiScale().get());String sheetId;
        try(var library=new LibraryService(root);var chars=new CharacterService(root)){
            library.reload().get(10,TimeUnit.SECONDS);chars.reload().get(5,TimeUnit.SECONDS);c.runOnClient(m->{m.options.guiScale().set(2);m.resizeDisplay();});
            c.setScreen(()->new WikiScreen(new TitleScreen(),library));c.clickScreenButton("Database");c.runOnClient(m->m.screen.onFilesDrop(List.of(Path.of(input))));c.waitFor(m->m.screen.getNarrationMessage().getString().contains("801 topics"),600);c.takeScreenshot("rebuilt-import-preview");
            c.clickScreenButton("Activate import");c.waitFor(m->m.screen.getNarrationMessage().getString().contains("Database active"),600);
            var loaded=library.view().library().orElseThrow();check(loaded.catalogue().documents().size()==801&&loaded.catalogue().currentDocuments().size()==773,"Rebuilt counts/statuses were not preserved");
            check(loaded.catalogue().documents().stream().filter(d->d.status()==LoreCatalogue.Status.LEGACY).count()==27,"Legacy overrides were lost");check(loaded.catalogue().documents().stream().filter(d->d.status()==LoreCatalogue.Status.SHELVED).count()==1,"Outdated Elder Tree became current");
            var marts=loaded.catalogue().documents().stream().filter(d->d.role()==LoreCatalogue.Role.MAGICAL_ARTIFACT).toList();check(marts.size()==209&&marts.stream().allMatch(d->d.exportSnapshotAt().toString().startsWith("2026-09-21")&&d.coverageGaps().contains("retained_restricted_snapshot_not_reverified")),"Restricted source dates/warnings were rewritten as new checks");
            var lore=loaded.characterLore();int count=0;for(var kind:CharacterBook.Kind.values())for(var choice:lore.choices(kind,"")){count++;check(lore.canReference(choice),"Current reference cannot be selected: "+choice.option().key());}check(count==73,"Rebuilt curated choice count differs");
            check(lore.choices(CharacterBook.Kind.MAGIC,"Zealotry").getFirst().availability()==CharacterLore.Availability.AVAILABLE,"New source-bound Zealotry evidence does not match");
            chars.edit(b->b.create("Rebuilt corpus spell fixture")).get();sheetId=chars.view().book().selectedId();final String id=sheetId;
            c.setScreen(()->new CharacterSheetScreen(new TitleScreen(),chars,id,library));c.clickScreenButton("Add lore");c.clickScreenButton("Magic");c.clickScreenButton("Choose lore");set(c,"Search lore options","Air Evocation");c.clickScreenButton("Air Evocation");set(c,"Tier","T2");c.clickScreenButton("Save entry");c.waitForScreen(CharacterSheetScreen.class);
            var sheet=chars.view().book().selected().orElseThrow();check(sheet.entries().getFirst().loreDocumentId().equals("lotc:topic:234439"),"Updated current lore could not be linked");
            var references=library.spells(sheet,"Gust",false).get(10,TimeUnit.SECONDS);check(references.hits().stream().anyMatch(h->h.ability().name().equals("GUST")&&h.ability().minimumTier()==1),"Actual Air Gust source/tier was not indexed");
            c.clickScreenButton("Available spells");c.waitForScreen(SpellBookScreen.class);set(c,"Search spells, effects and redlines","Gust");c.waitFor(m->m.screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).anyMatch(b->b.getMessage().getString().equals("GUST")&&b.active));c.takeScreenshot("rebuilt-air-spells");
            c.clickScreenButton("GUST");c.waitForScreen(SpellDetailScreen.class);c.waitFor(m->m.screen.getNarrationMessage().getString().contains("Recorded tier"));c.clickScreenButton("Bottom");c.waitTicks(3);c.takeScreenshot("rebuilt-gust-source-redlines");c.clickScreenButton("Practice rules");c.waitFor(m->m.screen.getNarrationMessage().getString().contains("[Image — view the forum source]"));c.runOnClient(m->check(!m.screen.getNarrationMessage().getString().contains("[Image:"),"Inline image identifier leaked into rendered source"));
            for(int n:new int[]{1,2}){c.runOnClient(m->{m.options.guiScale().set(n);m.resizeDisplay();});c.takeScreenshot("rebuilt-reference-scale-"+n);}c.setScreen(TitleScreen::new);
        }finally{c.setScreen(TitleScreen::new);c.runOnClient(m->{m.options.guiScale().set(scale);m.resizeDisplay();});}
        try(var restart=new LibraryService(root);var chars=new CharacterService(root)){check(restart.reload().get(30,TimeUnit.SECONDS).library().orElseThrow().catalogue().currentDocuments().size()==773,"Rebuilt statuses lost after restart");chars.reload().get();check(chars.view().book().selectedId().equals(sheetId)&&restart.spells(chars.view().book().selected().orElseThrow(),"Gust",false).get().eligible()>0,"Character/spell references did not survive restart");}
        LoggerFactory.getLogger("mclorehub-test").info("REBUILT_DATABASE_SCENARIO_PASS: real 801-record import, 773 normal references, 27 legacy/one shelved, 209 original-date MArts, 73 selectable curated references including Zealotry, Air T2 spell/redline workflow, restart and scales; no authenticated access claim");
    }
    private static void set(ClientGameTestContext c,String label,String value){c.runOnClient(m->m.screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).filter(e->e.getMessage().getString().equals(label)).findFirst().orElseThrow().setValue(value));}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
