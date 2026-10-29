package net.mclorehub.updates;

import net.mclorehub.catalogue.LoreCatalogue;
import net.mclorehub.library.LibraryService;
import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static net.mclorehub.importer.JsonSupport.*;

public final class UpdateCommand {
    private UpdateCommand(){}
    public static int run(String action,String[] args,PrintStream out,PrintStream err){
        Set<String> allowed=action.equals("trust")?Set.of("--pack","--output"):Set.of("--library","--document");Map<String,String> options=new HashMap<>();
        if(args.length!=4){err.println("Expected "+(action.equals("trust")?"--pack <pack.json> --output <new-trust.json>":"--library <config/mclorehub> --document <lotc:topic:id>"));return 2;}
        for(int i=0;i<args.length;i+=2)if(!allowed.contains(args[i])||options.putIfAbsent(args[i],args[i+1])!=null){err.println("Unknown/duplicate option");return 2;}
        if(!options.keySet().equals(allowed))return 2;
        try{
            if(action.equals("trust")){
                LoreCatalogue catalogue=LoreCatalogue.load(Path.of(options.get("--pack")),null);
                Path output=Path.of(options.get("--output")).toAbsolutePath().normalize();
                if(Files.exists(output,LinkOption.NOFOLLOW_LINKS))throw new IOException("Trust output already exists");
                Path temp=Files.createTempFile(output.getParent(),".trust-",".tmp");
                try{Files.writeString(temp,JSON.toJson(BaselineTrust.manifest(catalogue,Instant.now()))+"\n");Files.move(temp,output);}
                finally{Files.deleteIfExists(temp);}
                out.println("Recorded user confirmation for "+catalogue.documents().size()+" fingerprint-bound references; not a staff review.");
            }else try(var service=new LibraryService(Path.of(options.get("--library")))){
                var view=service.reload().get(30,TimeUnit.SECONDS);if(view.phase()!=LibraryService.Phase.READY)throw new IOException(view.message().isBlank()?"Library is not loaded":view.message());
                var result=(action.equals("undo")?service.rollback(options.get("--document")):service.refresh(options.get("--document"))).get(120,TimeUnit.SECONDS);
                out.println(result.message());
            }
            return 0;
        }catch(Exception e){err.println(action+" failed: "+(e.getCause()==null?e.getMessage():e.getCause().getMessage()));return 1;}
    }
}
