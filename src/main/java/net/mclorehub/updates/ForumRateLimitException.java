package net.mclorehub.updates;
import java.io.IOException;
import java.time.Instant;
public final class ForumRateLimitException extends IOException {
    private final Instant until;
    public ForumRateLimitException(Instant until){super("Forum rate limit; retry after "+until);this.until=until;}
    public Instant until(){return until;}
}
