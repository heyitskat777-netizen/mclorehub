package net.mclorehub.spell;

import net.mclorehub.catalogue.*;
import net.mclorehub.character.*;
import net.mclorehub.updates.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import static net.mclorehub.TestData.*;
import static net.mclorehub.importer.JsonSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class SpellIndexTest {
    @TempDir Path temp;
    LoreCatalogue catalogue(String html)throws Exception{
        var content=Jsoup.parseBodyFragment(html).body();String body=ForumHtml.plain(content);var raw=export();var record=record(raw);
        record.addProperty("content_text",body);record.add("source_outline",JSON.toJsonTree(ForumOutline.extract(content,body)));rehash(record);
        var pack=write(temp.resolve("pack.json"),candidate(temp,raw));var catalogue=LoreCatalogue.load(pack,null);
        return BaselineTrust.apply(catalogue,write(temp.resolve("trust.json"),BaselineTrust.manifest(catalogue,Instant.parse("2026-10-01T00:00:00Z"))));
    }
    CharacterBook.Entry entry(String tier,String branch){return new CharacterBook.Entry(UUID.randomUUID().toString(),CharacterBook.Kind.MAGIC,"Recorded practice",branch,tier,"",CharacterBook.RecordedStatus.NOT_RECORDED,"","lotc:topic:123");}
    CharacterBook.Sheet sheet(CharacterBook.Entry e){return new CharacterBook.Sheet(UUID.randomUUID().toString(),"Fixture","",false,List.of(e));}
    @Test void filtersByExplicitTierAndKeepsCompleteSpellRedlines()throws Exception{
        var c=catalogue("<h2>Rules</h2><p>All spells require concentration.</p><h3>Spark [T1]</h3><p>A small light.</p><h4>Redlines</h4><p>Never blinds or burns.</p><h3>Storm [T3]</h3><p>A larger spell.</p>");var index=new SpellIndex(c);
        var results=index.search(sheet(entry("T2","")),"",false);assertEquals(1,results.eligible());assertTrue(results.hits().stream().noneMatch(h->h.ability().name().equals("STORM")));
        var spark=results.hits().getFirst();assertEquals(1,spark.ability().minimumTier());assertTrue(SpellIndex.details(spark).sections().stream().anyMatch(s->s.text().contains("Never blinds or burns.")));
        assertTrue(SpellIndex.practice(spark).sections().stream().anyMatch(s->s.text().contains("All spells require concentration.")));assertEquals(2,index.search(sheet(entry("T3","")),"",false).eligible());
    }
    @Test void unknownCharacterTierNeverBecomesMastery()throws Exception{
        var index=new SpellIndex(catalogue("<h3>Storm [T3]</h3><p>Requires a specific tier.</p>"));var r=index.search(sheet(entry("","")),"Storm",false);assertEquals(0,r.eligible());assertEquals(SpellIndex.Access.CHECK_REQUIREMENTS,r.hits().getFirst().access());
    }
    @Test void unrelatedTierMentionInProseIsNotASpellHeading()throws Exception{
        var index=new SpellIndex(catalogue("<h3>Spark [T1]</h3><p>A caster may change color at [T4].</p><p>A Tier 5 illusionist casting an example.</p>"));assertEquals(1,index.references("lotc:topic:123").stream().filter(a->!a.sourceReference()).count());
    }
    @Test void unspecifiedSpellTierStaysExplicit()throws Exception{
        var index=new SpellIndex(catalogue("<h2>Spells and Abilities</h2><h3>Farsight - Non-Combative</h3><p>Check training and local restrictions.</p>"));var r=index.search(sheet(entry("T5","")),"Farsight",false);assertEquals(-1,r.hits().getFirst().ability().minimumTier());assertEquals(0,r.eligible());
    }
    @Test void branchAndExtraInvestmentCannotBeSilentlyAssumed()throws Exception{
        var index=new SpellIndex(catalogue("<h2>Sun Path</h2><h3>[+] Sun Strike [T3]</h3><p>Additional investment required.</p><h2>Moon Path</h2><h3>Moon Strike [T3]</h3><p>Other branch.</p>"));var e=entry("T3","Sun");var r=index.search(sheet(e),"",false);assertTrue(r.hits().stream().noneMatch(h->h.ability().name().contains("MOON")));assertEquals(0,r.eligible());
        var base=sheet(e);var withBudget=new CharacterBook.Sheet(base.id(),base.name(),"",false,base.entries(),base.facts(),base.history(),new SlotBudget(5,0,Map.of(SlotBudget.key(e),2)));
        assertEquals(1,index.search(withBudget,"Sun Strike",false).eligible());
    }
    @Test void lesserGreaterLabelsRemainNamedSourceStages()throws Exception{
        var index=new SpellIndex(catalogue("<h3>Shape [Lesser]</h3><p>Basic rite.</p><h3>Warp [Greater]</h3><p>Greater rite.</p>"));var r=index.search(sheet(entry("Lesser","")),"",false);assertEquals(1,r.eligible());assertEquals("LESSER",r.hits().getFirst().ability().stage());assertTrue(index.search(sheet(entry("Lesser","")),"Warp",true).hits().stream().anyMatch(h->h.access()==SpellIndex.Access.ABOVE_TIER));
    }
    @Test void explicitUnlockTableSuppliesTierWithEvidence()throws Exception{
        var c=catalogue("<h2>Spells and Abilities</h2><h3>Deadbreath</h3><p>A spectral cloud.</p><h2>Tier Progression</h2><p>Tier One</p><p>Spells: Deadbreath</p>");var r=new SpellIndex(c).search(sheet(entry("T1","")),"Deadbreath",false);var a=r.hits().getFirst().ability();assertEquals(1,a.minimumTier());assertTrue(a.tierEvidence().contains("Spells: Deadbreath"));
    }
    @Test void manualNamesDoNotInventSourceLinks()throws Exception{
        var index=new SpellIndex(catalogue("<h3>Spark [T1]</h3><p>Text.</p>"));var e=new CharacterBook.Entry(UUID.randomUUID().toString(),CharacterBook.Kind.MAGIC,"Spark","","T5","",CharacterBook.RecordedStatus.APPROVED,"","");var r=index.search(sheet(e),"",false);assertTrue(r.hits().isEmpty());assertFalse(r.notes().isEmpty());
    }
    @Test void unicodeTierLabelsAndSearchAreUsable()throws Exception{
        assertEquals(4,SpellText.tier("Tɪᴇʀ IV"));assertEquals(3,SpellText.tier("Tier Three"));var index=new SpellIndex(catalogue("<h3>Æther Ward [Tɪᴇʀ II]</h3><p>Stops a synthetic ray.</p>"));assertEquals(1,index.search(sheet(entry("T2","")),"ÆＴＨＥＲ",false).hits().size());
    }
    @Test void resultsAndSourceDefinitionsAreImmutable()throws Exception{
        var index=new SpellIndex(catalogue("<h3>Spark [T1]</h3><p>Text.</p>"));assertThrows(UnsupportedOperationException.class,()->index.references("lotc:topic:123").clear());assertThrows(UnsupportedOperationException.class,()->index.search(sheet(entry("T1","")),"",false).hits().clear());
    }
    @Test void tierRangeRemainsIndexedAndCreditsAreNotSpells()throws Exception{
        var index=new SpellIndex(catalogue("<h2>Spells and Abilities</h2><h3>[Tier I-III] Weakening - [Non-Combative]</h3><p>Variants improve at later tiers.</p><h2>Purpose and Credits</h2><h3>Spell Advising</h3><p>People who helped.</p>"));
        var references=index.references("lotc:topic:123").stream().filter(a->!a.sourceReference()).toList();assertEquals(1,references.size());assertEquals(1,references.getFirst().minimumTier());assertTrue(references.getFirst().tierEvidence().contains("I-III"));
    }
    @Test void secondInvestmentAndPlainSpiritCurseRemainConditional()throws Exception{
        var index=new SpellIndex(catalogue("<h2>Abilities</h2><h3>Surge [+]</h3><p>Requires the source's investment and training.</p><h2>Spell Index</h2><p>Curse of Ember - Spirit of Fire, a synthetic example</p><p>Itching only.</p><p>Redlines:</p><p>Cannot burn the victim.</p>"));
        var result=index.search(sheet(entry("T5","")),"Curse of Ember",false);assertEquals(0,result.eligible());assertEquals("CURSE OF EMBER",result.hits().getFirst().ability().name());assertTrue(SpellIndex.details(result.hits().getFirst()).sections().stream().anyMatch(s->s.text().contains("Cannot burn the victim.")));assertTrue(index.references("lotc:topic:123").stream().anyMatch(a->a.name().equals("SURGE")&&a.extraInvestment()));
    }
}
