# Ordered creation plan

Release 1.1.0 follows docs/RELEASE_1_1.md: unify Add lore, derive exact source-bound
spell cards with explicit tiers/conditions, test stable search/continuous references
and source refresh invalidation, rebuild the private main-source database, validate
801/773 counts and old MArt provenance, then run all native gates with both old and
new imports. No public corpus publication or inferred compatibility recuration.


Post-release 1.0.2 addresses user testing: retain the search editor through async
results, slow typing and mid-query edits; replace category cycling with explicit
navigation/result cards; center/space plain text and scroll continuously across
bounded cached layouts; add the source-backed Klone CA reference. Regress search
selection/reload, wheel boundaries/reverse, Home/End, resize and restart in actual
Loader 0.19.5 Minecraft, then verify the updated direct download and private exclusions.

Post-release 1.0.1 targets the user's Fabric Loader 0.19.5 installation: lower the
build/metadata minimum together, verify the new pinned artifacts, run actual client
acceptance on exactly 0.19.5, preserve existing data and publish a direct JAR download.
Keep Minecraft 1.21.11, Java 21, Fabric API and data schemas unchanged.

Release 1.0.0 completes the remaining implementation requested after Phase 10,
including the suggested import/comparison polish. Continue through all acceptance
gates in docs/RELEASE_1_0.md without another Continue/approval question. Actual
results are in STATUS.md. Historical milestone notes below describe earlier scope.

Latest scope: the supplied database is user-confirmed accepted/accurate; no new
per-record audit gate is required. Plain text is sufficient. Phase 7 defaults to
main-post-only reading/search/comparison and monthly automatic checks of saved
pages, plus manual refresh, persistent validated activation and Undo. Old discussion
is an explicit archive. Optional local forum-session import enables requests with
the user's existing account permissions; real MArt login requires acceptance testing.
Earlier scope notes below are historical where superseded.

At each phase: inspect status/user changes, implement a bounded slice, run relevant
positive/negative tests, fix setup/code failures, record evidence, back up source,
and stop with the next step. Never replace useful tests with trivial passing checks.

1. **M0: Fabric shell.** Verify exact 1.21.11 toolchain; pin and verify wrapper;
   build client-only metadata; keybinding and empty screen; real game input/render
   checks. Historically passed in Phase 2; reconstructed source also passed the
   full build and native scenario after environment publication restored access.
2. **M1: Import and review.** Validate ZIP/JSON limits, counts, IDs, hashes, provenance,
   replies and nulls; repeat imports safely; derive conservative roles; separate
   explicit current-status evidence from raw source. Phase 3 adds catalogue/review
   manifests and stale/orphan handling. Phase 4 connects async client loading and
   source/review presentation. Phase 6 trusts the user-confirmed supplied database;
   no fresh acceptance audit is required. Fine-grained entities remain future work.
3. **M2: Search/favorites.** Index reviewed current data, aliases/categories/body;
   deterministic ranking/snippets, cancellation/pagination and coverage information;
   persist favorites by stable ID. Test known queries, Unicode, filters, empty results,
   full-corpus performance, restart and missing references. Phase 4 implements
   deterministic current/audit search, categories, pagination and saved favorites.
4. **M3: Reader/resume.** Plain-text reader with provenance/status/as-of dates. Resume
   by document/revision/block/offset. Test long text, scales, changed/deleted sections,
   link handling, restarts, and source formatting comparisons in Minecraft.
   Phase 4 implements bounded plain-text main/reply reading, source/review details,
   URL copying and stable resume. Rich-source formatting/images are out of scope.
5. **M4: Character sheets.** Independent versioned storage; create/select/rename/delete
   with recovery; magic/branch/tier and CA/FA, optional race/application links/notes;
   unknown slot costs stay unknown. Test multiple characters, migration, restart,
   corrupt files, missing lore and pack replacement without losing personal data.
   Phase 5 implements manual magic/CA/FA sheets, independent storage, selection,
   entries/links/statuses/notes, rename, archive/restore and previous-file recovery.
   Curated lore selection, entity eligibility and richer editing remain future work.
6. **M5: Compatibility.** Curate rules with exact evidence and revision dependencies;
   evaluate conditions/exceptions/acquisition history and unknown facts. Test real
   reviewed examples, conflicting sources, stale rules, missing coverage and exceptions.
   Unknown does not mean compatible; warnings do not prohibit sheet editing.
   Phase 8 adds eight explicit MAGIC/CA/FA choices, advisory main-revision-bound
   exclusions, source disagreement and conditional requirements notices. Selection
   and checks survive no-change refresh and suspend after changed sources. Broader
   curated coverage, typed eligibility/history and complete exception handling remain.
   Phase 9 expands to 23 choices/26 rules, adds conditional notices and a source
   coverage directory, and tests per-rule revision isolation. Typed facts/history
   remain a separate next slice; no positive compatibility state is implied.
   Phase 10 adds explicit current facts, independent history, seven narrow checks
   and transactional schema-1-to-2 migration. Broader facts/rules, automatic slot
   accounting and chronological inference remain unimplemented.
7. **M6: Updates/recovery.** In-game import/review; added/changed/missing/status diffs;
   staged pack and index; atomic active pointer; backup/rollback; preserve favorites,
   character refs and review history. Test partial snapshots, cancellation, disk/crash
   failures, invalid schemas, migrations and restart. Never infer deletion from a gap.
8. **M7: Forum fetching.** Verify permitted sources/access; supported feeds/API if
   available; pagination, amendments, provenance, safe redirects, limits, caching,
   cancellation/backoff; restricted access only through authorized supported routes.
   Reuse M6 review pipeline. A missing page is not proof of revocation.
   Phase 6 provides the selected-topic public-page fetcher, complete pagination,
   strict host/topic redirects, cancellation/backoff, atomic snapshots and Undo.
   There is no global crawler or mandatory pre-activation review dialog. Whole-pack
   import UX/authenticated fetching remain optional later work.
9. **M8: Personal release.** Full clean install and actual client acceptance sequence;
   correct pins/metadata; source and binary hashes; private-data exclusions; tested
   install/start/backup guide; honest remaining coverage and modpack limitations.

No later milestone is complete merely because infrastructure exists. Historical
tests must not be presented as current-instance evidence after a reset.
