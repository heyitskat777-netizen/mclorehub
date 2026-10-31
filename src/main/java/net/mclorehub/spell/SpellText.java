package net.mclorehub.spell;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;

/** Search and explicit tier labels, including the small-cap typography used by forum lore. */
public final class SpellText {
    private SpellText(){}
    private static final String SMALL="ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘꞯʀꜱᴛᴜᴠᴡꭙʏᴢ",ASCII="ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final Pattern TIER=Pattern.compile("\\b(?:TIER\\s*|T)([0-9]{1,2}|VIII|VII|III|VI|IV|IX|II|X|V|I|ONE|TWO|THREE|FOUR|FIVE)\\b");
    public static String fold(String text){
        StringBuilder b=new StringBuilder();for(char c:Normalizer.normalize(text,Normalizer.Form.NFKC).toCharArray()){int i=SMALL.indexOf(c);b.append(i<0?c:ASCII.charAt(i));}
        return b.toString().toUpperCase(Locale.ROOT).replace('’','\'').replace('–','-').replace('—','-').replaceAll("[\\s\\p{Z}]+"," ").strip();
    }
    public static String compact(String text){return fold(text).replaceAll("[^\\p{L}\\p{N}]","");}
    public static String heading(String text){
        String[] tokens=text.strip().split("\\s+");long letters=Arrays.stream(tokens).filter(t->t.length()==1&&Character.isLetter(t.charAt(0))).count();
        if(tokens.length<4||letters<tokens.length*.75)return fold(text);
        StringBuilder b=new StringBuilder();char previous=0;for(String token:tokens){
            boolean single=token.length()==1&&Character.isLetter(token.charAt(0));
            if(!b.isEmpty()&&(!single||token.charAt(0)>='A'&&token.charAt(0)<='Z'&&SMALL.indexOf(previous)>=0))b.append(' ');
            b.append(fold(token));previous=token.charAt(token.length()-1);
        }return b.toString();
    }
    public static boolean decoratedHeading(String text){long styled=text.chars().filter(c->SMALL.indexOf(c)>=0).count(),letters=text.chars().filter(Character::isLetter).count();return styled>=3&&styled>=letters*.6||!heading(text).equals(fold(text));}
    public static int number(String value){return switch(value){case "I","ONE"->1;case "II","TWO"->2;case "III","THREE"->3;case "IV","FOUR"->4;case "V","FIVE"->5;case "VI"->6;case "VII"->7;case "VIII"->8;case "IX"->9;case "X"->10;default->{try{int n=Integer.parseInt(value);yield n>=0&&n<=10?n:-1;}catch(NumberFormatException e){yield -1;}}};}
    public static int tier(String text){
        String value=fold(text);if(value.matches("[0-9]{1,2}|[IVX]{1,4}"))return number(value);
        Matcher m=TIER.matcher(value);return m.find()?number(m.group(1)):-1;
    }
    public static int minimumTier(String text){Matcher m=TIER.matcher(fold(text));int result=99;while(m.find()){int n=number(m.group(1));if(n>=0)result=Math.min(result,n);}return result==99?-1:result;}
    public static String stage(String text){String s=fold(text);return s.matches(".*\\bGREATER\\b.*")?"GREATER":s.matches(".*\\bLESSER\\b.*")?"LESSER":"";}
    public static boolean matches(String query,String text){String hay=fold(text),compact=compact(text);return Arrays.stream(fold(query).split(" ")).allMatch(t->hay.contains(t)||compact.contains(compact(t)));}
}
