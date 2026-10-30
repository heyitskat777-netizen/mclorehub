package net.mclorehub.client;

import net.mclorehub.library.ReaderContent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.*;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.concurrent.*;

/** Paginated bounded read-only report; never overwrites the user's main reading position. */
final class ReportScreen extends Screen {
    private final Screen parent;private CompletableFuture<ReaderContent> loading;private ReaderContent content;private List<String> lines=List.of();private int page,scroll;private String error="";
    ReportScreen(Screen parent,String title,CompletableFuture<ReaderContent> loading){super(Component.literal(title));this.parent=parent;this.loading=loading;}
    private int visible(){return Math.max(1,(height-115)/11);}
    @Override protected void init(){
        if(content!=null){String text=content.sections().get(page).text();var wrapped=new ArrayList<String>();font.getSplitter().splitLines(text,Math.max(60,width-52),Style.EMPTY,false,(s,a,b)->wrapped.add(text.substring(a,b).replace("\n","").replace("\r","")));lines=List.copyOf(wrapped);scroll=Math.min(scroll,Math.max(0,lines.size()-visible()));}
        addRenderableWidget(Button.builder(Component.literal("Previous"),b->{page--;scroll=0;rebuildWidgets();}).bounds(16,height-54,80,20).build()).active=content!=null&&page>0;
        addRenderableWidget(Button.builder(Component.literal("Next"),b->{page++;scroll=0;rebuildWidgets();}).bounds(width-96,height-54,80,20).build()).active=content!=null&&page+1<content.sections().size();
        addRenderableWidget(Button.builder(Component.literal("Back"),b->onClose()).bounds((width-120)/2,height-28,120,20).build());
    }
    @Override public void tick(){if(loading!=null&&loading.isDone()){try{content=loading.join();}catch(CompletionException|CancellationException e){error="Report unavailable. Stored data retained.";}loading=null;rebuildWidgets();}}
    private void scroll(int n){scroll=Math.clamp(scroll+n,0,Math.max(0,lines.size()-visible()));}
    @Override public boolean mouseScrolled(double x,double y,double h,double v){scroll((int)-Math.signum(v)*3);return true;}
    @Override public boolean keyPressed(KeyEvent e){switch(e.key()){case GLFW.GLFW_KEY_DOWN->{scroll(1);return true;}case GLFW.GLFW_KEY_UP->{scroll(-1);return true;}case GLFW.GLFW_KEY_PAGE_DOWN->{scroll(visible());return true;}case GLFW.GLFW_KEY_PAGE_UP->{scroll(-visible());return true;}case GLFW.GLFW_KEY_HOME->{scroll(-lines.size());return true;}case GLFW.GLFW_KEY_END->{scroll(lines.size());return true;}default->{return super.keyPressed(e);}}}
    @Override public Component getNarrationMessage(){return Component.literal(title.getString()+". "+(content==null?error:content.sections().get(page).text()));}
    @Override public void render(GuiGraphics g,int x,int y,float d){g.drawCenteredString(font,title,width/2,10,0xFFFFFFFF);g.drawCenteredString(font,font.plainSubstrByWidth(content==null?"Wheel / arrows to scroll":content.sections().get(page).heading()+" • Scroll to read",width-32),width/2,29,0xFFBAC9D8);g.fill(16,48,width-16,height-62,0xC0151B24);g.enableScissor(20,51,width-20,height-64);
        for(int i=scroll;i<Math.min(lines.size(),scroll+visible());i++){String line=lines.get(i);g.drawString(font,Component.literal(line),24,53+(i-scroll)*11,line.startsWith("+ ")?0xFFB3DBBC:line.startsWith("- ")?0xFFFFC477:0xFFE1E6EE);}g.disableScissor();
        if(content==null)g.drawCenteredString(font,loading==null?error:"Loading…",width/2,66,0xFFFFC477);else g.drawCenteredString(font,(page+1)+" / "+content.sections().size(),width/2,height-47,0xFFFFFFFF);super.render(g,x,y,d);}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
