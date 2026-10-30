package net.mclorehub.character;

import java.util.*;

/** Explicit arithmetic inputs; capacity is usable capacity after any creature-specific restrictions. */
public record SlotBudget(int capacity,int lostSlots,Map<String,Integer> allocations) {
    public SlotBudget {
        if(capacity< -1||capacity>100||lostSlots< -1||lostSlots>100)throw new IllegalArgumentException("Slot counts must be blank/unknown or between 0 and 100");
        allocations=Map.copyOf(allocations);if(allocations.size()>200)throw new IllegalArgumentException("Too many slot allocations");
        for(var e:allocations.entrySet())if(!e.getKey().matches("(?:MAGIC|FA):lotc:topic:[1-9][0-9]{0,17}|entry:[a-f0-9-]{36}")||e.getValue()<0||e.getValue()>100)throw new IllegalArgumentException("Invalid slot allocation");
    }
    public static SlotBudget unknown(){return new SlotBudget(-1,-1,Map.of());}
    public static String key(CharacterBook.Entry e){return e.loreDocumentId().isEmpty()?"entry:"+e.id():e.kind()+":"+e.loreDocumentId();}
    public SlotBudget allocate(String key,int value){var next=new HashMap<>(allocations);if(value<0)next.remove(key);else next.put(key,value);return new SlotBudget(capacity,lostSlots,next);}
}
