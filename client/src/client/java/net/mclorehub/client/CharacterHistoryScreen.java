package net.mclorehub.client;

import net.mclorehub.character.*;
import net.mclorehub.library.LibraryService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

final class CharacterHistoryScreen extends Screen {
    private final Screen parent;private final CharacterService service;private final String id;private final LibraryService library;
    private int page;private long generation=-1;
    CharacterHistoryScreen(Screen parent,CharacterService service,String id,LibraryService library){super(Component.literal("Acquisition history"));this.parent=parent;this.service=service;this.id=id;this.library=library;}
    private CharacterBook.Sheet sheet(){return service.view().book().sheets().stream().filter(s->s.id().equals(id)).findFirst().orElse(null);}
    private int size(){return Math.max(1,(height-150)/24);}
    private Button button(String text,int x,int y,int w,Button.OnPress action){return addRenderableWidget(Button.builder(Component.literal(text),action).bounds(x,y,w,20).build());}
    @Override protected void init(){var s=sheet();if(s==null){button("Back",16,height-28,120,b->onClose());return;}
        boolean writable=service.view().writable()&&!s.archived();button("Record event",16,42,width-32,b->minecraft.setScreen(new CharacterHistoryEntryScreen(this,service,id,library,null))).active=writable&&s.history().size()<100;
        page=Math.min(page,Math.max(0,(s.history().size()-1)/size()));
        for(int i=page*size();i<Math.min(s.history().size(),(page+1)*size());i++){
            var e=s.history().get(i);var b=button(CharacterFacts.label(e.type().name())+": "+e.name()+(e.date().isEmpty()?"":" • "+e.date()),16,94+(i-page*size())*24,width-32,v->minecraft.setScreen(new CharacterHistoryEntryScreen(this,service,id,library,e)));
            b.active=writable;b.setTooltip(Tooltip.create(Component.literal(e.notes()+"\nEvents do not silently change current known facts.")));
        }
        button("Previous",16,height-54,80,b->{page--;rebuildWidgets();}).active=page>0;
        button("Next",width-96,height-54,80,b->{page++;rebuildWidgets();}).active=(page+1)*size()<s.history().size();
        button("Back",(width-120)/2,height-28,120,b->onClose());
    }
    @Override public void tick(){if(generation!=service.view().generation()){generation=service.view().generation();rebuildWidgets();}}
    @Override public void render(GuiGraphics g,int x,int y,float d){g.drawCenteredString(font,title,width/2,12,0xFFFFFFFF);
        g.drawCenteredString(font,font.plainSubstrByWidth("User-recorded events • Current facts stay separate",width-32),width/2,75,0xFFFFC477);
        var s=sheet();if(s!=null&&s.history().isEmpty())g.drawCenteredString(font,"No events recorded.",width/2,108,0xFFBAC9D8);
        g.drawCenteredString(font,"Page "+(page+1),width/2,height-47,0xFFFFFFFF);super.render(g,x,y,d);}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
