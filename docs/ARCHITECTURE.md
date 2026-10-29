# Architecture

The root Java application is independent of Minecraft. `client/` contains Fabric
metadata and client source sets. `-PcoreOnly` skips the client subproject. The client
depends on and nests the catalogue core JAR. `LibraryService` owns separate file
and search workers; screens consume immutable snapshots/futures. All reads, source
validation, indexing, searching and personal writes run away from the render thread.
Only bounded font layout of a reader section runs in the screen.

```text
Source ZIP/JSON -> ExportImporter -> immutable candidate directory
Candidate + separate reviews -> LoreCatalogue -> current documents + audit queues
Client UI -> LibraryService -> catalogue/search/reader/personal services
Future: fetch -> staging -> review -> validated pack/index -> active pointer
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

Implemented paths are `packs/active/pack.json`, optional `reviews.json`,
`personal/state.json` and `personal/characters.json`. Staging/index-cache/pack-rollback
directories are future update infrastructure. `CharacterService` owns a separate
worker and immutable `CharacterBook`; its schema and atomic previous-file backup
are independent of reading/favorites and lore. Application status is user-recorded;
there is no compatibility evaluator or automatic eligibility inference yet.

Future activation: validate candidate -> review differences/status -> build index
-> write immutable revision -> atomically switch pointer -> reopen/check -> retain
rollback. Current report writes do not implement that active-pack transaction.
