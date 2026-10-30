package net.mclorehub.character;

import org.junit.jupiter.api.*;
import java.util.*;
import static net.mclorehub.character.CharacterLore.*;
import static org.junit.jupiter.api.Assertions.*;

class PrerequisiteTest {
    CharacterLoreTest f;@BeforeEach void setup()throws Exception{f=new CharacterLoreTest();f.setup();}
    CharacterBook.Entry tier(String key,String tier)throws Exception{var e=f.entry(key);return new CharacterBook.Entry(e.id(),e.kind(),e.name(),"",tier,"",e.status(),"",e.loreDocumentId());}
    @Test void marriageReferenceKeepsBothRequiredPracticesExplicit()throws Exception {var r=f.lore.assess(f.sheet(f.entry("druidic-blood-rites")));assertTrue(r.findings().stream().anyMatch(v->v.message().contains("required references not recorded: Blood Magic")||v.message().contains("required references not recorded: Druidism")));assertEquals(0,f.count(r,Outcome.ELIGIBILITY_CONFLICT));}
    @Test void lesserBloodCannotUseMarriageAbilities()throws Exception {var r=f.lore.assess(f.sheet(f.entry("infernal-blood-rites"),f.entry("naztherak"),tier("blood-magic","Lesser")));assertTrue(r.findings().stream().anyMatch(v->v.outcome()==Outcome.ELIGIBILITY_CONFLICT&&v.message().contains("Greater Blood Mages")));}
    @Test void greaterBloodWithRecordedParentsDoesNotInventFailure()throws Exception {var r=f.lore.assess(f.sheet(f.entry("infernal-blood-rites"),f.entry("naztherak"),tier("blood-magic","Greater")));assertEquals(0,f.count(r,Outcome.ELIGIBILITY_CONFLICT));}
    @Test void herbloreTierAndSpriteAlternativeStaySeparate()throws Exception {var r=f.lore.assess(f.sheet(f.entry("herblore"),tier("druidism","T2")));assertTrue(r.findings().stream().anyMatch(v->v.outcome()==Outcome.ELIGIBILITY_CONFLICT&&v.message().contains("below required T3")));var sprite=f.lore.assess(f.sheet(f.entry("herblore"),f.entry("sprite")));assertEquals(0,f.count(sprite,Outcome.ELIGIBILITY_CONFLICT));assertTrue(sprite.findings().stream().anyMatch(v->v.message().contains("creature-specific alternative applies")));}
    @Test void missingTierIsNotAutomaticallyDenied()throws Exception {var r=f.lore.assess(f.sheet(f.entry("herblore"),f.entry("druidism")));assertEquals(0,f.count(r,Outcome.ELIGIBILITY_CONFLICT));assertTrue(r.findings().stream().anyMatch(v->v.message().contains("T3 is not fully recorded")));}
    @Test void maniSummoningRequiresBothTiers()throws Exception {var r=f.lore.assess(f.sheet(f.entry("mani-summoning"),tier("druidism","T5"),tier("blight-healing","T4")));assertTrue(r.findings().stream().anyMatch(v->v.outcome()==Outcome.ELIGIBILITY_CONFLICT&&v.message().contains("blight-healing tier is below required T5")));}
    @Test void currentVoidalInvestmentIsShownWithoutAnApprovalCertificate()throws Exception {var r=f.lore.assess(f.sheet(f.entry("voidstalking"),f.entry("water-evocation")));assertTrue(r.findings().stream().anyMatch(v->v.message().contains("requires 4 dedicated base Voidal slots")&&v.message().contains("not an acquisition approval")));}
    @Test void changedDependencySuspendsTierRulings()throws Exception {var lore=f.changed("lotc:topic:243150",d->f.change(d,d.body()+"changed",d.fingerprint(),d.status(),d.role()));var r=lore.assess(f.sheet(f.entry("herblore"),tier("druidism","T2")));assertTrue(r.findings().stream().noneMatch(v->v.message().contains("below required T3")));}
}
