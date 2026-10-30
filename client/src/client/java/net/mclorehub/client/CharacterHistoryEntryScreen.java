package net.mclorehub.client;

import net.mclorehub.character.*;
import net.mclorehub.library.LibraryService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.concurrent.CompletableFuture;

final class CharacterHistoryEntryScreen extends Screen {
    private final Screen parent;private final CharacterService service;private final String sheetId;private final LibraryService library;private final CharacterBook.HistoryEvent original;
    private String name="",loreId="",date="",notes="",error="";private CharacterBook.EventType type=CharacterBook.EventType.ACQUIRED;private CharacterBook.Kind kind=CharacterBook.Kind.MAGIC;
    private CompletableFuture<CharacterService.View> saving;
    CharacterHistoryEntryScreen(Screen parent,CharacterService service,String sheetId,LibraryService library,CharacterBook.HistoryEvent event){super(Component.literal(event==null?"Record event":"Edit event"));this.parent=parent;this.service=service;this.sheetId=sheetId;this.library=library;original=event;
        if(event!=null){name=event.name();loreId=event.loreDocumentId();type=event.type();date=event.date();notes=event.notes();}}
    private void field(String label,String value,int x,int y,int w,int max,java.util.function.Consumer<String> change){var box=addRenderableWidget(new EditBox(font,x,y,w,20,Component.literal(label)));box.setMaxLength(max);box.setValue(value);box.setResponder(v->{change.accept(v);error="";});}
    private Button button(String text,int x,int y,int w,Button.OnPress action){return addRenderableWidget(Button.builder(Component.literal(text),action).bounds(x,y,w,20).build());}
    @Override protected void init(){
        field("Event name",name,16,34,width-160,128,v->name=v);
        button("Choose lore",width-140,34,124,b->minecraft.setScreen(new CharacterLorePickerScreen(this,library,kind,c->{name=c.option().name();loreId=c.option().sourceId();}))).active=library!=null&&library.view().library().isPresent();
        int half=(width-36)/2;button("Lore type: "+(kind==CharacterBook.Kind.MAGIC?"magic":kind.name()),16,62,half,b->{kind=CharacterBook.Kind.values()[(kind.ordinal()+1)%CharacterBook.Kind.values().length];rebuildWidgets();});
        button("Event: "+CharacterFacts.label(type.name()),20+half,62,half,b->{type=CharacterBook.EventType.values()[(type.ordinal()+1)%CharacterBook.EventType.values().length];rebuildWidgets();});
        field("Event date",date,16,104,width-32,10,v->date=v);field("Event notes",notes,16,144,width-32,2048,v->notes=v);
        button("Unlink lore",width-100,174,84,b->{loreId="";rebuildWidgets();}).active=!loreId.isEmpty();
        int col=(width-40)/3;
        button("Save event",16,height-28,col,b->{if(saving!=null)return;try{var event=new CharacterBook.HistoryEvent(original==null?UUID.randomUUID().toString():original.id(),name,loreId,type,date,notes);saving=service.edit(book->book.putHistory(sheetId,event));}catch(IllegalArgumentException e){error=e.getMessage();}}).active=saving==null;
        button("Cancel",20+col,height-28,col,b->onClose());
        button("Delete event",24+2*col,height-28,col,b->minecraft.setScreen(new ConfirmScreen(yes->{if(yes)saving=service.edit(book->book.removeHistory(sheetId,original.id()));minecraft.setScreen(this);},Component.literal("Delete event?"),Component.literal("The previous character file is retained as a backup.")))).active=original!=null&&saving==null;
    }
    @Override public void tick(){if(saving!=null&&saving.isDone()){var result=saving.join();saving=null;if(result.message().isEmpty())minecraft.setScreen(parent);else {error=result.message();rebuildWidgets();}}}
    @Override public void render(GuiGraphics g,int x,int y,float d){g.drawCenteredString(font,title,width/2,10,0xFFFFFFFF);g.drawString(font,"Name",16,23,0xFFBAC9D8);
        g.drawString(font,"OOC date YYYY-MM-DD (optional reference)",16,91,0xFFBAC9D8);g.drawString(font,"Notes",16,131,0xFFBAC9D8);
        g.drawString(font,font.plainSubstrByWidth(loreId.isEmpty()?"Lore: unlinked user note":"Lore: "+loreId,width-124),16,180,0xFFBAC9D8);
        g.drawCenteredString(font,font.plainSubstrByWidth(error,width-32),width/2,198,0xFFFFC477);super.render(g,x,y,d);}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
