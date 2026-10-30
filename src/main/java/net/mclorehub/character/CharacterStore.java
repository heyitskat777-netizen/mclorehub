package net.mclorehub.character;

import com.google.gson.*;
import net.mclorehub.importer.ImportException;
import net.mclorehub.library.AtomicJsonFile;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import static net.mclorehub.importer.JsonSupport.*;

public final class CharacterStore {
    public record Loaded(CharacterBook book,Optional<String> hash){}
    private final AtomicJsonFile file;
    public CharacterStore(Path path){file=new AtomicJsonFile(path,16<<20);}
    public Loaded load()throws IOException{
        byte[] bytes=file.read();if(bytes==null)return new Loaded(CharacterBook.empty(),Optional.empty());
        JsonObject json=parse(bytes);keys(json,"schema_version","selected_id","sheets");
        int version=integer(json,"schema_version");if(version<1||version>3)throw new ImportException("Unsupported character schema; original retained");
        try{
            List<CharacterBook.Sheet> sheets=new ArrayList<>();
            for(var item:array(json.get("sheets"),"sheets")){
                JsonObject s=object(item,"sheet");if(version==1)keys(s,"id","name","notes","archived","entries");else if(version==2)keys(s,"id","name","notes","archived","entries","facts","history");else keys(s,"id","name","notes","archived","entries","facts","history","budget");JsonElement archived=s.get("archived");
                if(archived==null||!archived.isJsonPrimitive()||!archived.getAsJsonPrimitive().isBoolean())throw new ImportException("Archived must be boolean");
                List<CharacterBook.Entry> entries=new ArrayList<>();for(var entry:array(s.get("entries"),"entries")){
                    JsonObject e=object(entry,"entry");keys(e,"id","kind","name","branch","tier","applicationUrl","status","notes","loreDocumentId");
                    entries.add(new CharacterBook.Entry(text(e,"id"),CharacterBook.Kind.valueOf(text(e,"kind")),text(e,"name"),string(e,"branch"),string(e,"tier"),
                            string(e,"applicationUrl"),CharacterBook.RecordedStatus.valueOf(text(e,"status")),string(e,"notes"),string(e,"loreDocumentId")));}
                CharacterFacts facts=CharacterFacts.unknown();List<CharacterBook.HistoryEvent> history=new ArrayList<>();
                if(version>=2){
                    JsonObject f=object(s.get("facts"),"facts");keys(f,"soul","breathing","lungs","modifiedSoul","klone","descendantRace","blindBody","holyScornedDisconnection","templarSeverances");
                    facts=new CharacterFacts(CharacterFacts.Soul.valueOf(text(f,"soul")),truth(f,"breathing"),truth(f,"lungs"),truth(f,"modifiedSoul"),truth(f,"klone"),truth(f,"descendantRace"),truth(f,"blindBody"),truth(f,"holyScornedDisconnection"),CharacterFacts.Severances.valueOf(text(f,"templarSeverances")));
                    for(var event:array(s.get("history"),"history")){var h=object(event,"history event");keys(h,"id","name","loreDocumentId","type","date","notes");history.add(new CharacterBook.HistoryEvent(text(h,"id"),text(h,"name"),string(h,"loreDocumentId"),CharacterBook.EventType.valueOf(text(h,"type")),string(h,"date"),string(h,"notes")));}
                }
                SlotBudget budget=SlotBudget.unknown();if(version==3){var b=object(s.get("budget"),"budget");keys(b,"capacity","lostSlots","allocations");var allocations=object(b.get("allocations"),"allocations");Map<String,Integer> values=new HashMap<>();for(String key:allocations.keySet())values.put(key,integer(allocations,key));budget=new SlotBudget(integer(b,"capacity"),integer(b,"lostSlots"),values);}
                sheets.add(new CharacterBook.Sheet(text(s,"id"),text(s,"name"),string(s,"notes"),archived.getAsBoolean(),entries,facts,history,budget));}
            return new Loaded(new CharacterBook(sheets,string(json,"selected_id")),Optional.of(hash(bytes)));
        }catch(IllegalArgumentException e){throw new ImportException("Invalid character data; original retained",e);}
    }
    public Loaded save(CharacterBook book,Optional<String> expected)throws IOException{
        JsonObject json=new JsonObject();json.addProperty("schema_version",3);json.addProperty("selected_id",book.selectedId());json.add("sheets",JSON.toJsonTree(book.sheets()));
        return new Loaded(book,Optional.of(file.save(json,expected)));
    }
    private static void keys(JsonObject object,String... keys)throws ImportException{if(!Set.of(keys).equals(object.keySet()))throw new ImportException("Unknown or missing character fields; original retained");}
    private static CharacterFacts.Truth truth(JsonObject object,String key)throws IOException{return CharacterFacts.Truth.valueOf(text(object,key));}
}
