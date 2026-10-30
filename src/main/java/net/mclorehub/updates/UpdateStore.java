package net.mclorehub.updates;

import com.google.gson.*;
import net.mclorehub.catalogue.LoreCatalogue;
import net.mclorehub.catalogue.LoreCatalogue.Document;
import net.mclorehub.importer.ImportException;
import net.mclorehub.library.AtomicJsonFile;
import java.io.IOException;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static net.mclorehub.importer.JsonSupport.*;

/** Immutable per-topic snapshots plus one atomic active pointer; never touches packs/personal data. */
public final class UpdateStore {
    public record Entry(String baseFingerprint,String active,String previous){}
    public record State(Map<String,Entry> entries,Optional<String> fileHash){public State{entries=Map.copyOf(entries);}}
    public record Applied(LoreCatalogue catalogue,int ignored){}
    private final Path snapshots;private final AtomicJsonFile pointer;
    public UpdateStore(Path root){snapshots=root.resolve("updates/snapshots");pointer=new AtomicJsonFile(root.resolve("updates/active.json"),4<<20);}
    public State load()throws IOException{
        byte[] bytes=pointer.read();if(bytes==null)return new State(Map.of(),Optional.empty());JsonObject json=parse(bytes);
        if(!json.keySet().equals(Set.of("schema_version","topics")))throw new ImportException("Unknown/missing refresh pointer fields");
        if(integer(json,"schema_version")!=1)throw new ImportException("Unsupported refresh pointer schema");
        Map<String,Entry> entries=new HashMap<>();JsonObject list=object(json.get("topics"),"topics");if(list.size()>10000)throw new ImportException("Too many topic overrides");
        for(String id:list.keySet()){
            if(!id.matches("lotc:topic:[1-9][0-9]{0,17}"))throw new ImportException("Invalid refreshed document ID");
            JsonObject e=object(list.get(id),"topic pointer");if(!e.keySet().equals(Set.of("base_fingerprint","active","previous")))throw new ImportException("Unknown/missing topic pointer fields");
            String previous=string(e,"previous");
            if(!previous.isEmpty()&&!previous.matches("[a-f0-9]{64}"))throw new ImportException("Invalid previous snapshot hash");
            entries.put(id,new Entry(sha(e,"base_fingerprint"),sha(e,"active"),previous));
        }
        return new State(entries,Optional.of(hash(bytes)));
    }
    public Applied apply(LoreCatalogue baseline,State state)throws IOException{
        Map<String,Document> changed=new HashMap<>();long bytes=0;Set<String> applied=new HashSet<>();
        for(Document base:baseline.documents()){
            Entry entry=state.entries().get(base.id());if(entry==null||!entry.baseFingerprint().equals(base.fingerprint()))continue;
            Path file=snapshots.resolve(entry.active()+".json");bytes+=Files.size(file);if(bytes>64L<<20)throw new ImportException("Active topic updates exceed 64 MiB limit");
            byte[] data;try(var in=Files.newInputStream(file)){data=bounded(in,16<<20);}
            if(!hash(data).equals(entry.active()))throw new ImportException("Stored topic snapshot checksum mismatch");
            TopicSnapshot snapshot=TopicSnapshot.parse(parse(data));if(!snapshot.documentId().equals(base.id()))throw new ImportException("Stored topic ID mismatch");
            changed.put(base.id(),snapshot.apply(base));applied.add(base.id());
        }
        return new Applied(baseline.replace(changed),state.entries().size()-applied.size());
    }
    public State stage(State old,String baseFingerprint,TopicSnapshot snapshot,boolean changed)throws IOException{
        // Validate even injected/test fetchers before anything can become active.
        TopicSnapshot.parse(snapshot.json());byte[] bytes=(JSON.toJson(snapshot.json())+"\n").getBytes(StandardCharsets.UTF_8);
        if(bytes.length>16<<20)throw new ImportException("Topic snapshot exceeds size limit");String digest=hash(bytes);Files.createDirectories(snapshots);
        Path target=snapshots.resolve(digest+".json");
        if(Files.exists(target,LinkOption.NOFOLLOW_LINKS)){
            try(var in=Files.newInputStream(target)){if(!hash(bounded(in,16<<20)).equals(digest))throw new ImportException("Existing snapshot is damaged; original retained");}
        }else{
            Path temp=Files.createTempFile(snapshots,".topic-",".tmp");try{
                Files.write(temp,bytes);try(var channel=FileChannel.open(temp,StandardOpenOption.WRITE)){channel.force(true);}
                Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE);
            }finally{Files.deleteIfExists(temp);}
        }
        Map<String,Entry> next=new HashMap<>(old.entries());Entry previous=next.get(snapshot.documentId());
        String undo=previous==null||!previous.baseFingerprint().equals(baseFingerprint)?"":changed?previous.active():previous.previous();
        next.put(snapshot.documentId(),new Entry(baseFingerprint,digest,undo));return new State(next,old.fileHash());
    }
    public State rollback(State old,String id)throws IOException{
        Entry entry=old.entries().get(id);if(entry==null)throw new ImportException("No saved topic refresh to undo");Map<String,Entry> next=new HashMap<>(old.entries());
        if(entry.previous().isEmpty())next.remove(id);else next.put(id,new Entry(entry.baseFingerprint(),entry.previous(),entry.active()));
        return new State(next,old.fileHash());
    }
    public State activate(State next)throws IOException{
        JsonObject json=new JsonObject();json.addProperty("schema_version",1);JsonObject topics=new JsonObject();
        new TreeMap<>(next.entries()).forEach((id,e)->{JsonObject item=new JsonObject();item.addProperty("base_fingerprint",e.baseFingerprint());item.addProperty("active",e.active());item.addProperty("previous",e.previous());topics.add(id,item);});
        json.add("topics",topics);return new State(next.entries(),Optional.of(pointer.save(json,next.fileHash())));
    }
    public Document previous(Document base,State state)throws IOException {
        var entry=state.entries().get(base.id());if(entry==null||!entry.baseFingerprint().equals(base.fingerprint()))throw new IOException("No saved main-post comparison");
        if(entry.previous().isEmpty())return base;Path file=snapshots.resolve(entry.previous()+".json");byte[] bytes;try(var in=Files.newInputStream(file)){bytes=bounded(in,16<<20);}if(!hash(bytes).equals(entry.previous()))throw new IOException("Previous snapshot checksum mismatch");return TopicSnapshot.parse(parse(bytes)).apply(base);
    }
}
