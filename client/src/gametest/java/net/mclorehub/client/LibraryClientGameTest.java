package net.mclorehub.client;

import com.google.gson.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.mclorehub.catalogue.LoreCatalogue;
import net.mclorehub.importer.ExportImporter;
import net.mclorehub.library.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.TitleScreen;
import org.lwjgl.glfw.GLFW;
import org.slf4j.LoggerFactory;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import static net.mclorehub.importer.JsonSupport.*;

/** Real Minecraft UI and restart tests. Every approval in this fixture is synthetic. */
@SuppressWarnings("UnstableApiUsage")
public final class LibraryClientGameTest implements FabricClientGameTest {
    private static final String TITLE="Synthetic reference — not server canon";
    private static final String UNKNOWN="Unreviewed fixture — not server canon";
    @Override public void runTest(ClientGameTestContext context) {
        try { scenario(context); } catch(Exception e) { throw new AssertionError("Library scenario failed",e); }
    }
    private void scenario(ClientGameTestContext context) throws Exception {
        context.waitForScreen(TitleScreen.class);Path root=Files.createTempDirectory("mclorehub-ui-fixture-");fixture(root);
        LibraryService library=new LibraryService(root);
        int scale=context.computeOnClient(c->c.options.guiScale().get());
        try(TestSingleplayerContext world=context.worldBuilder().create()){
            context.waitFor(c->c.player!=null&&c.screen==null);world.getClientWorld().waitForChunksRender();
            context.runOnClient(c->{c.options.guiScale().set(2);c.resizeDisplay();});
            library.reload().get(10,TimeUnit.SECONDS);final LibraryService first=library;
            context.setScreen(()->new WikiScreen(null,first));
            context.waitFor(c->c.screen instanceof WikiScreen && c.screen.getNarrationMessage().getString().contains("2 results"));
            context.runOnClient(c->{check(c.screen.getNarrationMessage().getString().contains("2 current / 3 imported"),"Coverage missing");
                check(!has(c.screen,UNKNOWN),"Unreviewed result leaked into normal browsing");bounds(c.screen);});
            context.takeScreenshot("phase4-current-browser");
            context.runOnClient(c->{var box=c.screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow();c.screen.setFocused(box);box.setFocused(true);});
            context.getInput().typeChars("reference alias");
            context.waitFor(c->c.screen.getNarrationMessage().getString().contains("1 results"));
            context.clickScreenButton(TITLE);context.waitForScreen(ReaderScreen.class);
            context.waitFor(c->c.screen.getNarrationMessage().getString().contains("Line 0"));
            context.getInput().pressKey(GLFW.GLFW_KEY_PAGE_DOWN);
            context.waitFor(c->first.view().personal().reading().map(p->p.offset()>0).orElse(false));
            context.clickScreenButton("screen.mclorehub.favorite");context.waitFor(c->first.view().personal().favorites().contains("lotc:topic:123"));
            context.takeScreenshot("phase4-reader-scale-2");
            context.clickScreenButton("screen.mclorehub.details");context.waitFor(c->c.screen.getNarrationMessage().getString().contains("Source:"));
            context.takeScreenshot("phase4-source-details");context.clickScreenButton("screen.mclorehub.archived_replies");
            context.waitFor(c->c.screen.getNarrationMessage().getString().contains("starlight"));
            context.runOnClient(c->check(first.view().personal().reading().orElseThrow().blockId().equals("body"),"Archived discussion replaced main reading position"));
            context.takeScreenshot("phase7-archived-reader");context.clickScreenButton("gui.back");context.clickScreenButton("gui.back");
            context.clickScreenButton("screen.mclorehub.next");context.getInput().pressKey(GLFW.GLFW_KEY_END);
            context.waitFor(c->first.view().personal().reading().map(p->p.offset()>6000).orElse(false));
            context.clickScreenButton("gui.back");context.waitForScreen(WikiScreen.class);
            context.clickScreenButton("screen.mclorehub.favorites_off");context.waitFor(c->c.screen.getNarrationMessage().getString().contains("1 results"));
            context.clickScreenButton("screen.mclorehub.favorites_on");context.clickScreenButton("screen.mclorehub.audit_off");
            context.runOnClient(c->edit(c.screen).setValue("unreviewed fixture"));
            context.waitFor(c->has(c.screen,UNKNOWN));context.clickScreenButton(UNKNOWN);context.waitForScreen(ReaderScreen.class);
            context.runOnClient(c->check(c.screen.getNarrationMessage().getString().contains("AUDIT • Unknown • Not reviewed"),"Audit reader must label unknown status"));
            context.takeScreenshot("phase4-audit-reader");context.clickScreenButton("gui.back");context.waitForScreen(WikiScreen.class);
            context.runOnClient(c->edit(c.screen).setValue("reference alias"));context.waitFor(c->has(c.screen,TITLE));context.clickScreenButton(TITLE);
            context.waitFor(c->c.screen.getNarrationMessage().getString().contains("Line 0"));
            context.clickScreenButton("screen.mclorehub.next");context.getInput().pressKey(GLFW.GLFW_KEY_END);
            context.waitFor(c->first.view().personal().reading().map(p->p.offset()>6000).orElse(false));
            context.setScreen(TitleScreen::new);first.close();
            library=new LibraryService(root);final LibraryService restarted=library;restarted.reload().get(10,TimeUnit.SECONDS);
            check(restarted.view().personal().favorites().contains("lotc:topic:123"),"Favorite lost after service restart");
            check(restarted.view().personal().reading().orElseThrow().blockId().equals("body")&&restarted.view().personal().reading().orElseThrow().offset()>6000,"Main lore position lost after service restart");
            context.setScreen(()->new WikiScreen(null,restarted));context.clickScreenButton("screen.mclorehub.resume");
            context.waitFor(c->c.screen instanceof ReaderScreen&&c.screen.getNarrationMessage().getString().contains("Line"));
            context.runOnClient(c->{c.options.guiScale().set(1);c.resizeDisplay();bounds(c.screen);});
            context.takeScreenshot("phase4-reader-scale-1");context.clickScreenButton("gui.back");context.waitForScreen(WikiScreen.class);
            Files.writeString(root.resolve("packs/active/pack.json"),"invalid pack");context.clickScreenButton("screen.mclorehub.reload");
            context.waitFor(c->restarted.view().phase()==LibraryService.Phase.ERROR);
            check(restarted.view().library().orElseThrow().catalogue().documents().size()==3,"Reload lost last valid library");
            context.takeScreenshot("phase4-failed-reload");context.setScreen(TitleScreen::new);
        }finally{library.close();context.runOnClient(c->{c.options.guiScale().set(scale);c.resizeDisplay();});}
        String realPack=System.getenv("MCLOREHUB_TEST_PACK");
        if(realPack!=null&&!realPack.isBlank())realCorpus(context,Path.of(realPack));
        LoggerFactory.getLogger("mclorehub-test").info("LIBRARY_SCENARIO_PASS: current/audit search, actual typing, reader/replies, favorites, restart/resume, scales, failed reload");
    }
    private static void realCorpus(ClientGameTestContext context,Path pack)throws Exception{
        Path root=Files.createTempDirectory("mclorehub-real-corpus-");Files.createDirectories(root.resolve("packs/active"));
        Files.copy(pack,root.resolve("packs/active/pack.json"));long started=System.nanoTime();
        String trustPath=System.getenv("MCLOREHUB_TEST_TRUST");boolean confirmed=trustPath!=null&&!trustPath.isBlank();
        if(confirmed)Files.copy(Path.of(trustPath),root.resolve("packs/active/trust.json"));
        String liveTopic=System.getenv("MCLOREHUB_TEST_LIVE_TOPIC");String liveFingerprint=null;
        try(var service=new LibraryService(root)){
            var loaded=service.reload().get(30,TimeUnit.SECONDS);check(loaded.phase()==LibraryService.Phase.READY,"Real corpus failed to load: "+loaded.message());
            check(loaded.library().orElseThrow().catalogue().documents().size()==488,"Full corpus record count differs");
            check(loaded.library().orElseThrow().catalogue().currentDocuments().size()==(confirmed?488:0),"Source confirmation/current count differs");
            context.setScreen(()->new WikiScreen(new TitleScreen(),service));context.waitFor(c->c.screen.getNarrationMessage().getString().contains((confirmed?"488":"0")+" current / 488 imported"));
            context.clickScreenButton("screen.mclorehub.audit_off");context.waitFor(c->c.screen.getNarrationMessage().getString().contains("488 results"));
            context.runOnClient(c->edit(c.screen).setValue("magic"));context.waitTicks(5);boundsOnClient(context);context.takeScreenshot("phase4-real-corpus-audit");
            LoggerFactory.getLogger("mclorehub-test").info("REAL_CORPUS_PASS: 488 records, user confirmation={}, load/search/render {} ms",confirmed,(System.nanoTime()-started)/1_000_000);
            if(liveTopic!=null&&!liveTopic.isBlank()){
                var doc=service.view().library().orElseThrow().catalogue().documents().stream().filter(d->d.id().equals(liveTopic)).findFirst().orElseThrow();
                context.setScreen(()->new ReaderScreen(new TitleScreen(),service,doc,false,false));context.clickScreenButton("screen.mclorehub.refresh_topic");
                context.waitFor(c->service.refreshStatus().phase()==LibraryService.RefreshPhase.IDLE,1200);
                var updated=service.view().library().orElseThrow().catalogue().documents().stream().filter(d->d.id().equals(liveTopic)).findFirst().orElseThrow();
                check(updated.reviewState()==LoreCatalogue.ReviewState.SOURCE_REFRESHED,"Live topic update failed: "+service.refreshStatus().message());
                liveFingerprint=updated.fingerprint();context.waitTicks(5);context.takeScreenshot("phase6-live-topic-refreshed");
                check(java.util.Arrays.equals(Files.readAllBytes(pack),Files.readAllBytes(root.resolve("packs/active/pack.json"))),"Live refresh rewrote baseline");
            }
            context.setScreen(TitleScreen::new);
        }
        if(liveFingerprint!=null)try(var reopened=new LibraryService(root)){
            var data=reopened.reload().get(10,TimeUnit.SECONDS);var doc=data.library().orElseThrow().catalogue().documents().stream().filter(d->d.id().equals(liveTopic)).findFirst().orElseThrow();
            check(doc.fingerprint().equals(liveFingerprint),"Live topic did not survive restart");
            LoggerFactory.getLogger("mclorehub-test").info("LIVE_TOPIC_SCENARIO_PASS: {}, {} pages, {} replies, immutable baseline and restart verified",liveTopic,doc.trust().orElseThrow().pages().size(),doc.posts().size());
        }
    }
    private static EditBox edit(net.minecraft.client.gui.screens.Screen s){return s.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow();}
    private static boolean has(net.minecraft.client.gui.screens.Screen s,String title){return s!=null&&s.children().stream().filter(Button.class::isInstance).map(Button.class::cast).anyMatch(b->b.getMessage().getString().equals(title)&&b.active);}
    private static void boundsOnClient(ClientGameTestContext c){c.runOnClient(m->bounds(m.screen));}
    private static void bounds(net.minecraft.client.gui.screens.Screen s){s.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).forEach(b->{
        check(b.getX()>=0&&b.getY()>=0&&b.getRight()<=s.width&&b.getBottom()<=s.height,"Widget outside screen: "+b.getMessage().getString());});}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static void fixture(Path root)throws Exception{
        Files.createDirectories(root.resolve("packs/active"));JsonObject export=new JsonObject(),metadata=new JsonObject();metadata.addProperty("record_count",3);
        metadata.addProperty("generated_utc","2026-09-21T00:00:00Z");export.add("metadata",metadata);JsonArray records=new JsonArray();
        for(int i=0;i<3;i++){
            JsonObject r=new JsonObject();r.addProperty("topic_id",Integer.toString(123+i));r.addProperty("title",i==0?TITLE:i==1?UNKNOWN:"Other synthetic feat");
            r.addProperty("url","https://www.lordofthecraft.net/forums/topic/"+(123+i)+"-synthetic/");r.addProperty("author","Synthetic author");
            r.addProperty("family","synthetic");r.addProperty("category",i==2?"feat":"magic");r.add("tags",new JsonArray());
            JsonArray aliases=new JsonArray();if(i==0)aliases.add("reference alias");r.add("search_aliases",aliases);
            StringBuilder body=new StringBuilder();for(int line=0;line<(i==0?180:5);line++)body.append("Line ").append(line).append(" — Synthetic reference text, not actual lore.\n");
            r.addProperty("content_text",body.toString());r.addProperty("content_sha256",hash(body.toString()));r.add("scrape_error",JsonNull.INSTANCE);
            r.addProperty("audit_role","player-use candidate; requires source review");
            if(i==0){JsonObject post=new JsonObject();post.addProperty("post_id","456");post.addProperty("author","");post.addProperty("content_text","Reply-only starlight: synthetic discussion, not a ruling.");
                post.addProperty("url",r.get("url").getAsString()+"#comment-456");JsonArray posts=new JsonArray();posts.add(post);r.add("thread_posts",posts);}records.add(r);
        }
        export.add("records",records);Path source=root.resolve("synthetic.json");writeNew(source,export);Path pack=root.resolve("packs/active/pack.json");
        writeNew(pack,new ExportImporter().importFile(source).pack());JsonArray decisions=new JsonArray();
        for(var d:LoreCatalogue.load(pack,null).documents())if(!d.id().endsWith("124")){
            JsonObject r=new JsonObject();r.addProperty("document_id",d.id());r.addProperty("revision_sha256",d.revision());r.addProperty("review_fingerprint",d.fingerprint());
            r.addProperty("status","accepted_current");r.addProperty("reviewer","Synthetic reviewer");r.addProperty("reviewed_at","2026-10-20T00:00:00Z");
            JsonObject e=new JsonObject();e.addProperty("source_url",d.url());e.addProperty("passage","Synthetic test approval only; never actual server canon");
            JsonArray evidence=new JsonArray();evidence.add(e);r.add("evidence",evidence);decisions.add(r);
        }
        JsonObject reviews=new JsonObject();reviews.addProperty("schema_version",1);reviews.add("decisions",decisions);writeNew(pack.resolveSibling("reviews.json"),reviews);
    }
}
