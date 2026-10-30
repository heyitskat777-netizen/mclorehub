package net.mclorehub.library;

import com.google.gson.*;
import net.mclorehub.catalogue.LoreCatalogue;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static net.mclorehub.importer.JsonSupport.*;

/** Immutable complete pack revisions plus an atomic selection pointer. Legacy installation remains readable. */
public final class PackStore {
    public record Selection(String active,String previous,Optional<String> fileHash){}
    private final Path root,revisions;private final AtomicJsonFile pointer;
    public PackStore(Path directory){root=directory.resolve("packs");revisions=root.resolve("revisions");pointer=new AtomicJsonFile(root.resolve("selection.json"),16384);}
    public Selection load()throws IOException {
        byte[] bytes=pointer.read();if(bytes==null)return new Selection("","",Optional.empty());var r=parse(bytes);
        if(!r.keySet().equals(Set.of("schema_version","active","previous"))||integer(r,"schema_version")!=1)throw new IOException("Invalid pack selection; retained");
        String active=sha(r,"active"),previous=string(r,"previous");if(!previous.isEmpty()&&!previous.matches("[a-f0-9]{64}"))throw new IOException("Invalid previous pack selection");
        return new Selection(active,previous,Optional.of(hash(bytes)));
    }
    public Path selected(Selection selection)throws IOException {return selection.active().isEmpty()?root.resolve("active/pack.json"):verified(selection.active());}
    public Path verified(String id)throws IOException {
        if(!id.matches("[a-f0-9]{64}"))throw new IOException("Invalid pack revision");Path dir=revisions.resolve(id);var manifest=read(dir.resolve("manifest.json"),16384);
        if(!fingerprint(manifest).equals(id)||!manifest.keySet().equals(Set.of("pack.json","trust.json","reviews.json")))throw new IOException("Pack manifest checksum mismatch");
        sha(manifest,"pack.json");
        for(String name:manifest.keySet()){
            String expected=string(manifest,name);Path file=dir.resolve(name);
            if(expected.isEmpty()){if(Files.exists(file))throw new IOException("Unexpected pack file");continue;}
            if(Files.isSymbolicLink(file))throw new IOException("Stored pack cannot be a symbolic link");byte[] bytes;try(var in=Files.newInputStream(file)){bytes=bounded(in,name.equals("pack.json")?64<<20:4<<20);}if(!hash(bytes).equals(sha(manifest,name)))throw new IOException("Stored pack checksum mismatch");
        }
        return dir.resolve("pack.json");
    }
    public String stage(JsonObject pack,JsonObject trust,JsonObject reviews)throws IOException {
        var values=new LinkedHashMap<String,JsonObject>();values.put("pack.json",pack);values.put("trust.json",trust);values.put("reviews.json",reviews);
        var manifest=new JsonObject();Map<String,byte[]> bytes=new HashMap<>();
        for(var e:values.entrySet()){byte[] data=e.getValue()==null?null:(JSON.toJson(e.getValue())+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);if(data!=null&&data.length>64<<20)throw new IOException("Pack file exceeds size limit");manifest.addProperty(e.getKey(),data==null?"":hash(data));if(data!=null)bytes.put(e.getKey(),data);}
        String id=fingerprint(manifest);Files.createDirectories(revisions);Path target=revisions.resolve(id);
        if(!Files.exists(target)){
            Path temp=Files.createTempDirectory(revisions,".pack-");try{
                for(var e:bytes.entrySet())Files.write(temp.resolve(e.getKey()),e.getValue());writeNew(temp.resolve("manifest.json"),manifest);
                try(var files=Files.list(temp)){for(var file:files.toList())try(var channel=java.nio.channels.FileChannel.open(file,StandardOpenOption.WRITE)){channel.force(true);}}
                Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE);
            }finally{if(Files.exists(temp))try(var files=Files.list(temp)){for(var p:files.toList())Files.deleteIfExists(p);}Files.deleteIfExists(temp);}
        }
        verified(id);return id;
    }
    public String snapshot(Path pack)throws IOException {return stage(read(pack,64<<20),optional(pack.resolveSibling("trust.json")),optional(pack.resolveSibling("reviews.json")));}
    public String stamp(Path pack)throws IOException {
        if(!Files.exists(pack,LinkOption.NOFOLLOW_LINKS))return "";var manifest=new JsonObject();
        for(String name:List.of("pack.json","trust.json","reviews.json")){var value=name.equals("pack.json")?read(pack,64<<20):optional(pack.resolveSibling(name));manifest.addProperty(name,value==null?"":hash((JSON.toJson(value)+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8)));}return fingerprint(manifest);
    }
    private JsonObject optional(Path file)throws IOException{return Files.exists(file,LinkOption.NOFOLLOW_LINKS)?read(file,4<<20):null;}
    public Selection activate(Selection before,String active,String previous)throws IOException {verified(active);if(!previous.isEmpty())verified(previous);var r=new JsonObject();r.addProperty("schema_version",1);r.addProperty("active",active);r.addProperty("previous",previous);return new Selection(active,previous,Optional.of(pointer.save(r,before.fileHash())));}
}
