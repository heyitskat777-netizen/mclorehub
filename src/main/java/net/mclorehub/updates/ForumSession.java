package net.mclorehub.updates;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import static net.mclorehub.importer.JsonSupport.*;

/** Optional, explicitly imported session. Public views/errors never contain credential values. */
public final class ForumSession implements AutoCloseable {
    public record View(boolean available,String message){}
    private final Path file;private final Clock clock;
    private final ExecutorService worker=Executors.newSingleThreadExecutor(Thread.ofPlatform().daemon().name("mclorehub-session").factory());
    private volatile ForumCookies cookies=ForumCookies.empty();private volatile String message="No forum session loaded.";private final AtomicLong generation=new AtomicLong();
    public ForumSession(Path root){this(root,Clock.systemUTC());}
    public ForumSession(Path root,Clock clock){file=root.resolve("auth/forum-session.json");this.clock=clock;}
    public View view(){boolean available=!header(URI.create("https://www.lordofthecraft.net/forums/topic/1-session-check/")).isEmpty();
        return new View(available,!available&&message.startsWith("Forum session loaded")?"Forum session expired; sign in again and import a new session.":message);}
    public String header(URI uri){return cookies.header(uri,clock.instant());}
    private synchronized void publish(long request,ForumCookies loaded,String text){if(request==generation.get()){cookies=loaded;message=text;}}
    public CompletableFuture<View> reload(){long request=generation.incrementAndGet();return CompletableFuture.supplyAsync(()->{
        try{if(Files.isSymbolicLink(file))throw new IOException();ForumCookies loaded=Files.exists(file,LinkOption.NOFOLLOW_LINKS)?ForumCookies.fromJson(read(file,128<<10)):ForumCookies.empty();
            boolean available=!loaded.header(URI.create("https://www.lordofthecraft.net/forums/topic/1-session-check/"),clock.instant()).isEmpty();
            publish(request,loaded,available?"Forum session loaded; account access not verified.":"No active forum session. Import a new session after signing in.");}
        catch(Exception e){if(request==generation.get()){cookies=ForumCookies.empty();message="Forum session could not be loaded; file retained. Values withheld.";}}return view();},worker);}
    public CompletableFuture<View> importFile(Path source){long request=generation.incrementAndGet();return CompletableFuture.supplyAsync(()->{
        try{byte[] bytes;try(var in=Files.newInputStream(source)){bytes=bounded(in,256<<10);}
            ForumCookies loaded=ForumCookies.netscape(new String(bytes,StandardCharsets.UTF_8),clock.instant());Files.createDirectories(file.getParent());
            if(Files.isSymbolicLink(file))throw new IOException();
            Path temp=Files.getFileStore(file.getParent()).supportsFileAttributeView("posix")?
                    Files.createTempFile(file.getParent(),".session-",".tmp",PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))):Files.createTempFile(file.getParent(),".session-",".tmp");
            try{Files.writeString(temp,JSON.toJson(loaded.json())+"\n");try(var channel=FileChannel.open(temp,StandardOpenOption.WRITE)){channel.force(true);}
                if(request==generation.get()){Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);publish(request,loaded,"Forum session loaded; account access not verified.");}
            }finally{Files.deleteIfExists(temp);}
        }catch(Exception e){if(request==generation.get())message="Session import failed. Use a valid, unexpired Netscape cookie file for this forum. Values withheld.";}
        return view();},worker);}
    public synchronized CompletableFuture<View> forget(){generation.incrementAndGet();cookies=ForumCookies.empty();message="Forum session cleared.";return CompletableFuture.supplyAsync(()->{
        try{Files.deleteIfExists(file);}catch(IOException e){message="Session disabled, but its local file could not be deleted. Remove auth/forum-session.json manually.";}return view();},worker);}
    @Override public void close(){worker.shutdown();try{if(!worker.awaitTermination(5,TimeUnit.SECONDS))worker.shutdownNow();}catch(InterruptedException e){worker.shutdownNow();Thread.currentThread().interrupt();}}
}
