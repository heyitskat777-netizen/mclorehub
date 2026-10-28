# Phase 3 — Catalogue and acceptance-review layer

## Implemented boundary

An immutable catalogue validates importer candidates, retains titles/aliases/body
references and source roles, and applies separate local review decisions. It
generates a current-only list and audit queues. It does not connect data to the
Minecraft screen, verify forum authority, or implement search/characters/fetching.

## Read a catalogue

```sh
build/install/mclorehub-importer/bin/mclorehub-importer catalogue \
  --pack local-data/candidates/new-import/pack.json \
  --output local-data/catalogue.json
```

Optional `--reviews local-data/reviews.json` applies an explicit local manifest.
The output parent must exist; target must be new. Exit codes: 0 success, 2 invalid
options, 1 input/validation/write failure. Reports are staged beside their target;
input packs, reviews and existing outputs are never overwritten.

The report contains document/status/role counts, `current_documents`,
`review_queue`, `other_reviewed_documents`, and `orphaned_review_ids`. Normal player
queries must use current documents; the full catalogue is for audit. Hubs, context,
disconnection notices and artifacts remain distinct from player-option candidates.
No final magic/CA/FA eligibility taxonomy is inferred from these broad source roles.
Source authors are retained when available; unknown authors remain absent. The
export snapshot timestamp is distinct from the review date and does not certify
when a particular forum post was last checked live.

## Review manifest

Schema: [review-manifest.schema.json](../data/schema/review-manifest.schema.json).
Copy the ID, body revision and review fingerprint from the catalogue report into
a local decision with lower-case status, reviewer, ISO-8601 review time and at
least one official forum URL/passage citation. Never copy a synthetic test approval
into the real database. No real current review was created in this phase.

```json
{"schema_version": 1, "decisions": []}
```

Allowed statuses: unknown, accepted_current, legacy, shelved, superseded, pending.
Duplicate decisions, missing evidence/reviewer, invalid hashes/IDs/URLs, unknown
versions/statuses and future dates fail validation. Orphan decisions are reported,
not applied to another topic or deleted from the original manifest.

The fingerprint is SHA-256 of the recursively object-key-sorted raw source record,
serialized with `JsonSupport`'s fixed Gson settings. It retains array order, nulls,
status notes, audit metadata, source retrieval information and thread replies.
This is a deterministic change detector, not a signature or independent proof of
canon. Changed body OR changed raw metadata makes a bound review stale/unknown.
Equivalent object-key reordering does not. As-of/reviewer/evidence remain visible.

An applied accepted_current decision is the only route into current results.
This is user-recorded curation: citation presence is checked, quotation truth and
staff authority are not automatically verified. Use actual Lorekeeper/forum
sources before entering real decisions. No arbitrary expiration period is invented.

## Test gate and next phase

Run the core unit suite and independent full-export verifier. Positive synthetic
reviews must produce a current result, while changed body/status/replies and
unreviewed real imports must not. Compare all 488 raw records and 1,290 posts,
test repeatability/error exits, and verify role counts. See `STATUS.md` for actual
results and the current separate client-access blocker.

Next: fine-grained entity/eligibility representation and async client catalogue
loading/review presentation before current-only search and reader integration.
Do not mark M1's actual current coverage complete merely because this layer works.

Create source-only Git/ZIP backups before stopping. The active environment still
uses the older snapshot; a local file link or saved config draft is not durable
source publication. A published snapshot or authorized Git push is required.
