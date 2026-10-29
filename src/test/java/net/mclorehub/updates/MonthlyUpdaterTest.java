package net.mclorehub.updates;

import net.mclorehub.library.LibraryService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class MonthlyUpdaterTest {
    @TempDir Path root;
    static final class MutableClock extends Clock {Instant value=Instant.now().minus(Duration.ofDays(100));public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return Clock.fixed(value,z);}public Instant instant(){return value;}}
    @Test void calendarMonthHandlesEndOfMonthAndLeapYears(){assertEquals(Instant.parse("2026-02-28T10:00:00Z"),MonthlyPlan.nextMonth(Instant.parse("2026-01-31T10:00:00Z")));
        assertEquals(Instant.parse("2028-02-29T10:00:00Z"),MonthlyPlan.nextMonth(Instant.parse("2028-01-31T10:00:00Z")));}
    @Test void noImmediateFetchThenDuePagesCatchUpOnceAndPersistAcrossRestart()throws Exception{UpdateFixtures.install(root,true);var clock=new MutableClock();AtomicInteger requests=new AtomicInteger();
        try(var library=new LibraryService(root,d->{requests.incrementAndGet();return new TopicSnapshot(d.id(),d.url(),d.title(),d.author().orElse(""),d.body(),d.posts(),List.of(d.url()),clock.instant(),Optional.empty(),List.of());});var auto=new MonthlyUpdater(library,clock)){
            library.reload().get(5,TimeUnit.SECONDS);var initial=auto.runOnce().get(5,TimeUnit.SECONDS);assertEquals(0,requests.get());clock.value=initial.next().orElseThrow().plusSeconds(1);
            auto.runOnce().get(5,TimeUnit.SECONDS);auto.runOnce().get(5,TimeUnit.SECONDS);assertEquals(2,requests.get());auto.runOnce().get(5,TimeUnit.SECONDS);assertEquals(2,requests.get());}
        try(var library=new LibraryService(root,d->{throw new AssertionError("Already checked pages must not be fetched after restart");});var auto=new MonthlyUpdater(library,clock)){
            library.reload().get(5,TimeUnit.SECONDS);auto.runOnce().get(5,TimeUnit.SECONDS);assertEquals(0,auto.view().due());}}
    @Test void manualRefreshResetsMonthlyDueDate()throws Exception{UpdateFixtures.install(root,true);var clock=new MutableClock();AtomicInteger calls=new AtomicInteger();
        try(var library=new LibraryService(root,d->{calls.incrementAndGet();return new TopicSnapshot(d.id(),d.url(),d.title(),d.author().orElse(""),d.body(),d.posts(),List.of(d.url()),clock.instant(),Optional.empty(),List.of());});var auto=new MonthlyUpdater(library,clock)){
            library.reload().get(5,TimeUnit.SECONDS);auto.runOnce().get(5,TimeUnit.SECONDS);clock.value=clock.value.plus(Duration.ofDays(20));library.refresh("lotc:topic:123").get(5,TimeUnit.SECONDS);
            clock.value=clock.value.plus(Duration.ofDays(12));auto.runOnce().get(5,TimeUnit.SECONDS);assertEquals(2,calls.get(),"Only the other page should be due");}}
    @Test void disablingPersistsAndPreventsOverdueRequests()throws Exception{UpdateFixtures.install(root,true);var clock=new MutableClock();
        try(var library=new LibraryService(root,d->{throw new AssertionError("Paused scheduler requested a page");});var auto=new MonthlyUpdater(library,clock)){
            library.reload().get(5,TimeUnit.SECONDS);auto.runOnce().get(5,TimeUnit.SECONDS);auto.setEnabled(false).get(5,TimeUnit.SECONDS);clock.value=clock.value.plus(Duration.ofDays(90));auto.runOnce().get(5,TimeUnit.SECONDS);assertFalse(auto.view().enabled());}
        try(var library=new LibraryService(root);var auto=new MonthlyUpdater(library,clock)){library.reload().get(5,TimeUnit.SECONDS);assertFalse(auto.runOnce().get(5,TimeUnit.SECONDS).enabled());
            assertTrue(auto.setEnabled(true).get(5,TimeUnit.SECONDS).enabled());}}
    @Test void inaccessiblePagesWaitAnotherMonthAndKeepSavedLore()throws Exception{var base=UpdateFixtures.install(root,true);var clock=new MutableClock();AtomicInteger calls=new AtomicInteger();
        try(var library=new LibraryService(root,d->{calls.incrementAndGet();throw new java.io.IOException("Forum returned HTTP 403");});var auto=new MonthlyUpdater(library,clock)){
            library.reload().get(5,TimeUnit.SECONDS);clock.value=auto.runOnce().get(5,TimeUnit.SECONDS).next().orElseThrow().plusSeconds(1);auto.runOnce().get(5,TimeUnit.SECONDS);auto.runOnce().get(5,TimeUnit.SECONDS);
            assertEquals(2,calls.get());assertEquals(base.documents().getFirst().body(),library.view().library().orElseThrow().catalogue().documents().getFirst().body());auto.runOnce().get(5,TimeUnit.SECONDS);assertEquals(2,calls.get());}}
    @Test void retryAfterDefersEntireAutomaticQueue()throws Exception{UpdateFixtures.install(root,true);var clock=new MutableClock();AtomicInteger calls=new AtomicInteger();
        try(var library=new LibraryService(root,d->{calls.incrementAndGet();throw new ForumRateLimitException(clock.instant().plusSeconds(3600));});var auto=new MonthlyUpdater(library,clock)){
            library.reload().get(5,TimeUnit.SECONDS);clock.value=auto.runOnce().get(5,TimeUnit.SECONDS).next().orElseThrow().plusSeconds(1);auto.runOnce().get(5,TimeUnit.SECONDS);auto.runOnce().get(5,TimeUnit.SECONDS);assertEquals(1,calls.get());}}
    @Test void corruptSchedulePausesInsteadOfResettingOrFetching()throws Exception{UpdateFixtures.install(root,true);Files.createDirectories(root.resolve("updates"));Path file=Files.writeString(root.resolve("updates/monthly.json"),"invalid");
        try(var library=new LibraryService(root,d->{throw new AssertionError("Invalid schedule fetched a page");});var auto=new MonthlyUpdater(library)){
            library.reload().get(5,TimeUnit.SECONDS);assertFalse(auto.runOnce().get(5,TimeUnit.SECONDS).writable());assertEquals("invalid",Files.readString(file));}}
    @Test void explicitEnableWorksBeforeFirstPollOfPersistedPausedSchedule()throws Exception{UpdateFixtures.install(root,true);Files.createDirectories(root.resolve("updates"));Files.writeString(root.resolve("updates/monthly.json"),
        net.mclorehub.importer.JsonSupport.JSON.toJson(MonthlyPlan.empty().enabled(false).json()));
        try(var library=new LibraryService(root);var auto=new MonthlyUpdater(library)){library.reload().get(5,TimeUnit.SECONDS);assertTrue(auto.setEnabled(true).get(5,TimeUnit.SECONDS).enabled());}}
    @Test void discussionOnlyChangesDoNotBecomeMainLoreUpdates()throws Exception{var base=UpdateFixtures.install(root,true);try(var library=new LibraryService(root,d->{var s=UpdateFixtures.snapshot(d,d.body());
        return new TopicSnapshot(s.documentId(),s.url(),s.title(),s.author(),s.body(),List.of(new net.mclorehub.catalogue.LoreCatalogue.ThreadPost("999","Discussion",s.url(),"New reply should not change main lore")),s.pages(),s.fetchedAt(),Optional.empty(),List.of());})){
            library.reload().get(5,TimeUnit.SECONDS);assertFalse(library.refresh("lotc:topic:123").get(5,TimeUnit.SECONDS).changed());assertEquals(base.documents().getFirst().posts(),library.view().library().orElseThrow().catalogue().documents().getFirst().posts());}}
}
