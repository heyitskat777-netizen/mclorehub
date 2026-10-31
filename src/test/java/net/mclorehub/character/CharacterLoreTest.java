package net.mclorehub.character;

import com.google.gson.*;
import net.mclorehub.catalogue.LoreCatalogue.*;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import java.util.function.UnaryOperator;
import static net.mclorehub.importer.JsonSupport.*;
import static org.junit.jupiter.api.Assertions.*;
import static net.mclorehub.character.CharacterLore.*;

class CharacterLoreTest {
    JsonObject manifest;List<Document> docs;CharacterLore lore;
    @BeforeEach void setup()throws Exception {
        try(var in=CharacterLore.class.getResourceAsStream("lore-rules.json")){manifest=parse(bounded(in,256<<10));}
        docs=new ArrayList<>();
        for(var value:manifest.getAsJsonArray("sources")) {
            var s=value.getAsJsonObject();String body=String.join("\n",strings(s,"passages"))+"\nSynthetic fixture, not a complete source.";
            s.addProperty("revision",hash(body));
            var doc=new Document(text(s,"id"),text(s,"title"),text(s,"url"),Optional.empty(),Instant.parse(text(s,"snapshot")),body,text(s,"revision"),hash("synthetic:"+text(s,"id")),
                    "synthetic","synthetic",List.of("TEST ALIAS"),Role.PLAYER_OPTION_CANDIDATE,Status.ACCEPTED_CURRENT,ReviewState.USER_TRUSTED,Optional.empty(),List.of(),List.of(),Optional.of(new SourceTrust("Synthetic baseline trust",Instant.parse(text(s,"snapshot")),List.of(text(s,"url")))));
            s.addProperty("main_fingerprint",mainFingerprint(doc));docs.add(doc);
        }
        lore=CharacterLore.parse(manifest,docs);
    }
    CharacterBook.Entry entry(String key)throws Exception {
        var option=manifest.getAsJsonArray("options").asList().stream().map(JsonElement::getAsJsonObject).filter(o->o.get("key").getAsString().equals(key)).findFirst().orElseThrow();
        var e=new CharacterBook.Entry(UUID.randomUUID().toString(),CharacterBook.Kind.valueOf(text(option,"kind")),"Custom label","","","",CharacterBook.RecordedStatus.NOT_RECORDED,"private notes","");
        return lore.link(e,key);
    }
    CharacterBook.Entry branch(CharacterBook.Entry e,String branch){return new CharacterBook.Entry(e.id(),e.kind(),e.name(),branch,"T3",e.applicationUrl(),e.status(),e.notes(),e.loreDocumentId());}
    CharacterBook.Sheet sheet(CharacterBook.Entry... entries){return new CharacterBook.Sheet(UUID.randomUUID().toString(),"Fixture","private notes",false,List.of(entries));}
    long count(Report r,Outcome outcome){return r.findings().stream().filter(f->f.outcome()==outcome).count();}
    Document change(Document d,String body,String fingerprint,Status status,Role role){return new Document(d.id(),d.title(),d.url(),d.author(),d.exportSnapshotAt(),body,hash(body),fingerprint,d.family(),d.category(),d.aliases(),role,status,d.reviewState(),d.decision(),d.posts(),d.coverageGaps(),d.trust());}
    CharacterLore changed(String id,UnaryOperator<Document> change)throws Exception{return CharacterLore.parse(manifest,docs.stream().map(d->d.id().equals(id)?change.apply(d):d).toList());}
    @Test void curatedKindsAreExplicitAndOnlySelectedTopicsAreOffered(){
        assertEquals(31,lore.choices(CharacterBook.Kind.MAGIC,"").size());assertEquals(21,lore.choices(CharacterBook.Kind.FA,"").size());assertEquals(21,lore.choices(CharacterBook.Kind.CA,"").size());
        assertTrue(Arrays.stream(CharacterBook.Kind.values()).flatMap(k->lore.choices(k,"").stream()).allMatch(c->c.availability()==Availability.AVAILABLE));
    }
    @Test void searchUsesUnicodeCaseAliasesAndAllTerms(){assertEquals(1,lore.choices(CharacterBook.Kind.MAGIC,"ＲＡＤＩＡＮＴ　templar").size());assertEquals(31,lore.choices(CharacterBook.Kind.MAGIC,"test alias").size());assertTrue(lore.choices(CharacterBook.Kind.CA,"artifact").isEmpty());}
    @Test void kloneCaIsSeparateFromTawkinFeatAndSuspendsWithItsOwnSource()throws Exception {
        var choice=lore.choices(CharacterBook.Kind.CA,"Klones").getFirst();assertEquals("klone",choice.option().key());
        assertEquals("lotc:topic:198584",choice.option().sourceId());assertEquals(Availability.AVAILABLE,choice.availability());
        assertTrue(choice.source().passages().stream().anyMatch(p->p.contains("Klones require a valid Creature Application")));
        var ca=entry("klone");var feat=entry("tawkin");assertEquals(CharacterBook.Kind.CA,ca.kind());assertEquals(CharacterBook.Kind.FA,feat.kind());
        assertEquals(ca.loreDocumentId(),feat.loreDocumentId());assertEquals(-1,lore.slotCost(ca).maximum());
        assertEquals(0,lore.slots(sheet(ca)).minimum());
        var stale=changed(ca.loreDocumentId(),d->change(d,d.body()+" changed",hash("changed"),d.status(),d.role()));
        assertEquals(Availability.NEEDS_REVIEW,stale.choices(CharacterBook.Kind.CA,"Klones").getFirst().availability());
        assertEquals(Availability.AVAILABLE,stale.choices(CharacterBook.Kind.MAGIC,"Kani").getFirst().availability());
    }
    @Test void necromancyTemplarConflictIsSymmetricAndCited()throws Exception {
        var a=entry("necromancy");var b=entry("templar");var r=lore.assess(sheet(a,b));var reversed=lore.assess(sheet(b,a));
        assertEquals(1,count(r,Outcome.EXCLUSION));assertEquals(1,count(reversed,Outcome.EXCLUSION));assertEquals(1,r.checkedPairs());
        assertEquals(2,r.findings().getFirst().sources().size());assertTrue(r.findings().getFirst().sources().stream().allMatch(s->!s.passages().isEmpty()));
    }
    @Test void renamedEntriesMatchStableReferencesNotDisplayNames()throws Exception {
        var e=entry("necromancy");var renamed=new CharacterBook.Entry(e.id(),e.kind(),"My renamed magic","","","",e.status(),"",e.loreDocumentId());
        assertEquals(1,count(lore.assess(sheet(renamed,entry("templar"))),Outcome.EXCLUSION));
    }
    @Test void seerNecromancyExceptionDoesNotBecomeFalseExclusionOrCompatibility()throws Exception {
        var r=lore.assess(sheet(entry("seer-magic"),entry("necromancy")));assertEquals(0,count(r,Outcome.EXCLUSION));assertEquals(0,r.checkedPairs());assertTrue(r.findings().stream().anyMatch(f->f.message().contains("Palmreading")));
    }
    @Test void bothSeerKindsConflictWithTreeLord()throws Exception {
        assertEquals(1,count(lore.assess(sheet(entry("seer-magic"),entry("tree-lord"))),Outcome.EXCLUSION));assertEquals(1,count(lore.assess(sheet(entry("seer-feat"),entry("tree-lord"))),Outcome.EXCLUSION));
    }
    @Test void conflictingSourceStatementsAreReportedWithoutPickingAWinner()throws Exception {
        var r=lore.assess(sheet(entry("seer-magic"),entry("templar")));assertEquals(1,count(r,Outcome.SOURCE_CONFLICT));assertEquals(0,count(r,Outcome.EXCLUSION));assertEquals(2,r.findings().getFirst().sources().size());
    }
    @Test void simultaneousKaniPathsConflictWithSwitchingExceptionPreserved()throws Exception {
        var r=lore.assess(sheet(branch(entry("kani"),"SUN"),branch(entry("kani"),"Path of the Moon")));assertEquals(1,count(r,Outcome.EXCLUSION));assertTrue(r.findings().getFirst().message().contains("once per OOC month"));
    }
    @Test void sameOrUnrecognizedKaniPathsNeverInventConflict()throws Exception {
        assertEquals(0,count(lore.assess(sheet(branch(entry("kani"),"Sun"),branch(entry("kani"),"Sun"))),Outcome.EXCLUSION));
        var r=lore.assess(sheet(branch(entry("kani"),"Sun/Moon"),entry("kani")));assertEquals(0,count(r,Outcome.EXCLUSION));assertEquals(0,r.checkedPairs());
    }
    @Test void manualNameNeverCreatesALoreLink()throws Exception {
        var e=new CharacterBook.Entry(UUID.randomUUID().toString(),CharacterBook.Kind.MAGIC,"Necromancy","","","",CharacterBook.RecordedStatus.APPROVED,"","");
        var r=lore.assess(sheet(e,entry("templar")));assertEquals(0,count(r,Outcome.EXCLUSION));assertEquals(1,r.unresolvedEntries());assertEquals(0,r.checkedPairs());
    }
    @Test void changedMainPostSuspendsRulesButKeepsCurrentReferenceWithoutChangingSheet()throws Exception {
        var s=sheet(entry("necromancy"),entry("templar"));var before=s.toString();var updated=changed("lotc:topic:249533",d->change(d,d.body()+"changed",d.fingerprint(),d.status(),d.role()));
        assertEquals(Availability.NEEDS_REVIEW,updated.availability("lotc:topic:249533"));assertTrue(updated.canReference(updated.choices(CharacterBook.Kind.MAGIC,"Necromancy").getFirst()));var r=updated.assess(s);assertEquals(0,count(r,Outcome.EXCLUSION));assertEquals(1,count(r,Outcome.NEEDS_REVIEW));assertEquals(before,s.toString());assertEquals(0,r.checkedPairs());
    }
    @Test void changedAuthorMetadataAlsoSuspendsChecks()throws Exception {
        var updated=changed("lotc:topic:249533",d->new Document(d.id(),d.title(),d.url(),Optional.of("Different author"),d.exportSnapshotAt(),d.body(),d.revision(),d.fingerprint(),d.family(),d.category(),d.aliases(),d.role(),d.status(),d.reviewState(),d.decision(),d.posts(),d.coverageGaps(),d.trust()));
        assertEquals(Availability.NEEDS_REVIEW,updated.availability("lotc:topic:249533"));
    }
    @Test void noChangeRefreshAndArchiveOnlyChangesKeepMainBinding()throws Exception {
        var updated=changed("lotc:topic:249533",d->new net.mclorehub.updates.TopicSnapshot(d.id(),d.url(),d.title(),d.author().orElse(""),d.body(),List.of(new ThreadPost("123","Discussion author",d.url(),"Changed archived discussion")),List.of(d.url()),Instant.now(),Optional.empty(),List.of()).apply(d));
        assertEquals(Availability.AVAILABLE,updated.availability("lotc:topic:249533"));assertEquals(1,count(updated.assess(sheet(entry("necromancy"),entry("templar"))),Outcome.EXCLUSION));
    }
    @Test void missingOrShelvedSourceNeverChecksOldRule()throws Exception {
        var missing=CharacterLore.parse(manifest,docs.stream().filter(d->!d.id().equals("lotc:topic:249533")).toList());assertEquals(Availability.MISSING,missing.availability("lotc:topic:249533"));
        var shelved=changed("lotc:topic:249533",d->change(d,d.body(),d.fingerprint(),Status.SHELVED,d.role()));assertEquals(Availability.NOT_CURRENT,shelved.availability("lotc:topic:249533"));
        assertFalse(missing.canReference(missing.choices(CharacterBook.Kind.MAGIC,"Necromancy").getFirst()));assertFalse(shelved.canReference(shelved.choices(CharacterBook.Kind.MAGIC,"Necromancy").getFirst()));
        assertEquals(0,count(shelved.assess(sheet(entry("necromancy"),entry("templar"))),Outcome.EXCLUSION));
    }
    @Test void backgroundHubAndArtifactsCannotBePromotedToPlayerOptions()throws Exception {
        for(var role:List.of(Role.NAVIGATION_HUB,Role.BACKGROUND_CONTEXT,Role.CHARACTER_ARTIFACT,Role.MAGICAL_ARTIFACT,Role.DISCONNECTION_NOTICE)){
            assertEquals(Availability.NEEDS_REVIEW,changed("lotc:topic:249533",d->change(d,d.body(),d.fingerprint(),d.status(),role)).availability("lotc:topic:249533"));}
    }
    @Test void exactQuoteMustExistInMainPostNotOnlyReplies()throws Exception {
        var source=manifest.getAsJsonArray("sources").get(1).getAsJsonObject();source.getAsJsonArray("passages").add("This passage is absent from the main post");
        assertEquals(Availability.NEEDS_REVIEW,CharacterLore.parse(manifest,docs).availability("lotc:topic:249533"));
    }
    @Test void linkPreservesApplicationAndPersonalFields()throws Exception {
        var e=new CharacterBook.Entry(UUID.randomUUID().toString(),CharacterBook.Kind.MAGIC,"My label","Moon","T4","https://www.lordofthecraft.net/forums/topic/123-application/",CharacterBook.RecordedStatus.PENDING,"private","");
        var linked=lore.link(e,"kani");assertEquals(e.id(),linked.id());assertEquals(e.applicationUrl(),linked.applicationUrl());assertEquals(e.notes(),linked.notes());assertEquals(e.status(),linked.status());assertEquals("Moon",linked.branch());assertEquals("T4",linked.tier());
    }
    @Test void wrongKindAndStaleSelectionAreRejected()throws Exception {
        assertThrows(IllegalArgumentException.class,()->lore.link(entry("alchemy"),"necromancy"));
        var stale=changed("lotc:topic:249533",d->change(d,d.body()+"new",d.fingerprint(),d.status(),d.role()));assertThrows(IllegalArgumentException.class,()->stale.link(entry("necromancy"),"necromancy"));
    }
    @Test void noWarningsIsNeverACompatibilityCertificate()throws Exception {
        var r=lore.assess(sheet(entry("air-evocation"),entry("alchemy")));assertEquals(0,count(r,Outcome.EXCLUSION));assertEquals(0,r.checkedPairs());assertTrue(r.summary().contains("partial coverage"));assertTrue(r.findings().stream().anyMatch(f->f.message().contains("Zero warnings does not establish compatibility")));
    }
    @Test void pendingAndDeniedRecordsRemainRecordedPlansNotApproval()throws Exception {
        var a=entry("necromancy");var denied=new CharacterBook.Entry(a.id(),a.kind(),a.name(),"","","",CharacterBook.RecordedStatus.DENIED,"",a.loreDocumentId());
        assertEquals(1,count(lore.assess(sheet(denied,entry("templar"))),Outcome.EXCLUSION));
    }
    @Test void invalidManifestDependenciesAndDuplicateBindingsAreRejected(){
        var invalid=manifest.deepCopy();invalid.getAsJsonArray("options").add(invalid.getAsJsonArray("options").get(0));assertThrows(java.io.IOException.class,()->CharacterLore.parse(invalid,docs));
        var invalidRule=manifest.deepCopy();invalidRule.getAsJsonArray("rules").get(0).getAsJsonObject().getAsJsonArray("sources").remove(0);assertThrows(java.io.IOException.class,()->CharacterLore.parse(invalidRule,docs));
    }
    @Test void snapshotsAndFindingsAreImmutable()throws Exception {
        var r=lore.assess(sheet(entry("necromancy"),entry("templar")));assertThrows(UnsupportedOperationException.class,()->r.findings().clear());assertThrows(UnsupportedOperationException.class,()->r.findings().getFirst().sources().clear());assertThrows(UnsupportedOperationException.class,()->lore.rules().clear());
    }
    @Test void emptyAndHundredEntrySheetsHaveBoundedHonestCoverage()throws Exception {
        assertEquals(0,lore.assess(sheet()).totalPairs());List<CharacterBook.Entry> entries=new ArrayList<>();for(int i=0;i<100;i++)entries.add(entry("air-evocation"));var r=lore.assess(new CharacterBook.Sheet(UUID.randomUUID().toString(),"max","",false,entries));assertEquals(4950,r.totalPairs());assertEquals(0,r.checkedPairs());assertEquals(3,r.findings().size());
    }
    @Test void expandedVoidalAndDarkExclusionsAreSpecificToClericism()throws Exception {
        for(String key:List.of("air-evocation","earth-evocation","fire-evocation","water-evocation","sensory-illusion","transfiguration","voidal-translocation","blood-magic","mysticism","necromancy")) {
            var r=lore.assess(sheet(entry("clericism"),entry(key)));assertEquals(1,count(r,Outcome.EXCLUSION),key);assertEquals(1,r.checkedPairs(),key);
        }
    }
    @Test void templarRestrictionsCoverNamedDarkDeificAndTreeLordOptions()throws Exception {
        for(String key:List.of("necromancy","blood-magic","mysticism","clericism","tree-lord","mori-quessir"))assertEquals(1,count(lore.assess(sheet(entry("templar"),entry(key))),Outcome.EXCLUSION),key);
    }
    @Test void namedCreaturesHaveTheirOwnEvidence()throws Exception {
        for(String key:List.of("nephilim","sprite","mori-quessir")){
            var r=lore.assess(sheet(entry("clericism"),entry(key)));assertEquals(1,count(r,Outcome.EXCLUSION),key);assertEquals(2,r.findings().getFirst().sources().size());
        }
        assertEquals(1,count(lore.assess(sheet(entry("mysticism"),entry("nephilim"))),Outcome.EXCLUSION));
    }
    @Test void clericSeerInternalDisagreementIsNotAnExclusion()throws Exception {
        for(String key:List.of("seer-magic","seer-feat")){
            var r=lore.assess(sheet(entry("clericism"),entry(key)));assertEquals(1,count(r,Outcome.SOURCE_CONFLICT));assertEquals(0,count(r,Outcome.EXCLUSION));
            assertTrue(r.findings().getFirst().message().contains("specifically permits Seers"));
        }
    }
    @Test void spriteHerbloreProvisionDoesNotRequireAnInventedDruidEntry()throws Exception {
        var r=lore.assess(sheet(entry("sprite"),entry("herblore")));assertEquals(0,count(r,Outcome.EXCLUSION));assertEquals(1,r.checkedPairs());
        var context=r.findings().stream().filter(f->f.message().contains("creature-specific provision")).findFirst().orElseThrow();assertEquals(Outcome.REQUIREMENTS,context.outcome());assertEquals(2,context.sources().size());
    }
    @Test void shapeshiftingNoticeRestrictsCastingNotPossession()throws Exception {
        var r=lore.assess(sheet(entry("seer-magic"),entry("druidic-shapeshifting")));assertEquals(0,count(r,Outcome.EXCLUSION));assertEquals(1,r.checkedPairs());
        assertTrue(r.findings().stream().anyMatch(f->f.outcome()==Outcome.REQUIREMENTS&&f.message().contains("While in animal form")&&f.message().contains("does not prohibit holding both")));
    }
    @Test void unrelatedSourceChangeOnlySuspendsDependentChecks()throws Exception {
        var updated=changed("lotc:topic:249435",d->change(d,d.body()+"changed",d.fingerprint(),d.status(),d.role()));
        assertEquals(0,count(updated.assess(sheet(entry("clericism"),entry("fire-evocation"))),Outcome.EXCLUSION));
        assertEquals(1,count(updated.assess(sheet(entry("clericism"),entry("water-evocation"))),Outcome.EXCLUSION));
        assertEquals(1,count(updated.assess(sheet(entry("templar"),entry("blood-magic"))),Outcome.EXCLUSION));
    }
    @Test void conditionalNoticeSuspendsWhenEitherSourceChanges()throws Exception {
        for(String id:List.of("lotc:topic:242890","lotc:topic:223449")){
            var updated=changed(id,d->change(d,d.body()+"changed",d.fingerprint(),d.status(),d.role()));var r=updated.assess(sheet(entry("sprite"),entry("herblore")));
            assertEquals(0,r.checkedPairs());assertTrue(r.findings().stream().noneMatch(f->f.message().contains("creature-specific provision")));assertEquals(1,count(r,Outcome.NEEDS_REVIEW));
        }
    }
    @Test void familyWideInferencesDoNotExcludeVoidalDarkOrMiscPairs()throws Exception {
        for(String[] pair:new String[][]{{"mysticism","fire-evocation"},{"mysticism","blood-magic"},{"kani","blood-magic"},{"templar","housemagery"},{"clericism","housemagery"},{"sprite","blood-magic"}}){
            var r=lore.assess(sheet(entry(pair[0]),entry(pair[1])));assertEquals(0,count(r,Outcome.EXCLUSION),Arrays.toString(pair));assertEquals(0,r.checkedPairs());
        }
    }
    @Test void apostropheVariantsAndSourceTitleAreSearchable(){
        assertEquals(1,lore.choices(CharacterBook.Kind.CA,"Mori’Quessir").size());assertEquals(1,lore.choices(CharacterBook.Kind.CA,"Mori'Quessir").size());
        assertEquals(1,lore.choices(CharacterBook.Kind.MAGIC,"Tahariae").size());assertTrue(lore.choices(CharacterBook.Kind.FA,"Tahariae").isEmpty());
    }
    CharacterBook.Sheet facts(CharacterBook.Sheet s,CharacterFacts f){return new CharacterBook.Sheet(s.id(),s.name(),s.notes(),s.archived(),s.entries(),f,s.history());}
    @Test void unknownFactsRemainUnknownWithoutNewConflicts()throws Exception {
        var r=lore.assess(sheet(entry("kani")));assertEquals(0,count(r,Outcome.ELIGIBILITY_CONFLICT));assertTrue(r.findings().stream().anyMatch(f->f.message().contains("Facts missing:")));assertEquals(0,r.checkedPairs());
    }
    @Test void kaniChecksBreathingLungsAndMinimumSoul()throws Exception {
        var f=CharacterFacts.unknown().set(CharacterFacts.Field.BREATHING,"NO").set(CharacterFacts.Field.LUNGS,"NO").set(CharacterFacts.Field.SOUL,"NONE");var r=lore.assess(facts(sheet(entry("kani")),f));assertEquals(1,count(r,Outcome.ELIGIBILITY_CONFLICT));assertTrue(r.findings().getFirst().message().contains("at least a lesser soul"));
    }
    @Test void kaniModifiedSoulKeepsKloneExceptionAndUnknownDistinct()throws Exception {
        var s=sheet(entry("kani"));var f=CharacterFacts.unknown().set(CharacterFacts.Field.SOUL,"LESSER").set(CharacterFacts.Field.LUNGS,"YES").set(CharacterFacts.Field.BREATHING,"YES").set(CharacterFacts.Field.MODIFIED_SOUL,"YES");
        assertEquals(0,count(lore.assess(facts(s,f)),Outcome.ELIGIBILITY_CONFLICT));assertTrue(lore.assess(facts(s,f)).findings().stream().anyMatch(v->v.message().contains("whether the Klone exception applies")));
        assertEquals(1,count(lore.assess(facts(s,f.set(CharacterFacts.Field.KLONE,"NO"))),Outcome.ELIGIBILITY_CONFLICT));assertEquals(0,count(lore.assess(facts(s,f.set(CharacterFacts.Field.KLONE,"YES"))),Outcome.ELIGIBILITY_CONFLICT));
    }
    @Test void kloneExceptionDoesNotSupplyMissingLungs()throws Exception {
        var f=CharacterFacts.unknown().set(CharacterFacts.Field.MODIFIED_SOUL,"YES").set(CharacterFacts.Field.KLONE,"YES").set(CharacterFacts.Field.LUNGS,"NO");assertEquals(1,count(lore.assess(facts(sheet(entry("kani")),f)),Outcome.ELIGIBILITY_CONFLICT));
    }
    @Test void seerChecksCurrentBodyBlindnessForBothKinds()throws Exception {
        for(String key:List.of("seer-feat","seer-magic")){var s=sheet(entry(key));assertEquals(1,count(lore.assess(facts(s,CharacterFacts.unknown().set(CharacterFacts.Field.BLIND_BODY,"NO"))),Outcome.ELIGIBILITY_CONFLICT));assertEquals(0,count(lore.assess(facts(s,CharacterFacts.unknown().set(CharacterFacts.Field.BLIND_BODY,"YES"))),Outcome.ELIGIBILITY_CONFLICT));}
    }
    @Test void clericNeedsGreaterSoulAndExcludesKlones()throws Exception {
        var s=sheet(entry("clericism"));for(String soul:List.of("LESSER","NONE"))assertEquals(1,count(lore.assess(facts(s,CharacterFacts.unknown().set(CharacterFacts.Field.SOUL,soul))),Outcome.ELIGIBILITY_CONFLICT));
        assertEquals(1,count(lore.assess(facts(s,CharacterFacts.unknown().set(CharacterFacts.Field.SOUL,"GREATER").set(CharacterFacts.Field.KLONE,"YES"))),Outcome.ELIGIBILITY_CONFLICT));
    }
    @Test void clericScornAndDisconnectionIsExplicitCombinedFact()throws Exception {
        var r=lore.assess(facts(sheet(entry("clericism")),CharacterFacts.unknown().set(CharacterFacts.Field.HOLY_HISTORY,"YES")));assertEquals(1,count(r,Outcome.ELIGIBILITY_CONFLICT));assertTrue(r.findings().getFirst().message().contains("specific combined condition"));
    }
    @Test void templarRaceAndFourthAcquisitionAreSeparateChecks()throws Exception {
        var s=sheet(entry("templar"));var f=CharacterFacts.unknown().set(CharacterFacts.Field.DESCENDANT_RACE,"NO").set(CharacterFacts.Field.TEMPLAR_SEVERANCES,"THREE_OR_MORE");var r=lore.assess(facts(s,f));assertEquals(2,count(r,Outcome.ELIGIBILITY_CONFLICT));assertTrue(r.findings().stream().anyMatch(v->v.message().contains("further acquisition")));
        assertEquals(0,count(lore.assess(facts(s,f.set(CharacterFacts.Field.DESCENDANT_RACE,"YES").set(CharacterFacts.Field.TEMPLAR_SEVERANCES,"TWO"))),Outcome.ELIGIBILITY_CONFLICT));
    }
    @Test void changedSourceSuspendsFactChecks()throws Exception {
        var changed=changed("lotc:topic:228968",d->change(d,d.body()+"changed",d.fingerprint(),d.status(),d.role()));var s=facts(sheet(entry("kani")),CharacterFacts.unknown().set(CharacterFacts.Field.BREATHING,"NO"));assertEquals(0,count(changed.assess(s),Outcome.ELIGIBILITY_CONFLICT));assertEquals(1,count(changed.assess(s),Outcome.NEEDS_REVIEW));
    }
    @Test void disconnectedNecromancyHistorySurvivesWithoutActiveEntry()throws Exception {
        var h=new CharacterBook.HistoryEvent(UUID.randomUUID().toString(),"Former magic","lotc:topic:249533",CharacterBook.EventType.DISCONNECTED,"","History only");var s=new CharacterBook.Sheet(UUID.randomUUID().toString(),"Test","",false,List.of(),CharacterFacts.unknown(),List.of(h));var r=lore.assess(s);assertTrue(r.findings().stream().anyMatch(v->v.message().contains("dedicated slots are permanently lost")));assertEquals(0,r.totalPairs());
        var changed=changed("lotc:topic:249533",d->change(d,d.body()+"changed",d.fingerprint(),d.status(),d.role()));assertTrue(changed.assess(s).findings().stream().noneMatch(v->v.message().contains("dedicated slots are permanently lost")));assertEquals(1,count(changed.assess(s),Outcome.NEEDS_REVIEW));
    }
    @Test void partialHistoryDoesNotChangeFactsOrPretendToBeComplete()throws Exception {
        var b=CharacterBook.empty().create("Test");for(int i=0;i<3;i++)b=b.putHistory(b.selectedId(),new CharacterBook.HistoryEvent(UUID.randomUUID().toString(),"Templar severance","lotc:topic:228224",CharacterBook.EventType.DISCONNECTED,"",""));b=b.putEntry(b.selectedId(),entry("templar"));var s=b.selected().orElseThrow();assertEquals(CharacterFacts.Severances.UNKNOWN,s.facts().templarSeverances());assertEquals(0,count(lore.assess(s),Outcome.ELIGIBILITY_CONFLICT));
    }
    @Test void invalidEligibilityDependencyIsRejected(){var bad=manifest.deepCopy();bad.getAsJsonArray("eligibility").get(0).getAsJsonObject().getAsJsonArray("sources").remove(0);assertThrows(java.io.IOException.class,()->CharacterLore.parse(bad,docs));}
}
