package net.mclorehub.client;

import net.mclorehub.character.*;
import net.mclorehub.library.LibraryService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import java.util.concurrent.*;

public final class CharacterSheetScreen extends Screen {
    private final Screen parent;private final CharacterService service;private final String id;private int page=0;private long generation=-1;
    private final LibraryService library;
    private CompletableFuture<CharacterLore.Report> checking;private long checkedBook=-1,checkedLibrary=-1,pendingBook,pendingLibrary;
    private String assessment="Compatibility: not assessed";
    CharacterSheetScreen(Screen parent,CharacterService service,String id,LibraryService library){super(Component.literal("Character sheet"));this.parent=parent;this.service=service;this.id=id;this.library=library;}
    private CharacterBook.Sheet sheet(){return service.view().book().sheets().stream().filter(s->s.id().equals(id)).findFirst().orElse(null);}
    private int size(){return Math.max(1,(height-210)/24);}
    private Button button(String text,int x,int y,int w,Button.OnPress action){return addRenderableWidget(Button.builder(Component.literal(text),action).bounds(x,y,w,20).build());}
    @Override protected void init(){var sheet=sheet();if(sheet==null){button("Back",16,height-28,120,b->onClose());return;}
        boolean writable=service.view().writable()&&!sheet.archived();int half=(width-36)/2;
        button("Edit name / notes",16,42,half,b->minecraft.setScreen(new CharacterNameScreen(this,service,sheet,library))).active=writable;
        button(sheet.archived()?"Restore character":"Archive character",20+half,42,half,b->{if(sheet.archived())service.edit(book->book.archive(id,false));
            else minecraft.setScreen(new ConfirmScreen(yes->{if(yes)service.edit(book->book.archive(id,true));minecraft.setScreen(yes?parent:this);},
                    Component.literal("Archive character?"),Component.literal("The sheet is retained under Archived characters and can be restored.")));}).active=service.view().writable();
        page=Math.min(page,Math.max(0,(sheet.entries().size()-1)/size()));
        button("Known facts",16,68,half,b->minecraft.setScreen(new CharacterFactsScreen(this,service,sheet))).active=writable;
        button("History",20+half,68,half,b->minecraft.setScreen(new CharacterHistoryScreen(this,service,id,library)));
        for(int i=page*size();i<Math.min(sheet.entries().size(),(page+1)*size());i++){
            var e=sheet.entries().get(i);Button entry=button(e.kind()+": "+e.name()+(e.kind()==CharacterBook.Kind.MAGIC?" • "+e.branch()+" "+e.tier():" • "+e.status().name().toLowerCase().replace('_',' ')),16,114+(i-page*size())*24,width-32,b->minecraft.setScreen(new CharacterEntryScreen(this,service,id,e.kind(),e,library)));
            entry.active=writable;entry.setTooltip(Tooltip.create(Component.literal(e.notes()+"\nUser-recorded status. Open Compatibility for checks and evidence.")));}
        int col=(width-40)/3;int x=16;for(var kind:CharacterBook.Kind.values()){
            button("Add "+(kind==CharacterBook.Kind.MAGIC?"magic":kind.name()),x,height-82,col,b->minecraft.setScreen(new CharacterEntryScreen(this,service,id,kind,null,library))).active=writable;x+=col+4;}
        button("Compatibility",(width-120)/2,height-54,120,b->minecraft.setScreen(new CompatibilityScreen(this,service,id,library))).active=library!=null&&library.view().library().isPresent();
        button("Previous",16,height-54,80,b->{page--;rebuildWidgets();}).active=page>0;
        button("Next",width-96,height-54,80,b->{page++;rebuildWidgets();}).active=(page+1)*size()<sheet.entries().size();
        button("Back",(width-120)/2,height-28,120,b->onClose());
    }
    @Override public void tick(){
        if(generation!=service.view().generation()){generation=service.view().generation();rebuildWidgets();}
        if(checking!=null&&checking.isDone()){
            try{var report=checking.join();if(pendingBook==service.view().generation()&&pendingLibrary==library.view().generation())assessment=report.summary();}
            catch(CompletionException|CancellationException e){assessment="Compatibility: checks unavailable";}
            checkedBook=pendingBook;checkedLibrary=pendingLibrary;checking=null;
        }
        if(library!=null&&library.view().library().isPresent()&&sheet()!=null&&checking==null&&(checkedBook!=service.view().generation()||checkedLibrary!=library.view().generation())){
            pendingBook=service.view().generation();pendingLibrary=library.view().generation();checking=library.assess(sheet());
        }
    }
    @Override public Component getNarrationMessage(){var s=sheet();return Component.literal(s==null?"Sheet unavailable":s.name()+". "+assessment+". "+s.notes());}
    @Override public void render(GuiGraphics g,int x,int y,float d){var s=sheet();g.drawCenteredString(font,font.plainSubstrByWidth(s==null?"Sheet unavailable":s.name(),width-32),width/2,10,0xFFFFFFFF);
        g.drawCenteredString(font,font.plainSubstrByWidth(assessment,width-32),width/2,27,0xFFFFC477);
        if(y>=25&&y<38)g.setTooltipForNextFrame(Component.literal(assessment+". Open Compatibility for evidence and remaining requirements."),x,y);
        String info=service.view().message().isEmpty()?"CA: Creature Application • FA: Feat Application":service.view().message();g.drawCenteredString(font,font.plainSubstrByWidth(info,width-32),width/2,96,0xFFBAC9D8);
        g.drawCenteredString(font,"Page "+(page+1),width/2,height-47,0xFFFFFFFF);super.render(g,x,y,d);}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
