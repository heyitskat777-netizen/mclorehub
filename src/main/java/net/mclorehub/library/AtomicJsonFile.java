package net.mclorehub.library;

import com.google.gson.JsonObject;
import net.mclorehub.importer.ImportException;
import java.io.IOException;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static net.mclorehub.importer.JsonSupport.*;

/** Shared personal-file transaction. A caller must successfully decode before saving. */
public final class AtomicJsonFile {
    private final Path file;
    private final int limit;
    public AtomicJsonFile(Path file,int limit){this.file=file;this.limit=limit;}
    public byte[] read()throws IOException{
        if(Files.isSymbolicLink(file))throw new ImportException("Personal file must not be a symbolic link");
        if(!Files.exists(file,LinkOption.NOFOLLOW_LINKS))return null;
        try(var in=Files.newInputStream(file)){return bounded(in,limit);}
    }
    public String save(JsonObject value,Optional<String> expected)throws IOException{
        byte[] before=read();check(before,expected);
        byte[] serialized=(JSON.toJson(value)+"\n").getBytes(StandardCharsets.UTF_8);
        if(serialized.length>limit)throw new ImportException("Personal data exceeds size limit; original retained");
        Files.createDirectories(file.getParent());Path staged=Files.createTempFile(file.getParent(),".personal-",".tmp"),backup=null;
        try{
            force(staged,serialized);check(read(),expected);
            if(before!=null){backup=Files.createTempFile(file.getParent(),".previous-",".tmp");force(backup,before);
                String name=file.getFileName().toString().replaceFirst("\\.json$","")+".previous.json";
                Files.move(backup,file.resolveSibling(name),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            Files.move(staged,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);return hash(serialized);
        }finally{Files.deleteIfExists(staged);if(backup!=null)Files.deleteIfExists(backup);}
    }
    private static void check(byte[] bytes,Optional<String> expected)throws ImportException{
        if(!Objects.equals(expected,bytes==null?Optional.empty():Optional.of(hash(bytes))))
            throw new ImportException("Personal data changed outside the mod; reload before saving");
    }
    private static void force(Path path,byte[] bytes)throws IOException{Files.write(path,bytes);
        try(var channel=FileChannel.open(path,StandardOpenOption.WRITE)){channel.force(true);}}
}
