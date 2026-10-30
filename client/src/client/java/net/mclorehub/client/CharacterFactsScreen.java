package net.mclorehub.client;

import net.mclorehub.character.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.concurrent.CompletableFuture;

final class CharacterFactsScreen extends Screen {
    private final Screen parent;private final CharacterService service;private final String id;
    private CharacterFacts facts;private int page;private String error="";private CompletableFuture<CharacterService.View> saving;
    CharacterFactsScreen(Screen parent,CharacterService service,CharacterBook.Sheet sheet){super(Component.literal("Known facts"));this.parent=parent;this.service=service;id=sheet.id();facts=sheet.facts();}
    private int size(){return Math.max(1,(height-140)/26);}
    private Button button(String text,int x,int y,int w,Button.OnPress action){return addRenderableWidget(Button.builder(Component.literal(text),action).bounds(x,y,w,20).build());}
    @Override protected void init(){
        var fields=CharacterFacts.Field.values();page=Math.min(page,(fields.length-1)/size());
        for(int i=page*size();i<Math.min(fields.length,(page+1)*size());i++){
            var field=fields[i];var b=button(field.label+": "+CharacterFacts.label(facts.value(field)),16,64+(i-page*size())*26,width-32,v->{facts=facts.cycle(field);rebuildWidgets();});b.active=saving==null;b.setTooltip(Tooltip.create(Component.literal(field.help())));
        }
        button("Previous",16,height-54,80,b->{page--;rebuildWidgets();}).active=page>0;
        button("Next",width-96,height-54,80,b->{page++;rebuildWidgets();}).active=(page+1)*size()<fields.length;
        button("Save facts",16,height-28,(width-36)/2,b->{if(saving==null){var recorded=facts;saving=service.edit(book->book.facts(id,recorded));rebuildWidgets();}}).active=saving==null;
        button("Cancel",20+(width-36)/2,height-28,(width-36)/2,b->onClose());
    }
    @Override public void tick(){if(saving!=null&&saving.isDone()){var result=saving.join();saving=null;if(result.message().isEmpty())minecraft.setScreen(parent);else {error=result.message();rebuildWidgets();}}}
    @Override public void render(GuiGraphics g,int x,int y,float d){g.drawCenteredString(font,title,width/2,12,0xFFFFFFFF);
        g.drawCenteredString(font,font.plainSubstrByWidth(error.isEmpty()?"Record current facts • Unknown means not assessed":error,width-32),width/2,36,0xFFFFC477);
        g.drawCenteredString(font,(page+1)+" / "+((CharacterFacts.Field.values().length+size()-1)/size()),width/2,height-47,0xFFFFFFFF);super.render(g,x,y,d);}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
