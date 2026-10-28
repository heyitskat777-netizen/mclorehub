# Decisions and scope

## User-confirmed

Personal use; Minecraft 1.21.11; Fabric; accepted/current material in normal wiki
results; new imports for updates; formatting preservation; last document and scroll
position; character sheets with magic and CA/FA information and incompatibilities.
Investigate persistent forum fetching. Development proceeds in phases, with tests
required at every stop. Repository instructions should guide future sessions.

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
- Client pins: Loom 1.17.21, Loader 0.20.1, API 0.141.6+1.21.11, official mappings.
  These passed the prior actual client test. Recovery client revalidation is blocked.
- Default K, configurable in Controls; do not replace active screens. Vanilla
  navigation/narration; preserve source style later, not arbitrary UI restyling.
- Review manifests are explicit local curation records, never automatic verification
  of staff authority or quotation truth. No real accepted/current review was invented.
- A body hash alone is insufficient: review bindings also cover all source metadata
  and attributed replies through a recursively key-sorted JSON fingerprint.
- Unknown/stale reviews stay outside current-only results; orphan decisions are
  reported and the original manifest remains untouched. No arbitrary expiry age.
- Restore reading position with stable block anchors plus offset and safe fallback.
- Import replacement before fetching. Fetch explicitly, stage changes, review,
  validate, retain prior pack, and preserve personal data. No silent periodic crawl.

## Outstanding choices

Entity taxonomy below broad source audit roles; actual reviewed-current coverage;
rich-source conversion/rendering; initial compatibility rule coverage; public forum
policy and supported authentication. Resolve them when their milestone needs them,
not by repeating already settled requirements. No mcchatmod reference was available;
these are standalone project instructions.
