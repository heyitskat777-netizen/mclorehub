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

Normal commands enforce locks/checksums. Only intentional reviewed dependency
changes should regenerate them. Recovery necessarily regenerated lost dependency
metadata through trusted official repositories; known JDK/wrapper/distribution
checksums were retained and verified. Do not describe lost earlier files as preserved.

Report exact commands, exits/counts, skips, blockers, artifacts and next action.
A failed check needs diagnosis; a zero-test run is not validation. After testing,
check Markdown links/schema consistency/private exclusions and create source backups.
