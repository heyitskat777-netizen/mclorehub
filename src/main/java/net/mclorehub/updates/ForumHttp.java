package net.mclorehub.updates;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.Flow;

/** Official HTTPS transport, optional scoped session, bounded responses and verified TLS. */
public final class ForumHttp implements ForumClient.Transport {
    private final HttpClient client;
    private final java.util.function.Function<URI,String> session;
    public ForumHttp(){this(uri->"");}
    public ForumHttp(java.util.function.Function<URI,String> session){
        this.session=session;
        var builder=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER);
        String proxy=System.getenv("HTTPS_PROXY");if(proxy==null)proxy=System.getenv("https_proxy");
        if(proxy!=null&&!proxy.isBlank()){
            URI uri=URI.create(proxy);if(uri.getHost()!=null)builder.proxy(ProxySelector.of(new InetSocketAddress(uri.getHost(),uri.getPort()>0?uri.getPort():"https".equals(uri.getScheme())?443:80)));
        }
        client=builder.build();
    }
    @Override public ForumClient.Response get(URI uri)throws IOException,InterruptedException {
        var builder=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(25))
                .header("User-Agent","MCLoreHub/0.1 (personal lore reference; https://github.com/heyitskat777-netizen/mclorehub)")
                .header("Accept","text/html,text/plain;q=0.9").header("Accept-Encoding","identity");
        String cookie=session.apply(uri);if(!cookie.isEmpty())builder.header("Cookie",cookie);
        HttpRequest request=builder.GET().build();
        var future=client.sendAsync(request,info->new LimitedBody(4<<20));
        try{var response=future.get(30,TimeUnit.SECONDS);return new ForumClient.Response(uri,response.statusCode(),response.headers().firstValue("Content-Type").orElse(""),
                response.headers().firstValue("Location").orElse(""),response.headers().firstValue("Retry-After").orElse(""),response.body());}
        catch(ExecutionException e){throw new IOException("Forum network request failed; stored lore retained (credential values withheld)");}
        catch(TimeoutException e){future.cancel(true);throw new IOException("Forum request timed out; stored lore retained",e);}
        catch(InterruptedException e){future.cancel(true);throw e;}
    }
    private static final class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final CompletableFuture<byte[]> result=new CompletableFuture<>();private final ByteArrayOutputStream bytes=new ByteArrayOutputStream();private final int limit;private Flow.Subscription subscription;
        LimitedBody(int limit){this.limit=limit;}
        public CompletionStage<byte[]> getBody(){return result;}
        public void onSubscribe(Flow.Subscription s){subscription=s;s.request(1);}
        public void onNext(List<ByteBuffer> buffers){for(ByteBuffer b:buffers){if(b.remaining()>limit-bytes.size()){subscription.cancel();result.completeExceptionally(new IOException("Forum response exceeds 4 MiB limit"));return;}
                byte[] part=new byte[b.remaining()];b.get(part);bytes.writeBytes(part);}subscription.request(1);}
        public void onError(Throwable t){result.completeExceptionally(t);}
        public void onComplete(){result.complete(bytes.toByteArray());}
    }
}
