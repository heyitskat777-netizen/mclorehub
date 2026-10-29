package net.mclorehub.updates;

import com.google.gson.*;
import net.mclorehub.catalogue.LoreCatalogue;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static net.mclorehub.importer.JsonSupport.*;

class BaselineTrustTest {
    @TempDir Path root;
    @Test void explicitConfirmationMakesSuppliedReferencesCurrentWithoutFabricatedReviews()throws Exception{
        var c=UpdateFixtures.install(root,true);assertEquals(2,c.currentDocuments().size());
        for(var d:c.documents()){assertEquals(LoreCatalogue.ReviewState.USER_TRUSTED,d.reviewState());assertTrue(d.decision().isEmpty());assertTrue(d.trust().isPresent());}
        assertEquals(0,c.reviewQueue().size());assertEquals(2,c.report().getAsJsonArray("current_documents").size());
    }
    @Test void noConfirmationStillLeavesUntrustedImportsUnknown()throws Exception{assertEquals(0,UpdateFixtures.install(root,false).currentDocuments().size());}
    @Test void trustIsBoundToIndividualRecordFingerprints()throws Exception{
        var c=UpdateFixtures.install(root,false);JsonObject trust=BaselineTrust.manifest(c,Instant.now());trust.getAsJsonObject("records").addProperty(c.documents().getFirst().id(),"a".repeat(64));
        Path path=root.resolve("trust.json");writeNew(path,trust);assertEquals(1,BaselineTrust.apply(c,path).currentDocuments().size());
    }
    @Test void anotherPackRevisionDoesNotInheritTrust()throws Exception{var c=UpdateFixtures.install(root,false);JsonObject trust=BaselineTrust.manifest(c,Instant.now());
        trust.addProperty("pack_revision","sha256:"+"b".repeat(64));Path path=root.resolve("trust.json");writeNew(path,trust);assertEquals(0,BaselineTrust.apply(c,path).currentDocuments().size());}
    @Test void malformedTrustFailsWithoutChangingPack()throws Exception{var c=UpdateFixtures.install(root,false);byte[] before=Files.readAllBytes(root.resolve("packs/active/pack.json"));
        JsonObject trust=BaselineTrust.manifest(c,Instant.now().plusSeconds(1000));Path path=root.resolve("trust.json");writeNew(path,trust);
        assertThrows(java.io.IOException.class,()->BaselineTrust.apply(c,path));assertArrayEquals(before,Files.readAllBytes(root.resolve("packs/active/pack.json")));}
    @Test void explicitBoundShelvingOverrideTakesPrecedenceOverBaselineDefault()throws Exception{
        var c=UpdateFixtures.install(root,true);var d=c.documents().getFirst();JsonObject decision=new JsonObject();decision.addProperty("document_id",d.id());decision.addProperty("revision_sha256",d.revision());
        decision.addProperty("review_fingerprint",d.fingerprint());decision.addProperty("status","shelved");decision.addProperty("reviewer","Synthetic user");decision.addProperty("reviewed_at","2026-10-20T00:00:00Z");
        JsonObject evidence=new JsonObject();evidence.addProperty("source_url",d.url());evidence.addProperty("passage","Synthetic shelving override, not actual lore");JsonArray citations=new JsonArray();citations.add(evidence);decision.add("evidence",citations);
        JsonArray decisions=new JsonArray();decisions.add(decision);JsonObject reviews=new JsonObject();reviews.addProperty("schema_version",1);reviews.add("decisions",decisions);Path path=root.resolve("reviews.json");writeNew(path,reviews);
        var loaded=BaselineTrust.apply(LoreCatalogue.load(root.resolve("packs/active/pack.json"),path),root.resolve("packs/active/trust.json"));
        assertEquals(LoreCatalogue.Status.SHELVED,loaded.documents().getFirst().status());assertEquals(1,loaded.currentDocuments().size());}
}
