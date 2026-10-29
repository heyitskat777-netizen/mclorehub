package net.mclorehub.updates;

import com.google.gson.*;
import net.mclorehub.catalogue.LoreCatalogue;
import java.nio.file.*;
import java.time.Instant;
import static net.mclorehub.TestData.*;
import static net.mclorehub.importer.JsonSupport.*;

final class UpdateFixtures {
    static final String URL="https://www.lordofthecraft.net/forums/topic/123-synthetic/";
    static LoreCatalogue install(Path root,boolean trusted)throws Exception{
        Files.createDirectories(root.resolve("packs/active"));JsonObject source=export();var first=record(source);first.addProperty("url",URL);
        JsonObject second=first.deepCopy();second.addProperty("topic_id","124");second.addProperty("url",URL.replace("123-","124-"));second.addProperty("title","Second synthetic reference");
        source.getAsJsonArray("records").add(second);source.getAsJsonObject("metadata").addProperty("record_count",2);
        Path pack=write(root.resolve("packs/active/pack.json"),candidate(root,source));var catalogue=LoreCatalogue.load(pack,null);
        if(trusted){write(root.resolve("packs/active/trust.json"),BaselineTrust.manifest(catalogue,Instant.parse("2026-10-20T00:00:00Z")));
            catalogue=BaselineTrust.apply(catalogue,root.resolve("packs/active/trust.json"));}return catalogue;
    }
    static String html(int page,int total,String next,String body,String posts){return "<html><head><link rel='canonical' href='"+URL+"'>"
            +(next==null?"":"<link rel='next' href='"+next+"'>")+"</head><body><nav class='ipsBreadcrumb'><a>Implemented Lore</a></nav>"
            +"<h1 class='ipsType_pageTitle'><span class='ipsType_break'>Synthetic topic</span></h1><ul class='ipsPagination' data-pages='"+total+"'></ul>"
            +(page==1?post("1001","Alice",body):"")+posts+"</body></html>";}
    static String post(String id,String author,String body){return "<article id='elComment_"+id+"'><h3 class='cAuthorPane_author'><a href='/profile/1-author/'>"+author
            +"</a></h3><div data-role='commentContent'>"+body+"</div></article>";}
    static TopicSnapshot snapshot(LoreCatalogue.Document d,String text){return new TopicSnapshot(d.id(),d.url(),d.title(),d.author().orElse(""),text,d.posts(),
            java.util.List.of(d.url()),Instant.now(),java.util.Optional.empty(),java.util.List.of());}
}
