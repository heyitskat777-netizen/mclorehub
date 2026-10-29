package net.mclorehub.client;

import net.mclorehub.character.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.concurrent.CompletableFuture;

final class CharacterEntryScreen extends Screen {
    private final Screen parent;private final CharacterService service;private final String sheetId;private final CharacterBook.Kind kind;private final CharacterBook.Entry original;
    private String name="",branch="",tier="",url="",notes="",error="";private CharacterBook.RecordedStatus status=CharacterBook.RecordedStatus.NOT_RECORDED;
    private CompletableFuture<CharacterService.View> saving;private Button save;
    CharacterEntryScreen(Screen parent,CharacterService service,String sheetId,CharacterBook.Kind kind,CharacterBook.Entry entry){
        super(Component.literal((entry==null?"Add ":"Edit ")+(kind==CharacterBook.Kind.MAGIC?"magic":kind.name())));this.parent=parent;this.service=service;this.sheetId=sheetId;this.kind=kind;original=entry;
        if(entry!=null){name=entry.name();branch=entry.branch();tier=entry.tier();url=entry.applicationUrl();notes=entry.notes();status=entry.status();}}
    private EditBox field(String label,String value,int x,int y,int w,int max,java.util.function.Consumer<String> responder){var box=addRenderableWidget(new EditBox(font,x,y,w,20,Component.literal(label)));
        box.setMaxLength(max);box.setValue(value);box.setResponder(responder);return box;}
    @Override protected void init(){
        field("Entry name",name,16,34,width-32,128,v->name=v);int col=(width-36)/2;
        if(kind==CharacterBook.Kind.MAGIC){field("Branch",branch,16,70,col,128,v->branch=v);field("Tier",tier,20+col,70,col,64,v->tier=v);}
        field("Application URL",url,16,106,width-32,2048,v->url=v);field("Entry notes",notes,16,142,width-32,2048,v->notes=v);
        addRenderableWidget(Button.builder(Component.literal(statusText()),b->{status=CharacterBook.RecordedStatus.values()[(status.ordinal()+1)%CharacterBook.RecordedStatus.values().length];b.setMessage(Component.literal(statusText()));})
                .bounds(16,168,width-32,20).build());
        save=addRenderableWidget(Button.builder(Component.literal("Save entry"),b->{if(saving!=null)return;try{
            var entry=new CharacterBook.Entry(original==null?UUID.randomUUID().toString():original.id(),kind,name,branch,tier,url,status,notes,original==null?"":original.loreDocumentId());
            saving=service.edit(book->book.putEntry(sheetId,entry));save.active=false;
        }catch(IllegalArgumentException e){error=e.getMessage();}}).bounds(16,height-28,(width-40)/3,20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"),b->onClose()).bounds(20+(width-40)/3,height-28,(width-40)/3,20).build());
        Button remove=addRenderableWidget(Button.builder(Component.literal("Delete entry"),b->minecraft.setScreen(new ConfirmScreen(yes->{if(yes){saving=service.edit(book->book.removeEntry(sheetId,original.id()));}minecraft.setScreen(this);},
                Component.literal("Delete entry?"),Component.literal("This removes the local record. The previous character file is kept as a backup."))))
                .bounds(24+2*((width-40)/3),height-28,(width-40)/3,20).build());remove.active=original!=null;
    }
    private String statusText(){return "Recorded status: "+status.name().toLowerCase(Locale.ROOT).replace('_',' ');}
    @Override public void tick(){if(saving!=null&&saving.isDone()){var result=saving.join();saving=null;save.active=result.writable();if(result.message().isEmpty())minecraft.setScreen(parent);else error=result.message();}}
    @Override public void render(GuiGraphics g,int x,int y,float d){g.drawCenteredString(font,title,width/2,10,0xFFFFFFFF);g.drawString(font,"Name",16,23,0xFFBAC9D8);
        if(kind==CharacterBook.Kind.MAGIC){g.drawString(font,"Branch (optional)",16,59,0xFFBAC9D8);g.drawString(font,"Tier (optional)",20+(width-36)/2,59,0xFFBAC9D8);}
        g.drawString(font,"Application topic URL (optional)",16,95,0xFFBAC9D8);g.drawString(font,"Notes (user-recorded)",16,131,0xFFBAC9D8);
        g.drawCenteredString(font,font.plainSubstrByWidth(error.isEmpty()?"No server approval or compatibility is verified.":error,width-32),width/2,193,0xFFFFC477);super.render(g,x,y,d);}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
