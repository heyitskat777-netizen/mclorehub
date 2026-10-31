package net.mclorehub.client;

import net.mclorehub.library.*;
import net.mclorehub.spell.SpellIndex;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The same continuous reader, without replacing the user's wiki reading position. */
final class SpellDetailScreen extends ReaderScreen {
    private final Screen parent;private final LibraryService library;private final SpellIndex.Hit hit;
    SpellDetailScreen(Screen parent,LibraryService library,SpellIndex.Hit hit){super(parent,library,hit.source(),hit.ability().name(),library.spellReference(hit,false));this.parent=parent;this.library=library;this.hit=hit;}
    private void button(String text,int x,int y,int width,Button.OnPress action){addRenderableWidget(Button.builder(Component.literal(text),action).bounds(x,y,width,20).build());}
    @Override protected void init(){
        super.init();clearWidgets();int col=(width-40)/3;
        button("Spell & redlines",16,42,col,b->referenceContent(library.spellReference(hit,false)));
        button("Practice rules",20+col,42,col,b->referenceContent(library.spellReference(hit,true)));
        button("Copy source URL",24+col*2,42,col,b->minecraft.keyboardHandler.setClipboard(hit.source().url()));
        button("Top",16,height-54,80,b->top());button("Bottom",width-96,height-54,80,b->bottom());
        button("Open complete lore",16,height-28,col,b->minecraft.setScreen(new ReaderScreen(this,library,hit.source(),!LoreSearch.isCurrent(hit.source()),false)));
        button("Back",20+col,height-28,col,b->onClose());
    }
    @Override public void tick(){super.tick();var doc=library.view().library().flatMap(l->l.catalogue().documents().stream().filter(d->d.id().equals(hit.source().id())).findFirst());if(doc.isEmpty()||!doc.get().fingerprint().equals(hit.source().fingerprint()))minecraft.setScreen(parent);}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
