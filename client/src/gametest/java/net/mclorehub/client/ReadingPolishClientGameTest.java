package net.mclorehub.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.mclorehub.library.LibraryService;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.slf4j.LoggerFactory;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

/** Reproduces delayed keystrokes/caret edits and continuous movement through long lore. */
@SuppressWarnings("UnstableApiUsage")
public final class ReadingPolishClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c){try{scenario(c);}catch(Exception e){throw new AssertionError("Reading polish failed",e);}}
    private void scenario(ClientGameTestContext c)throws Exception {
        c.waitForScreen(TitleScreen.class);Path root=Files.createTempDirectory("mclorehub-reading-polish-");LibraryClientGameTest.fixture(root);
        int scale=c.computeOnClient(m->m.options.guiScale().get());
        try(var library=new LibraryService(root)){
            library.reload().get(10,TimeUnit.SECONDS);c.runOnClient(m->{m.options.guiScale().set(2);m.resizeDisplay();});
            c.setScreen(()->new WikiScreen(new TitleScreen(),library));c.waitFor(m->m.screen.getNarrationMessage().getString().contains("2 results"));
            EditBox original=c.computeOnClient(m->edit(m.screen));
            c.runOnClient(m->{m.screen.setFocused(original);original.setFocused(true);});
            for(char ch:"reference alias".toCharArray()){c.getInput().typeChars(String.valueOf(ch));c.waitTicks(3);}
            c.waitFor(m->m.screen.getNarrationMessage().getString().contains("1 results"));
            c.runOnClient(m->{check(edit(m.screen)==original,"Search editor replaced after async results");check(original.isFocused(),"Search lost focus");check(original.getValue().equals("reference alias"),"Delayed typing lost/reordered text");check(original.getCursorPosition()==15,"Caret reset after results");original.setCursorPosition(3);original.setHighlightPos(3);});
            c.getInput().typeChars("X");c.waitTicks(4);
            c.runOnClient(m->{check(original.getValue().equals("refXerence alias")&&original.getCursorPosition()==4,"Middle edit moved the caret");});
            c.getInput().pressKey(GLFW.GLFW_KEY_BACKSPACE);c.waitTicks(4);
            c.runOnClient(m->{original.setCursorPosition(0);original.setHighlightPos(9);});library.reload().get(10,TimeUnit.SECONDS);c.waitTicks(5);
            c.runOnClient(m->check(edit(m.screen)==original&&original.isFocused()&&original.getHighlighted().equals("reference"),"Library refresh destroyed editor selection"));
            c.getInput().typeChars("reference");c.waitTicks(4);c.takeScreenshot("polish-stable-search");
            c.clickScreenButton("All categories");c.waitForScreen(CategoryScreen.class);c.takeScreenshot("polish-category-chooser");c.clickScreenButton("Magic (1)");
            c.waitForScreen(WikiScreen.class);c.waitFor(m->m.screen.getNarrationMessage().getString().contains("1 results"));
            c.runOnClient(m->check(edit(m.screen).getValue().equals("reference alias"),"Category navigation cleared the query"));
            c.clickScreenButton("Synthetic reference — not server canon");c.waitFor(m->m.screen instanceof ReaderScreen&&m.screen.getNarrationMessage().getString().contains("Line 0"));
            c.runOnClient(m->{check(has(m.screen,"Text: centered"),"Reader must default to centered text");check(!has(m.screen,"Next"),"Lore still requires section pagination");for(int i=0;i<100;i++)m.screen.mouseScrolled(100,130,0,-1);});
            c.waitFor(m->library.view().personal().reading().map(p->p.offset()>6000).orElse(false));int forward=library.view().personal().reading().orElseThrow().offset();
            c.takeScreenshot("polish-continuous-cross-section");
            c.runOnClient(m->{for(int i=0;i<45;i++)m.screen.mouseScrolled(100,130,0,1);});
            c.waitFor(m->library.view().personal().reading().map(p->p.offset()>0&&p.offset()<forward).orElse(false));
            c.runOnClient(m->{
                var info=new MouseButtonInfo(0,0);var first=new MouseButtonEvent(m.screen.width-24,102,info);
                var last=new MouseButtonEvent(m.screen.width-24,m.screen.height-65,info);
                check(m.screen.mouseClicked(first,false),"Scrollbar did not capture click");
                check(m.screen.mouseDragged(last,0,m.screen.height-167),"Scrollbar did not capture drag");m.screen.mouseReleased(last);
            });
            c.waitFor(m->m.screen.getNarrationMessage().getString().contains("Line 179"));
            c.getInput().pressKey(GLFW.GLFW_KEY_END);c.waitFor(m->m.screen.getNarrationMessage().getString().contains("Line 179"));c.waitFor(m->library.view().personal().reading().map(p->p.offset()>9000).orElse(false));
            c.takeScreenshot("polish-reader-bottom");c.getInput().pressKey(GLFW.GLFW_KEY_HOME);c.waitFor(m->m.screen.getNarrationMessage().getString().contains("Line 0"));
            c.getInput().pressKey(GLFW.GLFW_KEY_END);c.waitFor(m->library.view().personal().reading().map(p->p.offset()>9000).orElse(false));
            for(int guiScale:new int[]{1,2}){c.runOnClient(m->{m.options.guiScale().set(guiScale);m.resizeDisplay();bounds(m.screen);});c.takeScreenshot("polish-reader-scale-"+guiScale);}
            c.clickScreenButton("Text: centered");c.runOnClient(m->check(has(m.screen,"Text: left"),"Alignment toggle failed"));c.setScreen(TitleScreen::new);
        }finally{c.setScreen(TitleScreen::new);c.runOnClient(m->{m.options.guiScale().set(scale);m.resizeDisplay();});}
        try(var restart=new LibraryService(root)){
            restart.reload().get(10,TimeUnit.SECONDS);check(restart.view().personal().reading().orElseThrow().offset()>9000,"Continuous position lost on restart");
            var doc=restart.view().library().orElseThrow().catalogue().documents().stream().filter(d->d.id().equals("lotc:topic:123")).findFirst().orElseThrow();
            c.setScreen(()->new ReaderScreen(new TitleScreen(),restart,doc,false,false));c.waitFor(m->m.screen.getNarrationMessage().getString().contains("Line 179"));c.setScreen(TitleScreen::new);
        }
        LoggerFactory.getLogger("mclorehub-test").info("READING_POLISH_SCENARIO_PASS: slow typing, caret/selection retention through async results/reload, category chooser, wheel crossing/reverse, scrollbar click/drag, true Home/End, alignment, scales and source-offset restart");
    }
    private static EditBox edit(Screen s){return s.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow();}
    private static boolean has(Screen s,String title){return s.children().stream().filter(Button.class::isInstance).map(Button.class::cast).anyMatch(b->b.getMessage().getString().equals(title));}
    private static void bounds(Screen s){s.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).forEach(w->check(w.getX()>=0&&w.getY()>=0&&w.getRight()<=s.width&&w.getBottom()<=s.height,"Widget clipped: "+w.getMessage().getString()));}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
