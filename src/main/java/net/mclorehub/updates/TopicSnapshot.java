package net.mclorehub.updates;

import com.google.gson.*;
import net.mclorehub.catalogue.LoreCatalogue.*;
import net.mclorehub.importer.ImportException;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import static net.mclorehub.importer.JsonSupport.*;

public record TopicSnapshot(String documentId,String url,String title,String author,String body,List<ThreadPost> posts,
                            List<String> pages,Instant fetchedAt,Optional<Status> observedStatus,List<String> absentPosts) {
    public TopicSnapshot { posts=List.copyOf(posts);pages=List.copyOf(pages);absentPosts=List.copyOf(absentPosts); }
    public JsonObject json(){
        JsonObject r=new JsonObject();r.addProperty("schema_version",1);r.addProperty("document_id",documentId);r.addProperty("url",url);r.addProperty("title",title);
        r.addProperty("author",author);r.addProperty("body",body);r.add("posts",JSON.toJsonTree(posts));r.add("pages",JSON.toJsonTree(pages));
        r.addProperty("fetched_at",fetchedAt.toString());r.addProperty("observed_status",observedStatus.map(Enum::name).orElse(""));r.add("absent_posts",JSON.toJsonTree(absentPosts));return r;
    }
    public String contentFingerprint(){JsonObject r=json();r.remove("fetched_at");r.remove("pages");return fingerprint(r);}
    public static TopicSnapshot parse(JsonObject r)throws IOException{
        if(!r.keySet().equals(Set.of("schema_version","document_id","url","title","author","body","posts","pages","fetched_at","observed_status","absent_posts")))
            throw new ImportException("Unknown/missing topic snapshot fields");
        if(integer(r,"schema_version")!=1)throw new ImportException("Unsupported topic snapshot");String id=text(r,"document_id");ForumUrls.checked(text(r,"url"),id);
        List<String> pages=strings(r,"pages");if(pages.isEmpty()||pages.size()>20)throw new ImportException("Invalid topic page count");
        for(String p:pages)ForumUrls.checked(p,id);
        List<ThreadPost> posts=new ArrayList<>();Set<String> ids=new HashSet<>();
        for(JsonElement item:array(r.get("posts"),"posts")){
            JsonObject p=object(item,"post");if(!p.keySet().equals(Set.of("id","author","url","text")))throw new ImportException("Unknown/missing refreshed post fields");
            String postId=identifier(p,"id");if(!ids.add(postId))throw new ImportException("Duplicate refreshed post ID");
            ForumUrls.checked(text(p,"url"),id);posts.add(new ThreadPost(postId,string(p,"author"),text(p,"url"),string(p,"text")));
        }
        if(posts.size()>1000)throw new ImportException("Too many refreshed replies");
        try{
            String status=string(r,"observed_status");Instant at=Instant.parse(text(r,"fetched_at"));if(at.isAfter(Instant.now().plusSeconds(60)))throw new IllegalArgumentException("Future refresh date");
            List<String> absent=strings(r,"absent_posts");if(absent.stream().anyMatch(p->!ids.contains(p)))throw new IllegalArgumentException("Unknown absent post");
            return new TopicSnapshot(id,text(r,"url"),text(r,"title"),string(r,"author"),text(r,"body"),posts,pages,at,
                    status.isEmpty()?Optional.empty():Optional.of(Status.valueOf(status)),absent);
        }catch(RuntimeException e){throw new ImportException("Invalid topic snapshot metadata",e);}
    }
    public Document apply(Document base){
        if(!base.id().equals(documentId))throw new IllegalArgumentException("Topic ID mismatch");
        boolean userTrusted=base.trust().isPresent();Status status=observedStatus.orElse(userTrusted?base.status():Status.UNKNOWN);
        List<String> gaps=new ArrayList<>(base.coverageGaps());
        gaps.removeIf(g->g.startsWith("unattributed_thread_post:")||g.startsWith("previous_reply_not_returned:"));
        posts.stream().filter(p->p.author().isBlank()).forEach(p->gaps.add("unattributed_thread_post:"+p.id()));
        for(String p:absentPosts)gaps.add("previous_reply_not_returned:"+p);
        return new Document(base.id(),title,url,author.isBlank()?Optional.empty():Optional.of(author),base.exportSnapshotAt(),body,hash(body),contentFingerprint(),base.family(),base.category(),base.aliases(),base.role(),
                status,ReviewState.SOURCE_REFRESHED,base.decision(),posts,gaps,
                Optional.of(new SourceTrust("Forum main-post check; archived replies are not refreshed",fetchedAt,pages)));
    }
}
