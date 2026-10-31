package net.mclorehub.spell;

import com.google.gson.*;
import net.mclorehub.catalogue.LoreCatalogue;
import net.mclorehub.character.*;
import net.mclorehub.updates.BaselineTrust;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static net.mclorehub.importer.JsonSupport.*;

/** Offline database companion index. Exact excerpts remain tied to their current source. */
public final class SpellCommand {
    private SpellCommand(){}
    public static int run(String[] args,PrintStream out,PrintStream err){
        try{
            Map<String,String> options=new HashMap<>();if(args.length%2!=0)throw new IOException("Expected option/value pairs");
            for(int i=0;i<args.length;i+=2)if(!Set.of("--pack","--trust","--reviews","--output").contains(args[i])||options.putIfAbsent(args[i],args[i+1])!=null)throw new IOException("Unknown/duplicate spell-index option");
            if(!options.containsKey("--pack")||!options.containsKey("--output"))throw new IOException("Expected --pack and --output");
            var catalogue=LoreCatalogue.load(Path.of(options.get("--pack")),options.containsKey("--reviews")?Path.of(options.get("--reviews")):null);
            if(options.containsKey("--trust"))catalogue=BaselineTrust.apply(catalogue,Path.of(options.get("--trust")));
            var lore=CharacterLore.bundled(catalogue);Set<String> ids=new TreeSet<>();for(var kind:CharacterBook.Kind.values())lore.choices(kind,"").forEach(c->ids.add(c.option().sourceId()));
            SpellIndex index=new SpellIndex(catalogue);JsonArray sources=new JsonArray(),abilities=new JsonArray();
            for(var d:catalogue.documents())if(ids.contains(d.id())){
                JsonObject source=new JsonObject();source.addProperty("id",d.id());source.addProperty("title",d.title());source.addProperty("url",d.url());source.addProperty("snapshot_at",d.exportSnapshotAt().toString());source.addProperty("fingerprint",CharacterLore.mainFingerprint(d));source.addProperty("current",LoreCatalogue.isCurrent(d));
                int count=0;
                for(var a:index.references(d.id()))if(!a.sourceReference()){
                    JsonObject record=object(JSON.toJsonTree(a),"ability");record.addProperty("information",d.body().substring(a.start(),a.end()));record.addProperty("source_url",d.url());record.addProperty("source_snapshot_at",d.exportSnapshotAt().toString());
                    record.addProperty("parent_context",a.parentStart()<0?"":d.body().substring(a.parentStart(),a.parentEnd()));abilities.add(record);count++;
                }
                source.addProperty("indexed_sections",count);sources.add(source);
            }
            JsonObject result=new JsonObject();result.addProperty("schema_version",1);result.addProperty("basis","source-heading reference extraction; unresolved tiers/conditions remain explicit; not learning or application approval");result.add("sources",sources);result.add("abilities",abilities);
            writeNew(Path.of(options.get("--output")),result);out.println("Wrote "+abilities.size()+" source sections from "+sources.size()+" linked practice sources.");return 0;
        }catch(IOException|IllegalArgumentException e){err.println("Spell index failed: "+e.getMessage());return 1;}
    }
}
