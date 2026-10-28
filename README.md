# MCLoreHub

Personal-use **Minecraft 1.21.11 Fabric** wiki mod, developed in tested phases.

The core imports the attached LoTC database, preserves source evidence, and now
provides a validated catalogue with separate acceptance reviews. The client source
opens an empty-library screen with K; data loading/search/favorites/characters are
not connected to it yet.

**Recovery note:** this environment reset to an empty checkout on October 28.
Source was reconstructed from the recorded implementation and the original ZIP
was recovered. Historical Phase 2 screenshots/binaries/caches were not restored.
Current core checks pass; current client revalidation is blocked by network policy.
See the status report for the exact distinction and persistence steps.

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
fresh clone requires supplying the original export separately. No real topics
have been reviewed as current by the software in this phase.
