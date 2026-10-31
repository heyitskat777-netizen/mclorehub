package net.mclorehub.catalogue;

import com.google.gson.*;
import java.io.IOException;
import java.util.*;
import static net.mclorehub.importer.JsonSupport.*;

/** Display structure bound to exact source-text offsets, never an independent lore ruling. */
public record SourceSection(String title,int start,int end,int level) {
    public static List<SourceSection> read(JsonElement value,String body)throws IOException {
        if(value==null)return List.of();var data=array(value,"source outline");if(data.size()>10000)throw new IOException("Too many source headings");
        List<SourceSection> result=new ArrayList<>();int previous=-1;
        for(var item:data){var o=object(item,"source heading");if(!o.keySet().equals(Set.of("title","start","end","level")))throw new IOException("Unknown source heading fields");
            var h=new SourceSection(text(o,"title"),integer(o,"start"),integer(o,"end"),integer(o,"level"));
            if(h.start()<0||h.start()<previous||h.end()<=h.start()||h.end()>body.length()||h.level()<1||h.level()>9||h.title().length()>240
                    ||!h.title().equals(body.substring(h.start(),h.end()))||h.start()>0&&Character.isLowSurrogate(body.charAt(h.start()))&&Character.isHighSurrogate(body.charAt(h.start()-1))
                    ||h.end()<body.length()&&Character.isHighSurrogate(body.charAt(h.end()-1))&&Character.isLowSurrogate(body.charAt(h.end())))
                throw new IOException("Source heading does not match main text");result.add(h);previous=h.end();
        }return List.copyOf(result);
    }
}
