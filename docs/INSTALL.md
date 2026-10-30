# Personal installation

1. Use an instance of **Minecraft 1.21.11**, Java 21, Fabric Loader 0.20.1 or later,
   and Fabric API 0.141.6+1.21.11 (the verified combination).
2. Copy **mclorehub-0.1.0-phase10.jar** from `client/build/libs/` into that instance's
   `mods` folder. The core is included; no second importer JAR is needed. Do not
   install source JARs or developer CLI distributions as mods. Remove the older
   MCLoreHub mod JAR when upgrading so two versions are not installed together.
3. With Minecraft closed, create `config/mclorehub/packs/active/` in the instance.
   Copy the supplied **pack.json and trust.json** there. The separate private trusted
   data ZIP can be extracted there; it must contain these files directly in active,
   not another nested folder. Keep the previous pack/source backed up.
4. `trust.json` records your confirmation that the supplied database is accepted/
   accurate, bound to its record fingerprints. All 488 references are available
   normally. Optional **reviews.json** remains available for explicit status overrides;
   see [Phase 3](PHASE_3.md). A different export needs its own explicit confirmation.
5. Enter a world and press **K** (configurable under Controls). Normal browsing shows
   trusted/current references. **Audit** also lets you inspect unconfirmed/historical material.
   Search titles/aliases/main text; filter categories or favorites. Discussion is
   excluded by default. Old replies remain under Source and review → Archived replies.
6. Open a result to read. Use the wheel/arrows/Page Up/Down/Home/End to scroll,
   Previous/Next for source sections, and Source and review for evidence/dates.
   Save favorites, return with Back/Escape, and use Resume reading or K to reopen
   the last available document. Revisions are checked before restoring an offset.
7. Choose **Characters** to maintain local magic/CA/FA records. **Choose lore** links
   an entry to the initial curated list; manual entry remains available. Approval
   labels are user-recorded. **Compatibility** shows initial cited checks, source
   disagreements and requirements, with explicit partial coverage. Changed sources
   suspend their checks without deleting your entries. Phase 9 offers 23 choices,
   26 rules and condition-specific notices. See [Phase 9](PHASE_9.md).
   **Known facts** records current soul/body/history facts; **History** records
   acquisitions/disconnections/transformations separately. Seven initial source-bound
   checks preserve unknowns and exceptions. Existing schema-1 sheets load unchanged;
   the first saved edit migrates to schema 2 with a previous-file backup. Older mod
   versions cannot read schema 2. See [Phase 10](PHASE_10.md).
8. In a document, choose **Refresh topic** to fetch its official forum topic and
   main first post, compare and save changes locally. Replies/pagination are not fetched.
   **Cancel refresh** stops a fetch.
   **Source and review → Undo refresh** restores that topic's previous saved version.
   The action does not search unrelated topics or change favorites/character sheets.

9. **Updates** opens monthly-check controls and optional forum-session import.
   Monthly checks are enabled by default and run while Minecraft is open. New pages
   get their first due date one calendar month after activation; existing successful
   checks use one month from that check. Overdue pages catch up on later launches.
   See [Phase 7](PHASE_7.md) and [forum session instructions](FORUM_SESSION.md).

Plain text is the selected format; richer formatting is out of scope. Monthly
checks compare saved main lore with known forum pages; they do not discover new topics.
For a replacement export, use the importer while the game is closed and back up
the previous pack/reviews before copying the new candidate. Reload files validates
it; matching user trust or explicit reviews determine normal access. Per-topic
updates use atomic snapshots/Undo. Whole-pack in-game imports and automatic forum
discovery are not implemented.

Back up the entire **config/mclorehub/personal/** directory for favorites, reading
state and characters. Keep it separate from pack replacement. Read-only warnings
mean the mod preserved invalid/externally changed files; inspect and back them up
before manual repair. Reload files/Reload sheets retry corrected files.

Back up **config/mclorehub/updates/** too if retaining topic revisions, Undo history
and monthly schedule. The original pack remains immutable. **auth/** contains
optional session credentials; do not share it or include it in database/source
bundles. Prefer signing in again on another machine.

This development build was tested in a real Minecraft client using software
rendering. Your actual modpack, GPU, audio and authenticated multiplayer still need
personal acceptance testing. See [Phase 4](PHASE_4.md) and [Phase 5](PHASE_5.md).
