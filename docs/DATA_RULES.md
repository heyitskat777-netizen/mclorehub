# Data and acceptance rules

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
- Refresh only the selected official HTTPS topic, following its pagination. Public
  breadcrumbs/title prefixes can record explicit accepted/shelved/pending locations;
  body keywords do not decide status. HTTP failures/missing pages never erase lore.
- Validate complete fetched pages and index the proposed catalogue before switching
  the atomic topic pointer. Retain immutable snapshots/previous pointer and per-topic
  Undo. A failed/cancelled/partial fetch does not activate. Old replies absent from
  the public refresh are retained and labeled as previously captured, not deleted.
- Do not mutate the original pack, trust/review manifests, characters or favorites
  during refresh. Reading resumes safely with the new source fingerprint. Manual
  full-pack replacement remains separate from the implemented per-topic transaction.
- Compatibility later requires exact source passages, conditions, exceptions,
  prerequisites and revision dependency. Missing coverage is not assessed.

Machine contracts: `data/schema/candidate-pack.schema.json` and
`data/schema/review-manifest.schema.json`. Java validation additionally checks
cross-field consistency, unique IDs, hashes, future dates, and stale bindings.
