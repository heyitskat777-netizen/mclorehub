# Current status

## Phase 3 state

Catalogue/review core implemented and tested. The original workspace reset to an
empty checkout; phases 1–2 source and guides were reconstructed from the recorded
implementation, and the original upload was recovered and hash-verified.

Historical Phase 2 passed 47 unit tests and an actual Minecraft client scenario.
Those files/JAR/screenshots/caches did not survive. They are not presented as
current-instance passes. The reconstructed client has now been revalidated after
the user published the environment, as detailed below.

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

## Client recovery and validation — October 28

Earlier attempts failed during plugin resolution with proxy tunnel HTTP 403 for:

- `maven.fabricmc.net`
- `meta.fabricmc.net`
- `piston-meta.mojang.com`

After the user published the environment, all three read-only checks returned
**HTTP 200**. The saved configuration now reports unrestricted networking; the
agent did not broaden that policy. Minecraft libraries and assets also downloaded
successfully. The previous access blocker is resolved in this running instance.

The intentional recovery command
`python3 tools/run_client_tests.py --write-locks --write-verification-metadata sha256`
passed: client/test compilation, actual Minecraft startup, isolated singleplayer,
input/navigation/rebind/guard/scale/parent assertions, screenshots and runner exit 0.
Recovered `client/gradle.lockfile` and expanded verification metadata retain all
26 previous checksum entries (567 entries after recovery).

`python3 tools/cloud_gradle.py build installDist --console=plain` then passed with
locks/checksums enforced and no rewrite flags. The 89 core tests were up to date
from their previously passing run; the client has no JUnit cases. The separate
native scenario is the client behavior check, not the zero-test client JUnit task.

`python3 tools/run_client_tests.py` passed again with no rewrite flags, runner exit 0
and `CLIENT_SCENARIO_PASS`. Screenshots at GUI scales 1/2 show readable text and an
unclipped Back button. Logs/screenshots are under ignored `client/build/`.

Production client JAR: `client/build/libs/mclorehub-0.1.0-phase3.jar`, 5,768 bytes;
SHA-256 `c6730ed56122b11c5756e6855f7b9dafa096f2e1a824946359441cd2d56d7370`.
Metadata is client-only/exact 1.21.11; test entrypoints and private data are absent.
This is the empty-library shell, not the completed wiki.

Client pins/source: Minecraft 1.21.11, Loom 1.17.21, Loader 0.20.1, API
0.141.6+1.21.11, official mappings. The reconstructed K/Back/Escape shell and native
test run on Xorg/Mesa software rendering. Profile/authentication requests and sound
device setup logged failures; authenticated multiplayer, audio, physical GPU and
user modpack compatibility remain untested. An unavailable standard X11 cursor was
logged at shutdown without failing the scenario. No secret is needed for public
build downloads.

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
of local-only commits has not been independently tested for restoration. The user
authorized the initial GitHub push; `main` was verified at
`8a904225c7178cac36fbe26b999b235c289114de`. The user subsequently reported publishing
the environment; its configuration version changed and runtime downloads now work.
This is not a clean-task restoration test. The client recovery checkpoint uses
`/workspace/mclorehub-client-recovery-source.{bundle,zip}` with identity recorded in
`/workspace/mclorehub-client-recovery-backup.json`; it excludes private data/builds.

## Next actions

1. Retain the tested client lock/checksum recovery and validation documentation
   through the source checkpoint/backup. Saved setup guidance must reflect the
   now-working full build and native test commands.
2. Continue with entity classification and async catalogue/review presentation;
   current-only search must use `currentDocuments()` and show reviewer/as-of data.
   Actual current coverage, character sheets/rules, favorites, rich reader/resume,
   update activation/rollback and forum fetch remain incomplete.

Stop after the tested phase and record the next gate. Do not promote all imported
topics to current just to populate the UI or imply that a core-only pass is a mod pass.
