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
        if(integer(json,"schema_version")!=1)throw new ImportException("Unsupported character schema; original retained");
        try{
            List<CharacterBook.Sheet> sheets=new ArrayList<>();
            for(var item:array(json.get("sheets"),"sheets")){
                JsonObject s=object(item,"sheet");keys(s,"id","name","notes","archived","entries");JsonElement archived=s.get("archived");
                if(archived==null||!archived.isJsonPrimitive()||!archived.getAsJsonPrimitive().isBoolean())throw new ImportException("Archived must be boolean");
                List<CharacterBook.Entry> entries=new ArrayList<>();for(var entry:array(s.get("entries"),"entries")){
                    JsonObject e=object(entry,"entry");keys(e,"id","kind","name","branch","tier","applicationUrl","status","notes","loreDocumentId");
                    entries.add(new CharacterBook.Entry(text(e,"id"),CharacterBook.Kind.valueOf(text(e,"kind")),text(e,"name"),string(e,"branch"),string(e,"tier"),
                            string(e,"applicationUrl"),CharacterBook.RecordedStatus.valueOf(text(e,"status")),string(e,"notes"),string(e,"loreDocumentId")));}
                sheets.add(new CharacterBook.Sheet(text(s,"id"),text(s,"name"),string(s,"notes"),archived.getAsBoolean(),entries));}
            return new Loaded(new CharacterBook(sheets,string(json,"selected_id")),Optional.of(hash(bytes)));
        }catch(IllegalArgumentException e){throw new ImportException("Invalid character data; original retained",e);}
    }
    public Loaded save(CharacterBook book,Optional<String> expected)throws IOException{
        JsonObject json=new JsonObject();json.addProperty("schema_version",1);json.addProperty("selected_id",book.selectedId());json.add("sheets",JSON.toJsonTree(book.sheets()));
        return new Loaded(book,Optional.of(file.save(json,expected)));
    }
    private static void keys(JsonObject object,String... keys)throws ImportException{if(!Set.of(keys).equals(object.keySet()))throw new ImportException("Unknown or missing character fields; original retained");}
}
