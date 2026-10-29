package net.mclorehub.updates;

import com.google.gson.*;
import net.mclorehub.importer.ImportException;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import static net.mclorehub.importer.JsonSupport.*;

/** Explicitly supplied IPS session cookies only. No browser-store access or challenge cookies. */
public final class ForumCookies {
    private record Cookie(String name,String value,String path,long expiry){
        @Override public String toString(){return name+"=[redacted]";}
    }
    private final List<Cookie> cookies;
    private ForumCookies(List<Cookie> cookies){this.cookies=List.copyOf(cookies);}
    public static ForumCookies empty(){return new ForumCookies(List.of());}
    @Override public String toString(){return "ForumCookies[redacted; count="+cookies.size()+"]";}
    private static boolean supported(String name){return name.matches("(?:ips[0-9]*_)?(?:IPSSessionFront|member_id|pass_hash|device_key)");}
    private static Cookie checked(String name,String value,String path,long expiry)throws ImportException{
        if(!supported(name)||value.isEmpty()||value.length()>4096||!path.startsWith("/")||path.length()>1024||expiry<0)
            throw new ImportException("Invalid forum session cookie (value withheld)");
        for(char c:value.toCharArray())if(!(c==0x21||c>=0x23&&c<=0x2b||c>=0x2d&&c<=0x3a||c>=0x3c&&c<=0x5b||c>=0x5d&&c<=0x7e))
            throw new ImportException("Invalid forum session cookie (value withheld)");
        if(path.chars().anyMatch(c->c<0x21||c>0x7e))throw new ImportException("Invalid forum cookie path");
        return new Cookie(name,value,path,expiry);
    }
    public static ForumCookies netscape(String input,Instant now)throws ImportException{
        List<Cookie> result=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(String raw:input.split("\\R")){
            if(raw.startsWith("#HttpOnly_"))raw=raw.substring(10);else if(raw.isBlank()||raw.startsWith("#"))continue;
            String[] fields=raw.split("\\t",-1);if(fields.length!=7)throw new ImportException("Expected a Netscape-format cookie file; values withheld");
            String domain=fields[0].toLowerCase(Locale.ROOT);boolean parent=domain.equals(".lordofthecraft.net")||domain.equals("lordofthecraft.net")&&fields[1].equals("TRUE");
            if(!parent&&!domain.equals("www.lordofthecraft.net")&&!domain.equals(".www.lordofthecraft.net"))continue;
            if(!supported(fields[5]))continue;
            long expires;try{expires=Long.parseLong(fields[4]);}catch(NumberFormatException e){throw new ImportException("Invalid forum cookie expiry; values withheld");}
            Cookie cookie=checked(fields[5],fields[6],fields[2],expires);
            if(expires>0&&expires<=now.getEpochSecond())continue;
            if(!seen.add(cookie.name()+"\n"+cookie.path()))throw new ImportException("Duplicate forum session cookie; values withheld");
            result.add(cookie);if(result.size()>16)throw new ImportException("Too many forum session cookies");
        }
        var jar=new ForumCookies(result);if(jar.header(URI.create("https://www.lordofthecraft.net/forums/topic/1-session-check/"),now).isEmpty())
            throw new ImportException("No usable forum session cookies found (expired or unsupported)");
        return jar;
    }
    public String header(URI uri,Instant now){
        if(!"https".equals(uri.getScheme())||!"www.lordofthecraft.net".equals(uri.getHost())||uri.getPort()!=-1||uri.getUserInfo()!=null
                ||uri.getRawQuery()!=null||!uri.getPath().startsWith("/forums/topic/"))return "";
        var topic=java.util.regex.Pattern.compile("^/forums/topic/([1-9][0-9]{0,17})(?:-|/)").matcher(uri.getRawPath());if(!topic.find())return "";
        try{ForumUrls.checked(uri.toString(),"lotc:topic:"+topic.group(1));}catch(ImportException e){return "";}
        Map<String,String> values=new LinkedHashMap<>();cookies.stream().sorted(Comparator.comparingInt((Cookie c)->c.path().length()).reversed()).forEach(c->{
            boolean path=uri.getPath().equals(c.path())||uri.getPath().startsWith(c.path().endsWith("/")?c.path():c.path()+"/");
            if(path&&(c.expiry()==0||c.expiry()>now.getEpochSecond()))values.putIfAbsent(c.name(),c.value());});
        return values.entrySet().stream().map(e->e.getKey()+"="+e.getValue()).collect(java.util.stream.Collectors.joining("; "));
    }
    JsonObject json(){JsonObject r=new JsonObject();r.addProperty("schema_version",1);JsonArray list=new JsonArray();for(Cookie c:cookies){JsonObject i=new JsonObject();
        i.addProperty("name",c.name());i.addProperty("value",c.value());i.addProperty("path",c.path());i.addProperty("expiry",c.expiry());list.add(i);}r.add("cookies",list);return r;}
    static ForumCookies fromJson(JsonObject root)throws ImportException{
        if(integer(root,"schema_version")!=1||!root.keySet().equals(Set.of("schema_version","cookies")))throw new ImportException("Unsupported forum session format; values withheld");
        List<Cookie> list=new ArrayList<>();for(JsonElement item:array(root.get("cookies"),"cookies")){JsonObject c=object(item,"cookie");
            if(!c.keySet().equals(Set.of("name","value","path","expiry")))throw new ImportException("Unsupported forum session fields");
            long expiry;try{expiry=c.get("expiry").getAsBigDecimal().longValueExact();}catch(RuntimeException e){throw new ImportException("Invalid cookie expiry");}
            list.add(checked(string(c,"name"),string(c,"value"),string(c,"path"),expiry));if(list.size()>16)throw new ImportException("Too many session cookies");}
        return new ForumCookies(list);
    }
}
