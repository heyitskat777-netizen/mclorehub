package net.mclorehub.library;

import net.mclorehub.catalogue.LoreCatalogue.Document;
import net.mclorehub.catalogue.LoreCatalogue;
import java.util.*;

/** Exact plain-text chunks bound to stable source block IDs. Font layout stays small. */
public record ReaderContent(List<Section> sections) {
    public static String statusLabel(LoreCatalogue.Status status){return switch(status){
        case ACCEPTED_CURRENT->"Accepted/current";case UNKNOWN->"Unknown";case LEGACY->"Legacy";
        case SHELVED->"Shelved";case SUPERSEDED->"Superseded";case PENDING->"Pending";};}
    public static String reviewLabel(LoreCatalogue.ReviewState review){return switch(review){
        case UNREVIEWED->"Not reviewed";case APPLIED->"Local review recorded";case STALE->"Review stale";
        case USER_TRUSTED->"Trusted supplied database";case SOURCE_REFRESHED->"Forum source checked";};}
    public record Section(String blockId, int offset, String heading, String text) {}
    public record Resume(int section, int offset, boolean changed) {}
    public ReaderContent { sections=List.copyOf(sections); }
    public static ReaderContent text(String heading,String text){var sections=new ArrayList<Section>();add(sections,"report",heading,text);return new ReaderContent(sections);}
    public static ReaderContent comparison(Document before,Document after){
        var sections=new ArrayList<Section>();add(sections,"diff","Changes (- previous / + current)",LoreDiff.compare(before,after).text());
        add(sections,"previous","Full previous main post",before.body());add(sections,"current","Full current main post",after.body());return new ReaderContent(sections);
    }
    public static ReaderContent document(Document doc) {
        List<Section> result=new ArrayList<>();add(result,"body","Main post",doc.body());
        return new ReaderContent(result);
    }
    public static ReaderContent discussion(Document doc) {
        List<Section> result=new ArrayList<>();
        doc.posts().forEach(p->add(result,"post:"+p.id(),"Reply " + p.id() + " — " + (p.author().isBlank()?"Unknown author":p.author())
                + (doc.coverageGaps().contains("previous_reply_not_returned:"+p.id())?" (previously captured; not returned by latest refresh)":" (discussion is not automatically a ruling)"),p.text()));
        if(result.isEmpty())add(result,"body","Archived discussion","No archived replies. Main-post updates do not fetch discussion.");
        return new ReaderContent(result);
    }
    public static ReaderContent details(Document doc) {
        StringBuilder text=new StringBuilder("Source: ").append(doc.url()).append("\nAuthor: ").append(doc.author().orElse("Unknown"))
                .append("\nExport snapshot: ").append(doc.exportSnapshotAt()).append("\nCategory: ").append(doc.category())
                .append("\nSource role: ").append(doc.role().name().toLowerCase(Locale.ROOT).replace('_',' ')).append("\nStatus: ").append(statusLabel(doc.status())).append(" • ").append(reviewLabel(doc.reviewState()))
                .append("\nAliases: ").append(String.join(", ",doc.aliases())).append("\n\nPlain-text source: original colors, images and layout are unavailable.")
                .append("\nCoverage gaps:\n").append(String.join("\n",doc.coverageGaps()));
        doc.decision().ifPresent(d->{text.append("\n\nLocal review by ").append(d.reviewer()).append(" at ").append(d.reviewedAt())
                .append("\nRecorded status: ").append(d.status()).append("\nEvidence is user-recorded; authority and quotations are not automatically verified.");
            if(doc.reviewState()!=net.mclorehub.catalogue.LoreCatalogue.ReviewState.APPLIED)text.append("\nSTALE: this decision does not certify the current source.");
            d.evidence().forEach(e->text.append("\n\n").append(e.url()).append("\n").append(e.passage()));});
        if(!doc.posts().isEmpty())text.append("\n\nArchived replies: ").append(doc.posts().size()).append(". Excluded from normal reading/search and not checked by main-post updates.");
        doc.trust().ifPresent(t -> text.append("\n\nReference basis: ").append(t.basis()).append("\nRecorded/checked: ").append(t.checkedAt())
                .append("\nSource pages:\n").append(String.join("\n",t.pages())));
        List<Section> result=new ArrayList<>();add(result,"body","Source and review details",text.toString());return new ReaderContent(result);
    }
    private static void add(List<Section> result,String id,String heading,String text){
        if(text.isEmpty()){result.add(new Section(id,0,heading,""));return;}
        for(int start=0;start<text.length();){
            int end=Math.min(start+6000,text.length());
            if(end<text.length()){
                int newline=text.lastIndexOf('\n',end-1);if(newline>start+4000)end=newline+1;
                if(Character.isHighSurrogate(text.charAt(end-1))&&Character.isLowSurrogate(text.charAt(end)))end--;
            }
            result.add(new Section(id,start,heading,text.substring(start,end)));start=end;
        }
    }
    public Resume resume(Document doc,Optional<PersonalState.ReadingPosition> position){
        if(position.isEmpty()||!position.get().documentId().equals(doc.id()))return new Resume(0,0,false);
        var p=position.get();if(!p.fingerprint().equals(doc.fingerprint()))return new Resume(0,0,true);
        for(int i=sections.size()-1;i>=0;i--){var s=sections.get(i);
            if(s.blockId().equals(p.blockId())&&p.offset()>=s.offset())return new Resume(i,Math.min(p.offset(),s.offset()+s.text().length()),false);}
        return new Resume(0,0,true);
    }
}
