package net.mclorehub.updates;

import net.mclorehub.catalogue.LoreCatalogue.*;
import net.mclorehub.importer.ImportException;
import java.io.IOException;
import java.net.URI;
import java.time.*;
import java.util.*;

/** One known topic's main first post, no discussion/discovery/login/search requests. */
public final class ForumClient implements TopicFetcher {
    public record Response(URI uri,int status,String contentType,String location,String retryAfter,byte[] body){}
    @FunctionalInterface public interface Transport { Response get(URI uri)throws IOException,InterruptedException; }
    private final Transport transport;private final Duration spacing;private final Clock clock;private long lastRequest;private Instant retryUntil=Instant.EPOCH;
    public ForumClient(){this(new ForumHttp(),Duration.ofSeconds(1),Clock.systemUTC());}
    public ForumClient(Transport transport,Duration spacing,Clock clock){this.transport=transport;this.spacing=spacing;this.clock=clock;}
    private Response get(URI uri)throws IOException,InterruptedException{
        if(Thread.interrupted())throw new InterruptedException("Refresh cancelled");
        if(clock.instant().isBefore(retryUntil))throw new ForumRateLimitException(retryUntil);
        long wait=spacing.toNanos()-(System.nanoTime()-lastRequest);if(wait>0)java.util.concurrent.TimeUnit.NANOSECONDS.sleep(wait);
        Response response=transport.get(uri);lastRequest=System.nanoTime();
        if(response.body().length>4<<20)throw new ImportException("Forum response exceeds size limit");
        if(response.status()==429){long seconds=60;try{seconds=Long.parseLong(response.retryAfter());}catch(NumberFormatException ignored){
                try{seconds=Duration.between(clock.instant(),ZonedDateTime.parse(response.retryAfter(),java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()).getSeconds();}catch(RuntimeException unavailable){}}
            try{retryUntil=clock.instant().plusSeconds(Math.max(1,seconds));}catch(DateTimeException e){retryUntil=Instant.MAX;}
            throw new ForumRateLimitException(retryUntil);}
        return response;
    }
    @Override public synchronized TopicSnapshot fetch(Document document)throws IOException,InterruptedException{
        URI first=ForumUrls.first(document.url(),document.id());URI robots=URI.create("https://"+first.getHost()+"/robots.txt");Response policy=get(robots);
        if(policy.status()!=200&&policy.status()!=404)throw new ImportException("Could not check forum access policy (HTTP "+policy.status()+"); stored lore retained");
        RobotsPolicy rules=new RobotsPolicy(policy.status()==404?"":new String(policy.body(),java.nio.charset.StandardCharsets.UTF_8));
        Set<URI> visited=new HashSet<>();LinkedHashMap<String,ThreadPost> fetched=new LinkedHashMap<>();String title=null,mainId=null,mainBody=null,author="";
        URI next=first,source=first;Optional<Status> status=Optional.empty();List<String> pages=new ArrayList<>();List<net.mclorehub.catalogue.SourceSection> outline=List.of();int expectedPages=-1,totalBytes=0;
        long deadline=System.nanoTime()+Duration.ofSeconds(90).toNanos();
        while(next!=null){
            if(Thread.currentThread().isInterrupted())throw new InterruptedException("Refresh cancelled");
            if(System.nanoTime()>deadline||pages.size()>=20)throw new ImportException("Topic refresh exceeded limits; no partial update saved");
            URI uri=ForumUrls.checked(next.toString(),document.id());if(!visited.add(uri))throw new ImportException("Forum pagination loop");
            Response response=null;
            for(int redirects=0;redirects<=4;redirects++){
                if(!rules.allowed(uri))throw new ImportException("Forum access policy disallows this topic page");
                response=get(uri);
                if(response.status()>=300&&response.status()<400){if(redirects==4||response.location().isBlank())throw new ImportException("Invalid/excessive forum redirect");
                    URI target=ForumUrls.checked(uri.resolve(response.location()).toString(),document.id());if(!visited.add(target))throw new ImportException("Forum redirect loop");uri=target;continue;}break;
            }
            if(response.status()!=200)throw new ImportException("Forum returned HTTP "+response.status()+"; stored lore retained (missing pages are not deletion)");
            if(System.nanoTime()>deadline)throw new ImportException("Topic refresh deadline exceeded; stored lore retained");
            if(!response.contentType().toLowerCase(Locale.ROOT).startsWith("text/html"))throw new ImportException("Expected a forum HTML page");
            totalBytes+=response.body().length;if(totalBytes>16<<20)throw new ImportException("Topic exceeds 16 MiB response limit");
            if(ForumUrls.page(uri)!=pages.size()+1)throw new ImportException("Redirect skipped topic pages; no partial update saved");
            ForumHtml.Page page=ForumHtml.main(response.body(),uri,document.id());pages.add(uri.toString());
            if(title==null){title=page.title();outline=page.outline();expectedPages=page.totalPages();status=page.status();source=uri;var main=page.posts().getFirst();mainId=main.id();mainBody=main.text();author=main.author();
                if(mainBody.replaceAll("\\[Image[^\\]]*\\]","").isBlank())throw new ImportException("Main lore post is empty or image-only; stored text retained");}
            else if(!title.equals(page.title())||expectedPages!=page.totalPages()||!status.equals(page.status()))throw new ImportException("Topic changed during refresh; retry rather than save inconsistent pages");
            for(ThreadPost p:page.posts()){
                ThreadPost old=fetched.putIfAbsent(p.id(),p);if(old!=null&&(!old.text().equals(p.text())||!old.author().equals(p.author())))throw new ImportException("Post changed during refresh");
            }
            if(fetched.size()>1000)throw new ImportException("Too many topic replies");next=page.next().orElse(null);
        }
        List<String> absent=document.coverageGaps().stream().filter(g->g.startsWith("previous_reply_not_returned:")).map(g->g.substring("previous_reply_not_returned:".length())).toList();
        return new TopicSnapshot(document.id(),source.toString(),title,author,mainBody,document.posts(),pages,clock.instant(),status,absent,outline);
    }
}
