package net.mclorehub.spell;

import net.mclorehub.catalogue.*;
import net.mclorehub.catalogue.LoreCatalogue.Document;
import net.mclorehub.character.*;
import net.mclorehub.library.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/** Exact source sections and explicit tier evidence. Reference availability never certifies learning/approval. */
public final class SpellIndex {
    public enum Access { BY_TIER, CHECK_REQUIREMENTS, ABOVE_TIER, OTHER_BRANCH, NOT_CURRENT }
    public record Ability(String id,String sourceId,String sourceFingerprint,String name,int minimumTier,String stage,String branch,
                          boolean extraInvestment,int start,int end,int parentStart,int parentEnd,String tierEvidence,boolean sourceReference){}
    public record Hit(Ability ability,Document source,CharacterBook.Entry entry,Access access,String requirement){}
    public record Results(List<Hit> hits,int eligible,int conditional,int locked,List<String> notes){public Results{hits=List.copyOf(hits);notes=List.copyOf(notes);}}
    private record Header(SourceSection section,String name,int tier,String stage,String branch,boolean extra,String evidence){}
    private record TierEvidence(int tier,String passage,String branch){TierEvidence(int tier,String passage){this(tier,passage,"");}}
    private static final Pattern TYPE=Pattern.compile("(?:\\[|\\(|\\s[-:]\\s*|:)\\s*(?:NON[ -]?)?(?:COMBATIVE|COMBAT|PASSIVE|ACTIVE|RITUAL)\\b");
    private static final Pattern BODY=Pattern.compile("\\b(?:REQUIRES?|REQUIRED|CAPABLE|WOULD|MUST|COOLDOWN|UNLOCKS?|SLOTS?|WEEKS?|MONTHS?|DAYS?|BLOCKS?|METERS?|RADIUS|CASTS?|LITURGIES|MAY|CAN|COSTS?|SUCCESS|HAZARD|TASK|EXAMPLE|CASTING|SUMMONING)\\b");
    private static final Pattern GENERIC=Pattern.compile("^(?:(?:GENERAL |GLOBAL |SPELL |MAGIC |OOC )?(?:RED ?LINES?|RULES?|MECHANICS|DESCRIPTION|EXPLANATION|EXAMPLES?|NOTES|LIMITATIONS|REQUIREMENTS|PROGRESSION|COSTS)|ORIGIN|LORE ORIGINS|BACKGROUND|TIER PROGRESSION|SPELL LIST|SPELLS|ABILITIES|CANTRIPS|RITUALS|TARGET|RANGE|DURATION|COMBAT|NON-COMBAT|PASSIVE|ACTIVE|REAGENTS|CREDITS|TIER LEVELS.*|SPELL SLOTS|FRAZZLE|OBSESSION|THE SHOW MUST GO ON.*)[: .]*$");
    private static final Pattern ABILITY_AREA=Pattern.compile("\\b(?:SPELLS|SPELL LIST|SPELL INDEX|ABILITIES|CANTRIPS|LITURGIES|SACRAMENTS|RITUALS|MALEDICTIONS|RITES)\\b");
    private static final Pattern END_AREA=Pattern.compile("\\b(?:CREDITS|CITATIONS|PURPOSE|COMPATIBILITIES|WEAKNESSES|ORIGINS?|MENTALITY|PHYSIOLOGY|TIER PROGRESSION|GENERAL REDLINES|ACQUIRING THE TA)\\b");
    private static final Pattern EXPLICIT_REQUIREMENT=Pattern.compile("(?i)(?:must be (?:at least )?|requires? (?:at least )?|minimum (?:of )?)(?:\\[)?(?:TIER\\s*|T)([0-9]+|VIII|VII|III|VI|IV|IX|II|V|I)\\b[^.\\n]{0,70}(?:use this ability|perform this|cast this|use the spell)");
    private final Map<String,Document> documents;
    private final ConcurrentMap<String,List<Ability>> cached=new ConcurrentHashMap<>();
    public SpellIndex(LoreCatalogue catalogue){Map<String,Document> map=new HashMap<>();catalogue.documents().forEach(d->map.put(d.id(),d));documents=Map.copyOf(map);}
    public List<Ability> references(String id){var doc=documents.get(id);return doc==null?List.of():cached.computeIfAbsent(id,key->extract(doc));}
    public Results search(CharacterBook.Sheet sheet,String query,boolean allTiers){
        if(query.length()>256)throw new IllegalArgumentException("Spell search is too long");Map<String,Hit> found=new LinkedHashMap<>();List<String> notes=new ArrayList<>();int unlinked=0;
        for(var entry:sheet.entries()){
            cancelled();Document source=documents.get(entry.loreDocumentId());if(source==null){unlinked++;continue;}
            boolean matched=false;
            for(var ability:references(source.id())){
                if(ability.sourceReference())continue;cancelled();
                if(!SpellText.matches(query,ability.name()+" "+entry.name()+" "+source.title()+" "+source.body().substring(ability.start(),ability.end())))continue;
                var hit=assess(ability,source,entry,sheet);if(!allTiers&&Set.of(Access.ABOVE_TIER,Access.OTHER_BRANCH,Access.NOT_CURRENT).contains(hit.access()))continue;
                matched=true;found.merge(ability.id(),hit,(old,next)->rank(next.access())<rank(old.access())?next:old);
            }
            if(!matched){var fallback=references(source.id()).getLast();if(SpellText.matches(query,entry.name()+" "+source.title()+" "+source.body())){
                var hit=assess(fallback,source,entry,sheet);if(allTiers||hit.access()!=Access.NOT_CURRENT)found.putIfAbsent(fallback.id(),hit);
            }}
            if(found.size()>=10000){notes.add("Result limit reached; narrow the search or use fewer linked entries.");break;}
        }
        if(unlinked>0)notes.add(unlinked+" recorded entries have no available source link; choose lore on those entries to show their references.");
        var hits=found.values().stream().sorted(Comparator.comparingInt((Hit h)->SpellText.matches(query,h.ability().name())?0:1).thenComparing(h->SpellText.fold(h.entry().name())).thenComparingInt(h->h.ability().minimumTier()<0?99:h.ability().minimumTier()).thenComparing(h->h.ability().name())).toList();
        int eligible=(int)hits.stream().filter(h->h.access()==Access.BY_TIER).count(),conditional=(int)hits.stream().filter(h->h.access()==Access.CHECK_REQUIREMENTS).count();
        return new Results(hits,eligible,conditional,hits.size()-eligible-conditional,notes);
    }
    private static int rank(Access access){return switch(access){case BY_TIER->0;case CHECK_REQUIREMENTS->1;case ABOVE_TIER->2;case OTHER_BRANCH->3;case NOT_CURRENT->4;};}
    private static Hit assess(Ability a,Document source,CharacterBook.Entry entry,CharacterBook.Sheet sheet){
        Access access=Access.BY_TIER;List<String> reasons=new ArrayList<>();int tier=SpellText.tier(entry.tier());
        if(!LoreCatalogue.isCurrent(source)){access=Access.NOT_CURRENT;reasons.add("Source is not current; reference only.");}
        else if(a.sourceReference()){access=Access.CHECK_REQUIREMENTS;reasons.add("Full source reference; a structured spell/tier was not resolved for this search.");}
        else if(!a.stage().isEmpty()){
            String stage=SpellText.stage(entry.tier()+" "+entry.branch());if(stage.isEmpty()){access=Access.CHECK_REQUIREMENTS;reasons.add("Source stage: "+a.stage()+"; record the practice stage.");}
            else if(a.stage().equals("GREATER")&&stage.equals("LESSER")){access=Access.ABOVE_TIER;reasons.add("Requires the GREATER stage; recorded LESSER.");}
        }else if(a.minimumTier()<0){access=Access.CHECK_REQUIREMENTS;reasons.add("Minimum tier is not explicit in the indexed section; check its source requirements.");}
        else if(tier<0){access=Access.CHECK_REQUIREMENTS;reasons.add("Record a tier on this entry; source labels T"+a.minimumTier()+".");}
        else if(tier<a.minimumTier()){
            access=a.minimumTier()>5&&a.extraInvestment()?Access.CHECK_REQUIREMENTS:Access.ABOVE_TIER;
            reasons.add("Source tier T"+a.minimumTier()+"; recorded T"+tier+(a.minimumTier()>5?". Extended effect/investment tiers require reading the source.":"."));
        }
        if(!a.branch().isEmpty()){
            String branch=SpellText.fold(entry.branch());if(branch.isBlank()){if(access==Access.BY_TIER)access=Access.CHECK_REQUIREMENTS;reasons.add("Source branch: "+a.branch()+"; character branch not recorded.");}
            else if(!branch.contains(a.branch())){if(access!=Access.NOT_CURRENT)access=Access.OTHER_BRANCH;reasons.add("Source branch "+a.branch()+" differs from the recorded branch.");}
        }
        if(a.extraInvestment()&&sheet.budget().allocations().getOrDefault(SlotBudget.key(entry),-1)<2){if(access==Access.BY_TIER)access=Access.CHECK_REQUIREMENTS;reasons.add("Additional investment is marked by the source; record/check its second-slot and other requirements.");}
        if(reasons.isEmpty())reasons.add("Within the recorded tier/stage. Learning, branch conditions, training, resources and approval still require the source.");
        return new Hit(a,source,entry,access,String.join(" ",reasons));
    }
    public static String tierLabel(Ability a){return !a.stage().isEmpty()?a.stage():a.minimumTier()<0?"Tier not stated":"T"+a.minimumTier()+"+";}
    public static ReaderContent details(Hit hit){
        var a=hit.ability();var d=hit.source();String meta=a.name()+"\nPractice: "+hit.entry().name()+"\nSource tier/stage: "+tierLabel(a)+"\nRecorded tier: "+(hit.entry().tier().isBlank()?"Unknown":hit.entry().tier())+"\n"+hit.requirement()+"\n\nSource: "+d.url()+"\nSource snapshot: "+d.exportSnapshotAt()+"\n\nTier evidence: "+a.tierEvidence()+"\n\n";
        String text=meta+"SPELL / ABILITY INFORMATION, RULES AND REDLINES\n\n"+d.body().substring(a.start(),a.end());
        if(a.parentStart()>=0)text+="\n\nPARENT ABILITY CONTEXT\n\n"+d.body().substring(a.parentStart(),a.parentEnd());
        text+="\n\nUse Practice rules for shared restrictions and redlines. The excerpt is source text, not a shortened rules summary.";
        return ReaderContent.text("Spell information • exact main-source section",text);
    }
    public static ReaderContent practice(Hit hit){return ReaderContent.text("Practice rules, redlines and complete context",hit.source().body());}
    private static List<Ability> extract(Document d){
        Map<Integer,SourceSection> merged=new TreeMap<>();plainHeadings(d.body()).forEach(h->merged.put(h.start(),h));d.outline().forEach(h->merged.put(h.start(),h));
        List<SourceSection> outline=List.copyOf(merged.values());Map<String,TierEvidence> unlocked=unlockTable(d.body());List<Header> headers=new ArrayList<>();
        boolean area=false;int areaLevel=10,itemLevel=-1,groupTier=-1;String branch="",groupEvidence="";
        for(var section:outline){
            cancelled();String raw=section.title(),s=SpellText.heading(raw),name=name(raw);int direct=SpellText.minimumTier(raw);String stage=stageLabel(s);
            if(s.length()>180||s.startsWith("[IMAGE"))continue;
            String compact=SpellText.compact(raw);
            if(compact.equals("SUNPATH")||compact.equals("PATHOFTHESUN")){branch="SUN";continue;}
            if(compact.equals("MOONPATH")||compact.equals("PATHOFTHEMOON")){branch="MOON";continue;}
            boolean tierGroup=direct>=0&&(s.matches("(?:TIER\\s*|T)[IVX0-9]+(?: SPELLS| ABILITIES| CANTRIPS| RITUALS)?[: .]*")||s.contains("SPELLS")&&!BODY.matcher(s).find());
            if(tierGroup){groupTier=direct;groupEvidence=raw;area=true;continue;}
            if(END_AREA.matcher(s).find()&&s.length()<100){area=false;groupTier=-1;itemLevel=-1;continue;}
            if(ABILITY_AREA.matcher(s).find()&&s.length()<70&&!BODY.matcher(s).find()&&direct<0&&!TYPE.matcher(s).find()){
                area=true;areaLevel=section.level();itemLevel=-1;continue;
            }
            if(GENERIC.matcher(s).matches()||s.matches("T(?:IER\\s*)?[IVX0-9]+\\s*[:=-].*")||s.startsWith("A TIER ")||s.startsWith("AN ")||s.matches("(?:EMOTE |CASTING )?EXAMPLES?[: .]*"))continue;
            boolean metadata=TYPE.matcher(s).find()||!stage.isEmpty()||s.contains("[+]")||s.matches("CURSE OF .+ - SPIRIT OF .+");
            if(s.matches("(?:POSSIBLE SIDE EFFECTS|DEFINITIONS|EFFECTS OF SPIRITS)[: .]*")||s.startsWith("WRITING:")||s.startsWith("FORMATTING:"))continue;
            if(s.startsWith("-")||s.startsWith("•")||s.startsWith("⬢")||s.startsWith("➣")||!metadata&&(BODY.matcher(s).find()||s.contains(".")&&s.length()>65))continue;
            if(name.isEmpty()||name.length()>105||!name.matches(".*\\p{L}{3}.*")||GENERIC.matcher(name).matches())continue;
            boolean directHeader=direct>=0&&(s.contains("[")||TYPE.matcher(s).find()||!s.startsWith("TIER"));
            String following=following(d.body(),section.end());String metadataText=raw;
            if(following.length()<110&&TYPE.matcher("("+SpellText.fold(following)).find())metadataText+="\n"+following;
            boolean adjacent=!metadataText.equals(raw);
            TierEvidence mapped=unlocked.get(key(name));boolean shortName=name.split(" ").length<=9&&name.length()<85;
            boolean majorReference=area&&shortName&&!SpellText.heading(raw).equals(SpellText.fold(raw));
            if(area&&itemLevel>=0&&section.level()<Math.min(areaLevel,itemLevel)&&!majorReference){area=false;groupTier=-1;itemLevel=-1;}
            boolean sectionReference=area&&shortName&&(SpellText.decoratedHeading(raw)||(itemLevel<0?section.level()>areaLevel:section.level()==itemLevel));
            sectionReference|=majorReference;
            if(!directHeader&&!metadata&&!adjacent&&mapped==null&&!sectionReference)continue;
            if(!headers.isEmpty()&&direct<0&&!metadata){var last=headers.getLast();if(section.start()-last.section().end()<200&&d.body().substring(last.section().end(),section.start()).isBlank())continue;}
            int tier=direct;if(tier<0&&adjacent)tier=SpellText.minimumTier(metadataText);String evidence=tier>=0?metadataText:"Minimum tier not resolved; read full requirements.";
            if(tier<0&&mapped!=null){tier=mapped.tier();evidence=mapped.passage();}
            if(tier<0&&groupTier>=0){tier=groupTier;evidence=groupEvidence;}
            boolean extra=s.contains("[+]")||s.contains("SECOND SLOT")||s.contains("SECOND-SLOT");
            String requiredBranch=mapped!=null&&!mapped.branch().isEmpty()?mapped.branch():branch;
            headers.add(new Header(section,name,tier,stage,requiredBranch,extra,evidence));if(itemLevel<0||section.level()<itemLevel)itemLevel=section.level();if(headers.size()>=1000)break;
        }
        List<Ability> result=new ArrayList<>();Set<Integer> seen=new HashSet<>();
        for(int i=0;i<headers.size();i++){
            Header h=headers.get(i);if(!seen.add(h.section().start()))continue;int end=d.body().length();Header parent=null;
            for(int j=i+1;j<headers.size();j++)if(headers.get(j).section().level()<=h.section().level()){end=headers.get(j).section().start();break;}
            for(int j=i-1;j>=0;j--)if(headers.get(j).section().level()<h.section().level()){parent=headers.get(j);break;}
            int tier=h.tier();String evidence=h.evidence();String block=d.body().substring(h.section().start(),end);
            if(tier<0){var inferred=explicitLocalTier(block);if(inferred!=null){tier=inferred.tier();evidence=inferred.passage();}}
            int parentEnd=-1;if(parent!=null){parentEnd=d.body().length();for(var next:headers)if(next.section().start()>parent.section().start()&&next.section().level()<=parent.section().level()){parentEnd=next.section().start();break;}}
            result.add(new Ability(d.id()+":"+h.section().start(),d.id(),CharacterLore.mainFingerprint(d),h.name(),tier,h.stage(),h.branch(),h.extra(),h.section().start(),end,
                    parent==null?-1:parent.section().start(),parentEnd,evidence,false));
        }
        result.add(new Ability(d.id()+":source",d.id(),CharacterLore.mainFingerprint(d),"Full source reference",-1,"","",false,0,d.body().length(),-1,-1,"No automatic tier ruling is made for this source view.",true));return List.copyOf(result);
    }
    private static TierEvidence explicitLocalTier(String block){
        Matcher m=EXPLICIT_REQUIREMENT.matcher(SpellText.fold(block));if(m.find()){int n=SpellText.number(m.group(1));if(n>=0)return new TierEvidence(n,"Explicit ability prerequisite: "+m.group());}
        boolean progression=false;int minimum=99;String evidence="";
        for(String line:block.split("\\R")){
            String s=SpellText.fold(line);if(s.startsWith("TIER PROGRESSION")){progression=true;continue;}
            if(progression&&s.matches("[- ]*T(?:IER\\s*)?[IVX0-9]+\\s*[:=-].*")&&!s.contains("N/A")&&!s.contains("NOT AVAILABLE")){int n=SpellText.minimumTier(s);if(n>=0&&n<minimum){minimum=n;evidence=line;}}
        }return minimum==99?null:new TierEvidence(minimum,"Source progression row: "+evidence);
    }
    private static String following(String body,int end){int start=end;while(start<body.length()&&Character.isWhitespace(body.charAt(start)))start++;int next=body.indexOf('\n',start);if(next<0)next=body.length();return body.substring(start,Math.min(next,start+200));}
    private static String stageLabel(String header){return header.contains("[GREATER]")?"GREATER":header.contains("[LESSER]")?"LESSER":"";}
    private static String name(String raw){
        String s=SpellText.heading(raw).replaceAll("\\[(?:T(?:IER)?[^\\]]*|(?:NON[ -]?)?COMBAT[^\\]]*|PASSIVE[^\\]]*|ACTIVE[^\\]]*|[0-9]+[^\\]]*EMOTE[^\\]]*|LESSER|GREATER|[+]|S|C|NC)\\]","")
                .replaceAll("\\((?:NON[ -]?)?COMBAT[^)]*\\)","");
        s=s.split("\\s*[-:]\\s*(?:(?:NON[ -]?)?COMBAT|PASSIVE|ACTIVE|SPIRIT OF |T(?:IER)?\\s*[IVX0-9])",2)[0];
        return s.replace('[',' ').replace(']',' ').replaceAll("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$","").replaceAll("\\s+"," ").strip();
    }
    private static String key(String s){return SpellText.compact(s.replaceAll("\\[[^\\]]*\\]","")).replaceFirst("^(?:RITEOF|RITUALOF)","");}
    private static Map<String,TierEvidence> unlockTable(String body){
        Map<String,TierEvidence> result=new HashMap<>();int tier=-1,age=99;String evidence="";
        for(String line:body.split("\\R")){
            cancelled();String s=SpellText.fold(line);if(s.matches("(?:\\[[^\\]]*\\] )?TIER (?:[IVX0-9]+|ONE|TWO|THREE|FOUR|FIVE)(?:[: -].*)?")&&s.length()<150){tier=SpellText.minimumTier(s);age=0;evidence=line;continue;}
            if(!s.isBlank())age++;if(age>9||tier<0||s.length()>350)continue;
            if(s.contains("UNLOCKED:")||s.contains("UNLOCKS:")||s.contains("SPELLS:")||s.contains("RITUALS:")||s.startsWith("လ")&&s.contains(":")){
                for(String n:s.substring(s.indexOf(':')+1).split("[,;]")){String k=key(n),branch=n.contains("[BLADE]")?"BLADE":n.contains("[CONJURER]")?"CONJURER":"";if(k.length()>2&&!k.equals("NA")){var old=result.get(k);if(old==null||old.tier()>tier)result.put(k,new TierEvidence(tier,evidence+"\n"+line,branch));}}
            }
        }return result;
    }
    private static List<SourceSection> plainHeadings(String body){
        List<SourceSection> result=new ArrayList<>();int offset=0;
        for(String line:body.split("\n",-1)){
            if(!line.isBlank()&&line.length()<=180){String s=SpellText.fold(line);boolean direct=(s.contains("[")&&SpellText.minimumTier(s)>=0)||TYPE.matcher(s).find()||!stageLabel(s).isEmpty()||s.matches("CURSE OF .+ - SPIRIT OF .+");
                boolean spaced=!SpellText.heading(line).equals(s),decorated=SpellText.decoratedHeading(line)&&line.length()<100;
                if(direct||spaced||decorated||ABILITY_AREA.matcher(s).find()&&s.length()<65||s.matches("(?:TIER |T)[IVX0-9]+ SPELLS"))result.add(new SourceSection(line,offset,offset+line.length(),spaced?3:5));
            }offset+=line.length()+1;
        }return List.copyOf(result);
    }
    private static void cancelled(){if(Thread.currentThread().isInterrupted())throw new CancellationException("Spell search superseded");}
}
