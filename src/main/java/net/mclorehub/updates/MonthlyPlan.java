package net.mclorehub.updates;

import com.google.gson.*;
import net.mclorehub.catalogue.LoreCatalogue.*;
import net.mclorehub.importer.ImportException;
import java.time.*;
import java.util.*;
import static net.mclorehub.importer.JsonSupport.*;

/** Calendar-month cadence; source check times also account for manual refreshes. */
public record MonthlyPlan(boolean enabled,Instant notBefore,Map<String,Item> topics) {
    public record Item(Instant next,Instant lastSuccess,int failures,String outcome){}
    public MonthlyPlan{topics=Map.copyOf(topics);}
    public static MonthlyPlan empty(){return new MonthlyPlan(true,Instant.EPOCH,Map.of());}
    public static Instant nextMonth(Instant date){return date.atZone(ZoneOffset.UTC).plusMonths(1).toInstant();}
    public MonthlyPlan enabled(boolean value){return new MonthlyPlan(value,notBefore,topics);}
    public MonthlyPlan sync(List<Document> documents,Instant now){Map<String,Item> next=new HashMap<>(topics);
        for(var d:documents){Instant checked=d.reviewState()==ReviewState.SOURCE_REFRESHED?d.trust().map(SourceTrust::checkedAt).orElse(Instant.EPOCH):Instant.EPOCH;
            Item old=next.get(d.id());if(old==null)next.put(d.id(),new Item(nextMonth(checked.equals(Instant.EPOCH)?now:checked),checked,0,""));
            else if(checked.isAfter(old.lastSuccess()))next.put(d.id(),new Item(nextMonth(checked),checked,0,""));}
        return new MonthlyPlan(enabled,notBefore,next);
    }
    public MonthlyPlan result(String id,Instant now,String outcome,Instant retry){Map<String,Item> next=new HashMap<>(topics);Item old=next.get(id);
        if(old==null)return this;boolean success=outcome.equals("OK");int failures=success?0:Math.min(30,old.failures()+1);
        Instant due=success||outcome.equals("ACCESS")?nextMonth(now):retry!=null?retry:now.plus(Duration.ofDays(Math.min(7,failures)));
        next.put(id,new Item(due,success?now:old.lastSuccess(),failures,success?"":outcome));
        return new MonthlyPlan(enabled,outcome.equals("RATE_LIMIT")&&retry!=null?retry:notBefore,next);
    }
    public Optional<String> due(List<Document> documents,Instant now){if(!enabled||now.isBefore(notBefore))return Optional.empty();Set<String> known=new HashSet<>();documents.forEach(d->known.add(d.id()));
        return topics.entrySet().stream().filter(e->known.contains(e.getKey())&&!e.getValue().next().isAfter(now))
                .sorted(Comparator.<Map.Entry<String,Item>,Instant>comparing(e->e.getValue().next()).thenComparing(Map.Entry::getKey)).map(Map.Entry::getKey).findFirst();}
    public JsonObject json(){JsonObject r=new JsonObject();r.addProperty("schema_version",1);r.addProperty("enabled",enabled);r.addProperty("not_before",notBefore.toString());JsonObject list=new JsonObject();
        new TreeMap<>(topics).forEach((id,item)->{JsonObject i=new JsonObject();i.addProperty("next_check",item.next().toString());i.addProperty("last_success",item.lastSuccess().toString());i.addProperty("failures",item.failures());i.addProperty("outcome",item.outcome());list.add(id,i);});r.add("topics",list);return r;}
    public static MonthlyPlan parse(JsonObject r)throws ImportException{
        if(!r.keySet().equals(Set.of("schema_version","enabled","not_before","topics"))||integer(r,"schema_version")!=1)throw new ImportException("Unsupported monthly schedule");
        JsonElement enabled=r.get("enabled");if(!enabled.isJsonPrimitive()||!enabled.getAsJsonPrimitive().isBoolean())throw new ImportException("Invalid monthly setting");
        JsonObject list=object(r.get("topics"),"topics");if(list.size()>10000)throw new ImportException("Monthly schedule exceeds limits");Map<String,Item> items=new HashMap<>();
        try{for(String id:list.keySet()){
            if(!id.matches("lotc:topic:[1-9][0-9]{0,17}"))throw new IllegalArgumentException();var i=object(list.get(id),"topic schedule");
            if(!i.keySet().equals(Set.of("next_check","last_success","failures","outcome")))throw new IllegalArgumentException();int count=integer(i,"failures");String outcome=string(i,"outcome");
            if(count<0||count>30||!Set.of("","ACCESS","NETWORK","RATE_LIMIT","CANCELLED").contains(outcome))throw new IllegalArgumentException();
            items.put(id,new Item(Instant.parse(text(i,"next_check")),Instant.parse(text(i,"last_success")),count,outcome));}
            return new MonthlyPlan(enabled.getAsBoolean(),Instant.parse(text(r,"not_before")),items);
        }catch(RuntimeException e){throw new ImportException("Invalid monthly schedule; original retained");}
    }
}
