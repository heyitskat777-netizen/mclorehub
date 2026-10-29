# Phase 5 — Local character sheets

Open Characters in the wiki browser. Create multiple sheets, select a character,
edit name/notes, add/edit/delete entries, and archive/restore sheets. Archiving keeps
the original sheet/entries under Archived characters. Entry deletion asks for
confirmation; the immediately previous character file is retained for recovery.

Entries have explicit types:

| Type | Stored information |
| --- | --- |
| Magic | Name, optional branch/tier, application URL, recorded status, notes |
| CA | Creature Application name, optional application URL, recorded status, notes |
| FA | Feat Application name, optional application URL, recorded status, notes |

CArt/MArt are artifacts, not application types. Entries are manually recorded;
they do not imply magic eligibility, slot costs, ownership or staff acceptance.
Recorded status cycles through not recorded, pending, approved and denied; an
approved label is the user's own record, not a verified server approval.
Application links are optional official HTTPS forum-topic URLs. Text editors are
single-line fields in this first slice; notes can be longer than the visible field.
Character notes remain available in Edit name / notes. No fields fetch forum data.

Compatibility is explicitly **not assessed**. No actual mutual-exclusivity rules
were invented or inferred from document titles/categories. A later phase needs
source-backed conditions/exceptions and revision-bound rules before producing
compatibility conclusions. Unknown coverage must never mean compatible.

## Storage and recovery

The independent schema-1 `config/mclorehub/personal/characters.json` stores stable
UUID sheet/entry IDs, selected sheet, archived flags, types and fields. Entries can
retain an optional stable lore reference for future integrations; the current
editor uses manually entered names/URLs and is not a curated lore picker.
The file is independent of packs and `personal/state.json` (favorites/reading).
See [the character schema](../data/schema/character-book.schema.json) and
[reading/favorites schema](../data/schema/personal-state.schema.json). Java adds
global ID uniqueness, selected-sheet integrity, URL policy and size constraints.

Writes are queued on a dedicated worker, bounded, flushed and atomically replaced.
`characters.previous.json` retains the immediately previous good file. Before
manual recovery, close Minecraft and back up both files; restore a validated good
backup to characters.json. Reload sheets retries a corrected file. Corrupt/future/
unknown-field data is read-only and never silently reset. External edits are checked
before replacing the file. Up to 100 sheets and 100 entries per sheet are supported,
within the overall 16 MiB file limit; invalid changes leave the old book intact.

## Test gate

**135 core tests** pass, including round-trip multiple characters/all entry types,
stable IDs, selection, edits, archive/restore, recoverable deletion, corrupt/future
schema protection, external-edit protection, immutable data and independent files.
The native character scenario creates and edits sheets, records magic branch/tier,
CA and FA links/statuses, renames, archives/restores, creates another character and
reloads the saved file. Character and entry screenshots were inspected at GUI scale 2.
The library/shell/488-record native checks also pass; final commands/results belong
in STATUS.md. No physical hardware/authenticated-server acceptance is claimed.

Next: curated entity/eligibility metadata and actual current-lore coverage; sourced
compatibility rules; reviewed transactional pack updates/rollback; supported forum
fetching; rich sources and final personal-install/modpack acceptance.
