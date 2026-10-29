package net.mclorehub.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.mclorehub.catalogue.LoreCatalogue;
import net.mclorehub.catalogue.LoreCatalogue.ThreadPost;
import net.mclorehub.library.*;
import net.mclorehub.updates.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import org.slf4j.LoggerFactory;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static net.mclorehub.importer.JsonSupport.*;

@SuppressWarnings("UnstableApiUsage")
public final class RefreshClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context){try{scenario(context);}catch(Exception e){throw new AssertionError("Refresh scenario failed",e);}}
    private void scenario(ClientGameTestContext c)throws Exception{
        c.waitForScreen(TitleScreen.class);Path root=Files.createTempDirectory("mclorehub-refresh-ui-");LibraryClientGameTest.fixture(root);
        var baseline=LoreCatalogue.load(root.resolve("packs/active/pack.json"),null);
        writeNew(root.resolve("packs/active/trust.json"),BaselineTrust.manifest(baseline,Instant.now()));
        AtomicInteger calls=new AtomicInteger();TopicFetcher transport=d->{int n=calls.incrementAndGet();if(n==2)throw new java.io.IOException("HTTP 403");if(n==3)Thread.sleep(30000);
            List<ThreadPost> posts=new ArrayList<>(d.posts());posts.add(new ThreadPost("999","Synthetic updater",d.url()+"#comment-999","New synthetic reply"));
            return new TopicSnapshot(d.id(),d.url(),d.title(),d.author().orElse(""),"Manually refreshed synthetic content.\nNo actual server lore in this fixture.",posts,List.of(d.url()),Instant.now(),Optional.empty(),List.of());};
        LibraryService service=new LibraryService(root,transport);
        try{
            service.reload().get(5,TimeUnit.SECONDS);check(service.view().library().orElseThrow().catalogue().currentDocuments().size()==3,"Confirmed baseline not available normally");
            final LibraryService first=service;var original=first.view().library().orElseThrow().catalogue().documents().getFirst();
            first.toggleFavorite(original.id()).get(5,TimeUnit.SECONDS);
            c.setScreen(()->new ReaderScreen(new TitleScreen(),first,original,false,false));c.waitFor(m->m.screen.getNarrationMessage().getString().contains("Line 0"));
            c.clickScreenButton("screen.mclorehub.refresh_topic");c.waitFor(m->m.screen.getNarrationMessage().getString().contains("Manually refreshed synthetic content"));
            c.takeScreenshot("phase6-topic-refreshed");c.setScreen(TitleScreen::new);first.close();
            service=new LibraryService(root,transport);final LibraryService reopened=service;reopened.reload().get(5,TimeUnit.SECONDS);
            var refreshed=reopened.view().library().orElseThrow().catalogue().documents().getFirst();check(refreshed.body().contains("Manually refreshed"),"Refresh did not survive restart");
            check(reopened.view().personal().favorites().contains(original.id()),"Favorite lost during refresh");
            c.setScreen(()->new ReaderScreen(new TitleScreen(),reopened,refreshed,false,true));
            c.waitFor(m->m.screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).anyMatch(b->b.getMessage().getString().equals("Undo refresh")&&b.active));
            c.waitFor(m->m.screen.getNarrationMessage().getString().contains("Source:"));
            c.takeScreenshot("phase6-topic-source-undo");c.clickScreenButton("screen.mclorehub.undo_refresh");
            c.waitFor(m->reopened.view().library().orElseThrow().catalogue().documents().getFirst().body().equals(original.body()));
            var restored=reopened.view().library().orElseThrow().catalogue().documents().getFirst();c.setScreen(()->new ReaderScreen(new TitleScreen(),reopened,restored,false,false));
            c.waitFor(m->m.screen.getNarrationMessage().getString().contains("Line 0"));c.clickScreenButton("screen.mclorehub.refresh_topic");
            c.waitFor(m->m.screen.getNarrationMessage().getString().contains("HTTP 403"));c.takeScreenshot("phase6-refresh-error");
            check(reopened.view().library().orElseThrow().catalogue().documents().getFirst().body().equals(original.body()),"Failed refresh replaced content");
            c.clickScreenButton("screen.mclorehub.refresh_topic");c.waitFor(m->m.screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).anyMatch(b->b.getMessage().getString().equals("Cancel refresh")));
            c.clickScreenButton("screen.mclorehub.cancel_refresh");c.waitFor(m->m.screen.getNarrationMessage().getString().contains("Refresh cancelled"));
            check(reopened.view().library().orElseThrow().catalogue().documents().getFirst().body().equals(original.body()),"Cancelled refresh replaced content");
            c.setScreen(TitleScreen::new);
        }finally{service.close();}
        LoggerFactory.getLogger("mclorehub-test").info("REFRESH_SCENARIO_PASS: trusted baseline, topic button, persisted content/replies, restart, undo, HTTP failure, cancel, favorites");
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
