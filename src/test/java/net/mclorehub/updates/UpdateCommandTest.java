package net.mclorehub.updates;

import net.mclorehub.importer.ImportCommand;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;

class UpdateCommandTest {
    @TempDir Path root;
    int run(String...args){return ImportCommand.run(args,new PrintStream(new ByteArrayOutputStream()),new PrintStream(new ByteArrayOutputStream()));}
    @Test void confirmationCommandBindsAllSuppliedRecords()throws Exception{var c=UpdateFixtures.install(root,false);Path output=root.resolve("trust.json");
        assertEquals(0,run("trust","--pack",root.resolve("packs/active/pack.json").toString(),"--output",output.toString()));assertEquals(2,BaselineTrust.apply(c,output).currentDocuments().size());}
    @Test void existingTrustAndInvalidInputAreNotOverwritten()throws Exception{UpdateFixtures.install(root,false);Path output=Files.writeString(root.resolve("trust.json"),"keep");
        assertEquals(1,run("trust","--pack",root.resolve("packs/active/pack.json").toString(),"--output",output.toString()));assertEquals("keep",Files.readString(output));
        Path invalid=Files.writeString(root.resolve("bad.json"),"invalid");Path absent=root.resolve("absent.json");assertEquals(1,run("trust","--pack",invalid.toString(),"--output",absent.toString()));assertFalse(Files.exists(absent));}
    @Test void duplicateUnknownOptionsAndMissingLibraryFail(){assertEquals(2,run("trust","--pack","x","--pack","y"));assertEquals(2,run("refresh","--unexpected","x","--document","lotc:topic:123"));
        assertEquals(1,run("refresh","--library",root.toString(),"--document","lotc:topic:123"));}
}
