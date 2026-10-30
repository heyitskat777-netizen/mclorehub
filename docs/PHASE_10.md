# Phase 10 — Known facts and acquisition history

Characters now have a **Known facts** editor and an independent **History** log.
Facts start unknown, including on older sheets. Facts and events are user records;
they never verify an application, infer race from a name, or certify eligibility.
The existing 23 curated choices and 26 pair/conditional rules remain available.

## Current facts

Open a character → Known facts. Click a value to cycle it, use Previous/Next for
the remaining fields, then Save facts. Hover a field for its exact meaning.

- Soul: unknown, greater, lesser or none.
- Ability to breathe, lungs, corruption/modification of the Body portion of the
  soul, current Klone body, Human/Orc/Elf/Dwarf race and current physical blindness:
  unknown, yes or no.
- Prior combined scorn **and** disconnection from holy-deific magic: unknown/yes/no.
  An ordinary disconnection alone is not this condition.
- Prior Templar severances: unknown, zero, one, two or three-or-more.

The Body-portion fact is specific to Kani's source language. Holding a Dark or
Voidal practice alone does not establish it. Facts are not automatically filled
from the entries or history. An incomplete event log never becomes a zero count.

## Seven source-bound checks

These use the user-confirmed September 2026 main-post snapshot. No live ruling or
forum fetch is performed for this phase. Each check retains exact source passages
and the same main body/metadata binding as the picker. Changed/missing/non-current
sources suspend their checks; no-change checks and Undo retain/restore them.

| Check | Recorded facts used | Limits retained |
| --- | --- | --- |
| Kani body | Breathing, lungs, at least a lesser soul, Body-soul corruption and Klone exception | The Klone exception does not supply lungs, breathing or a soul; equipment/casting conditions remain manual. |
| Seer body | Current physical blindness, for feat or bonded magic | Other acquisition/creature conditions, eye covering and vision limits remain manual. |
| Cleric body | Greater soul, explicit Klone exclusion | Other CA exclusions and the Seer/Cleric disagreement remain separate. |
| Cleric history | Combined holy scorn and disconnection | Does not treat every disconnection as this condition. |
| Templar race | The four descendant races stated in the source | Hybrid/creature eligibility is not inferred. |
| Templar acquisition | Prior severance count | Three bars receiving the blessing a fourth time; this is not an automatic ruling about when an existing record was acquired. |
| Disconnected Necromancy | Explicit sourced disconnection event, even after active entry removal | Warns about permanently lost dedicated slots; no slot total is calculated. |

Known failures appear as **Eligibility conflict** with citations. Missing facts
produce requirements marked not assessed. Matching the selected facts is still a
limited prerequisite check, not a positive compatibility or approval state. The
sheet summary counts known eligibility findings alongside pair conflicts. Pair
coverage counts continue to describe active-entry pairs, separately from facts.

## History

Open History → Record event. Enter a label or choose a curated lore reference;
Lore type cycles magic/CA/FA browsing. Record acquired, disconnected, transformed
or other, an optional OOC `YYYY-MM-DD` reference date and notes. Edit/delete entries
with the normal previous-file backup. Up to 100 events per sheet are supported.

History is separate from active entries and survives their removal, rename and
archive/restore. Unlinked notes remain available without inferred lore. Dates are
reference notes, not a chronological state machine; events never silently change
current facts, severance counts, approvals or character identity. Only the narrowly
curated Necromancy-disconnection notice consumes a matching event. If its source
changes, the notice suspends and the history remains intact.

## Migration and recovery

The character writer now uses **schema 2**. Schema-1 files load with unknown facts
and empty history, without writing on load. The first saved edit writes schema 2
using the existing atomic file transaction and copies the exact old bytes to
`personal/characters.previous.json`. Later saves rotate that previous-file backup
normally; keep a separate pre-upgrade copy if you need long-term downgrade recovery.
Older mod versions cannot read saved schema-2 sheets.

Invalid/unknown fields, enums, dates, duplicate IDs, external edits and future
versions retain the original file rather than resetting it. Facts/history are
preserved through other character edits. Current schema is
`data/schema/character-book.schema.json`; the retained schema-1 contract is
`data/schema/character-book-v1.schema.json`. No lore pack/trust replacement is needed.

## Required phase gate

Core checks must cover exact legacy backup/no-write reads, schema migration,
facts/history persistence and isolation, invalid data retention, known failures,
unknown facts, Klone exceptions, acquisition limits and stale-source suspension.
Native tests with private pack/trust require **CHARACTER_FACTS_SCENARIO_PASS** plus
all earlier markers and exit 0. They exercise the editors, legacy migration,
invalid date recovery, Kani failure/recovery with the Klone exception, Necromancy
history without an active entry, source suspension/Undo and unchanged player data.
Without private inputs that source-backed native scenario explicitly reports not run.
Inspect screenshots and validate actual saved files against both schema versions.

See STATUS.md for actual test/artifact identities. Current remaining limits include
broader eligibility and source coverage, slot accounting, real account MArt access,
user modpack/hardware acceptance, and optional whole-pack import/update-diff UX.
Publishing was deferred by the user; continue normal source checkpoints without
reopening environment publication as a coding prerequisite.
