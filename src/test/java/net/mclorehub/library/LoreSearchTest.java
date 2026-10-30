package net.mclorehub.library;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CancellationException;
import static org.junit.jupiter.api.Assertions.*;

class LoreSearchTest {
    @TempDir Path temp;
    LoreSearch index;
    @BeforeEach void setup() throws Exception { index=new LoreSearch(LibraryFixtures.catalogue(temp)); }
    LoreSearch.Page search(String q,boolean audit){return index.search(new LoreSearch.Query(q,"",audit,false,Set.of(),0,10));}
    @Test void defaultExcludesUnreviewedAndRanksTitleAliasBeforeBody(){
        assertEquals(List.of("lotc:topic:101","lotc:topic:102","lotc:topic:104"),search("æther",false).hits().stream().map(h->h.document().id()).toList());
        assertEquals(4,search("æther",true).total());
    }
    @Test void allTermsMustMatchButCanSpanFields(){assertEquals(2,search("æther dawn",false).total());assertEquals(0,search("dawn nonexistent",true).total());}
    @Test void unicodeCaseAndCompatibilityNormalization(){assertEquals(1,search("薄明",false).total());assertEquals(3,search("  ÆTHER  ",false).total());
        assertEquals(1,search("ＡＬＩＡＳ　ＯＮＥ",false).total());}
    @Test void discussionDoesNotAppearInDefaultLoreSearch(){assertEquals(0,search("starlight",false).total());assertEquals(0,search("starlight",true).total());}
    @Test void paginationAndBlankQueriesAreDeterministic(){var first=index.search(new LoreSearch.Query("","",false,false,Set.of(),0,1));
        var next=index.search(new LoreSearch.Query("","",false,false,Set.of(),1,2));assertEquals(3,first.total());
        assertEquals("lotc:topic:104",first.hits().getFirst().document().id());assertEquals(2,next.hits().size());
        assertEquals(0,index.search(new LoreSearch.Query("","",false,false,Set.of(),999,2)).hits().size());}
    @Test void categoryAndFavoritesIntersectCurrentScope(){assertEquals(1,index.search(new LoreSearch.Query("","feat",false,false,Set.of(),0,10)).total());
        var favorites=Set.of("lotc:topic:103","lotc:topic:104","lotc:topic:999");
        assertEquals(1,index.search(new LoreSearch.Query("","",false,true,favorites,0,10)).total());
        assertEquals(2,index.search(new LoreSearch.Query("","",true,true,favorites,0,10)).total());}
    @Test void categoryListIsSortedAndImmutable(){assertEquals(List.of("feat","magic"),index.categories(false));assertThrows(UnsupportedOperationException.class,()->index.categories(true).add("x"));}
    @Test void alphabeticalResultsIgnoreForumPrefixesWhileRetainingOriginalTitles()throws Exception {
        var catalogue=LibraryFixtures.catalogue(temp.resolve("sorting"));var replacements=new HashMap<String,net.mclorehub.catalogue.LoreCatalogue.Document>();
        for(var d:catalogue.documents())if(d.id().equals("lotc:topic:101")||d.id().equals("lotc:topic:102")){
            String title=d.id().endsWith("101")?"[✓] [CA] Zebra needle":"[Magic Lore] Alpha needle";
            replacements.put(d.id(),new net.mclorehub.catalogue.LoreCatalogue.Document(d.id(),title,d.url(),d.author(),d.exportSnapshotAt(),d.body(),d.revision(),d.fingerprint(),d.family(),d.category(),d.aliases(),d.role(),d.status(),d.reviewState(),d.decision(),d.posts(),d.coverageGaps(),d.trust()));
        }
        var page=new LoreSearch(catalogue.replace(replacements)).search(new LoreSearch.Query("needle","",false,false,Set.of(),0,10));
        assertEquals(List.of("lotc:topic:102","lotc:topic:101"),page.hits().stream().map(h->h.document().id()).toList());
        assertEquals("[✓] [CA] Zebra needle",page.hits().getLast().document().title());
    }
    @Test void resultsAreImmutable(){assertThrows(UnsupportedOperationException.class,()->search("",true).hits().clear());}
    @Test void invalidBoundsFail(){assertThrows(IllegalArgumentException.class,()->new LoreSearch.Query("x".repeat(257),"",false,false,Set.of(),0,10));
        assertThrows(IllegalArgumentException.class,()->new LoreSearch.Query("","",false,false,Set.of(),-1,10));
        assertThrows(IllegalArgumentException.class,()->new LoreSearch.Query("","",false,false,Set.of(),0,101));}
    @Test void cancelledSearchStopsAndRetainsInterruption(){Thread.currentThread().interrupt();try{assertThrows(CancellationException.class,()->search("",true));assertTrue(Thread.currentThread().isInterrupted());}finally{Thread.interrupted();}}
    @Test void localeDoesNotChangeResults(){Locale old=Locale.getDefault();try{Locale.setDefault(Locale.forLanguageTag("tr-TR"));assertEquals(1,search("ALIAS ONE",false).total());}finally{Locale.setDefault(old);}}
    @Test void replyAttributionAndGapsAreImmutableAndRetained(){var d=search("third document",false).hits().getFirst().document();assertEquals("",d.posts().getFirst().author());
        assertTrue(d.coverageGaps().contains("unattributed_thread_post:456"));assertThrows(UnsupportedOperationException.class,()->d.posts().clear());}
}
