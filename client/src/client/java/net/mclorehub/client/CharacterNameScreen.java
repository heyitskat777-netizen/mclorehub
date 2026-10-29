package net.mclorehub.client;

import net.mclorehub.character.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.concurrent.CompletableFuture;

final class CharacterNameScreen extends Screen {
    private final Screen parent;private final CharacterService service;private final CharacterBook.Sheet sheet;
    private EditBox name,notes;private String nameValue="",notesValue="",error="";private CompletableFuture<CharacterService.View> saving;
    CharacterNameScreen(Screen parent,CharacterService service,CharacterBook.Sheet sheet){super(Component.literal(sheet==null?"New character":"Rename character"));
        this.parent=parent;this.service=service;this.sheet=sheet;if(sheet!=null){nameValue=sheet.name();notesValue=sheet.notes();}}
    @Override protected void init(){
        name=addRenderableWidget(new EditBox(font,16,54,width-32,20,Component.literal("Character name")));name.setMaxLength(128);name.setValue(nameValue);name.setResponder(v->nameValue=v);
        notes=addRenderableWidget(new EditBox(font,16,100,width-32,20,Component.literal("Character notes")));notes.setMaxLength(4096);notes.setValue(notesValue);notes.setResponder(v->notesValue=v);
        addRenderableWidget(Button.builder(Component.literal("Save character"),b->{if(saving!=null)return;if(nameValue.isBlank()){error="Enter a character name.";return;}
            String recordedName=nameValue,recordedNotes=notesValue;
            saving=service.edit(book->{if(sheet!=null)return book.rename(sheet.id(),recordedName,recordedNotes);
                var next=book.create(recordedName);return next.rename(next.selectedId(),recordedName,recordedNotes);});
        }).bounds(16,height-28,(width-36)/2,20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"),b->onClose()).bounds(20+(width-36)/2,height-28,(width-36)/2,20).build());setInitialFocus(name);
    }
    @Override public void tick(){if(saving!=null&&saving.isDone()){var result=saving.join();saving=null;if(result.message().isEmpty()){
        minecraft.setScreen(sheet==null?new CharacterSheetScreen(parent,service,result.book().selectedId()):parent);}else error=result.message();}}
    @Override public void render(GuiGraphics g,int x,int y,float d){g.drawCenteredString(font,title,width/2,12,0xFFFFFFFF);g.drawString(font,"Name",16,40,0xFFBAC9D8);
        g.drawString(font,"Notes",16,86,0xFFBAC9D8);g.drawWordWrap(font,Component.literal(error),16,140,width-32,0xFFFFC477);super.render(g,x,y,d);}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
