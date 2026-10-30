# Decisions and scope

## User-confirmed

Personal use; Minecraft 1.21.11; Fabric; accepted/current material in normal wiki
results; new imports for updates; last document and scroll
position; character sheets with magic and CA/FA information and incompatibilities.
Investigate persistent forum fetching. Development proceeds in phases, with tests
required at every stop. Repository instructions should guide future sessions.

Latest clarification overrides the earlier conservative review gate: the user
confirms all lore in the supplied database is accepted/accurate. Its matching
fingerprint-bound `trust.json` makes all 488 references available normally without
per-record acceptance reviews. This is user confirmation, not invented staff
citations. Roles/eligibility/ownership remain separate. Rich formatting is dropped;
plain text is sufficient. Updates are manual and per document: open Tawkin (for
example), Refresh topic, fetch that topic's pages/replies and save its changes.
Latest update: normal reading/search and update comparisons use the main lore post
only. Previously captured discussion is an explicit archive. Check every saved
main page automatically once per calendar month while the mod runs, with persisted
catch-up after missed time. Keep manual refresh and pause/cancel. No discovery of
unrelated/new topics or mandatory review dialog. Optional explicitly imported forum
session cookies support account-authorized reads; no password collection/browser
store extraction/challenge bypass. Actual restricted MArt access needs that account.

## Selected defaults

- Client-only, offline-first; no mandatory service/account/AI/telemetry.
- Multiple local characters, with user-recorded application status. No submission
  or server approval is performed by editing a character sheet.
- Normalized JSON candidate packs; derived search index later. SQLite is currently
  an input/reference export, not a required runtime dependency.
- Deterministic search before optional fuzzy/AI features. Titles/aliases precede
  body matches. Filtering and source dates remain visible.
- Independent core and `client` Gradle project; `-PcoreOnly` omits Minecraft setup.
- Gradle 9.8.2; Java 21 (cloud Temurin 21.0.13+9); Gson 2.13.2; JUnit 5.13.4.
- Client pins: Loom 1.17.21, Loader 0.19.5, API 0.141.6+1.21.11, official mappings.
  The user requested Loader 0.19.5 compatibility after release 1.0.0; version 1.0.1
  lowers the declared minimum and tests on exactly 0.19.5. Earlier phases used 0.20.1.
  The reconstructed client passed the full build and native scenario after the
  environment publication restored download access; see `STATUS.md`.
- Default K, configurable in Controls; do not replace active screens. Vanilla
  navigation/narration; preserve source style later, not arbitrary UI restyling.
- Review manifests remain optional explicit overrides. User-confirmed baseline
  trust and manual source refresh are separate provenance, not staff decisions.
- A body hash alone is insufficient: review bindings also cover all source metadata
  and attributed replies through a recursively key-sorted JSON fingerprint.
- Unconfirmed imports stay unknown unless explicitly trusted or reviewed; orphan decisions are
  reported and the original manifest remains untouched. No arbitrary expiry age.
- Restore reading position with stable block anchors plus offset and safe fallback.
- Refresh the main post manually or on the monthly schedule; validate/index before atomic local
  activation, retain Undo and immutable snapshots, and preserve personal data.
  Optional local session import; no login/challenge bypass or unrelated discovery.

## Release completion

The latest request authorized all remaining implementation and polish except real
MArt access testing. Release 1.0.0 delivers expanded source coverage, prerequisites,
slot arithmetic, in-game database import/Undo and readable/full-text comparisons.
Unknown source conditions and staff-level contradictions remain visible rather than
fabricated. Character schema 3 reads versions 1/2 without writing until explicit save.
The acceptance target is clean Fabric 1.21.11: an optional mod-list question received
no response during this task. Environment publication stays deferred.

## Earlier checkpoints

Phase 9 expands to 23 curated choices and 26 rules, bound to main-post
content/metadata, with explicit source disagreements and conditional requirements.
Sprite/Herblore and Seer/Shapeshifting notices are not permanent exclusions.
Phase 10 records typed current facts and independent history, with seven initial
source-bound checks and schema-1-to-2 migration. No automatic timeline, positive
eligibility or slot accounting is implied. Broader coverage and modpack acceptance
remain. Environment publishing is deferred by the user after timeouts and is not
a coding prerequisite. Optional local session import exists; real authenticated MArt access
remains unverified. Inaccessible pages retain stored lore. Rich formatting and a fresh audit of
the supplied database are no longer required. Resolve remaining choices when needed,
not by repeating already settled requirements. No mcchatmod reference was available;
these are standalone project instructions.
