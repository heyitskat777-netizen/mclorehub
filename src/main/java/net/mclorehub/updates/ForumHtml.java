package net.mclorehub.updates;

import net.mclorehub.catalogue.LoreCatalogue.*;
import net.mclorehub.catalogue.SourceSection;
import net.mclorehub.importer.ImportException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.*;
import java.net.URI;
import java.util.*;

/** Parse forum data only. Scripts, styles, forms and remote media are never executed/fetched. */
public final class ForumHtml {
    public record Page(String title,List<ThreadPost> posts,Optional<URI> next,int totalPages,Optional<Status> status,List<SourceSection> outline){}
    private ForumHtml(){}
    public static Page parse(byte[] html,URI url,String id)throws ImportException{
        return parse(html,url,id,false);
    }
    public static Page main(byte[] html,URI url,String id)throws ImportException{
        return parse(html,url,id,true);
    }
    private static Page parse(byte[] html,URI url,String id,boolean mainOnly)throws ImportException{
        var dom=Jsoup.parse(new String(html,java.nio.charset.StandardCharsets.UTF_8),url.toString());
        var canonical=dom.selectFirst("head link[rel=canonical]");if(canonical!=null)ForumUrls.checked(canonical.absUrl("href"),id);
        Element heading=dom.selectFirst("h1.ipsType_pageTitle .ipsType_break");if(heading==null)heading=dom.selectFirst("h1.ipsType_pageTitle");
        if(heading==null||heading.text().isBlank())throw new ImportException("Forum topic unavailable, login required, or page layout unsupported");
        List<ThreadPost> posts=new ArrayList<>();Set<String> seen=new HashSet<>();List<SourceSection> outline=List.of();
        for(Element article:dom.select("article[id^=elComment_]")){
            String postId=article.id().substring("elComment_".length());
            if(!postId.matches("[1-9][0-9]{0,17}")||!seen.add(postId))throw new ImportException("Invalid/duplicate forum post ID");
            Element body=article.selectFirst("[data-role=commentContent]");if(body==null)throw new ImportException("Incomplete forum post; no data saved");
            Element author=article.selectFirst(".cAuthorPane_author a[href*=/profile/]");
            String text=plain(body);posts.add(new ThreadPost(postId,author==null?"":author.text().strip(),url+"#comment-"+postId,text));
            if(posts.size()==1)outline=ForumOutline.extract(body,text);
            if(mainOnly)break;
        }
        if(posts.isEmpty())throw new ImportException("Forum returned no posts; stored lore retained");
        if(mainOnly)return new Page(heading.text().strip(),List.copyOf(posts),Optional.empty(),1,status(dom,heading.text()),outline);
        Element pager=dom.selectFirst(".ipsPagination[data-pages]");int pages=1;
        if(pager==null&&dom.selectFirst(".ipsPagination")!=null)throw new ImportException("Unsupported forum pagination layout");
        try{if(pager!=null)pages=Integer.parseInt(pager.attr("data-pages"));}catch(NumberFormatException e){throw new ImportException("Invalid forum pagination",e);}
        if(pages<1||pages>20)throw new ImportException("Topic exceeds supported 20-page limit; no partial update saved");
        Element next=dom.selectFirst("head link[rel=next]");if(next==null)next=dom.selectFirst(".ipsPagination_next:not(.ipsPagination_inactive) a[rel=next]");
        Optional<URI> nextUrl=next==null?Optional.empty():Optional.of(ForumUrls.checked(next.absUrl("href"),id));
        int current=ForumUrls.page(url);
        if((current<pages)!=nextUrl.isPresent()||nextUrl.isPresent()&&ForumUrls.page(nextUrl.get())!=current+1)
            throw new ImportException("Incomplete/looping forum pagination; no partial update saved");
        return new Page(heading.text().strip(),List.copyOf(posts),nextUrl,pages,status(dom,heading.text()),outline);
    }
    private static Optional<Status> status(org.jsoup.nodes.Document dom,String title){
        List<String> labels=new ArrayList<>();dom.select(".ipsBreadcrumb a").forEach(a->labels.add(a.text().strip().toLowerCase(Locale.ROOT)));
        String prefix=title.strip().toLowerCase(Locale.ROOT);
        if(labels.stream().anyMatch(s->s.contains("outdated lore")||s.equals("shelved lore")||s.equals("denied lore"))||prefix.startsWith("[shelved]")||prefix.startsWith("[denied]"))return Optional.of(Status.SHELVED);
        if(labels.contains("superseded lore")||prefix.startsWith("[superseded]"))return Optional.of(Status.SUPERSEDED);
        if(labels.contains("legacy lore"))return Optional.of(Status.LEGACY);
        if(labels.contains("pending lore")||prefix.startsWith("[pending]"))return Optional.of(Status.PENDING);
        if(labels.contains("implemented lore")||labels.contains("accepted lore"))return Optional.of(Status.ACCEPTED_CURRENT);
        return Optional.empty();
    }
    public static String plain(Element source){
        Element clean=source.clone();clean.select("script,style,noscript,iframe,form,input,button,svg,.ipsSpoiler_header").remove();StringBuilder text=new StringBuilder();
        Set<String> block=Set.of("p","div","li","h1","h2","h3","h4","h5","h6","blockquote","pre","tr");
        NodeTraversor.traverse(new NodeVisitor(){
            public void head(Node node,int depth){
                if(node instanceof TextNode t)text.append(t.getWholeText().replaceAll("[\\s\\u00a0]+"," "));
                if(node instanceof Element e){if(block.contains(e.normalName())||e.normalName().equals("br"))newline();
                    if(e.normalName().equals("img"))text.append("[Image").append(e.attr("alt").isBlank()?"":": "+e.attr("alt")).append("]");}
            }
            public void tail(Node node,int depth){if(node instanceof Element e&&block.contains(e.normalName()))newline();}
            private void newline(){if(!text.isEmpty()&&text.charAt(text.length()-1)!='\n')text.append('\n');}
        },clean);
        return text.toString().replaceAll("[ \\t]+\\n","\n").replaceAll("\\n[ \\t]+","\n").replaceAll("\\n{3,}","\n\n").strip();
    }
}
