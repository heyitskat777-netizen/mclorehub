package net.mclorehub.library;

import net.mclorehub.catalogue.LoreCatalogue;
import net.mclorehub.catalogue.LoreCatalogue.Document;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.CancellationException;

/** Immutable, offline search projection. Normal results require applied current reviews. */
public final class LoreSearch {
    public record Query(String text, String category, boolean audit, boolean favoritesOnly,
                        Set<String> favorites, int offset, int limit) {
        public Query {
            Objects.requireNonNull(text); Objects.requireNonNull(category);
            favorites = Set.copyOf(favorites);
            if (text.length() > 256 || offset < 0 || limit < 1 || limit > 100)
                throw new IllegalArgumentException("Invalid search bounds");
        }
    }
    public record Hit(Document document, String snippet) {}
    public record Page(List<Hit> hits, int total, int offset) {
        public Page { hits = List.copyOf(hits); }
    }
    private record Entry(Document document, String title, List<String> aliases, String body, String originalBody) {}
    private record Ranked(Entry entry, int rank) {}
    private final List<Entry> entries;
    public LoreSearch(LoreCatalogue catalogue) {
        List<Entry> result = new ArrayList<>();
        for (Document d : catalogue.documents()) {
            cancelled();
            StringBuilder full = new StringBuilder(d.body()); d.posts().forEach(p -> full.append('\n').append(p.text()));
            String body = full.toString();
            result.add(new Entry(d, normalize(d.title()), d.aliases().stream().map(LoreSearch::normalize).toList(), normalize(body), body));
        }
        entries = List.copyOf(result);
    }
    public static boolean isCurrent(Document d) {
        return LoreCatalogue.isCurrent(d);
    }
    public List<String> categories(boolean audit) {
        return entries.stream().filter(e -> audit || isCurrent(e.document())).map(e -> e.document().category()).distinct().sorted().toList();
    }
    public Page search(Query query) {
        String phrase = normalize(query.text());
        List<String> terms = phrase.isEmpty() ? List.of() : Arrays.asList(phrase.split(" "));
        List<Ranked> found = new ArrayList<>();
        for (Entry e : entries) {
            cancelled(); Document d = e.document();
            if ((!query.audit() && !isCurrent(d)) || (!query.category().isEmpty() && !d.category().equals(query.category()))
                    || (query.favoritesOnly() && !query.favorites().contains(d.id()))) continue;
            String aliases = String.join(" ", e.aliases());
            if (!terms.stream().allMatch(t -> e.title().contains(t) || aliases.contains(t) || e.body().contains(t))) continue;
            int rank = phrase.isEmpty() ? 0 : e.title().equals(phrase) ? 0 : e.aliases().contains(phrase) ? 1
                    : e.title().contains(phrase) ? 2 : e.aliases().stream().anyMatch(a -> a.contains(phrase)) ? 3
                    : terms.stream().allMatch(t -> e.title().contains(t) || aliases.contains(t)) ? 4 : 5;
            found.add(new Ranked(e, rank));
        }
        found.sort(Comparator.comparingInt(Ranked::rank).thenComparing(r -> r.entry().title()).thenComparing(r -> r.entry().document().id()));
        int start = Math.min(query.offset(), found.size());
        List<Hit> hits = new ArrayList<>();
        for (Ranked r : found.subList(start, Math.min(start + query.limit(), found.size()))) {
            cancelled(); hits.add(new Hit(r.entry().document(), snippet(r.entry().originalBody(), terms)));
        }
        return new Page(hits, found.size(), start);
    }
    private static String snippet(String body, List<String> terms) {
        // Indices in normalized Unicode are not source indices; select original sentences instead.
        String compact = body.replaceAll("\\s+", " ").strip();
        int start = 0;
        for (int i = 0; !terms.isEmpty() && i < compact.length(); i = advance(compact,i,120)) {
            cancelled(); int end = advance(compact,i,160);
            if (terms.stream().anyMatch(normalize(compact.substring(i, end))::contains)) { start = i; break; }
        }
        int end = advance(compact,start,180);
        return (start > 0 ? "…" : "") + compact.substring(start, end) + (end < compact.length() ? "…" : "");
    }
    private static int advance(String value,int start,int codePoints) {
        int end=start;
        for(int i=0;i<codePoints&&end<value.length();i++)end+=Character.charCount(value.codePointAt(end));
        return end;
    }
    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replaceAll("[\\s\\p{Z}]+", " ").strip();
    }
    private static void cancelled() { if (Thread.currentThread().isInterrupted()) throw new CancellationException("Search superseded"); }
}
