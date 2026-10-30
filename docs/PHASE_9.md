# Phase 9 — Expanded character lore and conditional notices

The picker now contains **23 explicit choices from 22 main lore sources**: 15 MAGIC,
four FA and four CA references. The wiki still contains all 488 user-confirmed
references. This remains a curated selection layer; searchable source categories
never automatically classify a document as a magic or application.

## Added choices and checks

- MAGIC: Blood Magic, Housemagery, Fire/Water/Earth Evocation, Sensory Illusion,
  Transfiguration, Voidal Translocation, Clericism and Mysticism.
- CA: Nephilim, Sprite and Mori'Quessir.
- FA: Druidic Herblore and Druidic Shapeshifting.

The original eight choices retain their keys and kind/document bindings. No
character migration or pack replacement is required. Replace the mod JAR; existing
Phase 7 trusted data remains compatible. See [installation](INSTALL.md) and the
[complete choice/source directory](CHARACTER_LORE_COVERAGE.md).

There are **26 rules**: 21 pair exclusions, one Kani branch exclusion, two source
disagreements and two conditional notices. Added exclusions cover:

- Templar with Blood Magic, Mysticism, Clericism, Tree Lord or Mori'Quessir.
- Clericism with Necromancy, Blood Magic, Mysticism, the seven curated Voidal
  practices, Nephilim, Sprite or Mori'Quessir.
- Mysticism with Nephilim.

Existing Seer/Tree Lord and Necromancy/Templar exclusions remain. The original
Seer/Templar disagreement is preserved. A second disagreement is explicit: Cleric
explanation specifically allows Seers, while its redlines broadly exclude other
deific magic. Neither conflict is resolved by arbitrarily picking a sentence.

`CONDITIONAL_NOTICE` produces a requirements finding, never a permanent exclusion:

1. Sprite lore provides for Herblore through Wealdweaver/floramancy with proper
   guidance. Herblore separately describes Druidic prerequisites. Cite both sources
   without inventing a requirement for every Sprite to record Druidism separately,
   or certifying that every ability/prerequisite is met.
2. Shapeshifting restricts other casting while in animal form, specifically naming
   Seer magic and Greensight. Preserve this condition without prohibiting the
   character from holding both practices or automatically granting Greensight.

There is no runtime family/category exclusion heuristic. Mysticism/Voidal,
Kani/Blood Magic, Templar/Housemagery and Sprite/Blood Magic are not turned into
conflicts. No applicable check means not assessed, rather than compatible.
Pending/denied entries remain recorded plans, not verified possession.

## Search and source handling

Picker search covers explicit names, aliases, source titles, family and category.
`voidal` groups curated Voidal choices; `Tahariae` locates Clericism. Straight/curly
apostrophes match, including Mori'Quessir. This projection is prepared on the file
worker. Screens perform no disk/index work.

Each rule retains its own option/source dependencies. A changed Fire Evocation
page suspends that pairing without retiring Water Evocation's pairing. Conditional
notices require both source revisions; changed evidence suspends that notice, not
personal records. No-change checks, archived replies and timestamps retain main
bindings. Undo restores matching checks; network results cannot rewrite rules.

Curation uses the user-confirmed **September 2026 main-post snapshot**, on October
29, 2026. No live forum fetch, reply review, staff ruling or MArt authentication was
performed in this phase. Bundled data contains only minimal selected public policy
excerpts, citations, dates and revision bindings; private corpus/player/auth data
remains excluded. No earlier acceptance audit is reinstated.

Slot/prerequisite figures are advisory notes. There is no automatic slot total or
typed race/soul/acquisition-history assessment. See Phase 8 for the curation steps;
add actual matching sources, explicit exceptions, dependencies and meaningful tests
before expanding this manifest. No positive compatibility state is implied.

## Verification gate

1. Normal full build/install with pins, locks, checksums and TLS unchanged.
2. Core tests for new exclusions, disagreements, conditions, exception boundaries,
   Unicode/title search and granular stale-source handling.
3. Native tests with separately supplied `MCLOREHUB_TEST_PACK` and matching
   `MCLOREHUB_TEST_TRUST`. Require all prior markers plus
   **EXPANDED_LORE_SCENARIO_PASS** and exit 0. It checks every real choice binding,
   pagination/category search, new magic/CA/FA saves, three exclusions, the
   conditional notice, suspension/Undo and unchanged character/base files. Without
   private inputs, the expanded scenario explicitly reports not run.
4. Inspect fresh picker/conflict/conditional screenshots, validate schema/links and
   production/source privacy, record actual results in STATUS, and make source-only
   Git/bundle/archive checkpoints before handoff.

Next substantial work is typed eligibility/acquisition facts and further curated
coverage as needed. Real MArt account access and user modpack/hardware acceptance
remain unverified. Whole-pack import UI and readable update diffs remain optional.
