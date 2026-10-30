package net.mclorehub.client;

import net.mclorehub.character.*;
import net.mclorehub.library.LibraryService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class CharacterListScreen extends Screen {
    private final Screen parent;private final CharacterService service;private boolean archived=false;private int page=0;private long generation=-1;
    private final LibraryService library;
    public CharacterListScreen(Screen parent,CharacterService service){this(parent,service,MCLoreHubClient.library());}
    public CharacterListScreen(Screen parent,CharacterService service,LibraryService library){super(Component.literal("Characters"));this.parent=parent;this.service=service;this.library=library;}
    private int size(){return Math.max(1,(height-150)/24);}
    private List<CharacterBook.Sheet> sheets(){return service.view().book().sheets().stream().filter(s->s.archived()==archived).toList();}
    private Button button(String text,int x,int y,int w,Button.OnPress action){return addRenderableWidget(Button.builder(Component.literal(text),action).bounds(x,y,w,20).build());}
    @Override protected void init(){
        int col=(width-40)/3;button("New character",16,42,col,b->minecraft.setScreen(new CharacterNameScreen(this,service,null,library))).active=service.view().writable();
        button(archived?"Archived characters":"Active characters",20+col,42,col,b->{archived=!archived;page=0;rebuildWidgets();});
        button("Reload sheets",24+col*2,42,col,b->service.reload());
        List<CharacterBook.Sheet> list=sheets();page=Math.min(page,Math.max(0,(list.size()-1)/size()));
        for(int i=page*size();i<Math.min(list.size(),(page+1)*size());i++){
            var sheet=list.get(i);button(sheet.name(),16,94+(i-page*size())*24,width-32,b->{if(!sheet.archived())service.edit(book->book.select(sheet.id()));
                minecraft.setScreen(new CharacterSheetScreen(this,service,sheet.id(),library));});}
        button("Previous",16,height-54,80,b->{page--;rebuildWidgets();}).active=page>0;
        button("Next",width-96,height-54,80,b->{page++;rebuildWidgets();}).active=(page+1)*size()<list.size();
        button("Back",(width-120)/2,height-28,120,b->onClose());
    }
    @Override public void tick(){if(generation!=service.view().generation()){generation=service.view().generation();rebuildWidgets();}}
    @Override public void render(GuiGraphics g,int x,int y,float d){g.drawCenteredString(font,title,width/2,10,0xFFFFFFFF);
        String info=service.view().message().isEmpty()?"Local sheets • Records do not establish server approval":service.view().message();
        g.drawCenteredString(font,font.plainSubstrByWidth(info,width-32),width/2,75,0xFFFFC477);
        if(sheets().isEmpty())g.drawCenteredString(font,"No characters here yet.",width/2,110,0xFFBAC9D8);
        g.drawCenteredString(font,(page+1)+" / "+Math.max(1,(sheets().size()+size()-1)/size()),width/2,height-47,0xFFFFFFFF);super.render(g,x,y,d);}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
