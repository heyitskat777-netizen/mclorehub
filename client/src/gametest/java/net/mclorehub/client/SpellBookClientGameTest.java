package net.mclorehub.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.mclorehub.catalogue.LoreCatalogue;
import net.mclorehub.character.*;
import net.mclorehub.importer.ExportImporter;
import net.mclorehub.library.*;
import net.mclorehub.updates.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.mclorehub.catalogue.SourceSection;
import org.lwjgl.glfw.GLFW;
import org.slf4j.LoggerFactory;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static net.mclorehub.importer.JsonSupport.*;

/** Real input, source context and tier changes; fixture text is never server lore. */
@SuppressWarnings("UnstableApiUsage")
public final class SpellBookClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c){try{scenario(c);}catch(Exception e){throw new AssertionError("Spell book failed",e);}}
    private void scenario(ClientGameTestContext c)throws Exception{
        c.waitForScreen(TitleScreen.class);Path root=Files.createTempDirectory("mclorehub-spells-");LibraryClientGameTest.fixture(root);
        var export=read(root.resolve("synthetic.json"),1<<20);var record=export.getAsJsonArray("records").get(0).getAsJsonObject();
        String body="Practice rules\nAll spells require concentration.\nSpark [T1]\nA synthetic light, never actual lore.\nRedlines\nNever blinds or burns.\nStorm [T3]\nA synthetic storm.\n";
        List<SourceSection> outline=new ArrayList<>();for(String label:List.of("Practice rules","Spark [T1]","Redlines","Storm [T3]")){int pos=body.indexOf(label);outline.add(new SourceSection(label,pos,pos+label.length(),label.equals("Practice rules")?2:label.equals("Redlines")?4:3));}
        record.addProperty("content_text",body);record.addProperty("content_sha256",hash(body));record.add("source_outline",JSON.toJsonTree(outline));
        Path source=root.resolve("spell-fixture.json"),pack=root.resolve("packs/active/pack.json");writeNew(source,export);Files.writeString(pack,JSON.toJson(new ExportImporter().importFile(source).pack()));Files.delete(pack.resolveSibling("reviews.json"));
        writeNew(pack.resolveSibling("trust.json"),BaselineTrust.manifest(LoreCatalogue.load(pack,null),Instant.now()));
        int scale=c.computeOnClient(m->m.options.guiScale().get());
        TopicFetcher transport=d->{String added="\nComet [T4]\nAn updated synthetic ability.\n";var outlineAfter=new ArrayList<>(d.outline());int pos=d.body().length()+1;outlineAfter.add(new SourceSection("Comet [T4]",pos,pos+10,3));return new TopicSnapshot(d.id(),d.url(),d.title(),d.author().orElse(""),d.body()+added,d.posts(),List.of(d.url()),Instant.now(),Optional.empty(),List.of(),outlineAfter);};
        try(var library=new LibraryService(root,transport);var chars=new CharacterService(root)){
            library.reload().get(10,TimeUnit.SECONDS);chars.reload().get(5,TimeUnit.SECONDS);chars.edit(b->b.create("Spell fixture")).get();String id=chars.view().book().selectedId();
            var entry=new CharacterBook.Entry(UUID.randomUUID().toString(),CharacterBook.Kind.MAGIC,"Fixture magic","","T2","",CharacterBook.RecordedStatus.NOT_RECORDED,"","lotc:topic:123");chars.edit(b->b.putEntry(id,entry)).get();
            var doc=library.view().library().orElseThrow().catalogue().documents().getFirst();var resume=new PersonalState.ReadingPosition(doc.id(),doc.fingerprint(),"body",42,false);library.remember(resume).get();
            c.runOnClient(m->{m.options.guiScale().set(2);m.resizeDisplay();});
            c.setScreen(()->new CharacterSheetScreen(new TitleScreen(),chars,id,library));
            c.runOnClient(m->{check(has(m.screen,"Add lore")&&!has(m.screen,"Add magic"),"Sheet needs a unified Add lore action");});c.clickScreenButton("Add lore");c.waitForScreen(AddLoreScreen.class);
            for(var kind:CharacterBook.Kind.values())c.runOnClient(m->check(has(m.screen,AddLoreScreen.label(kind)),"Missing entry type"));c.takeScreenshot("spell-unified-add-lore");c.clickScreenButton("Back");
            c.clickScreenButton("Available spells");c.waitForScreen(SpellBookScreen.class);c.waitFor(m->has(m.screen,"SPARK"));c.runOnClient(m->check(!has(m.screen,"STORM"),"Above-tier spell leaked into recorded tier list"));
            EditBox editor=c.computeOnClient(m->edit(m.screen));c.runOnClient(m->{m.screen.setFocused(editor);editor.setFocused(true);});
            for(char ch:"blinds".toCharArray()){c.getInput().typeChars(String.valueOf(ch));c.waitTicks(3);}c.waitFor(m->has(m.screen,"SPARK"));
            c.runOnClient(m->{check(edit(m.screen)==editor&&editor.getValue().equals("blinds")&&editor.getCursorPosition()==6,"Spell search reset input/caret");editor.setCursorPosition(2);editor.setHighlightPos(2);});
            c.getInput().typeChars("X");c.waitTicks(4);c.runOnClient(m->check(editor.getValue().equals("blXinds")&&editor.getCursorPosition()==3,"Middle spell query edit lost"));c.getInput().pressKey(GLFW.GLFW_KEY_BACKSPACE);c.waitFor(m->has(m.screen,"SPARK"));c.takeScreenshot("spell-search-redline");
            c.clickScreenButton("SPARK");c.waitForScreen(SpellDetailScreen.class);c.waitFor(m->m.screen.getNarrationMessage().getString().contains("Recorded tier"));c.clickScreenButton("Bottom");c.waitFor(m->m.screen.getNarrationMessage().getString().contains("Never blinds or burns."));c.takeScreenshot("spell-exact-redlines");
            c.clickScreenButton("Practice rules");c.waitFor(m->m.screen.getNarrationMessage().getString().contains("All spells require concentration."));c.clickScreenButton("Copy source URL");c.runOnClient(m->check(m.keyboardHandler.getClipboard().equals(doc.url()),"Wrong spell source URL"));c.waitTicks(12);
            check(library.view().personal().reading().orElseThrow().equals(resume),"Quick reference overwrote wiki resume");
            for(int n:new int[]{1,2}){c.runOnClient(m->{m.options.guiScale().set(n);m.resizeDisplay();bounds(m.screen);});c.takeScreenshot("spell-reader-scale-"+n);}c.clickScreenButton("Back");
            c.runOnClient(m->edit(m.screen).setValue(""));c.waitFor(m->has(m.screen,"SPARK"));c.clickScreenButton("By recorded tier");c.waitFor(m->has(m.screen,"STORM"));
            check(library.spells(chars.view().book().selected().orElseThrow(),"Storm",true).get().hits().stream().anyMatch(h->h.access()==net.mclorehub.spell.SpellIndex.Access.ABOVE_TIER),"All tiers lost locked requirement");
            var promoted=new CharacterBook.Entry(entry.id(),entry.kind(),entry.name(),entry.branch(),"T3",entry.applicationUrl(),entry.status(),entry.notes(),entry.loreDocumentId());chars.edit(b->b.putEntry(id,promoted)).get();c.clickScreenButton("All linked tiers");c.waitFor(m->has(m.screen,"STORM"));c.waitFor(m->m.screen.getNarrationMessage().getString().contains("2 within recorded tier"));c.takeScreenshot("spell-tier-change");
            c.clickScreenButton("STORM");c.waitForScreen(SpellDetailScreen.class);library.refresh(doc.id()).get(20,TimeUnit.SECONDS);c.waitForScreen(SpellBookScreen.class);
            check(library.spells(chars.view().book().selected().orElseThrow(),"Comet",true).get().hits().stream().anyMatch(h->h.ability().name().equals("COMET")&&h.ability().minimumTier()==4),"Source refresh reused old spell index");c.setScreen(TitleScreen::new);
            check(new CharacterStore(root.resolve("personal/characters.json")).load().book().selected().orElseThrow().entries().getFirst().tier().equals("T3"),"Spell tier edit was not persisted");
        }finally{c.setScreen(TitleScreen::new);c.runOnClient(m->{m.options.guiScale().set(scale);m.resizeDisplay();});}
        LoggerFactory.getLogger("mclorehub-test").info("SPELLBOOK_SCENARIO_PASS: unified Add lore, stable delayed search/caret, tier locks/change, exact redlines/shared practice rules, continuous reader, source URL, wiki resume isolation, refreshed index/stale detail invalidation, persistence and scales");
    }
    private static EditBox edit(Screen s){return s.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow();}
    private static boolean has(Screen s,String title){return s.children().stream().filter(Button.class::isInstance).map(Button.class::cast).anyMatch(b->b.getMessage().getString().equals(title));}
    private static void bounds(Screen s){s.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).forEach(w->check(w.getX()>=0&&w.getY()>=0&&w.getRight()<=s.width&&w.getBottom()<=s.height,"Clipped widget: "+w.getMessage().getString()));}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
