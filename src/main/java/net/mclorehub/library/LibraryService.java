package net.mclorehub.library;

import net.mclorehub.catalogue.LoreCatalogue;
import net.mclorehub.catalogue.LoreCatalogue.Document;
import net.mclorehub.updates.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.UnaryOperator;

/** All filesystem, parsing, indexing, searching and personal writes run on owned workers. */
public final class LibraryService implements AutoCloseable {
    public enum Phase { LOADING, EMPTY, READY, ERROR }
    public record Library(LoreCatalogue catalogue, LoreSearch search) {}
    public record View(long generation, Phase phase, Optional<Library> library, PersonalState personal,
                       boolean writable, String message) {}
    public enum RefreshPhase { IDLE, FETCHING, SAVING }
    public record RefreshStatus(String documentId,RefreshPhase phase,String message){}
    public record RefreshResult(boolean changed,int addedReplies,int editedReplies,String message){}
    private final Path directory;
    private final PersonalStore store;
    private final UpdateStore updateStore;
    private volatile UpdateStore.State updateState;
    private volatile LoreCatalogue baseline;
    private TopicFetcher fetcher;
    private final boolean testTransport;
    private final ExecutorService network=Executors.newSingleThreadExecutor(Thread.ofPlatform().daemon().name("mclorehub-forum").factory());
    private volatile RefreshStatus refreshStatus=new RefreshStatus("",RefreshPhase.IDLE,"");
    private final Map<String,Long> attempts=new HashMap<>();
    private RefreshTask refreshTask;
    private static final class RefreshTask {
        final String id;final CompletableFuture<RefreshResult> result=new CompletableFuture<>();Future<?> work;volatile boolean cancelled,saving;
        RefreshTask(String id){this.id=id;}
    }
    private final ExecutorService io = Executors.newSingleThreadExecutor(Thread.ofPlatform().daemon().name("mclorehub-files").factory());
    private final ExecutorService searches = Executors.newSingleThreadExecutor(Thread.ofPlatform().daemon().name("mclorehub-search").factory());
    private volatile View view = new View(0, Phase.LOADING, Optional.empty(), PersonalState.empty(), false, "");
    private PersonalStore.Loaded personal;
    private Future<?> currentSearch;
    private CompletableFuture<LoreSearch.Page> currentResult;
    public LibraryService(Path directory) { this(directory,null); }
    public LibraryService(Path directory,TopicFetcher fetcher) {
        this.directory = directory; store = new PersonalStore(directory.resolve("personal/state.json"));updateStore=new UpdateStore(directory);
        this.fetcher=fetcher;testTransport=fetcher!=null;
    }
    public View view() { return view; }
    public Path directory() { return directory; }
    public CompletableFuture<View> reload() {
        return CompletableFuture.supplyAsync(() -> {
            View previous = view; String warning = ""; boolean writable = true;
            try { personal = store.load(); }
            catch (IOException e) { writable = false; warning = "Personal state is read-only: " + e.getMessage(); }
            PersonalState state = personal == null ? PersonalState.empty() : personal.state();
            Optional<Library> library = previous.library(); Phase phase;
            try {
                Path pack = directory.resolve("packs/active/pack.json"), reviews = pack.resolveSibling("reviews.json");
                if (!Files.exists(pack, LinkOption.NOFOLLOW_LINKS)) {
                    if(library.isPresent()) throw new IOException("Active pack is missing");
                    phase = Phase.EMPTY;
                }
                else {
                    LoreCatalogue catalogue = LoreCatalogue.load(pack, Files.exists(reviews,LinkOption.NOFOLLOW_LINKS) ? reviews : null);
                    catalogue=BaselineTrust.apply(catalogue,pack.resolveSibling("trust.json"));
                    UpdateStore.State stateUpdates=updateStore.load();var applied=updateStore.apply(catalogue,stateUpdates);
                    Library prepared=new Library(applied.catalogue(),new LoreSearch(applied.catalogue()));
                    baseline=catalogue;updateState=stateUpdates;library=Optional.of(prepared);phase=Phase.READY;
                    if(applied.ignored()>0)warning=(warning+" "+applied.ignored()+" stored refreshes belong to a different baseline and were preserved without applying.").strip();
                }
            } catch (IOException | RuntimeException e) {
                phase = Phase.ERROR; warning = "Database reload failed; previous valid library retained. " + warning + " " + e.getMessage();
            }
            view = new View(previous.generation()+1,phase,library,state,writable,warning.strip()); return view;
        },io);
    }
    public RefreshStatus refreshStatus(){return refreshStatus;}
    public boolean canRollback(String id){var state=updateState;var base=baseline;return state!=null&&base!=null&&state.entries().containsKey(id)
            &&base.documents().stream().anyMatch(d->d.id().equals(id)&&d.fingerprint().equals(state.entries().get(id).baseFingerprint()));}
    public synchronized CompletableFuture<RefreshResult> refresh(String id){
        if(refreshStatus.phase()!=RefreshPhase.IDLE||refreshTask!=null&&!refreshTask.result.isDone())return CompletableFuture.failedFuture(new IOException("Another topic update is already running"));
        Document before=view.library().flatMap(l->l.catalogue().documents().stream().filter(d->d.id().equals(id)).findFirst()).orElse(null);
        if(before==null||baseline==null||updateState==null||view.phase()!=Phase.READY){
            refreshStatus=new RefreshStatus(id,RefreshPhase.IDLE,"Refresh not started: load a valid topic database first.");
            return CompletableFuture.failedFuture(new IOException("Load a valid topic database first"));}
        long now=System.nanoTime(),last=attempts.getOrDefault(id,Long.MIN_VALUE/2);
        if(!testTransport&&now-last<java.time.Duration.ofSeconds(30).toNanos()){
            String message="Wait 30 seconds between refreshes of the same topic.";refreshStatus=new RefreshStatus(id,RefreshPhase.IDLE,message);
            return CompletableFuture.failedFuture(new IOException(message));}
        attempts.put(id,now);RefreshTask task=new RefreshTask(id);refreshTask=task;refreshStatus=new RefreshStatus(id,RefreshPhase.FETCHING,"Checking this forum topic…");
        task.work=network.submit(()->{
            try{
                if(fetcher==null)fetcher=new ForumClient();TopicSnapshot snapshot=fetcher.fetch(before);
                if(task.cancelled)return;
                io.execute(()->{
                    try{
                        synchronized(this){if(task.cancelled)return;task.saving=true;refreshStatus=new RefreshStatus(id,RefreshPhase.SAVING,"Saving topic update…");}
                        Document current=view.library().orElseThrow().catalogue().documents().stream().filter(d->d.id().equals(id)).findFirst().orElseThrow();
                        if(!current.fingerprint().equals(before.fingerprint()))throw new IOException("Topic changed locally during refresh; retry with the current version");
                        var original=baseline.documents().stream().filter(d->d.id().equals(id)).findFirst().orElseThrow();
                        boolean changed=!normalize(before.body()).equals(normalize(snapshot.body()))||!before.title().equals(snapshot.title())||!before.author().orElse("").equals(snapshot.author())
                                ||!before.posts().equals(snapshot.posts())||snapshot.observedStatus().filter(s->s!=before.status()).isPresent();
                        Map<String,String> replies=new HashMap<>();before.posts().forEach(p->replies.put(p.id(),p.text()));int added=0,edited=0;
                        for(var p:snapshot.posts()){if(!replies.containsKey(p.id()))added++;else if(!normalize(replies.get(p.id())).equals(normalize(p.text())))edited++;}
                        UpdateStore.State staged=updateStore.stage(updateState,original.fingerprint(),snapshot,changed);
                        var applied=updateStore.apply(baseline,staged);Library prepared=new Library(applied.catalogue(),new LoreSearch(applied.catalogue()));
                        UpdateStore.State saved=updateStore.activate(staged);updateState=saved;
                        View old=view;view=new View(old.generation()+1,Phase.READY,Optional.of(prepared),old.personal(),old.writable(),old.message());
                        String message=changed?"Topic updated: "+added+" new replies, "+edited+" edited replies. Previous version saved.":"No content changes. Topic check saved.";
                        refreshStatus=new RefreshStatus(id,RefreshPhase.IDLE,message);task.result.complete(new RefreshResult(changed,added,edited,message));
                    }catch(Exception e){fail(task,e);}
                });
            }catch(Exception e){fail(task,e);}
        });return task.result;
    }
    private static String normalize(String text){return text.replaceAll("[\\s\\u00a0]+"," ").strip();}
    private synchronized void fail(RefreshTask task,Exception error){if(task.cancelled)return;
        refreshStatus=new RefreshStatus(task.id,RefreshPhase.IDLE,"Refresh failed; stored lore retained. "+error.getMessage());task.result.completeExceptionally(error);}
    public synchronized void cancelRefresh(){var task=refreshTask;if(task==null||task.saving||task.result.isDone())return;
        task.cancelled=true;if(task.work!=null)task.work.cancel(true);refreshStatus=new RefreshStatus(task.id,RefreshPhase.IDLE,"Refresh cancelled; stored lore retained.");task.result.cancel(false);}
    public CompletableFuture<RefreshResult> rollback(String id){return CompletableFuture.supplyAsync(()->{
        boolean ownOperation=false;
        try{
            synchronized(this){
                if(refreshStatus.phase()!=RefreshPhase.IDLE)throw new IOException("Finish or cancel the current refresh first");
                if(!canRollback(id))throw new IOException("No matching saved topic refresh to undo");
                refreshStatus=new RefreshStatus(id,RefreshPhase.SAVING,"Restoring previous topic version…");ownOperation=true;
            }
            UpdateStore.State staged=updateStore.rollback(updateState,id);var applied=updateStore.apply(baseline,staged);
            Library prepared=new Library(applied.catalogue(),new LoreSearch(applied.catalogue()));updateState=updateStore.activate(staged);
            var old=view;view=new View(old.generation()+1,Phase.READY,Optional.of(prepared),old.personal(),old.writable(),old.message());
            String message="Previous saved topic version restored.";refreshStatus=new RefreshStatus(id,RefreshPhase.IDLE,message);return new RefreshResult(true,0,0,message);
        }catch(IOException e){if(ownOperation)refreshStatus=new RefreshStatus(id,RefreshPhase.IDLE,"Undo failed; stored lore retained. "+e.getMessage());throw new CompletionException(e);}
    },io);}
    public synchronized CompletableFuture<LoreSearch.Page> search(LoreSearch.Query query) {
        if (currentSearch != null) currentSearch.cancel(true);
        if (currentResult != null) currentResult.cancel(false);
        Optional<Library> library = view.library(); CompletableFuture<LoreSearch.Page> result = new CompletableFuture<>();
        currentResult = result;
        currentSearch = searches.submit(() -> {
            try { result.complete(library.map(l -> l.search().search(query)).orElseGet(() -> new LoreSearch.Page(List.of(),0,0))); }
            catch (Throwable e) { result.completeExceptionally(e); }
        });
        return result;
    }
    public CompletableFuture<View> toggleFavorite(String id) { return change(s -> s.toggle(id)); }
    public CompletableFuture<ReaderContent> reader(LoreCatalogue.Document document, boolean details) {
        return CompletableFuture.supplyAsync(() -> details ? ReaderContent.details(document) : ReaderContent.document(document),io);
    }
    public CompletableFuture<View> remember(PersonalState.ReadingPosition position) { return change(s -> s.remember(position)); }
    private CompletableFuture<View> change(UnaryOperator<PersonalState> update) {
        return CompletableFuture.supplyAsync(() -> {
            View old = view;
            if (!old.writable() || personal == null) return old;
            try {
                personal = store.save(update.apply(personal.state()),personal.fileHash());
                view = new View(old.generation()+1,old.phase(),old.library(),personal.state(),true,old.message());
            } catch (IOException | RuntimeException e) {
                view = new View(old.generation()+1,old.phase(),old.library(),old.personal(),false,"Personal save failed; original retained. " + e.getMessage());
            }
            return view;
        },io);
    }
    @Override public void close() {
        cancelRefresh();network.shutdownNow();searches.shutdownNow(); io.shutdown();
        try { if (!io.awaitTermination(10,TimeUnit.SECONDS)) io.shutdownNow(); }
        catch (InterruptedException e) { io.shutdownNow(); Thread.currentThread().interrupt(); }
    }
}
