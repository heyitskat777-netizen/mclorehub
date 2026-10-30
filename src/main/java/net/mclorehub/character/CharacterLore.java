package net.mclorehub.character;

import com.google.gson.*;
import net.mclorehub.catalogue.LoreCatalogue;
import net.mclorehub.catalogue.LoreCatalogue.Document;
import java.io.*;
import java.text.Normalizer;
import java.time.*;
import java.util.*;
import static net.mclorehub.importer.JsonSupport.*;

/** Small curated main-post ruleset. No inference from titles, categories or unlinked names. */
public final class CharacterLore {
    public enum Availability { AVAILABLE, MISSING, NEEDS_REVIEW, NOT_CURRENT }
    public enum Type { EXCLUSION, SOURCE_CONFLICT, BRANCH_EXCLUSION, CONDITIONAL_NOTICE }
    public enum Outcome { EXCLUSION, SOURCE_CONFLICT, NEEDS_REVIEW, REQUIREMENTS, NOT_ASSESSED }
    public record Source(String id,String title,String url,String revision,String mainFingerprint,String snapshot,List<String> passages) {
        public Source { passages=List.copyOf(passages); }
    }
    public record Option(String key,String name,CharacterBook.Kind kind,String sourceId,String notes) {}
    public record Rule(String key,Type type,List<String> left,List<String> right,List<String> sources,String summary) {
        public Rule { left=List.copyOf(left);right=List.copyOf(right);sources=List.copyOf(sources); }
    }
    public record Choice(Option option,Source source,Availability availability) {}
    public record Finding(Outcome outcome,String message,List<Source> sources) {
        public Finding { sources=List.copyOf(sources); }
    }
    public record Report(List<Finding> findings,int checkedPairs,int totalPairs,int unresolvedEntries) {
        public Report { findings=List.copyOf(findings); }
        public String summary() {
            long conflicts=findings.stream().filter(f->f.outcome()==Outcome.EXCLUSION).count();
            long review=findings.stream().filter(f->f.outcome()==Outcome.SOURCE_CONFLICT||f.outcome()==Outcome.NEEDS_REVIEW).count();
            return conflicts+" conflict(s), "+review+" need review; partial coverage";
        }
    }
    private final Map<String,Source> sources;
    private final Map<String,Document> documents;
    private final List<Option> options;
    private final List<Rule> rules;
    private final String scope;
    private final Map<String,Availability> availability;
    private final Map<String,String> searchText;
    private CharacterLore(Map<String,Source> sources,List<Option> options,List<Rule> rules,List<Document> documents,String scope) {
        this.sources=Map.copyOf(sources);this.options=List.copyOf(options);this.rules=List.copyOf(rules);this.scope=scope;
        Map<String,Document> byId=new HashMap<>();documents.forEach(d->byId.put(d.id(),d));this.documents=Map.copyOf(byId);
        Map<String,Availability> checked=new HashMap<>();sources.keySet().forEach(id->checked.put(id,checkAvailability(id)));availability=Map.copyOf(checked);
        Map<String,String> searchable=new HashMap<>();for(var option:options){var doc=byId.get(option.sourceId());
            searchable.put(option.key(),normalize(option.name()+" "+(doc==null?"":doc.title()+" "+doc.family()+" "+doc.category()+" "+String.join(" ",doc.aliases()))));}
        searchText=Map.copyOf(searchable);
    }
    /** Called only on the library file worker, including after refresh and Undo. */
    public static CharacterLore bundled(LoreCatalogue catalogue) throws IOException {
        try(var in=CharacterLore.class.getResourceAsStream("lore-rules.json")) {
            if(in==null)throw new IOException("Bundled character rules are missing");
            return parse(net.mclorehub.importer.JsonSupport.parse(bounded(in,256<<10)),catalogue.documents());
        }
    }
    public static CharacterLore parse(JsonObject root,List<Document> documents) throws IOException {
        if(integer(root,"schema_version")!=1)throw new IOException("Unsupported character rules");
        LocalDate.parse(text(root,"curated_on"));String scope=text(root,"scope");Map<String,Source> sources=new LinkedHashMap<>();
        for(var value:array(root.get("sources"),"sources")) {
            var s=object(value,"source");String id=text(s,"id"),url=text(s,"url");forumUrl(url);topic(id);
            Instant.parse(text(s,"snapshot"));
            var source=new Source(id,text(s,"title"),url,sha(s,"revision"),sha(s,"main_fingerprint"),text(s,"snapshot"),strings(s,"passages"));
            if(source.passages().stream().anyMatch(String::isBlank)||sources.putIfAbsent(id,source)!=null)throw new IOException("Invalid/duplicate rule source");
        }
        List<Option> options=new ArrayList<>();Set<String> keys=new HashSet<>(),bindings=new HashSet<>();
        for(var value:array(root.get("options"),"options")) {
            var o=object(value,"option");var option=new Option(text(o,"key"),text(o,"name"),CharacterBook.Kind.valueOf(text(o,"kind")),text(o,"source_id"),text(o,"notes"));
            if(!sources.containsKey(option.sourceId())||!keys.add(option.key())||!bindings.add(option.kind()+option.sourceId()))throw new IOException("Invalid/duplicate character option");
            options.add(option);
        }
        List<Rule> rules=new ArrayList<>();Set<String> ruleIds=new HashSet<>();
        for(var value:array(root.get("rules"),"rules")) {
            var r=object(value,"rule");var rule=new Rule(text(r,"key"),Type.valueOf(text(r,"type")),strings(r,"left"),strings(r,"right"),strings(r,"sources"),text(r,"summary"));
            if(!ruleIds.add(rule.key())||rule.left().isEmpty()||rule.right().isEmpty()||rule.sources().isEmpty()
                    ||!keys.containsAll(rule.left())||!keys.containsAll(rule.right())||!sources.keySet().containsAll(rule.sources()))throw new IOException("Invalid rule references");
            for(var source:rule.sources())if(sources.get(source).passages().isEmpty())throw new IOException("A rule requires main-post evidence");
            for(var option:options)if((rule.left().contains(option.key())||rule.right().contains(option.key()))&&!rule.sources().contains(option.sourceId()))throw new IOException("Rule omits option revision dependency");
            rules.add(rule);
        }
        return new CharacterLore(sources,options,rules,documents,scope);
    }
    private static void topic(String id)throws IOException {if(!id.matches("lotc:topic:[1-9][0-9]{0,17}"))throw new IOException("Invalid character source ID");}
    public String scope(){return scope;}
    public List<Rule> rules(){return rules;}
    public Optional<Document> document(String id){return Optional.ofNullable(documents.get(id));}
    public Availability availability(String id) {return availability.getOrDefault(id,Availability.MISSING);}
    private Availability checkAvailability(String id) {
        var source=sources.get(id);var doc=documents.get(id);
        if(source==null||doc==null)return Availability.MISSING;
        if(!LoreCatalogue.isCurrent(doc))return Availability.NOT_CURRENT;
        if(!source.revision().equals(doc.revision())||!source.mainFingerprint().equals(mainFingerprint(doc))||!source.url().equals(doc.url())
                ||source.passages().stream().anyMatch(p->!doc.body().contains(p)))return Availability.NEEDS_REVIEW;
        return switch(doc.role()) {
            case PLAYER_OPTION_CANDIDATE, CORE_AND_HUB -> Availability.AVAILABLE;
            default -> Availability.NEEDS_REVIEW;
        };
    }
    /** Same main-post binding before/after a no-change refresh; excludes check dates and archived discussion. */
    public static String mainFingerprint(Document d) {
        JsonObject r=new JsonObject();r.addProperty("id",d.id());r.addProperty("title",d.title());r.addProperty("url",d.url());
        r.addProperty("author",d.author().orElse(""));r.addProperty("revision",d.revision());r.addProperty("family",d.family());r.addProperty("category",d.category());r.addProperty("role",d.role().name());
        return fingerprint(r);
    }
    public List<Choice> choices(CharacterBook.Kind kind,String query) {
        String[] terms=normalize(query).split(" ");
        return options.stream().filter(o->o.kind()==kind).filter(o->Arrays.stream(terms).allMatch(searchText.get(o.key())::contains))
                .sorted(Comparator.comparing(Option::name).thenComparing(Option::key))
                .map(o->new Choice(o,sources.get(o.sourceId()),availability(o.sourceId()))).toList();
    }
    public Optional<Choice> resolve(CharacterBook.Entry entry) {
        return options.stream().filter(o->o.kind()==entry.kind()&&o.sourceId().equals(entry.loreDocumentId())).findFirst()
                .map(o->new Choice(o,sources.get(o.sourceId()),availability(o.sourceId())));
    }
    public CharacterBook.Entry link(CharacterBook.Entry entry,String optionKey) {
        var option=options.stream().filter(o->o.key().equals(optionKey)&&o.kind()==entry.kind()).findFirst().orElseThrow(()->new IllegalArgumentException("Option does not match entry kind"));
        if(availability(option.sourceId())!=Availability.AVAILABLE)throw new IllegalArgumentException("Lore option needs review or is unavailable");
        return new CharacterBook.Entry(entry.id(),entry.kind(),option.name(),entry.branch(),entry.tier(),entry.applicationUrl(),entry.status(),entry.notes(),option.sourceId());
    }
    public Report assess(CharacterBook.Sheet sheet) {
        List<Finding> findings=new ArrayList<>();Map<String,Choice> choices=new HashMap<>();Set<String> noted=new HashSet<>();int unresolved=0;
        for(var e:sheet.entries()) {
            var resolved=resolve(e);
            if(resolved.isEmpty()) { unresolved++;findings.add(new Finding(Outcome.NOT_ASSESSED,e.name()+": no curated lore link; not assessed.",List.of()));continue; }
            var choice=resolved.get();choices.put(e.id(),choice);
            if(choice.availability()!=Availability.AVAILABLE) { unresolved++;findings.add(new Finding(Outcome.NEEDS_REVIEW,e.name()+": source "+choice.availability().name().toLowerCase(Locale.ROOT).replace('_',' ')+"; checks suspended, sheet retained.",List.of(choice.source()))); }
            else if(noted.add(choice.option().key()))findings.add(new Finding(Outcome.REQUIREMENTS,choice.option().name()+": "+choice.option().notes(),List.of(choice.source())));
        }
        int checked=0,total=sheet.entries().size()*(sheet.entries().size()-1)/2;
        for(int i=0;i<sheet.entries().size();i++)for(int j=i+1;j<sheet.entries().size();j++) {
            var a=sheet.entries().get(i);var b=sheet.entries().get(j);var ac=choices.get(a.id());var bc=choices.get(b.id());
            if(ac==null||bc==null)continue;boolean covered=false;
            for(var rule:rules) {
                boolean matches=rule.left().contains(ac.option().key())&&rule.right().contains(bc.option().key())
                        ||rule.left().contains(bc.option().key())&&rule.right().contains(ac.option().key());
                if(!matches)continue;
                var evidence=rule.sources().stream().map(sources::get).toList();
                if(rule.sources().stream().anyMatch(s->availability(s)!=Availability.AVAILABLE))continue;
                if(rule.type()==Type.BRANCH_EXCLUSION&&!(Set.of("sun","moon").contains(path(a.branch()))&&Set.of("sun","moon").contains(path(b.branch()))&&!path(a.branch()).equals(path(b.branch()))))continue;
                covered=true;Outcome outcome=switch(rule.type()) {
                    case SOURCE_CONFLICT -> Outcome.SOURCE_CONFLICT;
                    case CONDITIONAL_NOTICE -> Outcome.REQUIREMENTS;
                    default -> Outcome.EXCLUSION;
                };
                findings.add(new Finding(outcome,a.name()+" + "+b.name()+": "+rule.summary(),evidence));
            }
            if(covered)checked++;
        }
        findings.add(new Finding(Outcome.NOT_ASSESSED,(total-checked)+" of "+total+" entry pairs have no applicable current check. Requirements, exceptions, slots, acquisition history and application approvals remain manually assessed. Zero warnings does not establish compatibility.",List.of()));
        findings.sort(Comparator.comparing(Finding::outcome));
        return new Report(findings,checked,total,unresolved);
    }
    private static String normalize(String text){return Normalizer.normalize(text,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replace('\u2019','\'').replace('\u2018','\'').replaceAll("[\\s\\p{Z}]+"," ").strip();}
    private static String path(String value){return normalize(value).replaceFirst("^(?:path of (?:the )?|(?:the )?path of )","");}
}
