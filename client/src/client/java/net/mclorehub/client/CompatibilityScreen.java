package net.mclorehub.client;

import net.mclorehub.character.*;
import net.mclorehub.library.LibraryService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.*;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.concurrent.*;

final class CompatibilityScreen extends Screen {
    private final Screen parent;private final CharacterService characters;private final String sheetId;private final LibraryService library;
    private long characterGeneration=-1,libraryGeneration=-1;private CompletableFuture<CharacterLore.Report> loading;
    private CharacterLore.Report report;private int page,sourceIndex,scroll;private String error="";private List<String> lines=List.of();
    CompatibilityScreen(Screen parent,CharacterService characters,String sheetId,LibraryService library){
        super(Component.literal("Compatibility checks"));this.parent=parent;this.characters=characters;this.sheetId=sheetId;this.library=library;
    }
    private CharacterLore.Finding finding(){return report==null?null:report.findings().get(Math.min(page,report.findings().size()-1));}
    private CharacterLore.Source source(){var f=finding();return f==null||f.sources().isEmpty()?null:f.sources().get(Math.min(sourceIndex,f.sources().size()-1));}
    private String text(){
        var f=finding();if(f==null)return error.isEmpty()?"Loading checks…":error;
        var s=source();return f.outcome().name().replace('_',' ')+"\n\n"+f.message()+
                (s==null?"":"\n\n"+s.title()+"\nStored source: "+s.snapshot()+"\n"+s.url()+"\n\nMain-post evidence:\n"+String.join("\n\n",s.passages()))+
                "\n\nThese checks describe recorded entries, including pending or denied applications. They do not verify possession or server approval.";
    }
    private int visible(){return Math.max(1,(height-163)/11);}
    private void layout(){List<String> result=new ArrayList<>();String text=text();font.getSplitter().splitLines(text,Math.max(60,width-52),Style.EMPTY,false,(style,a,b)->result.add(text.substring(a,b).replace("\n","").replace("\r","")));
        lines=List.copyOf(result);scroll=Math.clamp(scroll,0,Math.max(0,lines.size()-visible()));}
    private Button button(String text,int x,int y,int w,Button.OnPress action){return addRenderableWidget(Button.builder(Component.literal(text),action).bounds(x,y,w,20).build());}
    @Override protected void init(){
        var f=finding();var s=source();int col=(width-40)/3;
        button(f==null||f.sources().isEmpty()?"No citation":"Source "+(sourceIndex+1)+" / "+f.sources().size(),16,42,col,b->{sourceIndex=(sourceIndex+1)%finding().sources().size();scroll=0;rebuildWidgets();}).active=f!=null&&f.sources().size()>1;
        button("Read source",20+col,42,col,b->{var chosen=source();if(chosen!=null)library.view().library().flatMap(l->l.characterLore().document(chosen.id()))
                .ifPresent(d->minecraft.setScreen(new ReaderScreen(this,library,d,!net.mclorehub.catalogue.LoreCatalogue.isCurrent(d),false)));})
                .active=s!=null&&library.view().library().flatMap(l->l.characterLore().document(s.id())).isPresent();
        button("Copy source URL",24+2*col,42,col,b->{if(source()!=null)minecraft.keyboardHandler.setClipboard(source().url());}).active=s!=null;
        button("Previous",16,height-54,80,b->{page--;sourceIndex=0;scroll=0;rebuildWidgets();}).active=report!=null&&page>0;
        button("Next",width-96,height-54,80,b->{page++;sourceIndex=0;scroll=0;rebuildWidgets();}).active=report!=null&&page+1<report.findings().size();
        button("Back",(width-120)/2,height-28,120,b->onClose());layout();
    }
    @Override public void tick(){
        if(loading==null&&(characterGeneration!=characters.view().generation()||libraryGeneration!=library.view().generation())){
            characterGeneration=characters.view().generation();libraryGeneration=library.view().generation();
            var sheet=characters.view().book().sheets().stream().filter(s->s.id().equals(sheetId)).findFirst();
            if(sheet.isPresent())loading=library.assess(sheet.get());else {error="Character unavailable.";report=null;rebuildWidgets();}
        }
        if(loading!=null&&loading.isDone()){
            try{var next=loading.join();if(characterGeneration==characters.view().generation()&&libraryGeneration==library.view().generation()){
                report=next;page=0;sourceIndex=0;scroll=0;error="";rebuildWidgets();}
            }catch(CompletionException|CancellationException e){report=null;error="Checks unavailable; your sheet is retained.";rebuildWidgets();}loading=null;
        }
    }
    private void scroll(int delta){scroll=Math.clamp(scroll+delta,0,Math.max(0,lines.size()-visible()));}
    @Override public boolean mouseScrolled(double x,double y,double h,double v){scroll((int)-Math.signum(v)*3);return true;}
    @Override public boolean keyPressed(KeyEvent e){switch(e.key()){
        case GLFW.GLFW_KEY_DOWN -> {scroll(1);return true;}
        case GLFW.GLFW_KEY_UP -> {scroll(-1);return true;}
        case GLFW.GLFW_KEY_PAGE_DOWN -> {scroll(visible());return true;}
        case GLFW.GLFW_KEY_PAGE_UP -> {scroll(-visible());return true;}
        case GLFW.GLFW_KEY_HOME -> {scroll(-lines.size());return true;}
        case GLFW.GLFW_KEY_END -> {scroll(lines.size());return true;}
        default -> {return super.keyPressed(e);}
    }}
    @Override public Component getNarrationMessage(){return Component.literal((report==null?"":report.summary()+". ")+text());}
    @Override public void render(GuiGraphics g,int x,int y,float d){
        g.drawCenteredString(font,title,width/2,10,0xFFFFFFFF);
        g.drawCenteredString(font,font.plainSubstrByWidth(report==null?"Partial checks • Not a server approval":report.summary(),width-32),width/2,27,0xFFFFC477);
        g.drawCenteredString(font,"Wheel / arrows to scroll • Next for more findings",width/2,69,0xFFBAC9D8);
        g.fill(16,84,width-16,height-62,0xC0151B24);g.enableScissor(20,88,width-20,height-64);
        for(int i=scroll;i<Math.min(lines.size(),scroll+visible());i++)g.drawString(font,Component.literal(lines.get(i)),24,90+(i-scroll)*11,0xFFE1E6EE);g.disableScissor();
        if(report!=null)g.drawCenteredString(font,(page+1)+" / "+report.findings().size(),width/2,height-47,0xFFFFFFFF);super.render(g,x,y,d);
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
}
