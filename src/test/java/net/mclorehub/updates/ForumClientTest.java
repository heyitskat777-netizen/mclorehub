package net.mclorehub.updates;

import net.mclorehub.catalogue.LoreCatalogue;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ForumClientTest {
    @TempDir Path root;LoreCatalogue.Document doc;
    @BeforeEach void setup()throws Exception{doc=UpdateFixtures.install(root,true).documents().getFirst();}
    ForumClient.Response response(URI uri,int status,String body){return new ForumClient.Response(uri,status,uri.getPath().equals("/robots.txt")?"text/plain":"text/html","","",body.getBytes(StandardCharsets.UTF_8));}
    ForumClient client(ForumClient.Transport t){return new ForumClient(t,Duration.ZERO,Clock.systemUTC());}
    String one(String text){return UpdateFixtures.html(1,1,null,"<p>"+text+"</p>","");}
    @Test void fetchesOnlySelectedTopicPaginationAndPreservesAttribution()throws Exception{
        List<URI> requests=new ArrayList<>();String page2=UpdateFixtures.URL+"page/2/";
        var snapshot=client(uri->{requests.add(uri);return response(uri,200,uri.getPath().equals("/robots.txt")?"User-agent: *\nAllow: /":
                ForumUrls.page(uri)==1?UpdateFixtures.html(1,2,page2,"<p>Main paragraph.</p><p>Second paragraph.</p><script>DO NOT EXECUTE</script>",UpdateFixtures.post("1002","Bob","First reply")):
                        UpdateFixtures.html(2,2,null,"",UpdateFixtures.post("1003","Carol","Second page amendment")));}).fetch(doc);
        assertEquals(3,requests.size());assertEquals(List.of("1002","1003"),snapshot.posts().stream().map(p->p.id()).toList());
        assertEquals("Alice",snapshot.author());assertEquals("Main paragraph.\nSecond paragraph.",snapshot.body());assertFalse(snapshot.body().contains("EXECUTE"));
        assertEquals(2,snapshot.pages().size());assertEquals(Optional.of(LoreCatalogue.Status.ACCEPTED_CURRENT),snapshot.observedStatus());
    }
    @Test void inaccessibleLaterPageFailsWholeFetch()throws Exception{
        assertThrows(java.io.IOException.class,()->client(uri->response(uri,uri.getPath().contains("/page/2/")?403:200,uri.getPath().equals("/robots.txt")?"":
                UpdateFixtures.html(1,2,UpdateFixtures.URL+"page/2/","Main","") )).fetch(doc));}
    @Test void loginChallengeOrMissingMainPostCannotReplaceLore(){assertThrows(java.io.IOException.class,()->ForumHtml.parse("<html><h1>Sign in</h1></html>".getBytes(),URI.create(UpdateFixtures.URL),doc.id()));}
    @Test void redirectsCannotLeaveHostTopicOrHttps(){
        for(String target:List.of("http://www.lordofthecraft.net/forums/topic/123-synthetic/","https://example.com/forums/topic/123/",UpdateFixtures.URL.replace("123-","999-"),"/login/")){
            assertThrows(java.io.IOException.class,()->client(uri->uri.getPath().equals("/robots.txt")?response(uri,200,""):
                    new ForumClient.Response(uri,302,"text/html",target,"",new byte[0])).fetch(doc));}
    }
    @Test void sameTopicSlugRedirectIsAllowed()throws Exception{String canonical=UpdateFixtures.URL.replace("synthetic","renamed");var s=client(uri->{
        if(uri.getPath().equals("/robots.txt"))return response(uri,200,"");if(uri.toString().equals(UpdateFixtures.URL))return new ForumClient.Response(uri,301,"text/html",canonical,"",new byte[0]);return response(uri,200,one("Main body"));}).fetch(doc);
        assertEquals(canonical,s.url());}
    @Test void paginationLoopWrongIdAndMissingNextAreRejected(){for(String next:List.of(UpdateFixtures.URL,UpdateFixtures.URL.replace("123-","124-")+"page/2/")){
        assertThrows(java.io.IOException.class,()->ForumHtml.parse(UpdateFixtures.html(1,2,next,"Main","").getBytes(),URI.create(UpdateFixtures.URL),doc.id()));}
        assertThrows(java.io.IOException.class,()->ForumHtml.parse(UpdateFixtures.html(1,2,null,"Main","").getBytes(),URI.create(UpdateFixtures.URL),doc.id()));}
    @Test void excessivePageCountFailsBeforeSaving(){assertThrows(java.io.IOException.class,()->ForumHtml.parse(UpdateFixtures.html(1,21,UpdateFixtures.URL+"page/2/","Main","").getBytes(),URI.create(UpdateFixtures.URL),doc.id()));}
    @Test void duplicateConflictingPostIdsFail(){assertThrows(java.io.IOException.class,()->ForumHtml.parse(UpdateFixtures.html(1,1,null,"Main",UpdateFixtures.post("1001","Bob","Different post")).getBytes(),URI.create(UpdateFixtures.URL),doc.id()));}
    @Test void robotsSpecificGroupsAndWildcardRulesAreHonored(){var robots=new RobotsPolicy("User-agent: GPTBot\nDisallow: /\nUser-agent: *\nAllow: /\nDisallow: /forums/topic/*/page/2/\nUser-agent: mclorehub\nDisallow: /forums/\nAllow: /forums/topic/123-synthetic/$\n");
        assertTrue(robots.allowed(URI.create(UpdateFixtures.URL)));assertFalse(robots.allowed(URI.create(UpdateFixtures.URL+"page/2/")));}
    @Test void disallowedTopicMakesNoTopicRequest(){List<URI> requested=new ArrayList<>();assertThrows(java.io.IOException.class,()->client(uri->{requested.add(uri);return response(uri,200,"User-agent: *\nDisallow: /forums/");}).fetch(doc));assertEquals(1,requested.size());}
    @Test void rateLimitRetryAfterPreventsImmediateRetry(){List<URI> requests=new ArrayList<>();var c=client(uri->{requests.add(uri);return new ForumClient.Response(uri,429,"text/plain","","120",new byte[0]);});
        assertThrows(java.io.IOException.class,()->c.fetch(doc));assertThrows(java.io.IOException.class,()->c.fetch(doc));assertEquals(1,requests.size());}
    @Test void missingPagesAndServerErrorsNeverMeanDeletion(){for(int status:List.of(401,403,404,500))assertThrows(java.io.IOException.class,()->client(uri->response(uri,uri.getPath().equals("/robots.txt")?200:status,"")).fetch(doc));}
    @Test void responseSizeIsBounded(){assertThrows(java.io.IOException.class,()->client(uri->new ForumClient.Response(uri,200,"text/html","","",new byte[(4<<20)+1])).fetch(doc));}
    @Test void absentOldRepliesAreRetainedAndFlagged()throws Exception{
        var original=new LoreCatalogue.Document(doc.id(),doc.title(),doc.url(),doc.author(),doc.exportSnapshotAt(),doc.body(),doc.revision(),doc.fingerprint(),doc.family(),doc.category(),doc.aliases(),doc.role(),doc.status(),doc.reviewState(),doc.decision(),
                List.of(new LoreCatalogue.ThreadPost("999","Prior author",doc.url()+"#comment-999","Previously captured amendment")),doc.coverageGaps(),doc.trust());
        var s=client(uri->response(uri,200,uri.getPath().equals("/robots.txt")?"":one("Current main"))).fetch(original);
        assertEquals(List.of("999"),s.absentPosts());assertEquals("Previously captured amendment",s.posts().getFirst().text());}
    @Test void bodyKeywordsDoNotDeclareShelving(){var p=assertDoesNotThrow(()->ForumHtml.parse(one("This paragraph discusses shelved lore and pending drafts.").getBytes(),URI.create(UpdateFixtures.URL),doc.id()));assertEquals(Optional.of(LoreCatalogue.Status.ACCEPTED_CURRENT),p.status());}
    @Test void explicitForumShelvedLocationIsRecorded(){String html=one("Main body").replace("Implemented Lore","Shelved Lore");var p=assertDoesNotThrow(()->ForumHtml.parse(html.getBytes(),URI.create(UpdateFixtures.URL),doc.id()));assertEquals(Optional.of(LoreCatalogue.Status.SHELVED),p.status());}
    @Test void queryEndpointsAreNotFetched(){for(String url:List.of(UpdateFixtures.URL+"?do=findComment&comment=1001",UpdateFixtures.URL+"?csrfKey=secret"))assertThrows(java.io.IOException.class,()->ForumUrls.checked(url,doc.id()));}
    @Test void httpDateRetryAfterIsHonoredBeyondDefaultMinute(){
        class MutableClock extends Clock{Instant time=Instant.parse("2026-10-20T00:00:00Z");public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return Clock.fixed(time,zone);}public Instant instant(){return time;}}
        var clock=new MutableClock();List<URI> requested=new ArrayList<>();String after=java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.format(clock.instant().plusSeconds(3600).atZone(ZoneOffset.UTC));
        var c=new ForumClient(uri->{requested.add(uri);return new ForumClient.Response(uri,429,"text/plain","",after,new byte[0]);},Duration.ZERO,clock);
        assertThrows(java.io.IOException.class,()->c.fetch(doc));clock.time=clock.time.plusSeconds(120);assertThrows(java.io.IOException.class,()->c.fetch(doc));assertEquals(1,requested.size());}
    @Test void unknownPagerLayoutFailsInsteadOfSavingPartialThread(){String html=one("Main body").replace("data-pages='1'","data-unknown-pages='2'");
        assertThrows(java.io.IOException.class,()->ForumHtml.parse(html.getBytes(),URI.create(UpdateFixtures.URL),doc.id()));}
    @Test void nakedHostAndReplyUrlsNormalizeToFirstCanonicalTopicPage(){assertEquals(UpdateFixtures.URL,assertDoesNotThrow(()->ForumUrls.first(UpdateFixtures.URL.replace("www.","")+"page/2/#comment-999",doc.id())).toString());}
    @Test void spoilerControlsAndLayoutWhitespaceAreRemovedButHiddenLoreIsKept(){
        var element=org.jsoup.Jsoup.parseBodyFragment("<div><p>Foreword</p> \n <div></div> \n <div></div> \n <div class='ipsSpoiler_header'>Spoiler control</div><div class='ipsSpoiler_contents'><p>Hidden lore text</p></div></div>").body();
        String text=ForumHtml.plain(element);assertTrue(text.contains("Foreword"));assertTrue(text.contains("Hidden lore text"));assertFalse(text.contains("Spoiler control"));assertFalse(text.contains("\n\n\n"));}
}
