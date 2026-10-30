package net.mclorehub.client;

import net.mclorehub.library.BrowseLabels;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;

/** Explicit category navigation instead of cycling a hidden list. */
final class CategoryScreen extends Screen {
    private final Screen parent;
    private final Map<String,Long> counts;
    private final Consumer<String> selected;
    private final List<String> categories;
    private int page;
    CategoryScreen(Screen parent,Map<String,Long> counts,Consumer<String> selected){
        super(Component.literal("Browse categories"));this.parent=parent;this.counts=Map.copyOf(counts);this.selected=selected;
        var all=new ArrayList<String>();all.add("");counts.keySet().stream().sorted(Comparator.comparing(BrowseLabels::category)).forEach(all::add);categories=List.copyOf(all);
    }
    private int size(){return Math.max(2,((height-116)/36)*2);}
    @Override protected void init(){
        int col=(width-44)/2;page=Math.min(page,Math.max(0,(categories.size()-1)/size()));
        for(int i=page*size();i<Math.min(categories.size(),(page+1)*size());i++){
            String key=categories.get(i);long count=key.isEmpty()?counts.values().stream().mapToLong(Long::longValue).sum():counts.get(key);
            addRenderableWidget(Button.builder(Component.literal(BrowseLabels.category(key)+" ("+count+")"),b->{selected.accept(key);minecraft.setScreen(parent);})
                    .bounds(16+(i%2)*(col+12),50+((i-page*size())/2)*36,col,28).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Previous"),b->{page--;rebuildWidgets();}).bounds(16,height-54,80,20).build()).active=page>0;
        addRenderableWidget(Button.builder(Component.literal("Next"),b->{page++;rebuildWidgets();}).bounds(width-96,height-54,80,20).build()).active=(page+1)*size()<categories.size();
        addRenderableWidget(Button.builder(Component.literal("Back"),b->onClose()).bounds((width-120)/2,height-28,120,20).build());
    }
    @Override public void render(GuiGraphics g,int x,int y,float delta){
        g.drawCenteredString(font,title,width/2,12,0xFFFFFFFF);
        g.drawCenteredString(font,"Choose a collection • Counts reflect the current/audit view",width/2,31,0xFFBAC9D8);
        g.drawCenteredString(font,(page+1)+" / "+Math.max(1,(categories.size()+size()-1)/size()),width/2,height-47,0xFFFFFFFF);super.render(g,x,y,delta);
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
}
