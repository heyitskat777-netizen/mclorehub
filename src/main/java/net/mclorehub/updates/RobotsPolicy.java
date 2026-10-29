package net.mclorehub.updates;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

/** Relevant robots groups for the honest MCLoreHub personal-reference client. */
public final class RobotsPolicy {
    private record Rule(String path,boolean allow){}
    private record Group(List<String> agents,List<Rule> rules){}
    private final List<Rule> rules;
    public RobotsPolicy(String text){
        List<Group> groups=new ArrayList<>();List<String> agents=new ArrayList<>();List<Rule> current=new ArrayList<>();boolean directives=false;
        for(String raw:text.split("\\R")){
            String line=raw.split("#",2)[0].strip();int colon=line.indexOf(':');if(colon<0)continue;
            String key=line.substring(0,colon).strip().toLowerCase(Locale.ROOT),value=line.substring(colon+1).strip();
            if(key.equals("user-agent")){
                if(directives){groups.add(new Group(List.copyOf(agents),List.copyOf(current)));agents.clear();current.clear();directives=false;}
                agents.add(value.toLowerCase(Locale.ROOT));
            }else if(!agents.isEmpty()){
                directives=true;if((key.equals("disallow")||key.equals("allow"))&&!value.isEmpty())current.add(new Rule(value,key.equals("allow")));
            }
        }
        groups.add(new Group(List.copyOf(agents),List.copyOf(current)));int best=-1;List<Rule> selected=new ArrayList<>();
        for(Group g:groups){int score=g.agents().stream().mapToInt(a->a.equals("*")?0:"mclorehub".contains(a)?a.length():-1).max().orElse(-1);
            if(score>best){best=score;selected.clear();}if(score>=0&&score==best)selected.addAll(g.rules());}
        rules=List.copyOf(selected);
    }
    public boolean allowed(URI uri){String path=uri.getRawPath()+(uri.getRawQuery()==null?"":"?"+uri.getRawQuery());int length=-1;boolean allow=true;
        for(Rule rule:rules){String value=rule.path();boolean end=value.endsWith("$");if(end)value=value.substring(0,value.length()-1);
            String regex=Arrays.stream(value.split("\\*",-1)).map(Pattern::quote).collect(java.util.stream.Collectors.joining(".*"));
            if(Pattern.compile("^"+regex+(end?"$":"")).matcher(path).find()&&(value.length()>length||value.length()==length&&rule.allow())){length=value.length();allow=rule.allow();}}
        return allow;
    }
}
