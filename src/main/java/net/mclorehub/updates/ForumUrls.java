package net.mclorehub.updates;

import net.mclorehub.importer.ImportException;
import java.net.URI;
import java.util.regex.*;
import static net.mclorehub.importer.JsonSupport.forumUrl;

public final class ForumUrls {
    private static final Pattern TOPIC=Pattern.compile("/forums/topic/([1-9][0-9]{0,17})(?:-[^/]*)?/(?:page/([1-9][0-9]{0,3})/)?");
    private ForumUrls(){}
    public static URI checked(String value,String id)throws ImportException{
        forumUrl(value);URI uri=URI.create(value);Matcher m=TOPIC.matcher(uri.getRawPath());
        if(uri.getRawQuery()!=null||!m.matches()||!id.equals("lotc:topic:"+m.group(1))||!TOPIC.matcher(uri.getPath()).matches()
                ||uri.getPath().contains("\\")||uri.getPath().chars().anyMatch(c->c<32||c==127))throw new ImportException("Refresh links must stay within the selected forum topic");
        return URI.create(value.split("#",2)[0]);
    }
    public static int page(URI uri){Matcher m=TOPIC.matcher(uri.getRawPath());if(!m.matches())throw new IllegalArgumentException("Invalid topic URL");return m.group(2)==null?1:Integer.parseInt(m.group(2));}
    public static URI first(String value,String id)throws ImportException{URI uri=checked(value,id);
        return URI.create("https://www.lordofthecraft.net"+uri.getRawPath().replaceFirst("/page/[0-9]+/$","/"));}
}
