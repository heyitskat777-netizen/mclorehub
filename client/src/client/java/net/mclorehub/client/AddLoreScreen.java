package net.mclorehub.client;

import net.mclorehub.character.*;
import net.mclorehub.library.LibraryService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** One sheet action for all three entry types; application semantics remain distinct. */
final class AddLoreScreen extends Screen {
    private final Screen parent;private final CharacterService characters;private final LibraryService library;private final String sheetId;
    AddLoreScreen(Screen parent,CharacterService characters,String sheetId,LibraryService library){
        super(Component.literal("Add lore"));this.parent=parent;this.characters=characters;this.sheetId=sheetId;this.library=library;
    }
    static String label(CharacterBook.Kind kind){return switch(kind){case MAGIC->"Magic";case CA->"Creature application (CA)";case FA->"Feat application (FA)";};}
    @Override protected void init(){
        int y=54;for(var kind:CharacterBook.Kind.values()){
            addRenderableWidget(Button.builder(Component.literal(label(kind)),b->minecraft.setScreen(new CharacterEntryScreen(parent,characters,sheetId,kind,null,library)))
                    .bounds(24,y,width-48,28).build());y+=42;
        }
        addRenderableWidget(Button.builder(Component.literal("Back"),b->onClose()).bounds((width-120)/2,height-28,120,20).build());
    }
    @Override public void render(GuiGraphics g,int x,int y,float d){g.drawCenteredString(font,title,width/2,12,0xFFFFFFFF);g.drawCenteredString(font,"Choose the type, then select lore or enter it manually",width/2,32,0xFFBAC9D8);super.render(g,x,y,d);}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
