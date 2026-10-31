package net.mclorehub.library;

import net.mclorehub.catalogue.LoreCatalogue;
import net.mclorehub.catalogue.LoreCatalogue.Document;
import net.mclorehub.updates.*;
import net.mclorehub.character.*;
import net.mclorehub.spell.SpellIndex;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.UnaryOperator;

/** All filesystem, parsing, indexing, searching and personal writes run on owned workers. */
public final class LibraryService implements AutoCloseable {
    public enum Phase { LOADING, EMPTY, READY, ERROR }
    public record Library(LoreCatalogue catalogue, LoreSearch search, CharacterLore characterLore,SpellIndex spells) {
        public Library(LoreCatalogue catalogue,LoreSearch search)throws IOException {this(catalogue,search,CharacterLore.bundled(catalogue),new SpellIndex(catalogue));}
        public Library(LoreCatalogue catalogue,LoreSearch search,CharacterLore lore){this(catalogue,search,lore,new SpellIndex(catalogue));}
    }
    public record View(long generation, Phase phase, Optional<Library> library, PersonalState personal,
                       boolean writable, String message) {}
    public enum RefreshPhase { IDLE, FETCHING, SAVING }
    public record RefreshStatus(String documentId,RefreshPhase phase,String message){}
    public record RefreshResult(boolean changed,int addedReplies,int editedReplies,String message){}
    public static final class BusyException extends IOException { public BusyException(){super("Another topic update is already running");} }
    private final Path directory;
    private final PersonalStore store;
    private final UpdateStore updateStore;
    private final ForumSession session;
    private final PackStore packs;
    private volatile PackStore.Selection packSelection;
    private Path baselinePath;
    private String baselineStamp="";
    private PreparedImport importPreview;
    private record PreparedImport(PackImport.Preview preview,String revision,Library before,Library after,LoreCatalogue baseline,PackStore.Selection selection,UpdateStore.State updates){}
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
        this.directory = directory; store = new PersonalStore(directory.resolve("personal/state.json"));updateStore=new UpdateStore(directory);session=new ForumSession(directory);packs=new PackStore(directory);
        this.fetcher=fetcher;testTransport=fetcher!=null;
    }
    public View view() { return view; }
    public Path directory() { return directory; }
    public ForumSession session(){return session;}
    public CompletableFuture<View> reload() {
        return CompletableFuture.supplyAsync(() -> {
            View previous = view; String warning = ""; boolean writable = true;
            try { personal = store.load(); }
            catch (IOException e) { writable = false; warning = "Personal state is read-only: " + e.getMessage(); }
            PersonalState state = personal == null ? PersonalState.empty() : personal.state();
            Optional<Library> library = previous.library(); Phase phase;
            try {
                var selected=packs.load();Path pack=packs.selected(selected), reviews = pack.resolveSibling("reviews.json");
                if (!Files.exists(pack, LinkOption.NOFOLLOW_LINKS)) {
                    if(library.isPresent()) throw new IOException("Active pack is missing");
                    phase = Phase.EMPTY;
                    packSelection=selected;baselinePath=pack;
                }
                else {
                    LoreCatalogue catalogue = LoreCatalogue.load(pack, Files.exists(reviews,LinkOption.NOFOLLOW_LINKS) ? reviews : null);
                    catalogue=BaselineTrust.apply(catalogue,pack.resolveSibling("trust.json"));
                    UpdateStore.State stateUpdates=updateStore.load();var applied=updateStore.apply(catalogue,stateUpdates);
                    Library prepared=new Library(applied.catalogue(),new LoreSearch(applied.catalogue()));
                    baseline=catalogue;baselinePath=pack;baselineStamp=packs.stamp(pack);packSelection=selected;updateState=stateUpdates;library=Optional.of(prepared);phase=Phase.READY;
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
        if(refreshStatus.phase()!=RefreshPhase.IDLE||refreshTask!=null&&!refreshTask.result.isDone())return CompletableFuture.failedFuture(new BusyException());
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
                if(fetcher==null){session.reload().get(5,TimeUnit.SECONDS);fetcher=new ForumClient(new ForumHttp(session::header),java.time.Duration.ofSeconds(1),java.time.Clock.systemUTC());}
                TopicSnapshot fetched=fetcher.fetch(before);
                if(task.cancelled)return;
                io.execute(()->{
                    TopicSnapshot snapshot=fetched;
                    try{
                        synchronized(this){if(task.cancelled)return;task.saving=true;refreshStatus=new RefreshStatus(id,RefreshPhase.SAVING,"Saving topic update…");}
                        Document current=view.library().orElseThrow().catalogue().documents().stream().filter(d->d.id().equals(id)).findFirst().orElseThrow();
                        if(!current.fingerprint().equals(before.fingerprint()))throw new IOException("Topic changed locally during refresh; retry with the current version");
                        var original=baseline.documents().stream().filter(d->d.id().equals(id)).findFirst().orElseThrow();
                        boolean changed=!normalize(before.body()).equals(normalize(snapshot.body()))||!before.title().equals(snapshot.title())||!before.author().orElse("").equals(snapshot.author())
                                ||snapshot.observedStatus().filter(s->s!=before.status()).isPresent();
                        int added=0,edited=0;
                        // Discussion is an archive, not part of main-lore updates/comparisons.
                        snapshot=new TopicSnapshot(snapshot.documentId(),snapshot.url(),snapshot.title(),snapshot.author(),snapshot.body(),before.posts(),snapshot.pages(),snapshot.fetchedAt(),snapshot.observedStatus(),
                                before.coverageGaps().stream().filter(g->g.startsWith("previous_reply_not_returned:")).map(g->g.substring("previous_reply_not_returned:".length())).toList());
                        UpdateStore.State staged=updateStore.stage(updateState,original.fingerprint(),snapshot,changed);
                        var applied=updateStore.apply(baseline,staged);Library prepared=new Library(applied.catalogue(),new LoreSearch(applied.catalogue()));
                        UpdateStore.State saved=updateStore.activate(staged);updateState=saved;
                        View old=view;view=new View(old.generation()+1,Phase.READY,Optional.of(prepared),old.personal(),old.writable(),old.message());
                        String message=changed?"Main lore updated. Previous version saved.":"No main lore changes. Check saved.";
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
    public synchronized void cancelRefresh(CompletableFuture<RefreshResult> expected){if(refreshTask!=null&&refreshTask.result==expected)cancelRefresh();}
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
    public CompletableFuture<ReaderContent> archivedReplies(Document document){return CompletableFuture.supplyAsync(()->ReaderContent.discussion(document),io);}
    public CompletableFuture<CharacterLore.Report> assess(CharacterBook.Sheet sheet){
        var snapshot=view.library();return CompletableFuture.supplyAsync(()->snapshot.orElseThrow(()->new IllegalStateException("Load a lore database first")).characterLore().assess(sheet),io);
    }
    public CompletableFuture<SpellIndex.Results> spells(CharacterBook.Sheet sheet,String query,boolean allTiers){
        var snapshot=view.library();return CompletableFuture.supplyAsync(()->snapshot.orElseThrow(()->new IllegalStateException("Load a lore database first")).spells().search(sheet,query,allTiers),searches);
    }
    public CompletableFuture<ReaderContent> spellReference(SpellIndex.Hit hit,boolean practice){return CompletableFuture.supplyAsync(()->practice?SpellIndex.practice(hit):SpellIndex.details(hit),io);}
    public CompletableFuture<ReaderContent> comparison(String id){return CompletableFuture.supplyAsync(()->{
        try{var base=baseline.documents().stream().filter(d->d.id().equals(id)).findFirst().orElseThrow();var before=updateStore.previous(base,updateState);var after=view.library().orElseThrow().catalogue().documents().stream().filter(d->d.id().equals(id)).findFirst().orElseThrow();return ReaderContent.comparison(before,after);}
        catch(IOException e){throw new CompletionException(e);}
    },io);}
    public boolean canUndoImport(){var selection=packSelection;return selection!=null&&!selection.previous().isEmpty();}
    public CompletableFuture<ReaderContent> importComparison(String token,String id){return CompletableFuture.supplyAsync(()->{
        var prepared=importPreview;if(prepared==null||!prepared.preview().token().equals(token))throw new CompletionException(new IOException("Prepare a new import preview"));
        var before=prepared.before()==null?null:prepared.before().catalogue().documents().stream().filter(d->d.id().equals(id)).findFirst().orElse(null);
        var after=prepared.after().catalogue().documents().stream().filter(d->d.id().equals(id)).findFirst().orElse(null);
        if(before==null&&after==null)throw new CompletionException(new IOException("Unknown topic in import preview"));
        return before!=null&&after!=null?ReaderContent.comparison(before,after):ReaderContent.text("Import comparison",before==null?"Added main lore\n\n"+after.body():"Not in replacement export. Personal references are retained; this does not establish revocation.\n\nPrevious main lore:\n"+before.body());
    },io);}
    public CompletableFuture<PackImport.Preview> prepareImport(Path path,boolean confirmCurrent){return CompletableFuture.supplyAsync(()->{
        try{
            if(refreshStatus.phase()!=RefreshPhase.IDLE)throw new IOException("Finish the current topic update first");
            if(baselinePath!=null&&!packs.stamp(baselinePath).equals(baselineStamp))throw new IOException("Active source files changed outside the mod; reload before importing");
            var before=view.library().orElse(null);var selection=packs.load();if(packSelection!=null&&!selection.equals(packSelection))throw new IOException("Pack selection changed outside the mod; reload before importing");var input=PackImport.readInput(path,directory.resolve("staging"));
            // Validate before creating trust. Existing explicit decisions remain bound and may become stale.
            var reviews=input.reviews();if(reviews==null&&baselinePath!=null&&Files.exists(baselinePath.resolveSibling("reviews.json")))reviews=net.mclorehub.importer.JsonSupport.read(baselinePath.resolveSibling("reviews.json"),4<<20);
            String revision=packs.stage(input.pack(),input.trust(),reviews);Path staged=packs.verified(revision);var candidate=LoreCatalogue.load(staged,Files.exists(staged.resolveSibling("reviews.json"))?staged.resolveSibling("reviews.json"):null);
            var trust=confirmCurrent?BaselineTrust.manifest(candidate,java.time.Instant.now()):input.trust();revision=packs.stage(input.pack(),trust,reviews);staged=packs.verified(revision);
            var confirmed=BaselineTrust.apply(candidate,staged.resolveSibling("trust.json"));var updates=updateStore.load();var effective=updateStore.apply(confirmed,updates).catalogue();var after=new Library(effective,new LoreSearch(effective));
            var preview=new PackImport.Preview(UUID.randomUUID().toString(),path.getFileName().toString(),PackImport.changes(before==null?null:before.catalogue(),effective),effective.documents().size(),effective.currentDocuments().size(),"Personal files stay separate. Missing topics are export gaps, not evidence of revocation.");
            importPreview=new PreparedImport(preview,revision,before,after,confirmed,selection,updates);return preview;
        }catch(IOException e){throw new CompletionException(e);}
    },io);}
    public CompletableFuture<View> activateImport(String token){return CompletableFuture.supplyAsync(()->{
        boolean owned=false;try{
            synchronized(this){if(refreshStatus.phase()!=RefreshPhase.IDLE)throw new BusyException();refreshStatus=new RefreshStatus("",RefreshPhase.SAVING,"Activating database…");owned=true;}
            var prepared=importPreview;if(prepared==null||!prepared.preview().token().equals(token)||view.library().orElse(null)!=prepared.before())throw new IOException("Database changed; prepare a new preview");
            if(!Objects.equals(packs.load(),prepared.selection()))throw new IOException("Pack selection changed outside the mod; reload");
            if(baselinePath!=null&&!packs.stamp(baselinePath).equals(baselineStamp))throw new IOException("Active source files changed; reload before importing");
            if(updateState!=null&&!updateStore.load().fileHash().equals(prepared.updates().fileHash()))throw new IOException("Topic updates changed; prepare a new preview");
            String previous=baselinePath!=null&&Files.exists(baselinePath)?packs.snapshot(baselinePath):"";
            var selected=packs.activate(prepared.selection(),prepared.revision(),previous);
            packSelection=selected;baselinePath=packs.verified(selected.active());baselineStamp=packs.stamp(baselinePath);baseline=prepared.baseline();updateState=prepared.updates();var old=view;
            view=new View(old.generation()+1,Phase.READY,Optional.of(prepared.after()),old.personal(),old.writable(),"");importPreview=null;return view;
        }catch(IOException e){throw new CompletionException(e);}finally{if(owned)refreshStatus=new RefreshStatus("",RefreshPhase.IDLE,"");}
    },io);}
    public CompletableFuture<View> undoImport(){return CompletableFuture.supplyAsync(()->{
        boolean owned=false;try{
            synchronized(this){if(refreshStatus.phase()!=RefreshPhase.IDLE)throw new BusyException();refreshStatus=new RefreshStatus("",RefreshPhase.SAVING,"Restoring database…");owned=true;}
            var selected=packs.load();if(packSelection!=null&&!selected.equals(packSelection))throw new IOException("Pack selection changed outside the mod; reload before Undo");if(selected.previous().isEmpty())throw new IOException("No previous imported database");var path=packs.verified(selected.previous());
            var original=BaselineTrust.apply(LoreCatalogue.load(path,Files.exists(path.resolveSibling("reviews.json"))?path.resolveSibling("reviews.json"):null),path.resolveSibling("trust.json"));
            var updates=updateStore.load();var effective=updateStore.apply(original,updates).catalogue();var library=new Library(effective,new LoreSearch(effective));
            String redo=selected.active(),warning="";try{packs.verified(redo);}catch(IOException damaged){redo="";warning="Previous database restored. Damaged revision retained separately; redo unavailable.";}
            var saved=packs.activate(selected,selected.previous(),redo);packSelection=saved;baselinePath=path;baselineStamp=packs.stamp(path);baseline=original;updateState=updates;var old=view;view=new View(old.generation()+1,Phase.READY,Optional.of(library),old.personal(),old.writable(),warning);importPreview=null;return view;
        }catch(IOException e){throw new CompletionException(e);}finally{if(owned)refreshStatus=new RefreshStatus("",RefreshPhase.IDLE,"");}
    },io);}
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
        cancelRefresh();network.shutdownNow();searches.shutdownNow(); io.shutdown();session.close();
        try { if (!io.awaitTermination(10,TimeUnit.SECONDS)) io.shutdownNow(); }
        catch (InterruptedException e) { io.shutdownNow(); Thread.currentThread().interrupt(); }
    }
}
