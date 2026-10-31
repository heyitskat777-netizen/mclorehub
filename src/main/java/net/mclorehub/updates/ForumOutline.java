package net.mclorehub.updates;

import net.mclorehub.catalogue.SourceSection;
import org.jsoup.nodes.*;
import java.util.*;
import java.util.regex.*;

/** Recover styled forum headings with exact plain-text offsets; never executes markup. */
public final class ForumOutline {
    private ForumOutline(){}
    private static final Pattern SIZE=Pattern.compile("font-size\\s*:\\s*([0-9.]+)\\s*(px|pt)",Pattern.CASE_INSENSITIVE);
    public static List<SourceSection> extract(Element source,String expected){
        Element copy=source.clone();copy.select("script,style,noscript,iframe,form,input,button,svg,.ipsSpoiler_header").remove();
        String prefix="\uE000"+UUID.randomUUID()+":";if(expected.contains(prefix))return List.of();
        List<Integer> levels=new ArrayList<>();int count=0;
        for(var e:copy.select("h1,h2,h3,h4,h5,h6,p,strong,b,u,span[style]")){
            String title=ForumHtml.plain(e).strip();if(title.isEmpty()||title.length()>200||title.contains("\n")||title.startsWith("[Image"))continue;
            int level=level(e);if(level<0)continue;if(count>=10000)break;
            e.prependText(prefix+count+"S\uE001");e.appendText(prefix+count+"E\uE001");levels.add(level);count++;
        }
        String marked=ForumHtml.plain(copy);var matcher=Pattern.compile(Pattern.quote(prefix)+"([0-9]+)([SE])\uE001").matcher(marked);
        StringBuilder clean=new StringBuilder();int last=0;int[] starts=new int[count],ends=new int[count];Arrays.fill(starts,-1);Arrays.fill(ends,-1);
        while(matcher.find()){
            clean.append(marked,last,matcher.start());int id=Integer.parseInt(matcher.group(1));if(id>=count)return List.of();
            if(matcher.group(2).equals("S"))starts[id]=clean.length();else ends[id]=clean.length();last=matcher.end();
        }clean.append(marked,last,marked.length());int[] positions=whitespaceMapping(clean.toString(),expected);if(positions==null)return List.of();
        Map<Integer,SourceSection> byLine=new TreeMap<>();
        for(int i=0;i<count;i++){
            if(starts[i]<0||ends[i]<starts[i])continue;int a=positions[starts[i]],b=positions[ends[i]];
            if(a<0||b<=a||b>expected.length())continue;while(a<b&&Character.isWhitespace(expected.charAt(a)))a++;while(b>a&&Character.isWhitespace(expected.charAt(b-1)))b--;
            int lineStart=expected.lastIndexOf('\n',Math.max(0,a-1))+1,lineEnd=expected.indexOf('\n',b);if(lineEnd<0)lineEnd=expected.length();
            if(lineEnd-lineStart>220||expected.substring(a,b).contains("\n"))continue;a=lineStart;b=lineEnd;
            while(a<b&&Character.isWhitespace(expected.charAt(a)))a++;while(b>a&&Character.isWhitespace(expected.charAt(b-1)))b--;
            if(b<=a)continue;var section=new SourceSection(expected.substring(a,b),a,b,levels.get(i));byLine.merge(a,section,(old,added)->old.level()<=added.level()?old:added);
        }return List.copyOf(byLine.values());
    }
    private static int[] whitespaceMapping(String marked,String expected){
        int[] result=new int[marked.length()+1];int a=0,b=0;
        while(a<marked.length()){
            result[a]=b;
            if(b<expected.length()&&marked.charAt(a)==expected.charAt(b)){a++;b++;}
            else if(Character.isWhitespace(marked.charAt(a)))a++;
            else if(b<expected.length()&&Character.isWhitespace(expected.charAt(b)))b++;
            else return null;
        }
        while(b<expected.length()&&Character.isWhitespace(expected.charAt(b)))b++;
        if(b!=expected.length())return null;result[a]=b;return result;
    }
    private static int level(Element e){
        if(e.normalName().matches("h[1-6]"))return Integer.parseInt(e.normalName().substring(1));
        double size=0;for(var node:e.getAllElements()){
            var m=SIZE.matcher(node.attr("style"));while(m.find())try{size=Math.max(size,Double.parseDouble(m.group(1))*(m.group(2).equalsIgnoreCase("pt")?4.0/3:1));}catch(NumberFormatException ignored){}
        }
        if(size>=32)return 1;if(size>=26)return 2;if(size>=22)return 3;if(size>=19)return 4;if(size>=16)return 5;
        String text=e.text();int bold=e.select("strong,b").stream().mapToInt(b->b.text().length()).max().orElse(0);
        return Set.of("strong","b","u").contains(e.normalName())||!text.isBlank()&&bold>=text.length()*.8?6:-1;
    }
}
