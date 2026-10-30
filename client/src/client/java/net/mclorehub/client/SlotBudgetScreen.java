package net.mclorehub.client;

import net.mclorehub.character.*;
import net.mclorehub.library.LibraryService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.concurrent.*;

final class SlotBudgetScreen extends Screen {
    private final Screen parent;private final CharacterService service;private final LibraryService library;private final CharacterBook.Sheet sheet;
    private SlotBudget budget;private String capacity,lost,error="";private int page;private CompletableFuture<CharacterService.View> saving;
    SlotBudgetScreen(Screen parent,CharacterService service,LibraryService library,CharacterBook.Sheet sheet){super(Component.literal("Slot budget"));this.parent=parent;this.service=service;this.library=library;this.sheet=sheet;budget=sheet.budget();capacity=budget.capacity()<0?"":Integer.toString(budget.capacity());lost=budget.lostSlots()<0?"":Integer.toString(budget.lostSlots());}
    private List<CharacterBook.Entry> entries(){var groups=new LinkedHashMap<String,CharacterBook.Entry>();for(var e:sheet.entries())if(e.kind()!=CharacterBook.Kind.CA)groups.putIfAbsent(SlotBudget.key(e),e);return List.copyOf(groups.values());}
    private int size(){return Math.max(1,(height-164)/24);}
    private Button button(String text,int x,int y,int w,Button.OnPress action){return addRenderableWidget(Button.builder(Component.literal(text),action).bounds(x,y,w,20).build());}
    @Override protected void init(){int half=(width-36)/2;var cap=addRenderableWidget(new EditBox(font,16,42,half,20,Component.literal("Usable slot capacity")));cap.setMaxLength(3);cap.setValue(capacity);cap.setResponder(v->capacity=v);
        var loss=addRenderableWidget(new EditBox(font,20+half,42,half,20,Component.literal("Total lost slots")));loss.setMaxLength(3);loss.setValue(lost);loss.setResponder(v->lost=v);
        var entries=entries();page=Math.min(page,Math.max(0,(entries.size()-1)/size()));
        for(int i=page*size();i<Math.min(entries.size(),(page+1)*size());i++){
            var e=entries.get(i);String key=SlotBudget.key(e);int value=budget.allocations().getOrDefault(key,-1);var cost=library==null?null:library.view().library().map(l->l.characterLore().slotCost(e)).orElse(null);
            String range=cost==null||cost.maximum()<0?"unknown":cost.minimum()==cost.maximum()?Integer.toString(cost.minimum()):cost.minimum()+"–"+cost.maximum();
            var b=button(e.name()+": "+(value<0?"Auto ("+range+")":value+" recorded"),16,92+(i-page*size())*24,width-32,v->{int max=cost==null||cost.maximum()<0?10:Math.max(0,cost.maximum());var values=new ArrayList<Integer>();values.add(-1);for(int n=0;n<=max;n++){final int number=n;if(library==null||library.view().library().map(l->l.characterLore().validAllocation(e,number)).orElse(true))values.add(n);}budget=budget.allocate(key,values.get((Math.max(0,values.indexOf(value))+1)%values.size()));rebuildWidgets();});
            b.setTooltip(Tooltip.create(Component.literal("Auto uses the bound source cost/range; click for an explicit allocation. Capacity and lost slots stay separate. Recorded values do not verify prerequisites.")));
        }
        button("Previous",16,height-54,80,b->{page--;rebuildWidgets();}).active=page>0;button("Next",width-96,height-54,80,b->{page++;rebuildWidgets();}).active=(page+1)*size()<entries.size();
        button("Save slots",16,height-28,half,b->{try{var recorded=new SlotBudget(number(capacity),number(lost),budget.allocations());saving=service.edit(book->book.budget(sheet.id(),recorded));}catch(IllegalArgumentException ex){error="Use whole numbers 0–100, or leave blank for unknown.";}}).active=saving==null;
        button("Cancel",20+half,height-28,half,b->onClose());
    }
    private int number(String text){if(text.isBlank())return -1;if(!text.matches("[0-9]{1,3}"))throw new IllegalArgumentException();return Integer.parseInt(text);}
    @Override public void tick(){if(saving!=null&&saving.isDone()){var result=saving.join();saving=null;if(result.message().isEmpty())minecraft.setScreen(parent);else {error=result.message();rebuildWidgets();}}}
    @Override public void render(GuiGraphics g,int x,int y,float d){g.drawCenteredString(font,title,width/2,10,0xFFFFFFFF);g.drawString(font,"Usable capacity (blank = unknown)",16,29,0xFFBAC9D8);g.drawString(font,"Total lost slots (blank = unknown)",20+(width-36)/2,29,0xFFBAC9D8);
        g.drawCenteredString(font,font.plainSubstrByWidth(error.isEmpty()?"Include body/CA restrictions in capacity • History may be incomplete":error,width-32),width/2,70,0xFFFFC477);g.drawCenteredString(font,"Page "+(page+1),width/2,height-47,0xFFFFFFFF);super.render(g,x,y,d);}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
