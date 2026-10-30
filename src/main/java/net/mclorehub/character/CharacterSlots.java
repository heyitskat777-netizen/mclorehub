package net.mclorehub.character;

import java.util.*;
import static net.mclorehub.character.CharacterLore.*;

/** Interval arithmetic over explicit source costs and user allocations, never a compatibility certificate. */
public final class CharacterSlots {
    private CharacterSlots(){}
    public record Report(int minimum,int maximum,int lostMinimum,int capacity,int unknownCosts,int contradictions,boolean overBudget,int credit,String text,List<Source> sources){public Report{sources=List.copyOf(sources);}}
    public static Report assess(CharacterLore lore,CharacterBook.Sheet sheet){
        var groups=new LinkedHashMap<String,List<CharacterBook.Entry>>();for(var e:sheet.entries())if(e.kind()!=CharacterBook.Kind.CA)groups.computeIfAbsent(SlotBudget.key(e),k->new ArrayList<>()).add(e);
        int min=0,max=0,unknown=0,contradictions=0,voidalMin=0,paidMagics=0;boolean optional=false,stalker=false;var evidence=new LinkedHashMap<String,Source>();var text=new StringBuilder("Slot accounting (recorded magic/feat entries)\n");
        for(var group:groups.entrySet()){
            int lo=0,hi=0;boolean isVoidal=false,known=true;var entry=group.getValue().getFirst();
            for(var e:group.getValue()){var cost=lore.slotCost(e);lo=Math.max(lo,cost.minimum());if(cost.maximum()<0)known=false;else hi=Math.max(hi,cost.maximum());isVoidal|=cost.voidal();cost.sources().forEach(s->evidence.put(s.id(),s));}
            boolean bardmaster=sheet.entries().stream().anyMatch(e->lore.resolve(e).map(c->c.option().key().equals("bardmastery")&&c.availability()==Availability.AVAILABLE).orElse(false));
            if(known&&bardmaster&&lore.resolve(entry).map(c->c.option().key().equals("bardmancy")).orElse(false)){lo=hi=2;text.append("Bardmastery requires the full two-slot parent Bardmancy dedication.\n");}
            Integer allocation=sheet.budget().allocations().get(group.getKey());String basis="source";
            if(allocation!=null){if(known&&(allocation<lo||allocation>hi||!lore.validAllocation(entry,allocation))){contradictions++;text.append("Allocation outside current source choices: ").append(entry.name()).append('\n');}lo=hi=Math.max(lo,allocation);known=true;basis="user-recorded allocation";}
            if(!known){unknown++;max=-1;}else if(max>=0)max+=hi;min+=lo;
            if(isVoidal){voidalMin+=lo;if(lo>=2&&lore.slotCost(entry).maximum()>=2)optional=true;}
            if(entry.kind()==CharacterBook.Kind.MAGIC&&lo>0)paidMagics++;
            if(lore.resolve(entry).map(c->c.option().key().equals("voidstalking")&&c.availability()==Availability.AVAILABLE).orElse(false))stalker=true;
            text.append(entry.name()).append(": ").append(known?(lo==hi?Integer.toString(lo):lo+"–"+hi):lo+"+ (unknown)").append(" • ").append(basis).append(group.getValue().size()>1?" • duplicate source grouped once":"").append(lore.permanentDedication(entry)?" • permanent dedication":"").append('\n');
        }
        // Only the explicit one-extra-slot allowance; never a general capacity increase or sixth MA.
        int credit=0;if(stalker&&optional&&voidalMin>=5&&paidMagics<=5){credit=1;min--;if(max>=0)max--;text.append("Voidstalker: one optional Voidal allocation credited; four base Voidal slots still required.\n");}
        if(stalker&&paidMagics>5){contradictions++;text.append("Voidstalking does not allow a sixth paid magic application.\n");}
        int inferred=0;Set<String> lostPractices=new HashSet<>();for(var h:sheet.history())if(h.type()==CharacterBook.EventType.DISCONNECTED&&h.loreDocumentId().equals("lotc:topic:249533")&&lostPractices.add(h.loreDocumentId())){
            if(lore.availability(h.loreDocumentId())==Availability.AVAILABLE){inferred+=3;lore.document(h.loreDocumentId()).ifPresent(d->lore.choices(CharacterBook.Kind.MAGIC,"").stream().filter(c->c.option().sourceId().equals(d.id())).findFirst().ifPresent(c->evidence.put(c.source().id(),c.source())));}else {unknown++;text.append("Disconnected Necromancy source stale: lost-slot amount needs review.\n");}
        }
        int recorded=sheet.budget().lostSlots();if(recorded>=0&&recorded<inferred){contradictions++;text.append("Recorded lost slots conflict with the source-backed Necromancy loss.\n");}
        int lost=Math.max(inferred,Math.max(0,recorded));min+=lost;if(recorded<0){max=-1;text.append("Total prior lost slots unknown; history is not assumed complete.\n");}else if(max>=0)max+=lost;
        text.append("Unavailable/lost slots: ").append(recorded<0?lost+"+ (total unknown)":lost).append("; repeated history for the same dedication is not multiplied.\n");
        int capacity=sheet.budget().capacity();boolean over=capacity>=0&&min>capacity;
        text.append("Total reserved: ").append(max<0?min+"+ (partial)":min==max?Integer.toString(min):min+"–"+max).append(" / ").append(capacity<0?"capacity unknown":capacity).append('\n');
        if(over)text.append("Over recorded usable capacity by at least ").append(min-capacity).append(".\n");
        text.append("Capacity must include current creature/body restrictions. All recorded plans, including pending/denied entries, are counted. Manual allocations do not verify lore, prerequisites or approval; stale/uncovered costs stay explicit. Seer/Naztherak past dedications and exceptions must be included in your recorded lost total.\n");
        return new Report(min,max,lost,capacity,unknown,contradictions,over,credit,text.toString(),List.copyOf(evidence.values()));
    }
}
