package net.mclorehub.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.mclorehub.character.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.TitleScreen;
import org.slf4j.LoggerFactory;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("UnstableApiUsage")
public final class CharacterClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context){try{scenario(context);}catch(Exception e){throw new AssertionError("Character scenario failed",e);}}
    private void scenario(ClientGameTestContext context)throws Exception{
        context.waitForScreen(TitleScreen.class);Path root=Files.createTempDirectory("mclorehub-characters-test-");
        int scale=context.computeOnClient(c->c.options.guiScale().get());
        try(var service=new CharacterService(root)){
            service.reload().get(5,TimeUnit.SECONDS);context.runOnClient(c->{c.options.guiScale().set(2);c.resizeDisplay();});
            context.setScreen(()->new CharacterListScreen(new TitleScreen(),service));context.clickScreenButton("New character");
            set(context,"Character name","Synthetic character");set(context,"Character notes","User notes, not actual lore");context.clickScreenButton("Save character");
            context.waitFor(c->c.screen instanceof CharacterSheetScreen);String id=service.view().book().selectedId();
            for(var kind:CharacterBook.Kind.values()){
                context.clickScreenButton("Add lore");context.clickScreenButton(AddLoreScreen.label(kind));set(context,"Entry name","Synthetic "+kind);
                if(kind==CharacterBook.Kind.MAGIC){set(context,"Branch","Test branch");set(context,"Tier","T2");}
                set(context,"Application URL","https://www.lordofthecraft.net/forums/topic/123-synthetic/");set(context,"Entry notes","Synthetic user-recorded status only");
                context.clickScreenButton("Recorded status: not recorded");context.clickScreenButton("Recorded status: pending");context.takeScreenshot("phase5-entry-"+kind);
                context.clickScreenButton("Save entry");context.waitFor(c->c.screen instanceof CharacterSheetScreen);
            }
            check(service.view().book().selected().orElseThrow().entries().size()==3,"Entries not saved");
            context.takeScreenshot("phase5-character-sheet");context.clickScreenButton("Next");context.takeScreenshot("phase5-character-sheet-page-2");
            context.clickScreenButton("Edit name / notes");set(context,"Character name","Renamed fixture");context.clickScreenButton("Save character");
            context.waitFor(c->c.screen instanceof CharacterSheetScreen&&c.screen.getNarrationMessage().getString().contains("Renamed fixture"));
            context.clickScreenButton("Archive character");context.clickScreenButton("gui.yes");context.waitFor(c->c.screen instanceof CharacterListScreen);
            context.waitFor(c->service.view().book().sheets().getFirst().archived());context.clickScreenButton("Active characters");context.clickScreenButton("Renamed fixture");
            context.clickScreenButton("Restore character");context.waitFor(c->!service.view().book().sheets().getFirst().archived());
            context.clickScreenButton("Back");context.clickScreenButton("Archived characters");context.clickScreenButton("New character");set(context,"Character name","Second fixture");
            context.clickScreenButton("Save character");context.waitFor(c->c.screen instanceof CharacterSheetScreen);
            check(service.view().book().sheets().size()==2,"Multiple characters lost");context.setScreen(TitleScreen::new);
            var reloaded=new CharacterStore(root.resolve("personal/characters.json")).load().book();
            check(reloaded.sheets().getFirst().entries().stream().allMatch(e->e.status()==CharacterBook.RecordedStatus.APPROVED),"Recorded statuses lost");
            check(reloaded.sheets().getFirst().id().equals(id),"Rename changed stable ID");
        }finally{context.runOnClient(c->{c.options.guiScale().set(scale);c.resizeDisplay();});}
        LoggerFactory.getLogger("mclorehub-test").info("CHARACTER_SCENARIO_PASS: multiple sheets, magic branch/tier, separate CA/FA, links, recorded statuses, rename/archive/restore, persistence");
    }
    private static void set(ClientGameTestContext context,String label,String value){context.runOnClient(c->c.screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast)
            .filter(e->e.getMessage().getString().equals(label)).findFirst().orElseThrow().setValue(value));}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
