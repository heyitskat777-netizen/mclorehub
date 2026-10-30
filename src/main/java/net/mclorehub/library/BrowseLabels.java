package net.mclorehub.library;

import java.util.Locale;
import java.util.Map;

/** Display labels only: source titles/categories and their provenance stay intact. */
public final class BrowseLabels {
    private BrowseLabels() {}
    private static final Map<String,String> CATEGORIES=Map.ofEntries(
            Map.entry("alchemy","Alchemy"),Map.entry("materials","Materials"),
            Map.entry("flora","Plants & botany"),Map.entry("racial_lore","Races & creatures"),
            Map.entry("deity","Deific magic"),Map.entry("voidal","Voidal magic"),
            Map.entry("dark","Dark magic"),Map.entry("natural","Natural magic"),
            Map.entry("magical","Magical creatures & lore"),Map.entry("spectral_undead","Spectral & undead"),
            Map.entry("magic_related","Magic feats & references"),Map.entry("magic_inventions","Magical inventions"),
            Map.entry("inventions","Inventions"),Map.entry("techlock","Technology rules"),
            Map.entry("origin_backstory","Origins & history"),Map.entry("misc","Other lore"),
            Map.entry("character_artifacts","Character artifacts (CArts)"),Map.entry("magical_artifacts","Magical artifacts (MArts)"));
    public static String category(String value) {
        if(value.isEmpty())return "All categories";
        String fallback=value.substring(0,1).toUpperCase(Locale.ROOT)+value.substring(1).replace('_',' ');
        return CATEGORIES.getOrDefault(value,fallback);
    }
    public static String title(String value) {
        String clean=value.replaceFirst("^(?:\\s*\\[[^\\]]{1,80}\\]\\s*)+","").strip();
        return clean.isEmpty()?value:clean;
    }
}
