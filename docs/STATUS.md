# Current status

## Latest checkpoint — Release 1.0.1 / Fabric Loader 0.19.5

The user requested compatibility with Fabric Loader **0.19.5**. Version **1.0.1**
builds against exactly that loader and lowers the production metadata minimum to
`>=0.19.5`. Minecraft stays **1.21.11**, Java stays **21**, and Fabric API stays
**0.141.6+1.21.11** (its declared loader minimum is 0.17.3). Features, lore data and
character schema 3 are unchanged. Existing configuration can be retained.

The only dependency lock change is Loader 0.20.1 to 0.19.5. SHA-256 entries for
the new loader JAR, sources and POM were checked against official Fabric Maven
downloads over HTTPS; every previous verification entry remains unchanged.
No checksum, locking or TLS checks were disabled.

Normal `build installDist` passed. The native runner plus an explicit core-test
rerun passed, exit 0: **292 core tests**, zero failures/errors/skips, and all **11
native markers**. Minecraft's startup log and the release scenario both verify
**Loader 0.19.5**, rather than only a relaxed metadata minimum. Tests imported the
original ZIP in-game, loaded 488 references, matched all curated sources, exercised
character/slot/migration behavior, updates/Undo, persistence and GUI scales. Fresh
screenshots were inspected. Acceptance remains clean Fabric with Mesa/software
rendering; real MArt access and a separate user modpack/hardware remain unverified.
No live forum requests were made.

Logs: `/tmp/mclorehub-1.0.1-build.log` and `/tmp/mclorehub-1.0.1-client.log`;
copies/screenshots: ignored `local-data/verification/release-1.0.1-evidence/`.
Production JAR: `client/build/libs/mclorehub-1.0.1.jar`, **845,382 bytes**, SHA-256
`1bf0f2f87133ae1b301bb7c10a6cbd4003d34a7c475bf47394c03ee8b9f8706a`.
Production metadata/privacy and source exclusions were checked. Backups and
verified source/download identities are in `/workspace/mclorehub-1.0.1-backup.json`.

[Direct JAR download](https://raw.githubusercontent.com/heyitskat777-netizen/mclorehub/downloads/mclorehub-1.0.1.jar).
Replace the old mod JAR; keep the database and personal files. A new installation
still imports the original LoreDB ZIP through Database. The public download branch
contains only mod binaries and instructions, with no private database/player/session
data. Environment publication remains deferred; no environment settings changed.

## Previous checkpoint — Release 1.0.0

The requested implementation and polish are complete. In-game database import
accepts the original ZIP/JSON and normalized packs, previews changes with readable
comparisons, and activates immutable revisions with persistent Undo. Main-page
comparisons include added/removed text, metadata and complete before/after versions.
Topic refresh/monthly automation remain separate from whole-database replacement.

Character coverage now includes **71 references from 67 main sources**, **178
relationship rules**, **14 prerequisite policies**, **48 slot policies** and the
seven existing fact/history checks. The Slots editor supports unknown capacity,
allocations, duplicate dedication grouping, source ranges, permanent loss and narrow
documented allowances. Source disagreements and uncovered conditions stay explicit.
Schema 3 preserves every sheet field; versions 1/2 migrate only on an explicit save
with exact prior-file backup. No source rule claims overall eligibility or approval.

Final normal build/install and **292 core tests passed**, zero failures/errors/skips.
The native Minecraft 1.21.11 client runner exited 0 with all **11** required markers:
shell, library, real corpus, characters, refresh, monthly/session, curated lore,
expanded lore, facts/history, import/comparison and clean-release scenarios. The
fresh-install flow imported the original ZIP through the UI, exposed all 488 normal
references, matched all 71 selected source bindings, exercised slot overflow/loss
and recovery, persisted/restarted and rendered at GUI scales 1/2. Fresh screenshots
were inspected. Actual saved schema-3 files and legacy migration backups validated.
The production JAR metadata, private/test exclusions and kit contents passed checks.
Tool pins, dependency locks/checksums and exact Minecraft target are unchanged.

Final logs: `/tmp/mclorehub-release-build-frozen.log` and
`/tmp/mclorehub-release-client-frozen.log`; evidence is copied under ignored
`local-data/verification/release-1.0.0-evidence/`. These tests use native Minecraft
with Mesa/software rendering and clean Fabric. They do not establish a separately
configured user modpack, physical hardware or authenticated multiplayer behavior.
No actual MArt account access was tested, as explicitly excluded by the user.
Synthetic session checks are not a verified forum login. No live forum requests
were made for this release; prior public-page checks remain historical evidence.

Artifact: `client/build/libs/mclorehub-1.0.0.jar`, **845,381 bytes**, SHA-256
`f62cba9d06a89787eff284954b4ce952a7fb5606cc41cf84cdc9f51558b65a73`.
Private fresh-install kit:
`local-data/distributions/mclorehub-personal-kit-1.0.0.zip`, **5,395,221 bytes**,
SHA-256 `0d95efa2e3dfeca931f21b492d359b6766dbada0595267f63d23051800879b17`.
The kit includes only the JAR, original normalized pack, matching user trust and
installation text; no characters, session credentials, schedules or topic overlays.
Keep it private. Existing installations normally need only the replacement JAR.
Source backup identities and verified remote commit are recorded in
`/workspace/mclorehub-1.0.0-backup.json`; source archives exclude the private corpus.

See [release details](RELEASE_1_0.md), [installation](INSTALL.md) and
[coverage](CHARACTER_LORE_COVERAGE.md). There are no remaining planned feature
implementation items in this request. Real MArt account testing remains excluded;
unknown source details require matching evidence or staff clarification rather
than invented rules. Environment publication remains deferred by the user; no
environment settings were changed or publication requested.

## Previous checkpoint — Phase 10

Character sheets now include **Known facts** and independent **History** editors.
Nine typed current facts retain explicit unknown values. Events record acquisitions,
disconnections, transformations or other notes with optional lore links/OOC dates;
they never silently change current facts or imply a complete timeline. History
survives active-entry deletion and all other sheet edits.

Seven source-bound checks cover Kani body/soul requirements with the Klone exception,
Seer physical blindness, Cleric greater-soul/Klone and combined holy-scorn conditions,
Templar race/fourth-acquisition limits, and lost-slot guidance for recorded Necromancy
disconnection even without an active entry. Known failures are advisory eligibility
conflicts; missing facts remain not assessed. Source changes suspend checks and
Undo restores them without changing player data. Existing 23 choices/26 pair rules
and unresolved source disagreements remain intact. No overall positive eligibility,
automatic slot total, timeline inference or server approval is implied.

Character schema 1 reads remain untouched and default to unknown facts/empty history.
The first explicit saved edit writes schema 2 atomically with exact prior bytes in
characters.previous.json; later saves rotate that backup normally. Legacy schema is
retained separately. Future/invalid files stay intact/read-only. Older mod versions
cannot read saved schema 2; retain a separate pre-upgrade backup for downgrades.

Full build/install and **252 core tests passed**, zero failures/errors/skips. Native
Minecraft passed all prior scenarios plus CHARACTER_FACTS_SCENARIO_PASS and exited 0.
Tests verified real corpus/source bindings, exact legacy migration backup, editors,
Kani Klone exception with lungs failure/recovery, invalid-date correction, disconnected
Necromancy guidance, source suspension/Undo, unchanged character data, persistence
and scales. Screenshots were inspected. Both actual serialized migration files passed
schema-1/2 validation; the native saved schema-2 file also validated. Initial compilation
caught a Java enum method-name collision, corrected before passing these final gates.
Final logs: `/tmp/mclorehub-phase10-build-final.log` and
`/tmp/mclorehub-phase10-client-final.log`. Pins, locks/checksums and exact MC remain unchanged.

Artifact: `client/build/libs/mclorehub-0.1.0-phase10.jar`, **751,312 bytes**,
SHA-256 `d6c77073be3e1da574d61d2818d81cb9b10e3c78954960d8c5d8875fb9271c89`.
Existing Phase 7 trusted data is compatible. Production metadata and private/test
exclusions passed; source includes tests but excludes user data. Source backups and
identities are in `/workspace/mclorehub-phase10-backup.json`. See [Phase 10](PHASE_10.md)
and [installation](INSTALL.md). No real forum account/live fetch was used this phase.

Next substantial work: broader eligibility/rule coverage and slot accounting as
needed, real MArt account access, and actual user modpack/hardware/multiplayer
acceptance. Whole-pack import UI and readable update diffs remain optional additions.
Environment publication is deferred by the user after timeouts; it is not a coding
prerequisite. No environment settings were changed or publication requested in this phase.

## Previous checkpoint — Phase 9

Expanded character lore now offers **23 choices from 22 main sources**: 15 MAGIC,
four FA and four CA. Added options include common Voidal practices, Blood Magic,
Mysticism, Clericism, Housemagery, Nephilim, Sprite, Mori'Quessir and Druidic feats.
Source titles/family/category and apostrophe variants are searchable in the picker.
Existing character references and personal fields remain compatible.

The **26 rules** comprise 21 pair exclusions, one Kani branch exclusion, two source
disagreements and two conditional notices. Seer/Templar remains unresolved; Cleric
text's specific permission for Seers versus its broader deific exclusion is now
also explicit. Sprite/Herblore and Seer/Shapeshifting produce conditional requirements
rather than permanent exclusions. Zero warnings never establishes compatibility.
No typed race/soul/history facts, automatic slot totals or approval verification
are implied. Each rule binds its own sources, so changed evidence suspends only
its dependent checks. Undo restores matching rules without touching characters.

Normal full build/install and **231 core tests passed**, zero failures/errors/skips.
Native Minecraft exited 0 with every prior marker plus EXPANDED_LORE_SCENARIO_PASS.
The supplied 488-reference pack/trust matched every curated option's main evidence
and binding. Tests exercised picker pagination/category search, new magic/CA/FA
saves, three exclusions, the conditional notice, granular source invalidation and
Undo, unchanged character/base bytes and persisted links. Fresh picker/conflict/
conditional screenshots were inspected. Log: `/tmp/mclorehub-phase9-client.log`.
No live forum requests or real authenticated MArt tests were performed in this phase.
Pins, locks, dependency checksums and Minecraft 1.21.11 are unchanged.

Artifact: `client/build/libs/mclorehub-0.1.0-phase9.jar`, **719,428 bytes**,
SHA-256 `dafa7e11357fba43fb65ffaf7839e0b5fec7fed95b3c6476bff6e0027395dcd2`.
The existing Phase 7 trusted-data ZIP works unchanged. The production JAR excludes
the private corpus, player/auth data and test entrypoints. Source checkpoints include
tests and exclude private files. Only selected public main-post policy excerpts
are deliberately bundled.
Source/backups are identified in `/workspace/mclorehub-phase9-backup.json`.
See [Phase 9](PHASE_9.md), [coverage directory](CHARACTER_LORE_COVERAGE.md) and
[installation](INSTALL.md). The older phase sections below describe historical scope.

Next substantial work: typed eligibility/acquisition history and further curated
coverage as needed. Actual MArt account access, user modpack/hardware/multiplayer
acceptance remain unverified. Whole-pack import UI and readable update diffs remain
optional additions. Rich formatting and a new baseline acceptance audit are out of scope.

## Previous checkpoint — Phase 8

Character entries now offer **Choose lore** with eight curated choices from seven
main documents: five MAGIC, two FA and one CA. Names may be edited without losing
stable lore links; application URLs/status, branch/tier and notes remain personal.
Unlink/manual entry remains available. This is a limited selection layer, not a
classification of all 488 references as learnable magics or applications.

Initial advisory checks cover Necromancy/Templar exclusion, Seer/Tree Lord exclusion,
opposite recorded Kani paths, and an explicit Seer/Templar **source disagreement**.
Requirements preserve named exceptions and missing history/eligibility facts.
The sheet shows an async summary; Compatibility shows findings, original URLs,
snapshot dates and exact main-post evidence. Unlinked/uncovered combinations stay
not assessed; zero warnings never certifies compatibility or application approval.

Curated rules bind main-post body and metadata, checked against trusted/current
sources and exact passages. No-change refreshes and archived discussion do not
retire them. Changed/missing/non-current sources suspend the relevant selection
and checks while preserving character entries. Reload, monthly/manual activation
and Undo rebuild the same immutable projection on the library worker.

Full build/install and **221 core tests passed**, with zero failures/errors/skips.
Native Minecraft scenarios passed with the supplied real pack/trust, including
488 normal references, curated source/evidence matching, MAGIC/CA/FA selection/save,
cited warnings, no-change refresh, changed-source suspension and Undo. Character
and original pack bytes remain unchanged by source updates. Screenshots of the
picker, sheet summary, evidence and source-review state were inspected at scales
1/2. Locks/checksums/dependencies and exact Minecraft version are unchanged.
One final native attempt failed in Fabric's network synchronizer before the new
character scenario; an unchanged retry passed every marker and exited 0, including
actual picker typing. Final log: `/tmp/mclorehub-phase8-client-frozen-retry.log`.
Source/artifact identities are recorded in `/workspace/mclorehub-phase8-backup.json`.
No live forum request or real MArt authentication was
performed in this phase; earlier public Tawkin verification remains historical.

Current artifact: `client/build/libs/mclorehub-0.1.0-phase8.jar`, **711,786 bytes**,
SHA-256 `d51c289f2022ab4ab55fd26acf5b0f5220faa733d6b8f891d6308ac1696f2dd3`.
The existing Phase 7 trusted-data ZIP is compatible and does not need replacement.
Only selected public main-post policy excerpts are bundled with curated rules;
private corpus/player/auth files are excluded from the JAR and source checkpoints,
and test entrypoints are excluded from the production JAR. See [Phase 8](PHASE_8.md)
and [installation](INSTALL.md).

Next: expand curated entities and source-backed rules; add typed eligibility,
acquisition history and fuller exception handling as needed. Real account MArt
access and actual user modpack/hardware/multiplayer acceptance remain unverified.
Whole-pack import UI and readable saved-vs-fetched diffs are optional later work.

## Previous checkpoint — Phase 7

Normal reader/search/update comparison now uses **main lore only**. Comment pages
are not requested; old replies remain an explicit archive and do not replace main
reading position. The user's trusted 488-reference database remains normally available.

Monthly automatic checks are enabled in the client, with UTC calendar-month due
dates persisted per saved topic. Newly activated pages wait one month; successful
manual checks reset their date. Overdue work catches up while Minecraft runs, with
pause/cancel, access-error retention, bounded retries and persisted Retry-After.
No process runs while Minecraft is closed and no new-topic discovery is performed.

Optional browser-session cookie import/forget is implemented under **Updates**.
It stores only supported IPS forum-session cookies, restricts headers to exact
official HTTPS topic URLs, applies expiry/redaction and POSIX owner-only file mode,
and excludes session data from bundles/source/logs. Passwords/browser stores are
not collected. **Real authenticated MArt access is not verified**: no actual user
account/session was supplied. Access remains limited to what that account can view;
no login/challenge bypass is implemented. See [session guide](FORUM_SESSION.md).

Full build/install and **198 core tests passed**, with no failures/errors/skips.
Native shell/library/characters/refresh and monthly/session scenarios passed with
normal pins/locks/checksums. A real scheduled worker was triggered using a simulated
calendar; pause and synthetic session import/redaction/forget worked in Minecraft.
Live Tawkin verification now fetched **one main page and zero new replies**, saved
and survived restart without rewriting the base pack. The final frozen run passed
again without repeating that live HTTP request. Screenshots of main reading,
explicit archives and monthly/session settings were inspected. No new dependency
or verification-policy changes were required.

See [Phase 7](PHASE_7.md), [installation](INSTALL.md), and [forum session](FORUM_SESSION.md).
Next substantial work remains sourced compatibility/eligibility and curated
character lore selection, plus real account/modpack/hardware acceptance. The older
checkpoints below describe earlier scope; Phase 6's pagination/reply fetching is
superseded by main-only monthly behavior.

Current client artifact: `client/build/libs/mclorehub-0.1.0-phase7.jar`, 673,563 bytes,
SHA-256 `6b8b8646eb85c93134723c4730d5d9fa2f0cef27d4244a80d3c79f01a2fa0c33`.
The separate trusted-data bundle is unchanged in content and excludes auth/session,
schedule, snapshots and player files. Existing Phase 6 data is compatible; upgrading
normally requires replacing only the mod JAR. Raw baseline verification again
preserved 488 complete records/1,290 posts, nulls, source bytes and repeatable packs.
The monthly schema validated the actual native-saved schedule. No dependency or
checksum changes were made. Live/synthetic UI evidence remains under ignored
`local-data/verification/phase7-live-evidence/`; final source/artifact identities
are recorded in `/workspace/mclorehub-phase7-backup.json`.

## Previous checkpoint — Phase 6

Latest user scope is implemented: the supplied database is accepted/accurate by
user confirmation, rich formatting is out of scope, and updates are manual per
open topic. A matching `trust.json` makes **all 488 references available normally**
without per-record acceptance reviews. The immutable raw import is still a candidate;
user trust, optional review overrides and fetched source provenance are separate.

**Refresh topic** fetches only that official topic and its pagination, compares
main text/replies, validates/indexes and atomically saves a persistent overlay.
Cancel, error retention and per-topic Undo are implemented. Original pack, favorites
and character sheets remain separate. Source moves into known shelving/pending
locations are recorded; missing/blocked pages do not erase stored lore.

Verification: full build/install and **179 core tests passed**, with no failures,
errors or skips. Native shell/library/characters/refresh scenarios passed with
locks/checksums enforced and no rewrite flags. The full real database rendered
with 488 normally available references. Live **Tawkin refresh inside Minecraft**
fetched two pages and 23 replies, saved persistently, survived library restart and
left the original pack unchanged. Screenshots of live reading, source/Undo and
HTTP-error retention were inspected. Live evidence is retained under ignored
`local-data/verification/phase6-live-native-evidence/`; the final frozen native run
also passed without repeating the live network request.

Jsoup 1.21.2 was added for non-executing HTML parsing. Its JAR/POM match official
Maven Central SHA-256 values; all prior checksum entries are preserved. The cloud
helper propagates the platform CA store to game launches without disabling TLS.
The optional live test requires explicit `MCLOREHUB_TEST_LIVE_TOPIC`; ordinary
fixture/native tests do not perform that live topic fetch.

See [Phase 6](PHASE_6.md) and [installation](INSTALL.md). Remaining work is sourced
compatibility/eligibility and curated character lore selection, plus actual user
modpack/physical hardware/authenticated-server acceptance. Whole-pack import UX
and supported authenticated forum fetching are optional later additions. Rich
formatting and a new acceptance audit of this supplied baseline are not required.

Release artifact: `client/build/libs/mclorehub-0.1.0-phase6.jar`, 642,231 bytes,
SHA-256 `595ec32d5cec158ec12b7906a22ba78e01406b12c7adcb5cec5b7dd799a0acdf`.
Nested core/Jsoup and exact client metadata were checked; Jsoup original entries
match the upstream JAR byte-for-byte. Private data is not embedded in the mod.
The separate ignored `local-data/distributions/lotc-trusted-data-phase6.zip` contains
the original deterministic pack plus its matching user-confirmation trust file;
no live snapshots, player state or staff review assertions are included.
Independent raw-import verification still preserves all 488 complete records,
1,290 posts, source bytes/nulls and deterministic packs. Its unsigned-candidate
report remains unknown by design; runtime with the supplied trust manifest is 488
normally available references. Live trust/pointer/snapshot files passed their formal
schemas. Current source checkpoint identity/backups are recorded outside the commit
in `/workspace/mclorehub-phase6-backup.json`.

The sections below are historical checkpoints under earlier scope and test counts.

## Previous checkpoint — Phase 5

Character sheets are now implemented alongside the Phase 4 browser/reader. Multiple
characters, magic branch/tier, explicit CA/FA, application URLs, recorded statuses,
notes, rename, archive/restore and persistent selection use independent local
storage. Compatibility remains **not assessed**; no actual rules/approvals were
invented. Current source acceptance coverage remains 0 of 488 reviewed records.

The full build and **135 core tests** passed (0 failures/errors/skips). Native shell,
library, full-export and character scenarios passed; screenshots at scales 1/2 for
the reader/browser and scale 2 for character editing were inspected. All normal
checks enforce pins/locks/checksums. See [installation](INSTALL.md), [Phase 4](PHASE_4.md)
and [Phase 5](PHASE_5.md). Final packaged checksums/commit identity are recorded in
the separate source-backup manifest, not embedded into a self-referencing commit.

Current client artifact: `client/build/libs/mclorehub-0.1.0-phase5.jar`, 123,757 bytes,
SHA-256 `421b8c65316f9c9cfd6e39a4f33e86a9a79dd5aedc5161f7b9149adb846d7511`.
The nested core, exact 1.21.11 client metadata and test/private-data exclusions were
verified. A separate private candidate ZIP under ignored `local-data/distributions/`
contains only the 488-record unreviewed pack and installation text; it is not pushed.
Fresh independent baseline verification also preserved all 1,290 posts and original
source bytes, repeat-import determinism and error/output protection. Both personal
schemas validated the actual files saved during the native scenarios.

Next substantial work: curated entity eligibility/current-status coverage, sourced
mutual-exclusivity evaluation, transactional import/review/rollback, forum fetching
and rich-source formatting. Manual pack installation and reload work; no in-game
update transaction, lore picker, rule engine or forum crawler is implied.

## Previous checkpoint — Phase 4

In-game async catalogue loading, current/audit search, categories, pagination,
plain-text main/reply reader, source/review details, favorites and stable reading
resume are implemented. **123 core tests passed** with no failures/errors/skips.
The full build and native shell/library/full-488-record scenarios passed with
locks/checksums enforced; screenshots at GUI scales 1/2 were inspected. All actual
records remain unreviewed. See [Phase 4](PHASE_4.md) for installation and limits.

The older sections below are the Phase 3/recovery history, not the current feature
boundary. The initial and recovery source checkpoints are now on GitHub `main`;
further tested checkpoints must also preserve private-data exclusions.

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
