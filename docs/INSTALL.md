# Personal installation — 1.0.2

[Direct mod JAR download](https://raw.githubusercontent.com/heyitskat777-netizen/mclorehub/downloads/mclorehub-1.0.2.jar).

1. Use **Minecraft 1.21.11**, Java 21, Fabric Loader 0.19.5 or later and Fabric API
   0.141.6+1.21.11 (the tested combination). This mod is client-only.
2. Put **mclorehub-1.0.2.jar** from `client/build/libs/` in the instance's `mods/`.
   Remove older MCLoreHub JARs. The core and Jsoup are included; do not install the
   source JAR or developer importer distribution as a mod.
3. Enter a world and press **K**, configurable in Controls. Choose **Database**,
   then drop your original LoreDB ZIP/JSON or normalized pack/data ZIP into that
   screen. The original supplied export and the older trusted-data ZIP both work.
4. Read the preview counts and click changed rows to compare. The default
   **Accepted/current: confirmed** records your personal confirmation for this
   import, bound to its source fingerprints; it does not invent staff decisions.
   The user-confirmed supplied database needs no new per-record acceptance audit.
   Advanced **Use supplied trust only** preserves a provided trust manifest instead.
5. Choose **Activate import**. Source validation and indexing finish before one
   atomic selection switch. Personal files remain separate. **Undo database import**
   restores the previous saved database and works after restart. A topic missing
   from an export is an export gap, not proof of revocation.

A fresh-install personal kit may instead be extracted into a **new** instance after
installing Fabric/API. It contains the mod and `config/mclorehub/packs/active/` data.
For an existing installation, replace only the JAR and keep your configuration.
Legacy manual `packs/active/{pack.json,trust.json,optional reviews.json}` installations
remain readable when no imported selection is active. After UI import, use Database
for further replacements; the selected revisions live under `packs/revisions/`.

## Reading and updates

Search titles/aliases/main text, choose a named category with counts, filter favorites
and open a result card. Results are alphabetized by their names rather than forum
prefix tags; the original titles remain visible. Typing and cursor/selection edits
stay focused while results arrive, including in the character lore picker.

Lore uses one continuous scroll through the whole document. Wheel/arrows/Page
Up/Down cross internal boundaries automatically; Home/End and Top/Bottom reach the
true beginning/end. Drag or click the right-hand scrollbar to seek. Text is centered
in a bounded column with space between source paragraphs; **Text: centered** toggles
left alignment. Source bytes/offsets remain intact, so old reading positions work.
Image placeholders show a short source-view notice instead of unreadable identifiers.
Forum spoiler control labels are hidden; the original saved source remains intact.
Back/Escape returns, Resume reading or K restores the last available main document
and position. Audit includes other/unconfirmed statuses. Archived replies are
explicitly separate and do not replace the main reading position.

Refresh topic fetches only the selected main first post. Source and review →
**Compare changes** shows `- previous` / `+ current`, metadata changes and nearby
context, followed by full previous/current main-text sections. Wheel scrolls within
sections; Next/Previous move between them. Very large diffs are explicitly abbreviated,
while the full saved texts remain available. **Undo refresh** restores the previous
saved topic. Failures/cancellation retain stored lore.

Updates controls monthly checks and optional forum sessions. Checks run only while
Minecraft is open, once per UTC calendar month per saved page, and catch up after
missed time. New pages first become due one month after activation. Pause or finish
an in-flight topic update before importing; a stale preview must be prepared again.
There is no global forum discovery. See [monthly behavior](PHASE_7.md) and
[local session instructions](FORUM_SESSION.md). Real authenticated MArt access is
excluded from this release's acceptance testing as requested.

## Characters and slots

Characters support magic/CA/FA references, branches/tiers, application links and
user-recorded status, rename, archive/restore, notes, Known facts and independent
History. Choose lore uses the expanded [coverage directory](CHARACTER_LORE_COVERAGE.md).
The **Klone** CA is a separate choice from the **Tawkin** FA, linked to Tawkin's
Kloning/Reincarnation section. Search either Klone or Klones. Choosing it does not
automatically set Known facts or verify a CA approval.
Facts and history never silently infer current identity or server approval.
Compatibility shows cited exclusions, conditions, missing prerequisites and source
conflicts. Missing information is not an automatic denial or a compatibility certificate.

**Slots** records usable capacity for the current body/CA and the total prior lost
slots; blank means unknown. Auto uses curated source costs/ranges, including optional
evocation slots and Mysticism tiers. Click an allocation to record a known amount.
Duplicate references share a dedication; Bardmastery retains its two-slot parent;
Voidstalking grants only the documented optional-slot allowance and no sixth MA.
Past Necromancy loss is retained independently of active entries and repeated logs
are not multiplied. Include other permanent loss/valid exceptions in the recorded
total. Unknown/stale source costs and manual allocations are clearly identified.

Current character schema is **3**. Versions 1/2 load without rewriting; an explicit
saved edit migrates atomically and preserves exact old bytes in
`personal/characters.previous.json`. Later edits rotate that backup. Keep a separate
pre-upgrade backup for long-term downgrade recovery; older mods cannot read schema 3.

## Backups and recovery

Back up `config/mclorehub/personal/` for characters, slots, history, favorites and
reading state. Back up `packs/` for imported revisions/selection and `updates/` for
topic versions/Undo/monthly due dates. No import deletes unresolved personal references.
Optional `auth/` contains credentials: exclude it from shared archives and source.

Invalid or externally edited files remain intact. Reload files/Reload sheets retry
repaired files; automation pauses on an invalid schedule. Import Undo can restore a
valid prior revision even if the current revision is damaged; damaged bytes remain
retained, without offering them as a valid redo. If a selection pointer itself is
corrupt, back it up and inspect its `.previous.json` backup before manual repair.

See [release verification and source limits](RELEASE_1_0.md). The tested target is
clean Fabric 1.21.11 in a native client with software rendering; a separate user mod
list/account/hardware was not provided for acceptance.
