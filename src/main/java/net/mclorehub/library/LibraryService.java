package net.mclorehub.library;

import net.mclorehub.catalogue.LoreCatalogue;
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
    private final Path directory;
    private final PersonalStore store;
    private final ExecutorService io = Executors.newSingleThreadExecutor(Thread.ofPlatform().daemon().name("mclorehub-files").factory());
    private final ExecutorService searches = Executors.newSingleThreadExecutor(Thread.ofPlatform().daemon().name("mclorehub-search").factory());
    private volatile View view = new View(0, Phase.LOADING, Optional.empty(), PersonalState.empty(), false, "");
    private PersonalStore.Loaded personal;
    private Future<?> currentSearch;
    private CompletableFuture<LoreSearch.Page> currentResult;
    public LibraryService(Path directory) { this.directory = directory; store = new PersonalStore(directory.resolve("personal/state.json")); }
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
                    library = Optional.of(new Library(catalogue,new LoreSearch(catalogue))); phase = Phase.READY;
                }
            } catch (IOException | RuntimeException e) {
                phase = Phase.ERROR; warning = "Database reload failed; previous valid library retained. " + warning + " " + e.getMessage();
            }
            view = new View(previous.generation()+1,phase,library,state,writable,warning.strip()); return view;
        },io);
    }
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
        searches.shutdownNow(); io.shutdown();
        try { if (!io.awaitTermination(10,TimeUnit.SECONDS)) io.shutdownNow(); }
        catch (InterruptedException e) { io.shutdownNow(); Thread.currentThread().interrupt(); }
    }
}
