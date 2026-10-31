/** Offline JSON-lines bridge for audited forum HTML. Requires the pinned importer distribution libraries. */
import com.google.gson.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import net.mclorehub.updates.ForumHtml;
import java.nio.file.*;
import java.net.URI;
import java.util.*;

public class ForumReference {
  static Gson json=new GsonBuilder().disableHtmlEscaping().create();
  static Map<String,Object> link(Element e){return Map.of("title",e.text().strip(),"url",e.absUrl("href"));}
  static Map<String,Object> parse(JsonObject in)throws Exception{
    Path path=Path.of(in.get("path").getAsString());String url=in.get("url").getAsString();
    var dom=Jsoup.parse(path.toFile(),"UTF-8",url);Map<String,Object> out=new LinkedHashMap<>();
    out.put("page_title",dom.title());Element canonical=dom.selectFirst("head link[rel=canonical]");out.put("canonical",canonical==null?url:canonical.absUrl("href"));
    out.put("breadcrumbs",dom.select(".ipsBreadcrumb a[href]").stream().map(ForumReference::link).toList());
    var title=dom.selectFirst("h1.ipsType_pageTitle");out.put("title",title==null?"":title.text());
    var pager=dom.selectFirst(".ipsPagination[data-ipsPagination-seoPagination=true][data-pages]");out.put("pages",pager==null?1:Integer.parseInt(pager.attr("data-pages")));
    var next=dom.selectFirst("head link[rel=next]");out.put("next",next==null?"":next.absUrl("href"));
    if(in.get("mode").getAsString().equals("forum")){
      Map<String,Map<String,Object>> forums=new LinkedHashMap<>();
      for(var a:dom.select("h4.ipsDataItem_title a[href*=/forums/forum/], .ipsDataItem_subList a[href*=/forums/forum/]"))forums.put(a.absUrl("href"),link(a));
      out.put("forums",forums.values());var count=dom.selectFirst("[data-tableid=topics] h2.ipsType_sectionTitle");out.put("reported_topics",count==null?"":count.text());List<Map<String,Object>> topics=new ArrayList<>();
      for(var row:dom.select("li.ipsDataItem[data-rowid][data-controller*=topicRow]")){
        var a=row.selectFirst("h4.ipsDataItem_title a[href*=/forums/topic/]");if(a==null)continue;
        var topic=new LinkedHashMap<String,Object>(link(a));topic.put("id",row.attr("data-rowid"));topics.add(topic);
      }out.put("topics",topics);
    }else{
      String id=in.get("id").getAsString();var page=ForumHtml.main(Files.readAllBytes(path),URI.create(url),"lotc:topic:"+id);
      out.put("source_outline",page.outline());out.put("observed_status",page.status().map(Enum::name).orElse(""));
      out.put("title",page.title());out.put("body",page.posts().getFirst().text());out.put("author",page.posts().getFirst().author());out.put("post_id",page.posts().getFirst().id());
      var article=dom.selectFirst("article[id^=elComment_]");var body=article.selectFirst("[data-role=commentContent]");
      out.put("main_times",article.select("time[datetime]").stream().map(e->e.attr("datetime")).toList());
      out.put("images",body.select("img").size());out.put("topic_links",body.select("a[href*=/forums/topic/]").stream().map(ForumReference::link).toList());
      out.put("other_posts_on_page",Math.max(0,dom.select("article[id^=elComment_]").size()-1));
    }return out;
  }
  public static void main(String[] args)throws Exception{
    try(var scanner=new Scanner(System.in)){
      while(scanner.hasNextLine())try{System.out.println(json.toJson(parse(JsonParser.parseString(scanner.nextLine()).getAsJsonObject())));}catch(Exception e){System.out.println(json.toJson(Map.of("parse_error",e.toString())));}
    }
  }
}
