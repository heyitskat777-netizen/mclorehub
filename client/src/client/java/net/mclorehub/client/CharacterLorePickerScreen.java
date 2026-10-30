package net.mclorehub.client;

import net.mclorehub.character.*;
import net.mclorehub.library.LibraryService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;

/** Only deliberately curated, revision-matching options. Manual entries remain available. */
final class CharacterLorePickerScreen extends Screen {
    private final Screen parent;private final LibraryService library;private final CharacterBook.Kind kind;
    private final Consumer<CharacterLore.Choice> selected;private String query="",error="";private int page;private long generation=-1;
    CharacterLorePickerScreen(Screen parent,LibraryService library,CharacterBook.Kind kind,Consumer<CharacterLore.Choice> selected){
        super(Component.literal("Choose "+kind.name().toLowerCase(Locale.ROOT)+" lore"));this.parent=parent;this.library=library;this.kind=kind;this.selected=selected;
    }
    private int size(){return Math.max(1,(height-140)/40);}
    private List<CharacterLore.Choice> choices(){return library.view().library().map(l->l.characterLore().choices(kind,query)).orElse(List.of());}
    private Button button(String text,int x,int y,int w,Button.OnPress action){return addRenderableWidget(Button.builder(Component.literal(text),action).bounds(x,y,w,20).build());}
    @Override protected void init(){
        var search=addRenderableWidget(new EditBox(font,16,34,width-32,20,Component.literal("Search lore options")));search.setMaxLength(128);search.setValue(query);
        search.setResponder(v->{query=v;page=0;rebuildWidgets();setFocused(children().stream().filter(EditBox.class::isInstance).findFirst().orElse(null));});
        var list=choices();page=Math.min(page,Math.max(0,(list.size()-1)/size()));
        for(int i=page*size();i<Math.min(list.size(),(page+1)*size());i++){
            var choice=list.get(i);int y=86+(i-page*size())*40;
            var choose=button(choice.option().name(),16,y,width-120,b->{
                var fresh=choices().stream().filter(c->c.option().key().equals(choice.option().key())).findFirst();
                if(fresh.isPresent()&&fresh.get().availability()==CharacterLore.Availability.AVAILABLE){selected.accept(fresh.get());minecraft.setScreen(parent);}
                else {error="This lore changed or is unavailable; use a manual entry.";rebuildWidgets();}
            });choose.active=choice.availability()==CharacterLore.Availability.AVAILABLE;choose.setTooltip(Tooltip.create(Component.literal(choice.option().notes())));
            button("Read lore",width-100,y,84,b->library.view().library().flatMap(l->l.characterLore().document(choice.option().sourceId()))
                    .ifPresent(d->minecraft.setScreen(new ReaderScreen(this,library,d,!net.mclorehub.catalogue.LoreCatalogue.isCurrent(d),false))))
                    .active=library.view().library().flatMap(l->l.characterLore().document(choice.option().sourceId())).isPresent();
        }
        button("Previous",16,height-54,80,b->{page--;rebuildWidgets();}).active=page>0;
        button("Next",width-96,height-54,80,b->{page++;rebuildWidgets();}).active=(page+1)*size()<list.size();
        button("Back",(width-120)/2,height-28,120,b->onClose());setInitialFocus(search);
    }
    @Override public void tick(){if(generation!=library.view().generation()){generation=library.view().generation();rebuildWidgets();}}
    @Override public void render(GuiGraphics g,int x,int y,float delta){
        g.drawCenteredString(font,title,width/2,10,0xFFFFFFFF);
        g.drawCenteredString(font,font.plainSubstrByWidth(error.isEmpty()?"Limited curated list • Manual entry remains available":error,width-32),width/2,65,0xFFFFC477);
        var list=choices();for(int i=page*size();i<Math.min(list.size(),(page+1)*size());i++) {
            var c=list.get(i);String label=c.availability()==CharacterLore.Availability.AVAILABLE?"Source linked • Requirements still apply":c.availability().name().toLowerCase(Locale.ROOT).replace('_',' ')+" • Selection suspended";
            g.drawString(font,font.plainSubstrByWidth(label,width-32),16,110+(i-page*size())*40,0xFFBAC9D8);
        }
        if(list.isEmpty())g.drawCenteredString(font,"No curated matches. You can record this entry manually.",width/2,96,0xFFBAC9D8);
        g.drawCenteredString(font,(page+1)+" / "+Math.max(1,(list.size()+size()-1)/size()),width/2,height-47,0xFFFFFFFF);super.render(g,x,y,delta);
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
}
