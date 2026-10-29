package net.mclorehub.updates;

import com.google.gson.*;
import net.mclorehub.catalogue.LoreCatalogue;
import net.mclorehub.catalogue.LoreCatalogue.*;
import net.mclorehub.importer.ImportException;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import static net.mclorehub.importer.JsonSupport.*;

/** Explicit user confirmation, bound to every supplied record, not invented staff reviews. */
public final class BaselineTrust {
    private BaselineTrust() {}
    public static JsonObject manifest(LoreCatalogue catalogue,Instant confirmedAt) {
        JsonObject root=new JsonObject();root.addProperty("schema_version",1);root.addProperty("basis","user_confirmed_database");
        root.addProperty("confirmed_at",confirmedAt.toString());root.addProperty("pack_revision",catalogue.revision());JsonObject records=new JsonObject();
        catalogue.documents().forEach(d->records.addProperty(d.id(),d.fingerprint()));root.add("records",records);return root;
    }
    public static LoreCatalogue apply(LoreCatalogue catalogue,Path path)throws IOException {
        if(!Files.exists(path,LinkOption.NOFOLLOW_LINKS))return catalogue;
        JsonObject root=read(path,2<<20);
        if(integer(root,"schema_version")!=1||!text(root,"basis").equals("user_confirmed_database"))throw new ImportException("Unsupported baseline trust manifest");
        Instant time;try{time=Instant.parse(text(root,"confirmed_at"));}catch(RuntimeException e){throw new ImportException("Invalid confirmation date",e);}
        if(time.isAfter(Instant.now()))throw new ImportException("Baseline confirmation is in the future");
        if(!text(root,"pack_revision").equals(catalogue.revision()))return catalogue;
        JsonObject records=object(root.get("records"),"records");if(records.size()>10000)throw new ImportException("Too many trust bindings");
        for(String id:records.keySet()){
            if(!id.matches("lotc:topic:[1-9][0-9]{0,17}"))throw new ImportException("Invalid trusted document ID");sha(records,id);
        }
        Map<String,Document> changed=new HashMap<>();
        for(Document d:catalogue.documents())if(records.has(d.id())&&sha(records,d.id()).equals(d.fingerprint())){
            boolean explicit=d.reviewState()==ReviewState.APPLIED;
            List<String> gaps=d.coverageGaps().stream().filter(g->!g.equals("current_status_requires_review")).toList();
            changed.put(d.id(),new Document(d.id(),d.title(),d.url(),d.author(),d.exportSnapshotAt(),d.body(),d.revision(),d.fingerprint(),d.family(),d.category(),d.aliases(),d.role(),
                    explicit?d.status():Status.ACCEPTED_CURRENT,explicit?d.reviewState():ReviewState.USER_TRUSTED,d.decision(),d.posts(),gaps,
                    Optional.of(new SourceTrust("User confirmed the supplied database is accepted/accurate",time,List.of(d.url())))));
        }
        return catalogue.replace(changed);
    }
}
