package net.mclorehub.client;

import net.mclorehub.library.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

final class DatabaseImportScreen extends Screen {
    private final Screen parent;private final LibraryService library;private Path input;private boolean trust=true;private String message="Drop a LoreDB ZIP/JSON or normalized data pack here.";
    private CompletableFuture<PackImport.Preview> preparing;private CompletableFuture<LibraryService.View> saving;private PackImport.Preview preview;private int page;
    DatabaseImportScreen(Screen parent,LibraryService library){super(Component.literal("Import database"));this.parent=parent;this.library=library;}
    private int size(){return Math.max(1,(height-170)/24);}
    private Button button(String text,int x,int y,int w,Button.OnPress action){return addRenderableWidget(Button.builder(Component.literal(text),action).bounds(x,y,w,20).build());}
    @Override protected void init(){int half=(width-36)/2;
        button(trust?"Accepted/current: confirmed":"Use supplied trust only",16,42,width-32,b->{trust=!trust;if(input!=null)prepare();else rebuildWidgets();}).active=preparing==null&&saving==null;
        button("Activate import",16,height-82,half,b->{saving=library.activateImport(preview.token());message="Activating…";rebuildWidgets();}).active=preview!=null&&saving==null&&preparing==null;
        button("Undo database import",20+half,height-82,half,b->{saving=library.undoImport();message="Restoring…";rebuildWidgets();}).active=library.canUndoImport()&&saving==null&&preparing==null;
        button("Previous",16,height-54,80,b->{page--;rebuildWidgets();}).active=page>0;
        button("Next",width-96,height-54,80,b->{page++;rebuildWidgets();}).active=preview!=null&&(page+1)*size()<preview.changes().size();
        button("Back",(width-120)/2,height-28,120,b->onClose());
        if(preview!=null)for(int i=page*size();i<Math.min(preview.changes().size(),(page+1)*size());i++){var change=preview.changes().get(i);button(change.kind()+": "+change.title(),16,91+(i-page*size())*24,width-32,b->minecraft.setScreen(new ReportScreen(this,"Import comparison",library.importComparison(preview.token(),change.id()))));}
    }
    private void prepare(){preview=null;page=0;preparing=library.prepareImport(input,trust);message="Preparing offline preview…";rebuildWidgets();}
    @Override public void onFilesDrop(List<Path> paths){if(paths.size()==1&&preparing==null&&saving==null){input=paths.getFirst();prepare();}}
    @Override public void tick(){
        if(preparing!=null&&preparing.isDone()){try{preview=preparing.join();message=preview.total()+" topics • "+preview.current()+" accepted/current • "+preview.changes().size()+" changes";}catch(CompletionException|CancellationException e){message=cause(e);}preparing=null;rebuildWidgets();}
        if(saving!=null&&saving.isDone()){try{saving.join();preview=null;message="Database active. Personal data preserved.";}catch(CompletionException|CancellationException e){message=cause(e);}saving=null;rebuildWidgets();}
    }
    private String cause(Exception e){return e.getCause()==null?"Operation cancelled; data retained.":e.getCause().getMessage();}
    @Override public Component getNarrationMessage(){return Component.literal(title.getString()+". "+message);}
    @Override public void render(GuiGraphics g,int x,int y,float d){g.drawCenteredString(font,title,width/2,10,0xFFFFFFFF);g.drawCenteredString(font,"Offline import • Preview first • Personal files stay separate",width/2,27,0xFFBAC9D8);g.drawCenteredString(font,font.plainSubstrByWidth(message,width-32),width/2,70,0xFFFFC477);
        super.render(g,x,y,d);}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
