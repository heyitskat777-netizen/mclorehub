package net.mclorehub.updates;

import net.mclorehub.library.*;
import java.time.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/** Runs only with Minecraft/mod active. No discovery and no overlapping/manual cancellation. */
public final class MonthlyUpdater implements AutoCloseable {
    public record View(boolean enabled,boolean writable,String current,int due,Optional<Instant> next,String message){}
    private final LibraryService library;private final Clock clock;private final AtomicJsonFile file;
    private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon().name("mclorehub-monthly").factory());
    private MonthlyPlan plan;private Optional<String> hash=Optional.empty();private volatile boolean allowed=true,closed=false;
    private volatile CompletableFuture<LibraryService.RefreshResult> active;
    private volatile View view=new View(true,true,"",0,Optional.empty(),"Monthly checks load when the library is ready.");
    public MonthlyUpdater(LibraryService library){this(library,Clock.systemUTC());}
    public MonthlyUpdater(LibraryService library,Clock clock){this.library=library;this.clock=clock;file=new AtomicJsonFile(library.directory().resolve("updates/monthly.json"),4<<20);}
    public View view(){return view;}
    public void start(){worker.scheduleWithFixedDelay(()->{try{cycle();}catch(Exception e){stopOnError();}},1,2,TimeUnit.SECONDS);}
    public CompletableFuture<View> runOnce(){return CompletableFuture.supplyAsync(()->{try{cycle();}catch(Exception e){stopOnError();}return view;},worker);}
    public CompletableFuture<View> setEnabled(boolean enabled){allowed=enabled;if(!enabled&&active!=null)library.cancelRefresh(active);
        return CompletableFuture.supplyAsync(()->{try{initialize();allowed=enabled;if(plan!=null){save(plan.enabled(enabled));publish("Monthly checks "+(enabled?"enabled.":"paused."));}}
            catch(Exception e){stopOnError();}return view;},worker);}
    private void initialize()throws IOException{if(plan!=null)return;byte[] bytes=file.read();plan=bytes==null?MonthlyPlan.empty():MonthlyPlan.parse(net.mclorehub.importer.JsonSupport.parse(bytes));
        hash=bytes==null?Optional.empty():Optional.of(net.mclorehub.importer.JsonSupport.hash(bytes));allowed=plan.enabled();}
    private void save(MonthlyPlan next)throws IOException{hash=Optional.of(file.save(next.json(),hash));plan=next;}
    private void cycle()throws Exception{
        if(closed||!view.writable()||library.view().phase()!=LibraryService.Phase.READY)return;
        initialize();var docs=library.view().library().orElseThrow().catalogue().documents();var synced=plan.sync(docs,clock.instant());if(!synced.equals(plan))save(synced);
        if(!allowed||!plan.enabled()||library.refreshStatus().phase()!=LibraryService.RefreshPhase.IDLE){publish(allowed?"Waiting for the active topic update.":"Monthly checks paused.");return;}
        var due=plan.due(docs,clock.instant());if(due.isEmpty()){publish("Monthly main-lore checks are scheduled.");return;}
        String id=due.get();view=new View(true,true,id,view.due(),view.next(),"Checking saved main lore…");
        try{active=library.refresh(id);if(!allowed)library.cancelRefresh(active);active.get(120,TimeUnit.SECONDS);
            save(plan.result(id,clock.instant(),"OK",null));publish("Main lore checked; next check in one calendar month.");
        }catch(CancellationException e){if(!closed&&allowed)save(plan.result(id,clock.instant(),"CANCELLED",clock.instant().plus(Duration.ofDays(1))));publish("Automatic check cancelled; stored lore retained.");}
        catch(ExecutionException e){Throwable cause=e.getCause();if(cause instanceof LibraryService.BusyException){publish("Waiting for the active topic update.");return;}
            String message=cause.getMessage()==null?"":cause.getMessage();String outcome=cause instanceof ForumRateLimitException?"RATE_LIMIT":message.contains("HTTP 401")||message.contains("HTTP 403")||message.contains("HTTP 404")?"ACCESS":"NETWORK";
            Instant retry=cause instanceof ForumRateLimitException r?r.until():null;save(plan.result(id,clock.instant(),outcome,retry));publish(outcome.equals("ACCESS")?"Page not accessible; stored lore retained until the next monthly check.":"Check failed; stored lore retained. Backoff saved.");
        }catch(TimeoutException e){library.cancelRefresh(active);save(plan.result(id,clock.instant(),"NETWORK",null));publish("Automatic check timed out; stored lore retained.");}
        finally{active=null;}
    }
    private void publish(String message){if(plan==null)return;var docs=library.view().library().map(l->l.catalogue().documents()).orElse(List.of());Set<String> ids=new HashSet<>();docs.forEach(d->ids.add(d.id()));Instant now=clock.instant();
        int due=(int)plan.topics().entrySet().stream().filter(e->ids.contains(e.getKey())&&!e.getValue().next().isAfter(now)).count();
        Optional<Instant> next=plan.topics().entrySet().stream().filter(e->ids.contains(e.getKey())).map(e->e.getValue().next()).min(Comparator.naturalOrder()).map(t->t.isBefore(plan.notBefore())?plan.notBefore():t);
        view=new View(allowed&&plan.enabled(),true,"",due,next,message);}
    private void stopOnError(){allowed=false;view=new View(false,false,"",view.due(),view.next(),"Monthly checks paused: schedule could not be read/saved. Original retained; repair updates/monthly.json.");}
    @Override public void close(){closed=true;allowed=false;if(active!=null)library.cancelRefresh(active);worker.shutdown();try{if(!worker.awaitTermination(10,TimeUnit.SECONDS))worker.shutdownNow();}
        catch(InterruptedException e){worker.shutdownNow();Thread.currentThread().interrupt();}}
}
