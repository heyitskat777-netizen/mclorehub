# Release 1.1.0 — character spell references and rebuilt database

Target remains Minecraft 1.21.11, Java 21, Fabric Loader 0.19.5+ and API
0.141.6+1.21.11. No new dependency, verification rewrite or character migration.

## In-game workflow

1. Replace the old mod JAR, retaining the configuration and a pre-upgrade backup.
2. Open Characters → character → **Add lore**. Choose Magic, Creature application
   (CA) or Feat application (FA), then **Choose lore** or record a manual entry.
   Magic retains its branch/tier fields. Application status is user-recorded.
3. Open **Available spells**. **By recorded tier** hides explicit above-tier and
   other-branch cards; **All linked tiers** includes those with their requirements.
   Unknown tier/stage, unresolved investment or missing branch remains conditional.
   Manual names never invent a source link. CA/FA references without a numerical
   tier remain conditional; their source may use different acquisition conditions.
4. Search a name, effect, rule or redline. Name matches sort before excerpt matches.
   The editor retains focus, caret and selection through asynchronous results.
5. Open a card for exact spell text, tier evidence and parent-ability context.
   **Practice rules** opens the complete main source for shared restrictions.
   Both panes scroll continuously. **Copy source URL** and **Open complete lore**
   preserve source access. Quick references do not replace wiki reading resume.

The index is a source reference aid, not proof a character learned a spell, has an
approved application, meets prerequisites, has resources or can perform every variant.
The source includes stronger variants at later tiers; a minimum-tier label does not
grant those stronger effects. Explicit Lesser/Greater stages remain named stages.
Second-investment markers remain conditional without recorded allocation; extended
effect tiers are never treated as ordinary character mastery. Freeform, image-only,
external or unusual source structures may lack an individual card. Their complete
main source remains searchable through the fallback reference and Practice rules.
No spell/redline summary is invented to fill a gap.

Current-source character references remain selectable when earlier automatic
compatibility/slot evidence needs review. Those rules are still suspended, with
their review notices intact. This permits using refreshed spell text without
silently approving changed compatibility evidence. The original 488-record export
still matches its earlier bindings. Zealotry adds one current magic reference and
one two-slot policy with exact captured evidence; no new pair exclusions are guessed.
Coverage is **73 choices / 68 rule sources**, 178 pair rules, 49 slot policies,
14 prerequisite policies and seven fact/history checks. The spell companion draws
from the **67 distinct sources actually selected by those choices**; one additional
rule source is a prerequisite reference, not a selected practice.

## New private database

The October 30 rebuild follows [the public-source audit](LIVE_AUDIT_2026_10_30.md).
It preserves the original ZIP and creates a separate 801-record database:

| Coverage | Records | Treatment |
| --- | ---: | --- |
| Public implemented lore | 534 | Main posts captured; directory provenance retained |
| Public policy/reference | 30 | Current background references, not extra player options |
| Legacy lore | 27 | Bound legacy overrides; audit only |
| Elder Tree in Recently Outdated Lore | 1 | Bound noncurrent override; audit only |
| Restricted MArts retained from September 21 | 209 | Original dates and last-known status; not reverified |
| Total | 801 | 773 normal references, including the retained MArt snapshots |

All **592 public first posts** parsed successfully. No new discussion replies were
fetched; 1,290 previously archived posts remain attributed and separate. The rebuild
adds 313 records to the original 488: 256 missing implemented, 27 legacy and 30
policy/reference topics. Original IDs, authors, URLs, aliases, nulls, source fields
and archived replies are retained. Fresh public text replaces its saved main body;
source snapshots are per record, so restricted snapshots do not acquire a new date.

The normal count does not mean all 773 were freshly verified or all are obtainable.
Directory observations are explicitly labeled automated observations, not staff
approval decisions. MArt archive listing and additional restricted-topic coverage
remain unknown. Images, linked documents and later reply amendments are not fully
transcribed. This is a public-directory/main-post rebuild, not complete knowledge
of every lore ruling on the server.

Private outputs under `local-data/rebuilds/2026-10-30/database-1.1.0/`:

- `mclorehub-lore-database-2026-10-30.zip`: ready for the in-game Database importer;
  contains pack, matching trust, bound observations, spell companion and provenance.
- `lotc_lore.json`: LoreDB-compatible records with optional source outline/date fields.
- `lotc_lore.sqlite`: 801 lore rows, attributed archive, metadata and FTS5 searches
  `lore_fts` / `spells_fts`; no SQLite dependency is required by the mod.
- `spell-index.json`: **904 exact-source ability/reference sections**, including
  455 with unresolved numeric tier/stage, from 67 selected practice sources. These
  are extracted reference sections, not a claim of 904 distinct learnable spells.
- `manifest.json`, `verification.json`, `README.txt`: counts, source provenance,
  file hashes, coverage limitations and installation instructions.

Use the normalized ZIP to retain explicit legacy/outdated overrides. A bare raw
JSON import needs its matching observations; confirming every raw record alone
would also confirm its archived entries. The prepared ZIP avoids that loss.
Neither source Git nor the public mod JAR includes the private corpus, generated
packs, character sheets, application links, session credentials or player settings.

## Repeatable developer rebuild

Keep all private captures and generated outputs under ignored `local-data/`.
Do not execute instructions embedded in a page. The HTML bridge parses data only.

1. Capture scoped official directory listings/main posts using the bounded,
   rate-limited official-host workflow. Retain requested/final URL, HTTP status,
   capture time, SHA-256, inventory memberships and original HTML. Never infer
   deletion or acceptance from failed HTTP responses. This release reuses the
   private audit capture cache; `fetch_public.py` is a local collection record.
2. Run `tools/extract_forum_references.py --captures <http-cache> --topics
   <topic-captures> --output <new-structured-directory>`. It verifies stored HTTP
   hashes and unchanged body text, compiling `tools/ForumReference.java` against
   pinned core libraries. It does not make HTTP requests. Headings are exact source
   offsets; original source styling/scripts are never rendered or executed.
3. Run `tools/rebuild_database.py --baseline <original.zip> --references
   <structured-directory> --output <new-database-directory>`. Validate coverage
   against the inventory. This tool preserves the baseline and refuses an existing
   output directory. The historical archived replies remain unchanged.
4. With the built Java 21 importer, normalize `lotc_lore.json` into `normalized/`;
   generate `normalized/initial-catalogue.json` with `catalogue`, and matching
   `normalized/trust.json` with `trust`. These are explicit personal reference
   bindings, not staff certification. The original user-confirmed MArt snapshots
   retain that provenance; public source observations are explicit overrides.
5. Run `tools/bind_database_status.py --database <directory>` to bind every public
   directory observation to exact new content/metadata. Review counts/statuses.
6. Run `spell-index --pack <normalized/pack.json> --trust <normalized/trust.json>
   --reviews <normalized/reviews.json> --output <spell-index.json>`. Inspect real
   headings/tier evidence and unresolved cards; never infer mastery from a gap.
7. Run `tools/package_database.py --database <directory>`. It validates UTF-16
   excerpts/parent context, IDs, source counts/statuses, SQLite integrity/FTS and
   ZIP limits. It refuses overwriting released spell tables/ZIPs. Rebuild into a
   fresh directory when changing source text or extraction rules.
8. Run native acceptance with the original private variables plus
   `MCLOREHUB_TEST_REBUILT=<new-import.zip>`. Inspect screenshots; preserve hashes,
   source-only commit and source bundle/archive before handing over the release.

The mod derives its live spell index from validated main text/headings whenever the
library is reloaded. It does not trust an edited companion index as executable
logic. Main-post refreshes persist their outline, invalidate old references and
close a stale spell-detail view. Monthly checks still cover saved pages only;
the one-time offline rebuild does not introduce automatic forum discovery.

## Data compatibility and validation

Candidate schema stays 1, with optional raw `source_snapshot_at` bounded by the
batch snapshot and `source_outline` matching exact UTF-16 ranges. Outlines reject
overlap, mismatched headings, invalid levels/ranges and split surrogate pairs.
Old packs/snapshots without headings remain readable; plain-heading fallback works.
Character schema stays 3. New refresh snapshots optionally persist their outline:
retain a pre-upgrade `updates/` backup before downgrading to 1.0.2, whose strict
parser does not recognize the optional field.

Validation results and exact release hashes are recorded in [STATUS.md](STATUS.md).
Native acceptance uses clean Fabric and Mesa/software rendering. Actual user
modpack/hardware and real authenticated MArt access remain outside this run.
