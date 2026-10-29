# Phase 4 — In-game catalogue, search, favorites and reader

Implemented asynchronous pack/review loading, deterministic Unicode title/alias/body
and reply search, categories, pagination, current-only defaults, explicit audit
browsing, source/review details, plain-text reader, favorites and reading resume.
The core is nested in the Fabric JAR; no separate importer JAR is needed in mods.

## Installation and use

Use Java 21, Fabric Loader 0.20.1+ and Fabric API 0.141.6+1.21.11 for exactly
Minecraft 1.21.11. Put the built `client/build/libs/mclorehub-0.1.0-phase4.jar`
in the instance's `mods` directory. Never install test/source/importer JARs as mods.

Generate a candidate with the existing importer CLI, then, with Minecraft closed,
copy its `pack.json` into the instance's `config/mclorehub/packs/active/` directory.
Optionally supply a separate `reviews.json` there using the Phase 3 schema. Keep
the original files/previous pack backed up before replacing them. This manual
installation is not the future transactional in-game update feature.

K opens the library (or restores the last available document after data has loaded).
Search is an AND query over Unicode-normalized titles/aliases/main text/replies;
exact title and alias matches rank first. Category and favorites filters intersect
the selected scope. Normal results require applied accepted/current reviews.
Audit explicitly includes unreviewed, stale and historical records. Missing favorite
references remain saved and are hidden when their documents are unavailable.

The reader preserves available plain text, paragraphs, Unicode and attributed
reply blocks. Use the wheel, arrows, Page Up/Down or Home/End to scroll; Previous/
Next move between source sections. Layout is bounded to 6000 source characters per
section. Source and review shows URLs, authors, snapshot dates, roles, coverage gaps,
reviewers/dates/evidence. Copy source URL does not launch a browser or fetch anything.
Colors/images/fonts/redlines absent from the export cannot be reconstructed.

Favorites and last reading position live in `config/mclorehub/personal/state.json`.
Resume uses document ID, full source fingerprint, body/reply block ID and source
character offset. Changed revisions/missing blocks reset safely; missing documents
do not delete the saved reference. Previously audited documents remain labeled audit
on resume. Current documents whose review becomes stale require explicit audit access.
Font-size changes restore the nearest visible source line.

Writes use staged atomic replacement, previous-good `state.previous.json`, strict
validation and external-edit detection. Invalid/future/corrupt personal data is
read-only, never silently replaced. Back up files before manual recovery; Reload
files retries both source and personal state. Invalid/missing pack reloads retain
the last valid in-memory library and expose the error. Runtime changes are not an
atomic cross-file activation/rollback protocol yet.

## Verified gate

- Full build/install passed with pinned dependencies and checksum verification.
- **123 core tests**, no failures/errors/skips: importer/catalogue plus search,
  immutable reply projections, atomic persistence, reload failure recovery, stable
  reader chunks and revision/offset fallbacks.
- Native Minecraft scenarios passed with locked dependencies and no rewrite flags:
  original hotkey/navigation checks plus actual search typing, current/audit filters,
  reader/replies/details, favorite persistence, service restart/resume, GUI scales
  1/2 and invalid reload retaining the last good library. Screenshots inspected.
- The actual 488-record export loaded/search-rendered in the native client in
  approximately 1 second in this environment; all real entries remained unreviewed.
  This is a single-run observation, not a performance guarantee.
- Client lock regeneration only added the already trusted annotation dependency to
  the newly connected core's game-test runtime configuration; no version/pin or
  checksum verification bypass was used.

No new real lore decisions, compatibility rules, rich-source formatting, entity
eligibility taxonomy or forum fetching are implied by this phase. Audio/authenticated
multiplayer/physical hardware/user modpack behavior remains untested.
