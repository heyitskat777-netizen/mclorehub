package net.mclorehub.client;

import net.mclorehub.character.*;
import net.mclorehub.library.*;
import net.mclorehub.spell.SpellIndex;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.concurrent.*;

/** Stable search editor over worker-produced, source-bound spell references. */
final class SpellBookScreen extends Screen {
    private final Screen parent;private final CharacterService characters;private final LibraryService library;private final String sheetId;
    private EditBox search;private Button filter,previous,next;private final List<Button> rows=new ArrayList<>();
    private String query="",error="";private boolean allTiers;private int page;private long bookGeneration=-1,libraryGeneration=-1,requestedBook,requestedLibrary;
    private SpellIndex.Results result=new SpellIndex.Results(List.of(),0,0,0,List.of());private CompletableFuture<SpellIndex.Results> pending;
    SpellBookScreen(Screen parent,CharacterService characters,String sheetId,LibraryService library){super(Component.literal("Available spells"));this.parent=parent;this.characters=characters;this.sheetId=sheetId;this.library=library;}
    private int size(){return Math.max(1,(height-154)/42);}
    private Optional<CharacterBook.Sheet> sheet(){return characters.view().book().sheets().stream().filter(s->s.id().equals(sheetId)).findFirst();}
    private Button button(String label,int x,int y,int w,Button.OnPress action){return addRenderableWidget(Button.builder(Component.literal(label),action).bounds(x,y,w,20).build());}
    @Override protected void init(){
        if(search==null){search=new EditBox(font,16,34,width-32,20,Component.literal("Search spells, effects and redlines"));search.setMaxLength(256);search.setHint(Component.literal("Search spells, effects and redlines"));search.setResponder(v->{query=v;page=0;request();});}
        search.setWidth(width-32);addRenderableWidget(search);
        filter=button(allTiers?"All linked tiers":"By recorded tier",16,60,(width-36)/2,b->{allTiers=!allTiers;filter.setMessage(Component.literal(allTiers?"All linked tiers":"By recorded tier"));page=0;request();});
        button("Character sheet",20+(width-36)/2,60,(width-36)/2,b->onClose());
        previous=button("Previous",16,height-54,80,b->{page--;refreshRows();});next=button("Next",width-96,height-54,80,b->{page++;refreshRows();});
        button("Back",(width-120)/2,height-28,120,b->onClose());refreshRows();request();
    }
    @Override protected void setInitialFocus(){setInitialFocus(search);}
    private void request(){if(pending!=null)pending.cancel(false);error="";rows.forEach(b->b.active=false);requestedBook=characters.view().generation();requestedLibrary=library.view().generation();sheet().ifPresentOrElse(s->pending=library.spells(s,query,allTiers),()->error="Character unavailable");}
    private void refreshRows(){
        rows.forEach(this::removeWidget);rows.clear();page=Math.min(page,Math.max(0,(result.hits().size()-1)/size()));
        for(int i=page*size();i<Math.min(result.hits().size(),(page+1)*size());i++){
            var hit=result.hits().get(i);var row=button(hit.ability().name(),16,99+(i-page*size())*42,width-32,b->minecraft.setScreen(new SpellDetailScreen(this,library,hit)));
            row.active=pending==null;row.setTooltip(Tooltip.create(Component.literal(hit.requirement()+"\n"+hit.source().url())));rows.add(row);
        }
        if(previous!=null)previous.active=page>0;if(next!=null)next.active=(page+1)*size()<result.hits().size();
    }
    @Override public void tick(){
        if(bookGeneration!=characters.view().generation()||libraryGeneration!=library.view().generation()){bookGeneration=characters.view().generation();libraryGeneration=library.view().generation();request();}
        if(pending!=null&&pending.isDone()){
            try{var found=pending.join();if(requestedBook==characters.view().generation()&&requestedLibrary==library.view().generation())result=found;}
            catch(CompletionException|CancellationException e){error="Spell references unavailable; character/source data retained.";}
            pending=null;refreshRows();
        }
    }
    @Override public Component getNarrationMessage(){return Component.literal(title.getString()+". "+result.hits().size()+" spell references. "+result.eligible()+" within recorded tier; "+result.conditional()+" need requirements checked. "+error);}
    @Override public void render(GuiGraphics g,int x,int y,float d){
        g.drawCenteredString(font,title,width/2,10,0xFFFFFFFF);g.drawCenteredString(font,font.plainSubstrByWidth("Tier reference • Learning and approval are not verified",width-32),width/2,85,0xFFFFC477);
        for(int i=page*size();i<Math.min(result.hits().size(),(page+1)*size());i++){
            var h=result.hits().get(i);String text=h.entry().name()+" • "+SpellIndex.tierLabel(h.ability())+" • "+(h.access()==SpellIndex.Access.BY_TIER?"Within tier":"Check: "+h.access().name().toLowerCase(Locale.ROOT).replace('_',' '));
            g.drawCenteredString(font,font.plainSubstrByWidth(text,width-40),width/2,123+(i-page*size())*42,h.access()==SpellIndex.Access.BY_TIER?0xFFB3DBBC:0xFFFFC477);
        }
        if(result.hits().isEmpty()&&pending==null)g.drawWordWrap(font,Component.literal(error.isEmpty()?result.notes().isEmpty()?"No matching references. Link lore and record a tier, or choose All linked tiers.":String.join(" ",result.notes()):error),20,104,width-40,0xFFBAC9D8);
        String footer=!error.isEmpty()?error:pending!=null?"Searching…":result.hits().size()+" references • "+(page+1)+" / "+Math.max(1,(result.hits().size()+size()-1)/size());
        g.drawCenteredString(font,font.plainSubstrByWidth(footer,width-204),width/2,height-47,0xFFFFFFFF);super.render(g,x,y,d);
    }
    @Override public void removed(){if(pending!=null)pending.cancel(false);}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
