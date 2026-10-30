package net.mclorehub.library;

import com.google.gson.*;
import net.mclorehub.catalogue.LoreCatalogue;
import net.mclorehub.importer.ExportImporter;
import net.mclorehub.updates.BaselineTrust;
import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.zip.*;
import static net.mclorehub.importer.JsonSupport.*;

/** Offline import preparation. ZIP contents are data and never extracted to supplied paths. */
public final class PackImport {
    private PackImport(){}
    public record Input(JsonObject pack,JsonObject trust,JsonObject reviews){}
    public record Change(String id,String title,String kind){}
    public record Preview(String token,String fileName,List<Change> changes,int total,int current,String message){public Preview{changes=List.copyOf(changes);}}
    public static Input readInput(Path path,Path scratch)throws IOException {
        byte[] input;try(var in=Files.newInputStream(path)){input=bounded(in,64<<20);}String name=path.getFileName().toString().toLowerCase(Locale.ROOT);
        Map<String,byte[]> normalized=new HashMap<>();
        if(name.endsWith(".zip"))try(var zip=new ZipInputStream(new ByteArrayInputStream(input))){int count=0;long expanded=0;ZipEntry entry;
            while((entry=zip.getNextEntry())!=null){if(++count>32)throw new IOException("Too many archive entries");String n=entry.getName();
                if(n.contains("\\")||n.startsWith("/")||Arrays.asList(n.split("/")).contains(".."))throw new IOException("Unsafe archive path");
                byte[] data=bounded(zip,64<<20);expanded+=data.length;if(expanded>64L<<20)throw new IOException("Archive exceeds expanded limit");
                if(Set.of("pack.json","trust.json","reviews.json").contains(n)&&normalized.putIfAbsent(n,data)!=null)throw new IOException("Duplicate pack archive entry");
            }
        }
        if(normalized.containsKey("pack.json"))return new Input(parse(normalized.get("pack.json")),normalized.containsKey("trust.json")?parse(normalized.get("trust.json")):null,normalized.containsKey("reviews.json")?parse(normalized.get("reviews.json")):null);
        if(name.endsWith(".json")){var object=parse(input);if(object.has("kind"))return new Input(object,null,null);}
        if(!name.endsWith(".zip")&&!name.endsWith(".json"))throw new IOException("Choose a LoreDB ZIP/JSON export or normalized pack");
        Files.createDirectories(scratch);Path copy=Files.createTempFile(scratch,"import-",name.endsWith(".zip")?".zip":".json");
        try{Files.write(copy,input);return new Input(new ExportImporter().importFile(copy).pack(),null,null);}finally{Files.deleteIfExists(copy);}
    }
    public static List<Change> changes(LoreCatalogue before,LoreCatalogue after){
        Map<String,LoreCatalogue.Document> old=new HashMap<>();if(before!=null)before.documents().forEach(d->old.put(d.id(),d));List<Change> changes=new ArrayList<>();
        for(var d:after.documents()){var prior=old.remove(d.id());if(prior==null)changes.add(new Change(d.id(),d.title(),"Added"));
            else if(!prior.body().equals(d.body())||!prior.title().equals(d.title())||!prior.author().equals(d.author())||prior.status()!=d.status())changes.add(new Change(d.id(),d.title(),"Changed"));}
        old.values().forEach(d->changes.add(new Change(d.id(),d.title(),"Not in replacement export")));
        changes.sort(Comparator.comparing(Change::kind).thenComparing(Change::title).thenComparing(Change::id));return List.copyOf(changes);
    }
}
