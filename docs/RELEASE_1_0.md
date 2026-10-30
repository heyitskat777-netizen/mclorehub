# Release 1.0.0 — completed implementation and acceptance

The user authorized completing the remaining implementation and polish, excluding
real MArt account access testing. Work continues through the test gates in one task;
environment publication remains deferred and is not a prerequisite for code delivery.

## Delivered scope

| Requested area | Result |
| --- | --- |
| Offline in-game wiki | Main text/title/alias search, categories, favorites, source details, explicit discussion archive. |
| Reading continuity | Last document and stable scroll restoration; comparisons/archives do not overwrite it. |
| Updates | Selected main-post fetch, persistent comparisons/snapshots/Undo, monthly due dates/catch-up/pause/rate limits. |
| Character sheets | Multiple characters, magic/CA/FA references, branch/tier, links/status, notes, archive/restore. |
| Broader source coverage | 71 selected references from 67 source documents, 178 relationship rules and preserved disagreements. |
| Eligibility/history | Nine explicit facts, independent event history, seven fact/history checks and 14 prerequisite policies. |
| Slot accounting | 48 curated policies, intervals/allocations, duplicate grouping, tier/optional costs, permanent loss and limited Voidstalker allowance. |
| Import polish | Original/normalized ZIP/JSON drop, confirmation/preview, per-row full comparisons, atomic activation and database Undo. |
| Comparison polish | Added/removed/context markers and totals, metadata changes, bounded diff, complete before/after main text. |
| Persistence/recovery | Schema-1/2 read-only migration to 3 on save, exact prior-file backup, independent data files and checksum-bound revisions. |

These features are implemented. Canon interpretation is deliberately not converted
into a blanket positive eligibility state. Some source text is inconsistent or
omits a cost/condition; those cases remain visible rather than becoming fabricated
rules. Real forum account permissions and an unspecified external modpack are not
established by offline/synthetic checks.

## Character rule and arithmetic details

Every selected source and rule uses exact main text plus a metadata/body binding.
Changed/non-current sources suspend their checks without deleting personal entries;
manual numeric allocations remain explicitly user-recorded. Source titles/categories
are search fields, not automatic eligibility classifications. Hubs, inventions,
ordinary materials and CArts/MArts are not extra magics or application permissions.
Shared-source creature variants stay references where their identities cannot be
collapsed into one rule. See [coverage](CHARACTER_LORE_COVERAGE.md).

The inventory covers core practices, feats, referenced extensions and creature
sources in the supplied snapshot. Marriage references do not create a separate MA;
parent practices still need recording. Prerequisite cards distinguish absent sheet
data from an actual lack, honor the Sprite/Herblore alternative, compare known tiers,
retain Greater Blood Magic requirements and show Voidal investment requirements.
All training, acquisition and consent details still require the cited source.

Slot arithmetic covers recorded magic/feat entries; capacity must include current
body/creature restrictions. It counts all recorded plans, including pending/denied
applications, rather than pretending to verify possession. Unknown total prior loss
stays unknown even with an empty event log. Necromancy's known lost dedication is
counted once per practice and checked against the recorded total; Seer/Naztherak
history, grandfathering and valid exceptions need an explicit total. Palmreading
preserves the zero/three-slot historical case. Contradictory recorded allocations
are flagged and cannot silently erase a known source minimum.

The core source snapshot is September 2026; no live authority/review is invented.
Newly fetched wiki text is usable under the configured trust/status policy even
when old curated character rules are suspended. Read the changed text and update
curation from matching evidence, or retain an explicitly manual character record.

## Database and update transactions

`PackImport` handles bounded offline inputs without extracting supplied ZIP paths.
It validates the catalogue, trust/decisions, matching topic overlays, search and
character projections before activation. `PackStore` writes immutable complete
revisions and a checksum manifest. One `packs/selection.json` switch activates the
prepared result; the previous revision is retained. Legacy source files, original
exports, personal data and topic snapshot history are never overwritten by import.
External selection/source edits and changed previews are rejected safely.

Topic updates use the separate `updates/active.json` pointer. Comparisons read the
previous snapshot/base and current main post; archived replies never enter them.
Diff work uses a bounded LCS or explicit large-document abbreviation. All full text
is divided into at most 6,000-character layout sections. Report screens are read-only
and use the same wheel/keyboard/page controls as the reader.

## Project layout

- `src/main/java/net/mclorehub/importer`: strict export handling and developer CLI.
- `catalogue`: immutable source/status projection, optional review bindings.
- `library`: async services, search/reader/diffs, personal state and pack transactions.
- `character`: sheets/facts/history, migration, source rules, prerequisites and slots.
- `updates`: monthly scheduling, main-post HTTP/parsing, snapshots, rollback, sessions.
- `client/src/client`: Fabric input and Minecraft screens; no server component.
- `src/main/resources/.../lore-rules.json`: selected public policy excerpts/bindings.
- `data/schema`: current and legacy machine contracts.
- `src/test` and `client/src/gametest`: synthetic behavior/failure and actual client gates.
- `tools`: pinned build/runtime helpers and original-corpus verification.
- `local-data`/build caches: ignored, private and excluded from source archives.

## Acceptance gates

Normal build: `python3 tools/cloud_gradle.py build installDist --console=plain`.
No dependency, lock, checksum or TLS weakening is permitted. Native tests:
`python3 tools/run_client_tests.py`, with private `MCLOREHUB_TEST_PACK`, matching
`MCLOREHUB_TEST_TRUST`, and original `MCLOREHUB_TEST_EXPORT` for the fresh full-import
scenario. Without those inputs the corresponding corpus scenarios explicitly
report not run; do not treat that as real-data acceptance.

Require successful runner exit and all existing markers, plus:

- `IMPORT_COMPARISON_SCENARIO_PASS`: dropped export, actual preview/comparison,
  activation/Undo, untouched personal/legacy files, topic diff and restart.
- `RELEASE_SCENARIO_PASS`: clean 1.0.0 startup, original ZIP imported via UI,
  488 normal references, all selected real sources, slot editor/over-capacity/
  loss retention, persistence and scales.

Inspect fresh screenshots, actual schema-3 files, schema-1/2 migration backups,
production privacy/metadata, source exclusions, artifact hashes and remote source
identity. Actual counts/logs are in STATUS.md and the external release manifest.
The supplied database is private and must remain outside Git/public CI/releases.

The acceptance target is clean Fabric 1.21.11 on this environment's native Minecraft
client with Mesa/software rendering. No user mod list was provided in response to
the optional compatibility question. Physical GPU/audio and an authenticated user
multiplayer setup are external acceptance boundaries. Real MArt account fetching
is explicitly excluded by the user's instruction; synthetic session tests are not
reported as a verified login.
