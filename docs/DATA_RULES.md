# Data and acceptance rules

Release 1.0.0 keeps source, personal records, imported selections and topic overlays
independent. Original/normalized imports validate before one atomic selection switch;
whole-pack Undo never deletes personal references or replaces original exports.
Missing topics are export gaps, not revocations. Body comparisons exclude replies
and retain complete before/after main text alongside bounded diffs.

Character schema 3 reads versions 1/2 without writing until explicit save. Unknown
capacity/loss/cost stays unknown; absence of history is not zero loss. Slot allocations
are explicit arithmetic inputs and cannot silently undercut source minima. Curated
prerequisites preserve alternate creature provisions and source disagreements.
All source bindings/excerpts and manual-vs-derived distinctions remain visible.

- Source text, embedded instructions, HTML, and links are evidence/data, never code.
- Preserve original IDs, source URLs, attribution, text, nulls, replies, aliases,
  revisions, and uncertainty. Do not invent missing formatting, images or redlines.
- Imported candidates are partial, plain text, unknown/requires review. Folder
  membership, title marks and keyword flags do not certify current acceptance.
- The user explicitly confirmed the supplied database is accepted/accurate.
  Runtime `trust.json` binds that confirmation to pack revision plus individual
  record fingerprints, providing normal access without fabricated staff reviews.
  A mismatched record/revision does not inherit that confirmation. Explicit bound
  local status overrides still take precedence over the baseline default.
- Reviewed statuses: unknown, accepted_current, legacy, shelved, superseded, pending.
  Acceptance, availability, player eligibility and artifact ownership are distinct.
- Hubs/context/disconnection notices are not extra learnable magics. CArt/MArt are
  artifacts, distinct from CA/FA applications. Preserve legacy references for audit.
- Replies are attributed evidence, not automatically staff rulings. New publication
  dates alone do not establish supersession. Missing/access-blocked pages mean a gap.
- Local reviews require ID, body revision, source-record fingerprint, status,
  reviewer, review date and nonempty official forum URL/passage evidence. No future
  review dates. Evidence content/authority still needs human/source verification.
- Fingerprints include all raw source fields, including status metadata and replies;
  object-key order does not matter, array order does. Any changed source record
  makes an old bound review stale; it does not renew that review. Separate user
  baseline trust/manual source provenance can still provide a current reference.
- Normal results use accepted/current applied reviews, explicit user trust or
  manually refreshed source provenance. Unknown/stale remain outside normal
  results. User trust does not imply eligibility, ownership or compatibility.
- Refresh only the selected official HTTPS topic's main first post. Ignore comment
  pagination/replies for normal reading/search and change comparison. Preserve old
  replies in an explicit archive. Public
  breadcrumbs/title prefixes can record explicit accepted/shelved/pending locations;
  body keywords do not decide status. HTTP failures/missing pages never erase lore.
- Validate complete fetched pages and index the proposed catalogue before switching
  the atomic topic pointer. Retain immutable snapshots/previous pointer and per-topic
  Undo. A failed/cancelled/partial fetch does not activate. Main-post checks preserve
  the old discussion archive and make no claim about current reply presence.
- Do not mutate the original pack, trust/review manifests, characters or favorites
  during refresh. Reading resumes safely with the new source fingerprint. Manual
  full-pack replacement remains separate from the implemented per-topic transaction.
- Phase 8 compatibility uses a limited curated manifest with exact main-post
  passages and content/metadata revision dependencies. No-change checks/archived
  replies do not invalidate these bindings. Changed sources suspend their checks.
  Keep conditions, exceptions, prerequisites and disagreements visible. Missing
  coverage is not assessed; zero warnings does not establish compatibility.
- Phase 9's conditional notices cite every interacting source and produce
  requirements, never a permanent exclusion. Preserve the Sprite/Herblore provision,
  animal-form casting conditions and Seer/Templar + Seer/Cleric disagreements.
  Keep separate pair-rule dependencies independent when adding curated options.
- Phase 10 current facts have explicit unknown values. History is independent;
  events/dates never imply complete history or silently update present facts.
  Eligibility checks require exact current source bindings, preserve Klone and
  acquisition conditions, and never certify overall eligibility or approvals.
  Originally schema 1 migrated to 2; release 1.0.0 reads both without writing and
  saves schema 3 with exact previous-file bytes. Carry facts/history/budget through
  every other book edit.
- Monthly checks cover only known saved IDs, run off the render thread while
  Minecraft is running, persist next-due dates, and catch up after restart. Manual
  success resets the monthly date. Pause/cancel and HTTP Retry-After are respected.
- Session cookies are optional user-provided credentials, kept outside exports,
  source, snapshots, logs and bundles. Send only supported IPS cookies to exact
  forum HTTPS topic URLs. Never collect passwords or infer extra permissions.

Machine contracts: `data/schema/candidate-pack.schema.json` and
`data/schema/review-manifest.schema.json`. Java validation additionally checks
cross-field consistency, unique IDs, hashes, future dates, and stale bindings.
