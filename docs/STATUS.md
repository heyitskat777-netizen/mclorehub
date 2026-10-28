# Current status

## Phase 3 state

Catalogue/review core implemented and tested. The original workspace reset to an
empty checkout; phases 1–2 source and guides were reconstructed from the recorded
implementation, and the original upload was recovered and hash-verified.

Historical Phase 2 passed 47 unit tests and an actual Minecraft client scenario.
Those files/JAR/screenshots/caches did not survive. They are not presented as
current-instance passes. Current client revalidation is blocked, as detailed below.

## Current results

- `python3 tools/cloud_gradle.py -PcoreOnly build installDist --console=plain`:
  passed with dependency locks/checksums enforced. **89 tests, zero failures,
  errors, or skips**. The reconstructed import suite has 43 cases; catalogue and
  CLI review coverage has 46. These replace the lost historical test files;
  their identities/counts are not claimed to be identical.
- `python3 tools/verify_baseline.py --output local-data/verification/phase3-final`:
  passed, preserving **488 complete source records**, **1,290 posts**, all body
  hashes and export provenance, explicit nulls, identical repeat-import pack bytes,
  invalid/existing-output protection, and the unchanged original ZIP.
- Real catalogue: 488 records awaiting review, **0 accepted/current**, exact audit
  roles (233 player candidates, 2 hubs, 1 core/hub, 8 context, 1 disconnection,
  34 CArts, 209 MArts). No actual lore approvals or compatibility rules invented.
- Synthetic positives prove explicit bound reviews can produce current results;
  stale text, metadata, replies and orphaned/invalid reviews cannot.
- Both JSON schemas, full-corpus author/export-date projection, wrapper hash,
  distribution privacy, Python syntax and documentation checks passed.

Core utility artifact: `build/distributions/mclorehub-importer-0.1.0-phase3.zip`,
312,581 bytes; SHA-256
`35e66d3f3db797d386744ad52015c226fcd56e1782cb8a585cc64d119fe419ec`.
This ZIP is the developer importer/catalogue utility, not a Minecraft mod.

Core pins: Java 21, Gradle 9.8.2, Gson 2.13.2, JUnit 5.13.4. Known original JDK,
wrapper JAR and Gradle distribution checksums were retained and verified. Earlier
dependency verification/lock files were lost; new metadata was resolved from trusted
repositories as a recovery baseline, then used without rewrite flags in normal builds.

## Current client blocker

`python3 tools/run_client_tests.py --write-locks --write-verification-metadata sha256`
failed during plugin resolution, before Minecraft or its scenario could run.
Direct read-only checks returned **proxy tunnel HTTP 403** for:

- `maven.fabricmc.net`
- `meta.fabricmc.net`
- `piston-meta.mojang.com`

The draft still lists these and the other three required domains
(`piston-data.mojang.com`, `libraries.minecraft.net`,
`resources.download.minecraft.net`). Saved draft policy is not runtime activation.
The base configuration ID is unchanged from the earlier empty environment. Do not
infer that a missing plugin release or bad credentials caused this access denial.

Client pins/source: Minecraft 1.21.11, Loom 1.17.21, Loader 0.20.1, API
0.141.6+1.21.11, official mappings. The reconstructed K/Back/Escape shell and native
test are retained; no new client JAR/readiness is claimed until access is applied
and build/game tests are rerun. No account/secret is needed for these public downloads.

## Persistence and recovery

Initial inspection: clean unborn `work` branch, empty remote refs, no recoverable
Git objects/source archives/caches. Only the environment draft and uploaded ZIP
were recoverable. No unknown files or user changes were overwritten.

A source-only commit and downloadable backups are prepared after final checks;
their identities are recorded outside the commit in
`/workspace/mclorehub-phase3-backup.json`. The Git bundle is
`/workspace/mclorehub-phase3-source.bundle`; a plain source archive is
`/workspace/mclorehub-phase3-source.zip`. They exclude all private
exports, review manifests, generated packs, character state and build caches.
Download and save a backup outside this cloud. Local commits/download paths still
require external preservation to survive another fresh environment. Publication
of local-only commits has not been verified; an authorized push to the empty
GitHub repository is the preferred next persistence step. No push or snapshot
publication is claimed.

## Next actions

1. Preserve the concrete tested source through an authorized Git push or publish
   the prepared environment snapshot; the draft alone is insufficient.
2. Review/save the existing network additions in environment settings, publish,
   then recheck runtime reachability and rerun the reconstructed client build/test.
3. Continue with entity classification and async catalogue/review presentation;
   current-only search must use `currentDocuments()` and show reviewer/as-of data.
   Actual current coverage, character sheets/rules, favorites, rich reader/resume,
   update activation/rollback and forum fetch remain incomplete.

Stop after the tested phase and record the next gate. Do not promote all imported
topics to current just to populate the UI or imply that a core-only pass is a mod pass.
