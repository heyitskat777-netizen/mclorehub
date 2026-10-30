# MCLoreHub

Personal-use **Minecraft 1.21.11 Fabric** wiki mod, developed in tested phases.

The core imports the attached LoTC database, preserves source evidence, and now
provides a validated catalogue with separate acceptance reviews. The Fabric client
loads local packs asynchronously and supports current/audit browsing, title/alias/
main-text search, categories, a plain-text reader, favorites and reading resume.
Local character sheets support magic branch/tier, separate CA/FA entries, links,
user-recorded statuses, notes and archive/restore. The supplied database is trusted
using the user's confirmation. Main-post refresh compares saved lore with the
forum, saves changes persistently and supports Undo. Monthly automatic checks,
pause controls and optional explicitly imported forum sessions are available.
A curated character lore picker and initial cited compatibility checks are available.
Coverage now includes 23 choices and 26 rules; unassessed combinations, conditional
provisions and conflicting source statements remain explicit. See Phase 9 for scope.

**Recovery note:** this environment reset to an empty checkout on October 28.
Source was reconstructed from the recorded implementation and the original ZIP
was recovered. Historical Phase 2 screenshots/binaries/caches were not restored.
Current core checks pass, and after environment publication the reconstructed
client passed its full build and native input/render scenario. See the status
report for the exact distinction, limits and persistence steps.

## Guides

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
