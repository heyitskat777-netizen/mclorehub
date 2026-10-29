# Personal installation

1. Use an instance of **Minecraft 1.21.11**, Java 21, Fabric Loader 0.20.1 or later,
   and Fabric API 0.141.6+1.21.11 (the verified combination).
2. Copy **mclorehub-0.1.0-phase5.jar** from `client/build/libs/` into that instance's
   `mods` folder. The core is included; no second importer JAR is needed. Do not
   install source JARs or developer CLI distributions as mods.
3. With Minecraft closed, create `config/mclorehub/packs/active/` in the instance.
   Copy the generated **pack.json** there. A separately supplied private candidate
   pack ZIP can be extracted there; it must contain pack.json directly in active,
   not another nested folder. Keep the previous pack/source backed up.
4. Optionally place a valid local review manifest as **reviews.json** beside it.
   See [Phase 3](PHASE_3.md). The imported database itself certifies no current status.
5. Enter a world and press **K** (configurable under Controls). Normal browsing shows
   reviewed current lore. **Audit** lets you inspect unreviewed/historical material.
   Search titles/aliases/main text/replies; filter categories or favorites.
6. Open a result to read. Use the wheel/arrows/Page Up/Down/Home/End to scroll,
   Previous/Next for source sections, and Source and review for evidence/dates.
   Save favorites, return with Back/Escape, and use Resume reading or K to reopen
   the last available document. Revisions are checked before restoring an offset.
7. Choose **Characters** to maintain local magic/CA/FA records. Approval labels are
   user-recorded. Compatibility remains not assessed until sourced rules exist.

The original export is plain text: missing original colors, images/fonts and
forum layout cannot be reproduced. Nothing automatically fetches forum changes.
For a replacement export, use the importer while the game is closed and back up
the previous pack/reviews before copying the new candidate. Reload files validates
it; old reviews become stale when bound source evidence changes. In-game staged
updates/rollback and automatic forum discovery are not implemented yet.

Back up the entire **config/mclorehub/personal/** directory for favorites, reading
state and characters. Keep it separate from pack replacement. Read-only warnings
mean the mod preserved invalid/externally changed files; inspect and back them up
before manual repair. Reload files/Reload sheets retry corrected files.

This development build was tested in a real Minecraft client using software
rendering. Your actual modpack, GPU, audio and authenticated multiplayer still need
personal acceptance testing. See [Phase 4](PHASE_4.md) and [Phase 5](PHASE_5.md).
