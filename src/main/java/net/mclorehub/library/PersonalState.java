package net.mclorehub.library;

import java.util.*;

/** Source-independent references survive missing documents and pack replacements. */
public record PersonalState(Set<String> favorites, Optional<ReadingPosition> reading) {
    public record ReadingPosition(String documentId, String fingerprint, String blockId, int offset, boolean audit) {
        public ReadingPosition {
            validId(documentId);
            if (fingerprint == null || !fingerprint.matches("[a-f0-9]{64}") || blockId == null
                    || !blockId.matches("body|post:[1-9][0-9]{0,17}") || offset < 0 || offset > 64 * 1024 * 1024)
                throw new IllegalArgumentException("Invalid reading position");
        }
    }
    public PersonalState {
        favorites = Collections.unmodifiableSet(new TreeSet<>(favorites)); Objects.requireNonNull(reading);
        if (favorites.size() > 10_000) throw new IllegalArgumentException("Too many favorites");
        favorites.forEach(PersonalState::validId);
    }
    private static void validId(String id) {
        if (id == null || !id.matches("lotc:topic:[1-9][0-9]{0,17}")) throw new IllegalArgumentException("Invalid document reference");
    }
    public static PersonalState empty() { return new PersonalState(Set.of(), Optional.empty()); }
    public PersonalState toggle(String id) {
        validId(id); Set<String> changed = new TreeSet<>(favorites);
        if (!changed.remove(id)) changed.add(id);
        return new PersonalState(changed, reading);
    }
    public PersonalState remember(ReadingPosition position) { return new PersonalState(favorites, Optional.of(position)); }
}
