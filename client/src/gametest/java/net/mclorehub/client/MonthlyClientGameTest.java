package net.mclorehub.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.mclorehub.library.LibraryService;
import net.mclorehub.updates.*;
import net.minecraft.client.gui.screens.TitleScreen;
import org.slf4j.LoggerFactory;
import java.time.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static net.mclorehub.importer.JsonSupport.*;

@SuppressWarnings("UnstableApiUsage")
public final class MonthlyClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c){try{scenario(c);}catch(Exception e){throw new AssertionError("Monthly/session scenario failed",e);}}
    private void scenario(ClientGameTestContext c)throws Exception{
        class TestClock extends Clock{volatile Instant now=Instant.now().minus(Duration.ofDays(100));public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return Clock.fixed(now,z);}public Instant instant(){return now;}}
        c.waitForScreen(TitleScreen.class);Path root=Files.createTempDirectory("mclorehub-monthly-ui-");LibraryClientGameTest.fixture(root);
        var baseline=net.mclorehub.catalogue.LoreCatalogue.load(root.resolve("packs/active/pack.json"),null);writeNew(root.resolve("packs/active/trust.json"),BaselineTrust.manifest(baseline,Instant.now()));
        TestClock clock=new TestClock();AtomicInteger calls=new AtomicInteger();int scale=c.computeOnClient(m->m.options.guiScale().get());
        try(var library=new LibraryService(root,d->{calls.incrementAndGet();return new TopicSnapshot(d.id(),d.url(),d.title(),d.author().orElse(""),"Automatically checked MAIN lore. Discussion excluded.",d.posts(),List.of(d.url()),clock.instant(),Optional.empty(),List.of());});var monthly=new MonthlyUpdater(library,clock)){
            library.reload().get(5,TimeUnit.SECONDS);var initial=monthly.runOnce().get(5,TimeUnit.SECONDS);check(calls.get()==0,"Monthly startup must not cause an immediate bulk fetch");
            clock.now=initial.next().orElseThrow().plusSeconds(1);monthly.start();
            c.waitFor(m->calls.get()>0,300);c.waitFor(m->library.view().library().orElseThrow().catalogue().documents().stream().anyMatch(d->d.body().contains("Automatically checked MAIN")));
            c.runOnClient(m->{m.options.guiScale().set(2);m.resizeDisplay();});c.setScreen(()->new UpdateSettingsScreen(new TitleScreen(),monthly,library.session()));
            c.clickScreenButton("Monthly checks: On");c.waitFor(m->!monthly.view().enabled());
            Path export=Files.writeString(root.resolve("synthetic-cookies.txt"),"# Netscape HTTP Cookie File\n.lordofthecraft.net\tTRUE\t/\tTRUE\t0\tips4_IPSSessionFront\tSYNTHETIC_NOT_A_REAL_FORUM_SESSION\n");
            c.runOnClient(m->m.screen.onFilesDrop(List.of(export)));c.waitFor(m->library.session().view().available());
            c.runOnClient(m->{check(!m.screen.getNarrationMessage().getString().contains("SYNTHETIC_NOT_A_REAL"),"Session value exposed in narration");});
            c.takeScreenshot("phase7-monthly-session-settings");c.clickScreenButton("Forget session");c.waitFor(m->!library.session().view().available());library.session().forget().get(5,TimeUnit.SECONDS);
            check(!Files.exists(root.resolve("auth/forum-session.json")),"Forgotten session retained on disk");
            c.clickScreenButton("gui.back");c.waitForScreen(TitleScreen.class);
        }finally{c.runOnClient(m->{m.options.guiScale().set(scale);m.resizeDisplay();});}
        LoggerFactory.getLogger("mclorehub-test").info("MONTHLY_SCENARIO_PASS: actual scheduled clock catch-up, main update, pause control, synthetic session import/redaction/forget; no real account login tested");
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
