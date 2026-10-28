# Phases 1–2 and recovery

Phase 1 built an offline Java importer: ZIP/JSON limits and strict UTF-8/JSON,
stable topic IDs, content hashes, raw provenance/replies/nulls, plain-text blocks,
review gaps, repeatability, and separate new candidate/report output. Every status
remains unknown. The baseline has 488 topics and 1,290 posts.

Phase 2 added the Fabric client shell for exactly Minecraft 1.21.11. Pins:
Java 21, Gradle 9.8.2, Loom 1.17.21, Loader 0.20.1, API 0.141.6+1.21.11,
official Mojang mappings. K opens an empty-library screen, configurable in Controls;
Back/Escape close it. It does not replace active screens or load data on keypress.

Historical evidence: 47 unit tests and one real Minecraft scenario passed. A
duplicate background/blur call was removed because 1.21.11 draws the background
before `Screen.render`. Screenshots at GUI scales 1/2 were inspected. The tests
used Xorg/Mesa software GL, not a mock UI; authenticated services were unavailable.

On October 28 the workspace had reverted to an empty unborn checkout. No commits,
remote refs, recoverable Git objects, source files, caches, old JAR or screenshots
were present. The saved config draft survived. Source was reconstructed from the
recorded implementation and the original source ZIP re-downloaded/hash-verified.
This is a reconstruction, not byte-identical restoration of the old artifacts.

Current importer and catalogue tests pass; see `STATUS.md` for actual counts.
After environment publication, Fabric/Mojang requests returned HTTP 200 and the
reconstructed client passed a full build plus native scenario with locks/checksums
enforced. Scale screenshots were inspected. These are new results, separate from
the historical pass; authenticated multiplayer and user hardware remain untested.

Core CLI:

```sh
build/install/mclorehub-importer/bin/mclorehub-importer \
  --input local-data/imports/lotc_current_lore_database.zip \
  --output local-data/candidates/new-import
```

The output parent can be created as needed; the target must not already exist.
The installed distribution has dependencies/launchers; a root importer JAR is not
the Minecraft mod. No active-pack/personal state is changed by the importer.

Future client commands and pins are retained in source. No dedicated-server EULA
flag was added. The headless helper starts a local-only Xorg and stops only its
own display. Assets require approximately 432 MiB over 4,591 files; download
concurrency is 8, compilation workers 2, heap 2 GiB. Preserve verified caches.
