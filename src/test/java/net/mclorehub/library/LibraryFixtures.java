package net.mclorehub.library;

import com.google.gson.*;
import net.mclorehub.catalogue.LoreCatalogue;
import java.nio.file.*;
import static net.mclorehub.TestData.*;
import static net.mclorehub.importer.JsonSupport.*;

final class LibraryFixtures {
    static LoreCatalogue catalogue(Path folder) throws Exception {
        Files.createDirectories(folder); JsonObject source=export(); JsonArray records=source.getAsJsonArray("records");
        String[][] entries={{"101","Æther","alias one","body dawn 薄明","magic"},
                {"102","Body reference","Æther","different text","magic"},
                {"103","Unreviewed Æther","","body unreviewed","feat"},
                {"104","A third document","","Long body about Æther and dawn. 😀","feat"}};
        JsonObject original=record(source).deepCopy(); records.remove(0);
        for(String[] row:entries){
            JsonObject r=original.deepCopy();r.addProperty("topic_id",row[0]);r.addProperty("title",row[1]);
            r.addProperty("content_text",row[3]);r.addProperty("category",row[4]);
            JsonArray aliases=new JsonArray();if(!row[2].isEmpty())aliases.add(row[2]);r.add("search_aliases",aliases);
            if(row[0].equals("104")){JsonArray posts=new JsonArray();posts.add(post("456"));
                posts.get(0).getAsJsonObject().addProperty("content_text","Reply-only keyword: starlight");r.add("thread_posts",posts);}
            rehash(r);records.add(r);
        }
        source.getAsJsonObject("metadata").addProperty("record_count",entries.length);
        Path pack=write(folder.resolve("pack.json"),candidate(folder,source));
        JsonArray decisions=new JsonArray();
        for(var doc:LoreCatalogue.load(pack,null).documents()){
            if(doc.id().endsWith("103"))continue;
            JsonObject d=new JsonObject();d.addProperty("document_id",doc.id());d.addProperty("revision_sha256",doc.revision());
            d.addProperty("review_fingerprint",doc.fingerprint());d.addProperty("status","accepted_current");
            d.addProperty("reviewer","Synthetic reviewer, not server canon");d.addProperty("reviewed_at","2026-10-20T00:00:00Z");
            JsonArray evidence=new JsonArray();JsonObject e=new JsonObject();e.addProperty("source_url",doc.url());
            e.addProperty("passage","Synthetic test evidence only");evidence.add(e);d.add("evidence",evidence);decisions.add(d);
        }
        JsonObject reviews=new JsonObject();reviews.addProperty("schema_version",1);reviews.add("decisions",decisions);
        Path reviewPath=write(folder.resolve("reviews.json"),reviews);
        return LoreCatalogue.load(pack,reviewPath);
    }
}
