# Validation

From the repository root:

```sh
python3 tools/cloud_gradle.py -PcoreOnly build installDist --console=plain
python3 tools/verify_baseline.py --output local-data/verification/new-run
```

Use a new output directory; the original private ZIP must be supplied separately
in a fresh clone. Core tests cover strict parsing/import guards, null/Unicode
preservation, duplicate IDs, limits, report failure safety, review evidence,
current filtering, stale body/metadata/replies, key-order independence, orphaned
decisions, conservative roles, and immutable public collections. Fixtures are
synthetic; no acceptance test example certifies actual lore.

Full client workflow (verified after environment publication restored access):

```sh
python3 tools/cloud_gradle.py build installDist --console=plain
python3 tools/run_client_tests.py
```

The client scenario runs Minecraft, creates an isolated singleplayer world, checks
K registration/translation, Back/Escape, active-screen guard, rebinding, two GUI
scales, screenshots, and parent return. Require the real runner's successful exit,
not only a marker or screenshot. Inspect screenshots visually. Headless Xorg/Mesa
tests do not prove user modpack/hardware/multiplayer compatibility.

Additional native library/character scenarios check actual typing, current/audit
filters, main/reply/source reading, favorites, service restart/resume, failed reload,
multiple sheets, magic branches/tiers, CA/FA, links/statuses, rename/archive/restore
and persistence. Require `CLIENT_SCENARIO_PASS`, `LIBRARY_SCENARIO_PASS`, and
`CHARACTER_SCENARIO_PASS` plus exit 0. With a separately supplied private candidate,
`MCLOREHUB_TEST_PACK=/absolute/path/to/pack.json python3 tools/run_client_tests.py`
also verifies 488 records, no automatic approvals and actual audit search/render;
require `REAL_CORPUS_PASS`. That optional corpus check is unrun without the variable.
All synthetic approval examples remain separate from real data and production JARs.

Phase 7 requires `MONTHLY_SCENARIO_PASS` in addition: a simulated calendar triggers
the real scheduled worker, checks main lore, pauses via the UI and imports/forgets
synthetic session cookies with no value in narration. Default search/reader exclude
discussion; explicit archive access must not overwrite main reading position.
The optional live-topic run now expects one main page, no newly fetched replies,
and persistence across restart. No real authenticated MArt session is provided by
tests; do not describe synthetic session success as verified forum sign-in.

Phase 6 additionally requires `REFRESH_SCENARIO_PASS`: trusted baseline, refresh UI,
persist/restart/Undo, HTTP failure and cancellation. With `MCLOREHUB_TEST_TRUST` set
to the matching private trust manifest, the full-export test expects 488 normally
available references; without it, unconfirmed candidates remain unknown. Set
`MCLOREHUB_TEST_LIVE_TOPIC=lotc:topic:198584` only when intentionally testing a real
public Tawkin refresh inside Minecraft; require `LIVE_TOPIC_SCENARIO_PASS`. The
helper passes the platform CA store to cloud game launches without weakening TLS.

Normal commands enforce locks/checksums. Only intentional reviewed dependency
changes should regenerate them. Recovery necessarily regenerated lost dependency
metadata through trusted official repositories; known JDK/wrapper/distribution
checksums were retained and verified. Do not describe lost earlier files as preserved.

Report exact commands, exits/counts, skips, blockers, artifacts and next action.
A failed check needs diagnosis; a zero-test run is not validation. After testing,
check Markdown links/schema consistency/private exclusions and create source backups.

Phase 8 adds `CharacterLoreTest` and a native curated-corpus scenario. Run with
both `MCLOREHUB_TEST_PACK` and `MCLOREHUB_TEST_TRUST` to require
`CHARACTER_LORE_SCENARIO_PASS`: exact main evidence/bindings, actual MAGIC/CA/FA
selection/save, sheet summary and cited conflict, no-change refresh retaining rules,
changed-source suspension, Undo, scales and unchanged character/base files. Without
these variables the test checks unavailable selection and explicitly reports that
the real curated-corpus scenario was not run. No private corpus fixture enters Git.

Phase 9 additionally requires `EXPANDED_LORE_SCENARIO_PASS` with the same private
pack/trust inputs and exit 0. Every one of the 23 choices must match its real main
source/evidence. The native flow checks pagination/category search, new magic/CA/FA
links, three exclusions, a conditional provision rather than an exclusion, granular
source invalidation/Undo, unchanged personal/base files and persistence. Without
private inputs it explicitly reports `EXPANDED_LORE_SCENARIO_NOT_RUN`.

Phase 10 additionally requires `CHARACTER_FACTS_SCENARIO_PASS` with private
pack/trust and exit 0: exact schema-1 backup, explicit-save migration, fact editing,
Kani Klone exception with lung failure/recovery, history/date validation, disconnected
Necromancy without active entry, source suspension/Undo, persistence and scales.
Without those inputs it explicitly reports not run. Validate the actual saved
schema-2 file and the initial legacy backup against their respective schemas.
