package net.mclorehub.character;

import java.util.*;

/** Current facts explicitly recorded by the user; no inference from an entry's name or approval. */
public record CharacterFacts(Soul soul,Truth breathing,Truth lungs,Truth modifiedSoul,Truth klone,
                             Truth descendantRace,Truth blindBody,Truth holyScornedDisconnection,Severances templarSeverances) {
    public enum Truth { UNKNOWN, YES, NO }
    public enum Soul { UNKNOWN, GREATER, LESSER, NONE }
    public enum Severances { UNKNOWN, ZERO, ONE, TWO, THREE_OR_MORE }
    public enum Field {
        SOUL("Soul"), BREATHING("Can breathe"), LUNGS("Has lungs"), MODIFIED_SOUL("Corrupted / modified Body portion"),
        KLONE("Klone body"), DESCENDANT_RACE("Human / Orc / Elf / Dwarf"), BLIND_BODY("Current body physically blind"),
        HOLY_HISTORY("Scorned and disconnected from holy magic"), TEMPLAR_SEVERANCES("Prior Templar severances");
        public final String label;Field(String label){this.label=label;}
        public List<String> choices(){return switch(this){case SOUL->Arrays.stream(Soul.values()).map(Enum::name).toList();case TEMPLAR_SEVERANCES->Arrays.stream(Severances.values()).map(Enum::name).toList();default->Arrays.stream(Truth.values()).map(Enum::name).toList();};}
        public String help(){return switch(this){
            case SOUL->"Known soul class; do not infer it from a name or application status.";
            case BREATHING->"Whether the current body can breathe. Casting conditions are still assessed separately.";
            case LUNGS->"Whether the current body has lungs. The Klone exception does not supply them.";
            case MODIFIED_SOUL->"For Kani: corruption/modification of the Body portion of the soul. Holding Dark or Voidal magic alone does not establish this; those practices may coexist with Kani.";
            case KLONE->"Explicit current Klone body status. Its Kani exception addresses only the stated soul restriction.";
            case DESCENDANT_RACE->"The four descendant races listed by Templar lore. Other creature and hybrid conditions are not inferred.";
            case BLIND_BODY->"Physical blindness of the current body, including an inhabited Klone body. Magical perception is a separate condition.";
            case HOLY_HISTORY->"The source requires both scorn and disconnection from holy-deific magic. An ordinary disconnection alone does not establish this.";
            case TEMPLAR_SEVERANCES->"Known prior severances. An incomplete history log does not mean zero. Three bars receiving the blessing a fourth time.";
        };}
    }
    public CharacterFacts {Objects.requireNonNull(soul);Objects.requireNonNull(breathing);Objects.requireNonNull(lungs);Objects.requireNonNull(modifiedSoul);Objects.requireNonNull(klone);Objects.requireNonNull(descendantRace);Objects.requireNonNull(blindBody);Objects.requireNonNull(holyScornedDisconnection);Objects.requireNonNull(templarSeverances);}
    public static CharacterFacts unknown(){return new CharacterFacts(Soul.UNKNOWN,Truth.UNKNOWN,Truth.UNKNOWN,Truth.UNKNOWN,Truth.UNKNOWN,Truth.UNKNOWN,Truth.UNKNOWN,Truth.UNKNOWN,Severances.UNKNOWN);}
    public String value(Field field){return switch(field){case SOUL->soul.name();case BREATHING->breathing.name();case LUNGS->lungs.name();case MODIFIED_SOUL->modifiedSoul.name();case KLONE->klone.name();case DESCENDANT_RACE->descendantRace.name();case BLIND_BODY->blindBody.name();case HOLY_HISTORY->holyScornedDisconnection.name();case TEMPLAR_SEVERANCES->templarSeverances.name();};}
    public CharacterFacts set(Field f,String value){Objects.requireNonNull(f);return new CharacterFacts(
            f==Field.SOUL?Soul.valueOf(value):soul,f==Field.BREATHING?Truth.valueOf(value):breathing,
            f==Field.LUNGS?Truth.valueOf(value):lungs,f==Field.MODIFIED_SOUL?Truth.valueOf(value):modifiedSoul,
            f==Field.KLONE?Truth.valueOf(value):klone,f==Field.DESCENDANT_RACE?Truth.valueOf(value):descendantRace,
            f==Field.BLIND_BODY?Truth.valueOf(value):blindBody,f==Field.HOLY_HISTORY?Truth.valueOf(value):holyScornedDisconnection,
            f==Field.TEMPLAR_SEVERANCES?Severances.valueOf(value):templarSeverances);}
    public CharacterFacts cycle(Field f){var values=f.choices();return set(f,values.get((values.indexOf(value(f))+1)%values.size()));}
    public static String label(String value){return value.toLowerCase(Locale.ROOT).replace('_',' ');}
}
