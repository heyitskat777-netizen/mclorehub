# Phase 8 — Curated character lore and initial compatibility checks

The wiki corpus remains 488 user-confirmed references. This phase adds a deliberately
smaller character selection layer: **eight options from seven main lore documents**.
It does not turn every wiki document into a learnable magic or application.

## Using the feature

1. Open Characters, select/create a sheet and Add magic, CA or FA.
2. Choose lore, search the curated list and optionally Read lore. Selection fills
   the name and stable lore link. Branch/tier, notes and application URL/status
   remain user-recorded. A lore URL never replaces a personal application URL.
3. Save. You can rename the label without losing its lore identity. Unlink lore
   returns it to manual assessment; manual entries remain available for all options
   outside the initial list. Picking a different entry never changes the entry kind.
4. The sheet shows a background assessment summary. Open Compatibility for each
   finding, requirements, source dates, exact main-post passages and original URLs.
   Source cycles citations; Read source opens the local main text, Copy source URL
   copies the official link. Wheel/arrows/Page Up/Down/Home/End scroll long evidence;
   Previous/Next move between findings. Checks never block sheet editing.

Initial MAGIC choices: bonded Seer, Necromancy, Radiant Templar, Kani, Air Evocation.
FA choices: Seer feat and Alchemy. CA choice: Tree Lord. The two Seer choices share
a document but are distinct kinds. CArts/MArts, hubs, context, inventions and
disconnection notices are not automatically offered. Selection is not approval,
ownership, an available teacher or proof that the character meets requirements.

## Source-backed checks and limits

These are interpretations of the stored **September 2026 main-post snapshot**,
curated October 29, 2026. No live lore ruling was sought during this phase.

- Necromancy + Templar: exclusion supported by both magic sources. Necromancy's
  explicit exceptions for Seer and Palmreading are retained; they are not converted
  into blanket compatibility/eligibility certificates.
- Seer (feat or bonded magic) + Tree Lord: Seer explicitly excludes treelords.
- Seer + Templar: **source conflict**, not an automatic exclusion or approval.
  Seer lists templars as compatible, while Templar forbids other deific magics.
  These snapshot statements require a ruling or a documented superseding source.
- Two Kani entries with recognized opposite Sun/Moon branches: simultaneous path
  exclusion. The once-per-OOC-month switching ritual and MA update/SREQ exception
  stays visible. This does not track training history or authorize a switch.

Requirements notices also retain Kani's breathing/lung/soul conditions and Klone
exception, Necromancy's permanent slot dedication and acquisition requirements,
Templar race/severance limits, and Alchemy's Further-Alchemy prerequisite with its
Affliction exception. Facts not represented in the sheet remain manually assessed.
No automatic slot total, race/age/history inference or approval verification exists.

Checks match kind + stable document ID, never a free-text name. Unlinked, missing,
uncurated and unmatched entry pairs remain **not assessed**. The report counts
entry pairs for which an applicable rule produced a finding, not full compatibility
coverage. Pending/denied entries are still recorded plans and appear in checks;
warnings describe that recorded combination rather than verified possession.
Zero warnings never means compatible. All findings are advisory.

## Revision dependencies and updates

The bundled `src/main/resources/net/mclorehub/character/lore-rules.json` contains
only selected public-main-post policy excerpts, citations and curated metadata,
not the private corpus, replies or player data. It records body SHA-256 plus a
main-post metadata binding (ID, title, URL, author, body revision, family, category,
role). Current trusted/reviewed status and exact quoted passages must still match.
Archived replies, personal data and check timestamps are outside this rule binding.

Rules/options are prepared on the library file worker. A no-change refresh keeps
them available. A changed body/metadata, missing source or non-current source
suspends affected selection/checks and shows needs review; existing character
links and notes survive. Refresh, monthly activation, reload and Undo rebuild the
same immutable projection. Restoring the bound source restores the checks.
No monthly network result can rewrite curated rules or silently invent replacements.

## Future curation procedure

1. Use Lorekeeper and search/read matching main sources plus every interacting
   option. Consult requirements/redlines and preserve conditional exceptions.
2. Decide explicit entity kind; mixed-use documents may need finer entity IDs before
   more sub-options can be supported. Do not classify from a check mark/title alone.
3. Add minimal exact evidence and dependencies to the manifest. Obtain the binding
   with `CharacterLore.mainFingerprint(document)` from the loaded source; do not
   hash a differently formatted JSON string or copy a review/source-record fingerprint.
4. Model unresolved source disagreements explicitly. Broader positive eligibility
   needs typed facts/history and exception evaluation; this initial engine has no
   positive compatible state. Do not add a generic catch-all family exclusion.
5. Validate the manifest against `data/schema/character-lore.schema.json` and Java
   cross-reference/evidence guards. Add meaningful behavior and stale-source tests.
6. Run the core build and native scenario with the private pack/trust, inspect
   screenshots, update STATUS/coverage, and create a source-only checkpoint.

## Verification gate

Core tests cover curated kinds/search, stable links, personal-field retention,
symmetry, explicit exceptions, source disagreements, Kani paths, unlinked names,
missing/non-current/changed evidence, no-change refresh/archive isolation,
immutable results and bounded maximum-size sheets. Native coverage with the
supplied private pack/trust exercises actual selection/save, the sheet summary,
cited findings, source update suspension, Undo, scales and unchanged character/base
files. Require `CHARACTER_LORE_SCENARIO_PASS` plus runner exit 0; without both
private input variables the corpus scenario is explicitly not run, and a missing-
source selection guard is checked instead. See STATUS.md for actual results.

Remaining work: expand curated selections/rules and typed eligibility/history as
needed; verify real MArt account access and the user's own modpack/hardware.
Whole-pack import UI and readable update diffs remain optional additions.
