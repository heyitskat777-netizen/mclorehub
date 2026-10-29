package net.mclorehub.client;

import net.mclorehub.catalogue.LoreCatalogue.Document;
import net.mclorehub.library.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.*;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.concurrent.*;

/** Bounded text layout (6000 source characters per section); original text is unchanged. */
public final class ReaderScreen extends Screen {
    private record Line(int offset,String text){}
    private final Screen parent;
    private final LibraryService service;
    private final Document document;
    private final boolean audit,details;
    private CompletableFuture<ReaderContent> loading;
    private ReaderContent content;
    private List<Line> lines=List.of();
    private int section=0,scroll=0,saveTicks=0;
    private boolean changed=false,dirty=false;
    private Button favorite;
    private PersonalState.ReadingPosition queuedPosition;
    private CompletableFuture<LibraryService.RefreshResult> updating;
    private Button refresh,undo;
    private long observedGeneration=-1;
    public ReaderScreen(Screen parent,LibraryService service,Document doc,boolean audit,boolean details){
        super(Component.literal(doc.title()));this.parent=parent;this.service=service;document=doc;this.audit=audit;this.details=details;
        loading=service.reader(doc,details);
    }
    public String documentId(){return document.id();}
    private int visibleLines(){return Math.max(1,(height-159)/11);}
    private void layout(int desiredOffset){
        if(content==null)return;var s=content.sections().get(section);List<Line> result=new ArrayList<>();
        font.getSplitter().splitLines(s.text(),Math.max(60,width-52),Style.EMPTY,false,(style,start,end)->{
            String text=s.text().substring(start,end).replace("\r","").replace("\n","");result.add(new Line(s.offset()+start,text));});
        if(result.isEmpty())result.add(new Line(s.offset(),""));lines=List.copyOf(result);scroll=0;
        for(int i=0;i<lines.size();i++){if(lines.get(i).offset()<=desiredOffset)scroll=i;else break;}
        scroll=Math.min(scroll,Math.max(0,lines.size()-visibleLines()));
    }
    @Override protected void init(){
        int desired=lines.isEmpty()?0:lines.get(Math.min(scroll,lines.size()-1)).offset();layout(desired);
        int col=(width-40)/3;
        favorite=addRenderableWidget(Button.builder(Component.translatable(details?"screen.mclorehub.copy_source":"screen.mclorehub.favorite"),b->{
            if(details)minecraft.keyboardHandler.setClipboard(document.url());else service.toggleFavorite(document.id());})
                .bounds(16,42,col,20).build());
        if(!details)addRenderableWidget(Button.builder(Component.translatable("screen.mclorehub.details"),b->minecraft.setScreen(new ReaderScreen(this,service,document,audit,true)))
                .bounds(20+col,42,col,20).build());
        else undo=addRenderableWidget(Button.builder(Component.translatable("screen.mclorehub.undo_refresh"),b->updating=service.rollback(document.id()))
                .bounds(20+col,42,col,20).build());
        refresh=addRenderableWidget(Button.builder(Component.translatable("screen.mclorehub.refresh_topic"),b->{
            if(service.refreshStatus().phase()==LibraryService.RefreshPhase.FETCHING)service.cancelRefresh();else updating=service.refresh(document.id());
        }).bounds(24+col*2,42,col,20).build());
        Button previous=addRenderableWidget(Button.builder(Component.translatable("screen.mclorehub.previous"),b->page(-1)).bounds(16,height-54,80,20).build());
        Button next=addRenderableWidget(Button.builder(Component.translatable("screen.mclorehub.next"),b->page(1)).bounds(width-96,height-54,80,20).build());
        previous.active=content!=null&&section>0;next.active=content!=null&&section+1<content.sections().size();
        addRenderableWidget(Button.builder(Component.translatable("gui.back"),b->onClose()).bounds((width-120)/2,height-28,120,20).build());
        updateFavorite();
    }
    private void updateFavorite(){
        if(favorite==null)return;var view=service.view();
        if(!details){favorite.active=view.writable();favorite.setMessage(Component.translatable(view.personal().favorites().contains(document.id())?"screen.mclorehub.unfavorite":"screen.mclorehub.favorite"));}
        if(!view.message().isEmpty())favorite.setTooltip(Tooltip.create(Component.literal(view.message())));
        if(refresh!=null){var operation=service.refreshStatus();boolean own=operation.documentId().equals(document.id());
            refresh.active=operation.phase()!=LibraryService.RefreshPhase.SAVING&&(operation.phase()==LibraryService.RefreshPhase.IDLE||own);
            refresh.setMessage(Component.translatable(operation.phase()==LibraryService.RefreshPhase.FETCHING&&own?"screen.mclorehub.cancel_refresh":"screen.mclorehub.refresh_topic"));
            refresh.setTooltip(Tooltip.create(Component.literal("Fetch only this forum topic and its reply pages; save changes locally.")));}
        if(undo!=null)undo.active=service.canRollback(document.id())&&service.refreshStatus().phase()==LibraryService.RefreshPhase.IDLE;
    }
    private void page(int delta){
        if(content==null)return;section=Math.clamp(section+delta,0,content.sections().size()-1);scroll=0;lines=List.of();rebuildWidgets();dirty=true;
    }
    private void scroll(int delta){scroll=Math.clamp(scroll+delta,0,Math.max(0,lines.size()-visibleLines()));dirty=true;}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){scroll((int)-Math.signum(vertical)*3);return true;}
    @Override public boolean keyPressed(KeyEvent event){
        switch(event.key()){
            case GLFW.GLFW_KEY_DOWN -> {scroll(1);return true;}
            case GLFW.GLFW_KEY_UP -> {scroll(-1);return true;}
            case GLFW.GLFW_KEY_PAGE_DOWN -> {if(scroll>=Math.max(0,lines.size()-visibleLines()))page(1);else scroll(visibleLines());return true;}
            case GLFW.GLFW_KEY_PAGE_UP -> {if(scroll==0)page(-1);else scroll(-visibleLines());return true;}
            case GLFW.GLFW_KEY_HOME -> {scroll(-lines.size());return true;}
            case GLFW.GLFW_KEY_END -> {scroll(lines.size());return true;}
            default -> {return super.keyPressed(event);}
        }
    }
    @Override public void tick(){
        if(updating!=null&&updating.isDone()){
            try{updating.join();updating=null;service.view().library().flatMap(l->l.catalogue().documents().stream().filter(d->d.id().equals(document.id())).findFirst())
                    .ifPresent(d->minecraft.setScreen(new ReaderScreen(parent,service,d,audit,details)));return;
            }catch(CompletionException|CancellationException ignored){updating=null;}
        }
        if(observedGeneration!=service.view().generation()){
            observedGeneration=service.view().generation();var latest=service.view().library().flatMap(l->l.catalogue().documents().stream().filter(d->d.id().equals(document.id())).findFirst());
            if(latest.isPresent()&&(!latest.get().fingerprint().equals(document.fingerprint())||!latest.get().trust().equals(document.trust())||latest.get().status()!=document.status())){
                minecraft.setScreen(new ReaderScreen(parent,service,latest.get(),audit,details));return;}
        }
        if(loading!=null&&loading.isDone()){
            try{content=loading.join();var resume=details?new ReaderContent.Resume(0,0,false):content.resume(document,service.view().personal().reading());
                section=resume.section();changed=resume.changed();layout(resume.offset());rebuildWidgets();dirty=true;
            }catch(CompletionException e){changed=true;}loading=null;
        }
        updateFavorite();if(dirty&&++saveTicks>=10){save();saveTicks=0;}
    }
    private void save(){
        if(details||content==null||lines.isEmpty())return;
        var s=content.sections().get(section);int offset=lines.get(scroll).offset();
        var position=new PersonalState.ReadingPosition(document.id(),document.fingerprint(),s.blockId(),offset,audit||!LoreSearch.isCurrent(document));
        if(!position.equals(queuedPosition)&&!service.view().personal().reading().equals(Optional.of(position))){queuedPosition=position;service.remember(position);}dirty=false;
    }
    @Override public void removed(){save();}
    @Override public void onClose(){save();minecraft.setScreen(parent);}
    @Override public Component getNarrationMessage(){
        var result=Component.empty().append(title).append(". ").append(status());
        var operation=service.refreshStatus();if(operation.documentId().equals(document.id()))result.append(". ").append(operation.message());
        if(content!=null){result.append(". ").append(content.sections().get(section).heading());
            for(int i=scroll;i<Math.min(lines.size(),scroll+visibleLines());i++)result.append(" ").append(lines.get(i).text());}
        return result;
    }
    private String status(){return (details?"Source and review details • ":audit?"AUDIT • ":"")+ReaderContent.statusLabel(document.status())+" • "+ReaderContent.reviewLabel(document.reviewState());}
    @Override public void render(GuiGraphics g,int x,int y,float delta){
        g.drawCenteredString(font,font.plainSubstrByWidth(title.getString(),width-32),width/2,10,0xFFFFFFFF);
        g.drawCenteredString(font,font.plainSubstrByWidth(status(),width-32),width/2,27,LoreSearch.isCurrent(document)?0xFFB3DBBC:0xFFFFC477);
        var operation=service.refreshStatus();
        String warning=operation.documentId().equals(document.id())&&!operation.message().isEmpty()?operation.message():changed?Component.translatable("screen.mclorehub.revision_changed").getString():!service.view().writable()?Component.translatable("screen.mclorehub.read_only").getString():
                Component.translatable("screen.mclorehub.reader_hint").getString();
        g.drawCenteredString(font,font.plainSubstrByWidth(warning,width-32),width/2,69,0xFFBAC9D8);
        if(operation.documentId().equals(document.id())&&!operation.message().isEmpty()&&y>=67&&y<81)g.setTooltipForNextFrame(Component.literal(operation.message()),x,y);
        g.fill(16,84,width-16,height-62,0xC0151B24);
        if(content!=null){
            g.drawString(font,font.plainSubstrByWidth(content.sections().get(section).heading(),width-48),24,89,0xFFBAC9D8);
            g.enableScissor(20,102,width-20,height-64);
            for(int i=scroll;i<Math.min(lines.size(),scroll+visibleLines());i++)g.drawString(font,Component.literal(lines.get(i).text()),24,103+(i-scroll)*11,0xFFE1E6EE);
            g.disableScissor();
            String label="Section "+(section+1)+"/"+content.sections().size()+" • Line "+(scroll+1)+"/"+lines.size();
            g.drawCenteredString(font,font.plainSubstrByWidth(label,width-204),width/2,height-47,0xFFFFFFFF);
        }else g.drawCenteredString(font,Component.translatable(loading==null?"screen.mclorehub.reader_failed":"screen.mclorehub.loading"),width/2,110,0xFFBAC9D8);
        super.render(g,x,y,delta);
    }
}
