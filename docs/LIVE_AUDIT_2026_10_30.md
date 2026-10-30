# Public LoTC database audit — 30 October 2026

The supplied September export is **incomplete and partly outdated** against the
official public directories checked on October 30. This audit preserves the
original export, runtime pack, user trust, character data and character-rule bindings.

| Check | Result |
| --- | ---: |
| Public topics listed under Implemented Lore | 534 |
| Those topic records present in the supplied export | 278 |
| Listed implemented topic records absent from the export | 256 |
| Saved public main posts fetched and compared | 279 |
| Matching text after display/spacing normalization | 243 |
| Main posts with wording differences | 36 |
| Saved entry found in an outdated archive | 1 |
| Stored MArts not reverified | 209 |

These are topic records, not counts of learnable magics/CAs or all canon concepts.
The 534 denominator includes public approved CArts and excludes inaccessible MArts,
legacy and policy references. Text agreement and location/status are independent:
an unchanged text can still be archived as outdated.

## Verified examples

- [The Elder Tree](https://www.lordofthecraft.net/forums/topic/92342-lorethe-elder-tree/)
  is under **Recently Outdated Lore**, within Outdated Lore Archives. The audit
  does not establish when it was moved; its current location does not support
  treating it as current implemented flora.
- [Musin](https://www.lordofthecraft.net/forums/topic/200075-%E2%9C%93-ca-race-lore-musin-the-mousemen-of-almaris/):
  the saved main post says up to 70 years (oldest recorded 78); the fetched main
  post says up to 150 years.
- [Zar'akal](https://www.lordofthecraft.net/forums/topic/247957-%E2%9C%93-ca-race-lore-zarakal-armed-dangerous/):
  lesser-death regeneration differs from 3 OOC days in the export to 1 OOC day
  in the fetched main post. Other facade/possession text also differs; consult
  the full source restrictions before applying it.
- [Nephilim](https://www.lordofthecraft.net/forums/topic/238660-%E2%9C%93-ca-nephilim-the-lesser-dragonkin/):
  fetched polymorph/flame-weaving text distinguishes combat and noncombat use.

The 36 wording differences include substantive changes and editorial changes;
they are **not a claim of 36 independent mechanical-rule amendments**. They also
include Necromancy, Mysticism, Seers, Templars, Clericism, Blood Magic, Boomsteel
and Aurum. Twenty-eight of the 67 curated character-source pages have wording
differences. Review context/evidence before rebinding; dependency alone does not
prove an existing character rule wrong.

## Missing topic records

| Directory group | Missing |
| --- | ---: |
| Non-playable/event creatures and Azuras Bestiary | 100 |
| General world lore | 44 |
| Maplore | 26 |
| Starting locations | 11 |
| Legends | 15 |
| Deities, Aenguls, Daemons, patrons and dragonkin | 59 |
| Implemented-root topic: Zealotry | 1 |
| **Total** | **256** |

Examples include [General Cosmology](https://www.lordofthecraft.net/forums/topic/242086-%E2%9C%93-world-lore-general-cosmology/),
[The Spirits](https://www.lordofthecraft.net/forums/topic/268732-%E2%9C%93-world-lore-the-spirits/),
[Language Hub](https://www.lordofthecraft.net/forums/topic/268682-%E2%9C%93-lore-system-language-hub/)
and [Zealotry](https://www.lordofthecraft.net/forums/topic/263819-%E2%9C%93-magic-lore-zealotry-the-insurgents-of-valaen/).
Missing topic records do not establish that their concepts are never mentioned
elsewhere in the existing export.

Another 27 legacy topics and 30 policy/reference topics are absent. Legacy is
accepted but no longer obtainable according to the official directory, and stays
separate from current/obtainable material. Policy topics include Ability Types,
CA Race Groupings, Default Magic Progression and Connection & Disconnection.

Public inventory files contain live topic IDs/titles/URLs/directory labels, with
no body text or saved records: [256 implemented topics](audits/2026-10-30/missing-implemented-topics.csv),
[27 legacy topics](audits/2026-10-30/missing-legacy-topics.csv), and
[30 policy references](audits/2026-10-30/missing-policy-references.csv).
These are reconciliation lists, not importable mod packs or independent approvals.

## Evidence and limits

- Starting from [Implemented Lore](https://www.lordofthecraft.net/forums/forum/289-implemented-lore/)
  and the selected official Lore criteria/reference branches, enumerated 37
  accessible directories and all 46 listing pages. Reported topic counts match
  unique listing row IDs. The inventory contains 534 implemented, 27 legacy and
  30 policy/reference topics, 591 distinct IDs total.
- Attempted the restricted MArt directory as a 38th directory: signed-out HTTP
  404. Three known MArt topic probes returned HTTP 403. No real session/login was
  supplied or used. The 209 stored MArts remain not reverified, and new restricted
  topics cannot be enumerated. Access failures are not deletion/revocation evidence.
- All 279 saved public first posts returned HTTP 200 and parsed. Comparison uses
  the supplied September export, not the user's locally refreshed mod configuration.
- Display normalization excludes image placeholders/spoiler controls, applies
  Unicode NFKC, and checks whitespace and word-token sequences. Raw hashes,
  captured HTML and original data are preserved. There are 201 display-normalized
  matches and 42 spacing/segmentation-only differences, totaling 243.
- Missing-topic main bodies were not imported or semantically compared. Directory
  membership is a coverage/status signal, not an independent per-topic staff
  decision audit. This is not a claim to verify all lore outside these branches.
- Images/image-contained rules, external documents and reply amendments are not
  fully audited. No ownership, crafting permission or character eligibility follows
  from artifact acceptance. Full rule verification needs context beyond text diffs.

Private evidence: `local-data/audits/2026-10-30/REPORT.md`, `summary.json`, directory
inventory, missing-topic CSVs, saved-topic comparison, curated-source impact,
`wording-diffs/` and the HTTP cache. The raw-extraction diff directory is diagnostic;
use the refined `comparison.json`/`wording-diffs/` results. None of that private
corpus/evidence is included in Git or public releases.

Reconciliation order: preserve the audit; separately record the Elder Tree archive
status; review changed main posts with previous versions retained; ingest missing
implemented topics with appropriate roles; keep legacy/policy references separate;
review/rebind character evidence; retain MArt access as an explicit remaining gap.
No database import, review decision, production code change or environment setting
change was made by this audit.
