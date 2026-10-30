# Architecture

The root Java application is independent of Minecraft. `client/` contains Fabric
metadata and client source sets. `-PcoreOnly` skips the client subproject. The client
depends on and nests the catalogue core JAR. `LibraryService` owns separate file
and search workers; screens consume immutable snapshots/futures. All reads, source
validation, indexing, searching and personal writes run away from the render thread.
Only bounded font layout of a reader section runs in the screen.

```text
Source ZIP/JSON -> ExportImporter -> immutable candidate directory
Candidate + optional reviews/user trust -> LoreCatalogue -> current documents + audit queues
Main-post fetch -> validated snapshot/catalogue/index -> atomic topic pointer + Undo
Client UI -> LibraryService -> catalogue/search/reader/character-lore/personal services
Personal state remains outside all source/import/update transactions
```

`JsonSupport` provides bounded strict UTF-8/JSON parsing, duplicate-key/depth guards,
checksums, immutable string lists, safe new-file writes, and sorted-key fingerprints.
`ExportImporter` preserves raw records and all export provenance, with plain-text
reader blocks and explicit review/formatting gaps. `LoreCatalogue` validates that
projection against raw evidence and exposes immutable document/review records.
`CatalogueCommand` writes a separate new audit report via sibling staging.

Normal wiki/search consumers must use `currentDocuments()`. `documents()` is the
complete audit catalogue, including unknown and historical records. Source roles
are candidates, not a final selectable magic/CA/FA entity taxonomy.

Runtime storage resolves Fabric's actual config directory. Keep `packs/`,
`staging/`, derived `cache/`, `backups/`, and `personal/` (characters, favorites,
reading-state) separate. Schema versions are independent. Never hardcode an OS's
Minecraft path or delete unresolved references. Runtime I/O/indexing/HTTP must be
off the render thread. No processes are assumed to survive a cloud snapshot.

Implemented paths include `packs/active/{pack.json,trust.json,reviews.json}`,
`updates/{active.json,snapshots/,monthly.json}`, `personal/state.json` and
`personal/characters.json`. Optional credentials live separately under `auth/`.
Whole-pack activation UI remains future work. `CharacterService` owns a separate
worker and immutable `CharacterBook`; its schema and atomic previous-file backup
are independent of reading/favorites and lore. Application status is user-recorded;
there is no automatic eligibility or application-approval inference.

Phase 8's `CharacterLore` is an immutable curated projection created with each
library on its file worker. The picker filters explicit kinds and links stable
document IDs. `LibraryService.assess` evaluates an immutable sheet on that worker;
screens consume futures and invalidate by library/character generations. Rules
bind main-post body/metadata and quoted evidence, excluding archived replies and
refresh timestamps. Phase 9's 26 rules distinguish exclusions, source disagreements
and conditional requirements, with independent source dependencies. Picker search
also projects source titles/family/category and apostrophe variants. Keep unknown
coverage visible; no positive compatibility or full eligibility engine is implied.
See PHASE_9.md for current scope.

Phase 10 adds immutable `CharacterFacts` and `HistoryEvent` values to sheets.
`CharacterStore` reads versions 1/2 and writes version 2 only on explicit save,
retaining exact old bytes through `AtomicJsonFile`. All book updates carry the new
fields. `CharacterEligibility` evaluates seven explicit source-bound rules on the
existing library file worker. Events do not mutate current facts or derive a
timeline. See PHASE_10.md for conditions, schema/migration and remaining limits.

Per-topic activation validates a snapshot, prepares the catalogue/search/character
projection, then atomically switches the pointer and publishes the prepared library.
Stored source/base IDs and checksums bind updates to the correct baseline. Manual
and monthly checks share that pipeline. Optional sessions only authenticate
authorized requests; no sign-in or access bypass is inferred. See PHASE_7.md.
