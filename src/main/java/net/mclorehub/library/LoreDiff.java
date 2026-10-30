package net.mclorehub.library;

import net.mclorehub.catalogue.LoreCatalogue.Document;
import java.util.*;

/** Bounded main-post comparison; discussion, credentials and review metadata never enter the diff. */
public final class LoreDiff {
    private LoreDiff(){}
    public record Result(String text,boolean abbreviated,int added,int removed){}
    public static Result compare(Document before,Document after){
        if(!before.id().equals(after.id()))throw new IllegalArgumentException("Compare the same topic");
        StringBuilder out=new StringBuilder("Main lore comparison\n- previous saved text\n+ current saved text\n  unchanged context\n\n");
        if(!before.title().equals(after.title()))out.append("Title:\n- ").append(clip(before.title(),1024)).append("\n+ ").append(clip(after.title(),1024)).append('\n');
        if(!before.author().equals(after.author()))out.append("Author:\n- ").append(clip(before.author().orElse("Unknown"),512)).append("\n+ ").append(clip(after.author().orElse("Unknown"),512)).append('\n');
        if(before.status()!=after.status())out.append("Status:\n- ").append(ReaderContent.statusLabel(before.status())).append("\n+ ").append(ReaderContent.statusLabel(after.status())).append('\n');
        var a=before.body().split("\\R",-1);var b=after.body().split("\\R",-1);List<String> edits=new ArrayList<>();boolean limited=false;int added=0,removed=0;
        if((long)a.length*b.length<=1_000_000&&a.length<=4096&&b.length<=4096){
            int[][] lengths=new int[a.length+1][b.length+1];for(int i=a.length-1;i>=0;i--)for(int j=b.length-1;j>=0;j--)lengths[i][j]=a[i].equals(b[j])?1+lengths[i+1][j+1]:Math.max(lengths[i+1][j],lengths[i][j+1]);
            int i=0,j=0;while(i<a.length||j<b.length){
                if(i<a.length&&j<b.length&&a[i].equals(b[j])){edits.add("  "+a[i++]);j++;}
                else if(i<a.length&&(j==b.length||lengths[i+1][j]>=lengths[i][j+1])){edits.add("- "+a[i++]);removed++;}
                else {edits.add("+ "+b[j++]);added++;}
            }
        }else{
            limited=true;edits.add("Comparison abbreviated for a large document; changed middle shown as replacement.");
            int prefix=0,suffix=0;while(prefix<a.length&&prefix<b.length&&a[prefix].equals(b[prefix]))prefix++;
            while(suffix<a.length-prefix&&suffix<b.length-prefix&&a[a.length-1-suffix].equals(b[b.length-1-suffix]))suffix++;
            for(int i=Math.max(0,prefix-2);i<prefix;i++)edits.add("  "+a[i]);
            removed=a.length-prefix-suffix;added=b.length-prefix-suffix;
            for(int i=prefix;i<Math.min(a.length-suffix,prefix+200);i++)edits.add("- "+a[i]);
            for(int i=prefix;i<Math.min(b.length-suffix,prefix+200);i++)edits.add("+ "+b[i]);
        }
        // Keep nearby context without flooding the screen with unchanged paragraphs.
        int last=-1;for(int i=0;i<edits.size();i++){
            boolean changed=edits.get(i).startsWith("- ")||edits.get(i).startsWith("+ ");boolean near=changed;
            for(int j=Math.max(0,i-2);!near&&j<Math.min(edits.size(),i+3);j++)near=edits.get(j).startsWith("- ")||edits.get(j).startsWith("+ ");
            if(!near&&!limited)continue;
            if(i>last+1)out.append("… unchanged lines …\n");
            String line=edits.get(i);if(line.length()>4000){line=clip(line,4000);limited=true;}if(out.length()+line.length()>48000){limited=true;break;}out.append(line).append('\n');last=i;
        }
        if(added==0&&removed==0&&before.title().equals(after.title())&&before.author().equals(after.author())&&before.status()==after.status())out.append("No main lore changes. Archived replies are excluded.\n");
        if(limited)out.append("\nComparison abbreviated. Complete versions remain saved; open the main source to read them.\n");
        return new Result("Added lines: "+added+" • Removed lines: "+removed+"\n"+out,limited,added,removed);
    }
    private static String clip(String text,int limit){if(text.length()<=limit)return text;int end=limit;if(Character.isHighSurrogate(text.charAt(end-1)))end--;return text.substring(0,end)+"…";}
}
