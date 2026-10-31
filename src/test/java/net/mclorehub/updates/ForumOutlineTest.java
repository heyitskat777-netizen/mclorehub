package net.mclorehub.updates;

import net.mclorehub.catalogue.SourceSection;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import static net.mclorehub.importer.JsonSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class ForumOutlineTest {
    @Test void styledInlineHeadingsKeepExactUnicodeSourceOffsets()throws Exception{
        var dom=Jsoup.parseBodyFragment("<p>Intro 😀</p><p>  <strong>Gust [T1]</strong> </p><p>Exact <em>redlines</em> remain.</p><p><u>Second [T2]</u></p>").body();String before=ForumHtml.plain(dom);var outline=ForumOutline.extract(dom,before);
        assertEquals(before,ForumHtml.plain(dom));assertTrue(outline.stream().anyMatch(h->h.title().equals("Gust [T1]")));assertTrue(outline.stream().anyMatch(h->h.title().equals("Second [T2]")));assertEquals(outline,SourceSection.read(JSON.toJsonTree(outline),before));
    }
    @Test void fabricatedOrOutOfRangeHeadingsAreRejected(){var a=new SourceSection("Fake",0,4,1);assertThrows(java.io.IOException.class,()->SourceSection.read(JSON.toJsonTree(List.of(a)),"Real"));}
    @Test void sourceHeadingsCannotSplitASurrogatePair(){String body="A😀Z";for(var h:List.of(new SourceSection(body.substring(0,2),0,2,1),new SourceSection(body.substring(2),2,4,1)))assertThrows(java.io.IOException.class,()->SourceSection.read(JSON.toJsonTree(List.of(h)),body));}
    @Test void refreshOutlineSurvivesSnapshotSerialization()throws Exception{
        var h=new SourceSection("Spark [T1]",0,10,3);var snapshot=new TopicSnapshot("lotc:topic:123","https://www.lordofthecraft.net/forums/topic/123-synthetic/","Title","Author","Spark [T1]\nBody",List.of(),List.of("https://www.lordofthecraft.net/forums/topic/123-synthetic/"),Instant.parse("2026-10-01T00:00:00Z"),Optional.empty(),List.of(),List.of(h));assertEquals(snapshot,TopicSnapshot.parse(snapshot.json()));
        var old=new TopicSnapshot(snapshot.documentId(),snapshot.url(),snapshot.title(),snapshot.author(),snapshot.body(),snapshot.posts(),snapshot.pages(),snapshot.fetchedAt(),snapshot.observedStatus(),snapshot.absentPosts());assertFalse(old.json().has("source_outline"));assertTrue(TopicSnapshot.parse(old.json()).outline().isEmpty());
    }
    @Test void legacyBreadcrumbWinsOverImplementedAncestor()throws Exception{
        String html="<html><head></head><body><nav class='ipsBreadcrumb'><a>Implemented Lore</a><a>Legacy Lore</a></nav><h1 class='ipsType_pageTitle'>Source</h1><article id='elComment_1'><div data-role='commentContent'><p>Some source text</p></div></article></body></html>";
        assertEquals(net.mclorehub.catalogue.LoreCatalogue.Status.LEGACY,ForumHtml.main(html.getBytes(java.nio.charset.StandardCharsets.UTF_8),URI.create("https://www.lordofthecraft.net/forums/topic/123-source/"),"lotc:topic:123").status().orElseThrow());
    }
}
