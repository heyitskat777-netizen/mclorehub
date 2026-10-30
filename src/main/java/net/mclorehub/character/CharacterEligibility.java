package net.mclorehub.character;

import java.util.*;
import static net.mclorehub.character.CharacterFacts.*;
import static net.mclorehub.character.CharacterLore.*;

/** Advisory checks of explicit current facts; never derives facts from labels, approvals or incomplete history. */
final class CharacterEligibility {
    private CharacterEligibility(){}
    static Optional<Finding> evaluate(EligibilityRule rule,CharacterFacts f,List<CharacterBook.Entry> entries,
                                      List<CharacterBook.HistoryEvent> history,List<Source> sources){
        List<String> failures=new ArrayList<>(),missing=new ArrayList<>();String context="";
        switch(rule.type()){
            case KANI_BODY -> {
                require(f.breathing(),"ability to breathe",failures,missing);require(f.lungs(),"lungs",failures,missing);
                if(f.soul()==Soul.NONE)failures.add("Kani requires at least a lesser soul");else if(f.soul()==Soul.UNKNOWN)missing.add("soul class");
                if(f.modifiedSoul()==Truth.YES){if(f.klone()==Truth.NO)failures.add("corrupted/modified Body portion of the soul with no recorded Klone exception");else if(f.klone()==Truth.UNKNOWN)missing.add("whether the Klone exception applies");}
                else if(f.modifiedSoul()==Truth.UNKNOWN&&f.klone()!=Truth.YES)missing.add("corruption/modification of the Body portion of the soul");
                context="The Klone exception only addresses this soul restriction; breathing, lungs and soul minimum still apply. Holding Dark or Voidal magic alone does not establish Body corruption. Equipment and other conditions remain unassessed.";
            }
            case SEER_BODY -> {require(f.blindBody(),"physical blindness of the current body",failures,missing);context="A non-blind inhabited Klone body makes Seer inaccessible. Other acquisition/creature conditions and covering requirements remain unassessed.";}
            case CLERIC_BODY -> {
                if(f.soul()==Soul.UNKNOWN)missing.add("soul class");else if(f.soul()!=Soul.GREATER)failures.add("Clericism requires a greater soul");
                if(f.klone()==Truth.YES)failures.add("Cleric lore explicitly excludes Klones");else if(f.klone()==Truth.UNKNOWN)missing.add("Klone body status");
                context="Other CA and source-disagreement checks remain separate; a greater soul alone does not establish eligibility.";
            }
            case CLERIC_HISTORY -> {
                if(f.holyScornedDisconnection()==Truth.YES)failures.add("recorded scorn and disconnection from holy-deific magic bars taking up Clericism");
                else if(f.holyScornedDisconnection()==Truth.UNKNOWN)missing.add("whether the character was both scorned and disconnected from holy-deific magic");
                context="This checks the specific combined condition in the source, not every ordinary disconnection. Individual history events do not automatically set this fact.";
            }
            case TEMPLAR_RACE -> {require(f.descendantRace(),"Human, Orc, Elf or Dwarf race",failures,missing);context="Hybrid/creature cases and other requirements are not inferred from the name or CA labels.";}
            case TEMPLAR_HISTORY -> {
                if(f.templarSeverances()==Severances.THREE_OR_MORE)failures.add("three or more prior Templar severances recorded; the source bars receiving the blessing a fourth time");
                else if(f.templarSeverances()==Severances.UNKNOWN)missing.add("prior Templar severance count");
                context="This is a restriction on further acquisition, not an automatic ruling about when an existing entry was acquired. History dates/events do not silently update the recorded count.";
            }
            case NECROMANCY_HISTORY -> {
                if(history.isEmpty())return Optional.empty();
                return Optional.of(new Finding(Outcome.REQUIREMENTS,"Past Necromancy disconnection recorded: its dedicated slots are permanently lost according to this source. Deleting the active entry does not erase this history. No slot total is calculated; event dates are reference notes, not current-state inference.",sources));
            }
        }
        String names=String.join(" / ",entries.stream().map(CharacterBook.Entry::name).distinct().toList());
        String message=names+": "+(failures.isEmpty()?(missing.isEmpty()?"Recorded facts match this limited prerequisite check; other requirements remain unassessed.":"Prerequisite check incomplete."):"Recorded eligibility issue: "+String.join("; ",failures)+".");
        if(!missing.isEmpty())message+=" Facts missing: "+String.join(", ",missing)+"; not assessed.";
        message+=" "+context+" User-recorded facts do not verify server approval or overall eligibility.";
        return Optional.of(new Finding(failures.isEmpty()?Outcome.REQUIREMENTS:Outcome.ELIGIBILITY_CONFLICT,message,sources));
    }
    private static void require(Truth fact,String name,List<String> failures,List<String> missing){if(fact==Truth.NO)failures.add("requires "+name);else if(fact==Truth.UNKNOWN)missing.add(name);}
}
