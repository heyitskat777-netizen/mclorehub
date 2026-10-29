package net.mclorehub.updates;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.net.URI;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class ForumSessionTest {
    @TempDir Path root;static final Instant NOW=Instant.parse("2026-01-01T00:00:00Z");
    static final String TOKEN="SYNTHETIC_SESSION_VALUE_NEVER_REAL";
    static String line(String domain,String name,String value,long expires){return domain+"\tTRUE\t/\tTRUE\t"+expires+"\t"+name+"\t"+value+"\n";}
    static String cookie(){return line(".lordofthecraft.net","ips4_IPSSessionFront",TOKEN,0);}
    @Test void onlyExplicitForumTopicHttpsRequestsGetSession()throws Exception{var c=ForumCookies.netscape(cookie(),NOW);
        assertTrue(c.header(URI.create(UpdateFixtures.URL),NOW).contains(TOKEN));
        for(String url:List.of("http://www.lordofthecraft.net/forums/topic/123/","https://www.lordofthecraft.net/robots.txt","https://www.lordofthecraft.net/login/",
                "https://evil.example/forums/topic/123/","https://www.lordofthecraft.net.evil.example/forums/topic/123/","https://www.lordofthecraft.net:443/forums/topic/123/",
                "https://www.lordofthecraft.net/forums/topic/123-x%2F..%2Fadmin/","https://www.lordofthecraft.net/forums/topic/123-x%5C..%5Cadmin/",UpdateFixtures.URL+"?do=report"))
            assertEquals("",c.header(URI.create(url),NOW),url);}
    @Test void unrelatedCookiesAndChallengeCookiesAreNotImported()throws Exception{var c=ForumCookies.netscape(cookie()+line(".example.com","ips4_pass_hash","UNRELATED",0)+line(".lordofthecraft.net","cf_clearance","CHALLENGE",0),NOW);
        String value=c.header(URI.create(UpdateFixtures.URL),NOW);assertFalse(value.contains("UNRELATED"));assertFalse(value.contains("CHALLENGE"));assertFalse(value.contains("cf_clearance"));}
    @Test void expiredAndHostOnlyParentCookiesAreNotUsed()throws Exception{assertThrows(java.io.IOException.class,()->ForumCookies.netscape(line(".lordofthecraft.net","ips4_member_id",TOKEN,NOW.getEpochSecond()-1),NOW));
        assertThrows(java.io.IOException.class,()->ForumCookies.netscape(cookie().replace(".lordofthecraft.net\tTRUE","lordofthecraft.net\tFALSE"),NOW));}
    @Test void cookieExpiryAlsoAppliesAfterLoading()throws Exception{var c=ForumCookies.netscape(line(".lordofthecraft.net","ips4_member_id",TOKEN,NOW.getEpochSecond()+10),NOW);
        assertEquals("",c.header(URI.create(UpdateFixtures.URL),NOW.plusSeconds(20)));}
    @Test void httpOnlyExportWorksAndDebugStringsRedact()throws Exception{var c=ForumCookies.netscape("#HttpOnly_"+cookie(),NOW);assertFalse(c.toString().contains(TOKEN));
        assertTrue(c.header(URI.create(UpdateFixtures.URL),NOW).contains(TOKEN));}
    @Test void invalidDataErrorsNeverEchoTokens(){for(String input:List.of(cookie()+cookie(),cookie().replace(TOKEN,TOKEN+";bad"),cookie().replace("\t0\t","\tinvalid\t"),TOKEN)){
        var error=assertThrows(java.io.IOException.class,()->ForumCookies.netscape(input,NOW));assertFalse(error.toString().contains(TOKEN));}}
    @Test void importPersistsOnlyFilteredSessionAndForgetDeletesIt()throws Exception{Path exported=Files.writeString(root.resolve("export.txt"),cookie()+line(".example.com","ips4_member_id","OTHER_SITE_SECRET",0));
        try(var session=new ForumSession(root,Clock.fixed(NOW,ZoneOffset.UTC))){assertTrue(session.importFile(exported).get(5,TimeUnit.SECONDS).available());
            Path stored=root.resolve("auth/forum-session.json");assertFalse(Files.readString(stored).contains("OTHER_SITE_SECRET"));
            if(Files.getFileStore(stored).supportsFileAttributeView("posix"))assertEquals("rw-------",java.nio.file.attribute.PosixFilePermissions.toString(Files.getPosixFilePermissions(stored)));
            assertFalse(session.view().toString().contains(TOKEN));
            try(var reopened=new ForumSession(root,Clock.fixed(NOW,ZoneOffset.UTC))){var reloaded=reopened.reload().get(5,TimeUnit.SECONDS);assertTrue(reloaded.available());assertTrue(reloaded.message().contains("loaded; account access not verified"));}
            session.forget().get(5,TimeUnit.SECONDS);assertFalse(Files.exists(stored));assertEquals("",session.header(URI.create(UpdateFixtures.URL)));assertTrue(Files.exists(exported));}}
    @Test void invalidImportRetainsPreviousSessionWithoutEchoingIt()throws Exception{Path good=Files.writeString(root.resolve("good.txt"),cookie());Path bad=Files.writeString(root.resolve("bad.txt"),TOKEN+"invalid");
        try(var session=new ForumSession(root,Clock.fixed(NOW,ZoneOffset.UTC))){session.importFile(good).get(5,TimeUnit.SECONDS);byte[] before=Files.readAllBytes(root.resolve("auth/forum-session.json"));
            var view=session.importFile(bad).get(5,TimeUnit.SECONDS);assertTrue(view.available());assertTrue(view.message().contains("failed"));assertFalse(view.toString().contains(TOKEN));
            assertArrayEquals(before,Files.readAllBytes(root.resolve("auth/forum-session.json")));}}
    @Test void corruptStoredSessionIsRetainedAndNotSent()throws Exception{Files.createDirectories(root.resolve("auth"));Path file=Files.writeString(root.resolve("auth/forum-session.json"),TOKEN);
        try(var session=new ForumSession(root)){assertFalse(session.reload().get(5,TimeUnit.SECONDS).available());assertEquals(TOKEN,Files.readString(file));assertFalse(session.view().toString().contains(TOKEN));}}
    @Test void forgetWinsOverAnAlreadyQueuedImport()throws Exception{Path export=Files.writeString(root.resolve("export.txt"),cookie());try(var session=new ForumSession(root,Clock.fixed(NOW,ZoneOffset.UTC))){
        var importing=session.importFile(export);var forgetting=session.forget();importing.get(5,TimeUnit.SECONDS);forgetting.get(5,TimeUnit.SECONDS);
        assertEquals("",session.header(URI.create(UpdateFixtures.URL)));assertFalse(Files.exists(root.resolve("auth/forum-session.json")));}}
}
