package net.mclorehub.character;

import java.util.*;
import java.util.function.UnaryOperator;
import static net.mclorehub.importer.JsonSupport.forumUrl;

/** Entirely user-recorded sheets. Entries do not establish canon, eligibility or approvals. */
public record CharacterBook(List<Sheet> sheets,String selectedId) {
    public enum Kind { MAGIC, CA, FA }
    public enum RecordedStatus { NOT_RECORDED, PENDING, APPROVED, DENIED }
    public record Entry(String id,Kind kind,String name,String branch,String tier,String applicationUrl,
                        RecordedStatus status,String notes,String loreDocumentId){
        public Entry{
            uuid(id);Objects.requireNonNull(kind);Objects.requireNonNull(status);name=text(name,128,true);
            branch=text(branch,128,false);tier=text(tier,64,false);notes=text(notes,2048,false);
            applicationUrl=text(applicationUrl,2048,false);loreDocumentId=text(loreDocumentId,40,false);
            if(!loreDocumentId.isEmpty()&&!loreDocumentId.matches("lotc:topic:[1-9][0-9]{0,17}"))throw new IllegalArgumentException("Invalid lore reference");
            if(kind!=Kind.MAGIC&&(!branch.isEmpty()||!tier.isEmpty()))throw new IllegalArgumentException("Branch/tier applies to magic entries");
            if(!applicationUrl.isEmpty())try{forumUrl(applicationUrl);}catch(java.io.IOException e){throw new IllegalArgumentException("Application URL must be an official HTTPS forum topic",e);}
        }
    }
    public record Sheet(String id,String name,String notes,boolean archived,List<Entry> entries){
        public Sheet{uuid(id);name=text(name,128,true);notes=text(notes,4096,false);entries=List.copyOf(entries);
            if(entries.size()>100)throw new IllegalArgumentException("A sheet supports up to 100 entries");}
    }
    public CharacterBook{
        sheets=List.copyOf(sheets);Objects.requireNonNull(selectedId);
        if(sheets.size()>100)throw new IllegalArgumentException("Up to 100 sheets supported");Set<String> ids=new HashSet<>();
        for(var sheet:sheets){if(!ids.add(sheet.id()))throw new IllegalArgumentException("Duplicate sheet ID");
            for(var entry:sheet.entries())if(!ids.add(entry.id()))throw new IllegalArgumentException("Duplicate entry ID");}
        if(!selectedId.isEmpty()&&sheets.stream().noneMatch(s->s.id().equals(selectedId)&&!s.archived()))throw new IllegalArgumentException("Selected sheet is missing or archived");
    }
    public static CharacterBook empty(){return new CharacterBook(List.of(),"");}
    private static void uuid(String id){try{if(!UUID.fromString(id).toString().equals(id))throw new IllegalArgumentException();}catch(RuntimeException e){throw new IllegalArgumentException("Invalid sheet/entry ID",e);}}
    private static String text(String value,int max,boolean required){if(value==null||value.length()>max||(required&&value.isBlank()))throw new IllegalArgumentException("Invalid sheet field (limit "+max+")");return value;}
    public CharacterBook create(String name){List<Sheet> next=new ArrayList<>(sheets);String id=UUID.randomUUID().toString();next.add(new Sheet(id,name,"",false,List.of()));return new CharacterBook(next,id);}
    public CharacterBook select(String id){return new CharacterBook(sheets,id);}
    public Optional<Sheet> selected(){return sheets.stream().filter(s->s.id().equals(selectedId)).findFirst();}
    public CharacterBook rename(String id,String name,String notes){return update(id,s->new Sheet(s.id(),name,notes,s.archived(),s.entries()),selectedId);}
    public CharacterBook archive(String id,boolean archived){return update(id,s->new Sheet(s.id(),s.name(),s.notes(),archived,s.entries()),archived&&id.equals(selectedId)?"":selectedId);}
    public CharacterBook putEntry(String sheetId,Entry entry){return update(sheetId,s->{List<Entry> next=new ArrayList<>(s.entries());next.removeIf(e->e.id().equals(entry.id()));next.add(entry);
        return new Sheet(s.id(),s.name(),s.notes(),s.archived(),next);},selectedId);}
    public CharacterBook removeEntry(String sheetId,String entryId){return update(sheetId,s->new Sheet(s.id(),s.name(),s.notes(),s.archived(),s.entries().stream().filter(e->!e.id().equals(entryId)).toList()),selectedId);}
    private CharacterBook update(String id,UnaryOperator<Sheet> update,String selected){boolean found=sheets.stream().anyMatch(s->s.id().equals(id));if(!found)throw new IllegalArgumentException("Sheet not found");
        return new CharacterBook(sheets.stream().map(s->s.id().equals(id)?update.apply(s):s).toList(),selected);}
}
