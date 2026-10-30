# MCLoreHub

Personal-use **Minecraft 1.21.11 Fabric** wiki mod, developed in tested phases.

**1.0.2** supports **Fabric Loader 0.19.5+** on Minecraft 1.21.11 and provides
offline main-lore search, favorites, reading resume, character
sheets, monthly page updates and manual refresh/Undo. Database import now runs in
Minecraft with previews, readable comparisons and whole-database Undo. Reports also
include complete previous/current main text in bounded sections; replies stay in
an explicit archive.

The supplied 488-reference database is trusted by the user's confirmation. Character
selection includes 72 references (including the Klone CA), 178 relationship rules, seven fact/history checks,
14 prerequisite policies and 48 slot policies. Known facts, history, optional slot
allocations, permanent loss and special allowances retain citations and explicit
unknowns/source disagreements. No server approval is fabricated.

The [October 30 public-source audit](docs/LIVE_AUDIT_2026_10_30.md) found missing
topic records and changed/outdated material in that original export. User trust is
not an independent live verification; the audit did not rewrite database/player data.

Start with [installation](docs/INSTALL.md), the [release guide](docs/RELEASE_1_0.md)
and [character coverage](docs/CHARACTER_LORE_COVERAGE.md). Java 21, Minecraft 1.21.11,
Fabric Loader and Fabric API are required. No server component or AI service is needed.

[Download MCLoreHub 1.0.2](https://raw.githubusercontent.com/heyitskat777-netizen/mclorehub/downloads/mclorehub-1.0.2.jar).
Requires Loader 0.19.5+, API 0.141.6+1.21.11 and Java 21. Import your original
LoreDB ZIP in-game; the private database is supplied separately.

Search retains the caret while results arrive. Browse named categories with counts
and separated result cards; lore pages use centered, paragraph-spaced text and one
continuous scroll with a scrollbar, Top/Bottom and an optional left-alignment toggle.

**Recovery note:** this environment reset to an empty checkout on October 28.
Source was reconstructed from the recorded implementation and the original ZIP
was recovered. Historical Phase 2 screenshots/binaries/caches were not restored.
Current core checks pass, and after environment publication the reconstructed
client passed its full build and native input/render scenario. See the status
report for the exact distinction, limits and persistence steps.

## Guides

- [Release 1.0.0 workflow and acceptance](docs/RELEASE_1_0.md)
- [Agent instructions](AGENTS.md)
- [Current status and test results](docs/STATUS.md)
- [Confirmed requirements and decisions](docs/DECISIONS.md)
- [Step-by-step implementation plan](docs/IMPLEMENTATION_PLAN.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Data/status/rules contract](docs/DATA_RULES.md)
- [Source export audit](docs/SOURCE_AUDIT.md)
- [Validation commands](docs/VALIDATION.md)
- [Phases 1–2 recovery guide](docs/PHASE_1_2.md)
- [Phase 3 catalogue and review workflow](docs/PHASE_3.md)
- [Phase 4 installation, browser and reader](docs/PHASE_4.md)
- [Phase 5 character sheets](docs/PHASE_5.md)
- [Personal installation](docs/INSTALL.md)
- [Phase 6 trusted baseline and per-topic updates](docs/PHASE_6.md)
- [Phase 7 main-only monthly checks](docs/PHASE_7.md)
- [Phase 8 character lore picker and sourced checks](docs/PHASE_8.md)
- [Phase 9 expanded character lore and conditions](docs/PHASE_9.md)
- [Character lore coverage directory](docs/CHARACTER_LORE_COVERAGE.md)
- [Phase 10 known facts, history and migration](docs/PHASE_10.md)
- [Optional forum session / MArt access](docs/FORUM_SESSION.md)

## Build and use the core

```sh
python3 tools/cloud_gradle.py -PcoreOnly build installDist --console=plain
build/install/mclorehub-importer/bin/mclorehub-importer \
  --input local-data/imports/lotc_current_lore_database.zip \
  --output local-data/candidates/new-import
build/install/mclorehub-importer/bin/mclorehub-importer catalogue \
  --pack local-data/candidates/new-import/pack.json \
  --output local-data/catalogue.json
```

Create the output parent directory first; output files/directories must be new.
With your own Java 21 JDK, use the same tasks via `./gradlew` (Windows:
`gradlew.bat`). Without `-PcoreOnly`, Gradle also configures/builds the client.
The root importer ZIP is a developer utility, not a Fabric mod JAR.

The private export and all generated/review/player data are ignored by Git. A
fresh clone requires supplying the original export separately. The private data
bundle includes matching pack.json/trust.json; it does not fabricate staff reviews.
