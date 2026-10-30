package net.mclorehub.client;

import net.mclorehub.catalogue.LoreCatalogue.Document;
import net.mclorehub.library.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.*;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.concurrent.*;

/** Continuous reading with a bounded cache of source-anchored font layouts. */
public final class ReaderScreen extends Screen {
    private record Line(int offset,String text){}
    private final Screen parent;
    private final LibraryService service;
    private final Document document;
    private final boolean audit,details,discussion;
    private CompletableFuture<ReaderContent> loading;
    private ReaderContent content;
    private record At(int section,int line){}
    private final Map<Integer,List<Line>> layouts=new LinkedHashMap<>(16,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<Integer,List<Line>> e){return size()>16;}
    };
    private int layoutWidth=-1;
    private boolean centered=true,draggingScrollbar=false,atDocumentEnd=false;
    private int section=0,scroll=0,saveTicks=0;
    private boolean changed=false,dirty=false;
    private Button favorite;
    private PersonalState.ReadingPosition queuedPosition;
    private CompletableFuture<LibraryService.RefreshResult> updating;
    private Button refresh,undo;
    private long observedGeneration=-1;
    public ReaderScreen(Screen parent,LibraryService service,Document doc,boolean audit,boolean details){
        this(parent,service,doc,audit,details,false);
    }
    private ReaderScreen(Screen parent,LibraryService service,Document doc,boolean audit,boolean details,boolean discussion){
        super(Component.literal(doc.title()));this.parent=parent;this.service=service;document=doc;this.audit=audit;this.details=details;this.discussion=discussion;
        loading=discussion?service.archivedReplies(doc):service.reader(doc,details);
    }
    public String documentId(){return document.id();}
    private int visibleLines(){return Math.max(1,(height-159)/11);}
    private List<Line> lines(int index){
        return layouts.computeIfAbsent(index,key->{
            var s=content.sections().get(key);List<Line> result=new ArrayList<>();
            for(int start=0;start<s.text().length();){
                int newline=s.text().indexOf('\n',start),end=newline<0?s.text().length():newline;
                String paragraph=s.text().substring(start,end).replace("\r","");int origin=start;
                if(!paragraph.isBlank()&&!Set.of("Spoiler","Hide contents","Reveal hidden contents").contains(paragraph.strip())){
                    // Image placeholders contain no readable image content. Keep the source unchanged.
                    boolean image=paragraph.strip().matches("(?is)\\[Image:[^\\]]*\\]");
                    if(image)result.add(new Line(s.offset()+origin,"[Image — view the forum source]"));
                    else font.getSplitter().splitLines(paragraph,layoutWidth,Style.EMPTY,false,(style,from,to)->
                            result.add(new Line(s.offset()+origin+from,paragraph.substring(from,to))));
                }
                if(newline>=0&&(result.isEmpty()||!result.getLast().text().isEmpty()))result.add(new Line(s.offset()+end,""));
                start=end+1;
            }
            if(result.isEmpty())result.add(new Line(s.offset(),""));return List.copyOf(result);
        });
    }
    private At at(){return new At(section,scroll);}
    private At move(At start,int delta){
        int part=start.section(),line=start.line();
        while(delta>0){int remaining=lines(part).size()-1-line;if(delta<=remaining)return new At(part,line+delta);
            if(part+1>=content.sections().size())return new At(part,lines(part).size()-1);
            delta-=remaining+1;part++;line=0;
        }
        while(delta<0){if(-delta<=line)return new At(part,line+delta);if(part==0)return new At(0,0);
            delta+=line+1;part--;line=lines(part).size()-1;}
        return new At(part,line);
    }
    private At endTop(){int last=content.sections().size()-1;return move(new At(last,lines(last).size()-1),-(visibleLines()-1));}
    private void position(At p){
        At end=endTop();if(p.section()>end.section()||p.section()==end.section()&&p.line()>end.line())p=end;
        section=p.section();scroll=p.line();atDocumentEnd=p.equals(end);dirty=true;
    }
    private void layout(int desiredOffset){
        if(content==null)return;
        int available=Math.max(60,Math.min(640,width-80));if(layoutWidth!=available){layoutWidth=available;layouts.clear();}
        var current=lines(section);int line=0;for(int i=0;i<current.size();i++){if(current.get(i).offset()<=desiredOffset)line=i;else break;}
        position(new At(section,line));
    }
    private List<Line> visible(){
        if(content==null)return List.of();List<Line> result=new ArrayList<>();At p=at();
        for(int i=0;i<visibleLines();i++){result.add(lines(p.section()).get(p.line()));At next=move(p,1);if(next.equals(p))break;p=next;}
        return result;
    }
    @Override protected void init(){
        boolean wasBottom=atDocumentEnd;
        int desired=content==null?0:lines(section).get(scroll).offset();layout(desired);if(wasBottom)bottom();
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
        addRenderableWidget(Button.builder(Component.literal("Top"),b->top()).bounds(16,height-54,80,20).build());
        addRenderableWidget(Button.builder(Component.literal("Bottom"),b->bottom()).bounds(width-96,height-54,80,20).build());
        if(!details)addRenderableWidget(Button.builder(Component.literal(centered?"Text: centered":"Text: left"),b->{
            centered=!centered;b.setMessage(Component.literal(centered?"Text: centered":"Text: left"));
        }).bounds(16,height-28,col,20).build());
        if(details&&!document.posts().isEmpty())addRenderableWidget(Button.builder(Component.translatable("screen.mclorehub.archived_replies"),b->minecraft.setScreen(new ReaderScreen(this,service,document,audit,false,true)))
                .bounds(16,height-28,col,20).build());
        if(details)addRenderableWidget(Button.builder(Component.literal("Compare changes"),b->minecraft.setScreen(new ReportScreen(this,"Main lore changes",service.comparison(document.id())))).bounds(24+col*2,height-28,col,20).build()).active=service.canRollback(document.id());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"),b->onClose()).bounds(20+col,height-28,col,20).build());
        updateFavorite();
    }
    private void updateFavorite(){
        if(favorite==null)return;var view=service.view();
        if(!details){favorite.active=view.writable();favorite.setMessage(Component.translatable(view.personal().favorites().contains(document.id())?"screen.mclorehub.unfavorite":"screen.mclorehub.favorite"));}
        if(!view.message().isEmpty())favorite.setTooltip(Tooltip.create(Component.literal(view.message())));
        if(refresh!=null){var operation=service.refreshStatus();boolean own=operation.documentId().equals(document.id());
            refresh.active=!discussion&&operation.phase()!=LibraryService.RefreshPhase.SAVING&&(operation.phase()==LibraryService.RefreshPhase.IDLE||own);
            refresh.setMessage(Component.translatable(operation.phase()==LibraryService.RefreshPhase.FETCHING&&own?"screen.mclorehub.cancel_refresh":"screen.mclorehub.refresh_topic"));
            refresh.setTooltip(Tooltip.create(Component.literal("Compare the saved main lore with the forum's first post; save changes locally. Discussion is excluded.")));}
        if(undo!=null)undo.active=service.canRollback(document.id())&&service.refreshStatus().phase()==LibraryService.RefreshPhase.IDLE;
    }
    private void top(){if(content!=null)position(new At(0,0));}
    private void bottom(){if(content!=null)position(endTop());}
    private void scroll(int delta){if(content!=null)position(move(at(),delta));}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){scroll((int)-Math.signum(vertical)*3);return true;}
    private double progress(){
        if(content==null)return 0;if(at().equals(endTop()))return 1;
        long before=0,total=0;for(int i=0;i<content.sections().size();i++){int size=content.sections().get(i).text().length();if(i<section)before+=size;total+=size;}
        before+=lines(section).get(scroll).offset()-content.sections().get(section).offset();return total==0?0:(double)before/total;
    }
    private void seek(double fraction){
        if(content==null)return;if(fraction<=0){top();return;}if(fraction>=1){bottom();return;}
        long total=content.sections().stream().mapToLong(s->s.text().length()).sum(),target=(long)(total*fraction);
        section=0;while(section+1<content.sections().size()&&target>=content.sections().get(section).text().length())target-=content.sections().get(section++).text().length();
        layout(content.sections().get(section).offset()+(int)target);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        if(event.button()==0&&event.x()>=width-29&&event.x()<=width-17&&event.y()>=102&&event.y()<=height-65){
            draggingScrollbar=true;seek((event.y()-102)/Math.max(1,height-167));return true;
        }return super.mouseClicked(event,doubleClick);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy){
        if(draggingScrollbar){seek((event.y()-102)/Math.max(1,height-167));return true;}return super.mouseDragged(event,dx,dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event){if(draggingScrollbar){draggingScrollbar=false;return true;}return super.mouseReleased(event);}
    @Override public boolean keyPressed(KeyEvent event){
        switch(event.key()){
            case GLFW.GLFW_KEY_DOWN -> {scroll(1);return true;}
            case GLFW.GLFW_KEY_UP -> {scroll(-1);return true;}
            case GLFW.GLFW_KEY_PAGE_DOWN -> {scroll(Math.max(1,visibleLines()-1));return true;}
            case GLFW.GLFW_KEY_PAGE_UP -> {scroll(-Math.max(1,visibleLines()-1));return true;}
            case GLFW.GLFW_KEY_HOME -> {top();return true;}
            case GLFW.GLFW_KEY_END -> {bottom();return true;}
            default -> {return super.keyPressed(event);}
        }
    }
    @Override public void tick(){
        if(updating!=null&&updating.isDone()){
            try{updating.join();updating=null;service.view().library().flatMap(l->l.catalogue().documents().stream().filter(d->d.id().equals(document.id())).findFirst())
                    .ifPresent(d->minecraft.setScreen(new ReaderScreen(parent,service,d,audit,details,discussion)));return;
            }catch(CompletionException|CancellationException ignored){updating=null;}
        }
        if(observedGeneration!=service.view().generation()){
            observedGeneration=service.view().generation();var latest=service.view().library().flatMap(l->l.catalogue().documents().stream().filter(d->d.id().equals(document.id())).findFirst());
            if(latest.isPresent()&&(!latest.get().fingerprint().equals(document.fingerprint())||!latest.get().trust().equals(document.trust())||latest.get().status()!=document.status())){
                minecraft.setScreen(new ReaderScreen(parent,service,latest.get(),audit,details,discussion));return;}
        }
        if(loading!=null&&loading.isDone()){
            try{content=loading.join();var resume=details||discussion?new ReaderContent.Resume(0,0,false):content.resume(document,service.view().personal().reading());
                section=resume.section();changed=resume.changed();layouts.clear();layout(resume.offset());dirty=true;
            }catch(CompletionException e){changed=true;}loading=null;
        }
        updateFavorite();if(dirty&&++saveTicks>=10){save();saveTicks=0;}
    }
    private void save(){
        if(details||discussion||content==null)return;
        var s=content.sections().get(section);int offset=lines(section).get(scroll).offset();
        var position=new PersonalState.ReadingPosition(document.id(),document.fingerprint(),s.blockId(),offset,audit||!LoreSearch.isCurrent(document));
        if(!position.equals(queuedPosition)&&!service.view().personal().reading().equals(Optional.of(position))){queuedPosition=position;service.remember(position);}dirty=false;
    }
    @Override public void removed(){save();}
    @Override public void onClose(){save();minecraft.setScreen(parent);}
    @Override public Component getNarrationMessage(){
        var result=Component.empty().append(title).append(". ").append(status());
        var operation=service.refreshStatus();if(operation.documentId().equals(document.id()))result.append(". ").append(operation.message());
        if(content!=null){result.append(". ").append(content.sections().get(section).heading());
            for(var line:visible())result.append(" ").append(line.text());}
        return result;
    }
    private String status(){return discussion?"Archived discussion • Not part of main lore":(details?"Source and review details • ":audit?"AUDIT • ":"")+ReaderContent.statusLabel(document.status())+" • "+ReaderContent.reviewLabel(document.reviewState());}
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
            g.drawCenteredString(font,font.plainSubstrByWidth(content.sections().get(section).heading(),width-48),width/2,89,0xFFBAC9D8);
            g.enableScissor(24,102,width-32,height-64);int yLine=103;
            for(var line:visible()){
                if(centered)g.drawCenteredString(font,line.text(),width/2,yLine,0xFFE1E6EE);
                else g.drawString(font,line.text(),(width-layoutWidth)/2,yLine,0xFFE1E6EE);yLine+=11;
            }
            g.disableScissor();
            int track=Math.max(12,height-167),thumb=Math.min(track,Math.max(12,track/8));int thumbY=102+(int)((track-thumb)*progress());
            g.fill(width-27,102,width-21,102+track,0xFF263342);g.fill(width-27,thumbY,width-21,thumbY+thumb,0xFFB5C7D7);
            String label=Math.round(progress()*100)+"% • Continuous scroll";
            g.drawCenteredString(font,font.plainSubstrByWidth(label,width-204),width/2,height-47,0xFFFFFFFF);
        }else g.drawCenteredString(font,Component.translatable(loading==null?"screen.mclorehub.reader_failed":"screen.mclorehub.loading"),width/2,110,0xFFBAC9D8);
        super.render(g,x,y,delta);
    }
}
