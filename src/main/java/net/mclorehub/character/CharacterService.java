package net.mclorehub.character;

import java.nio.file.Path;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.function.UnaryOperator;

/** Independent character storage cannot be overwritten by a lore-pack import. */
public final class CharacterService implements AutoCloseable {
    public record View(long generation,CharacterBook book,boolean writable,String message){}
    private final CharacterStore store;
    private final ExecutorService io=Executors.newSingleThreadExecutor(Thread.ofPlatform().daemon().name("mclorehub-characters").factory());
    private CharacterStore.Loaded loaded;
    private volatile View view=new View(0,CharacterBook.empty(),false,"Loading characters…");
    public CharacterService(Path root){store=new CharacterStore(root.resolve("personal/characters.json"));}
    public View view(){return view;}
    public CompletableFuture<View> reload(){return CompletableFuture.supplyAsync(()->{
        try{loaded=store.load();view=new View(view.generation()+1,loaded.book(),true,"");}
        catch(IOException e){view=new View(view.generation()+1,view.book(),false,"Character data is read-only: "+e.getMessage());}return view;},io);}
    public CompletableFuture<View> edit(UnaryOperator<CharacterBook> edit){return CompletableFuture.supplyAsync(()->{
        if(!view.writable()||loaded==null)return view;
        try{var next=edit.apply(loaded.book());loaded=store.save(next,loaded.hash());view=new View(view.generation()+1,next,true,"");}
        catch(IllegalArgumentException e){view=new View(view.generation()+1,view.book(),true,"Changes not saved: "+e.getMessage());}
        catch(IOException e){view=new View(view.generation()+1,view.book(),false,"Character save failed; original retained. "+e.getMessage());}return view;},io);}
    @Override public void close(){io.shutdown();try{if(!io.awaitTermination(10,TimeUnit.SECONDS))io.shutdownNow();}catch(InterruptedException e){io.shutdownNow();Thread.currentThread().interrupt();}}
}
