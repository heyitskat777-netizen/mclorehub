# Phase 6 — Trusted supplied database and manual topic refresh

The user confirmed the attached database is accepted/accurate and dropped richer
formatting. Its separate `trust.json` records that confirmation against the pack
revision and every document fingerprint. All 488 references appear in normal
browsing; there is no new per-record acceptance-review requirement. The raw export
and candidate pack remain unchanged, and no staff review/citation is fabricated.
Normal roles, CA/FA versus artifacts, eligibility and compatibility remain distinct.

## Refresh one document

Open Tawkin or another lore document and select **Refresh topic**. The worker checks
public forum access policy, fetches that topic's first page and its pagination,
extracts the main post and attributed replies, compares text/metadata and prepares
the new catalogue/search index. On success an atomic local pointer activates that
topic's immutable snapshot. The reader reopens with the saved version; subsequent
unchanged checks retain its source fingerprint/position. The first normalized
snapshot may reset an older imported-source position. Changed text resets safely.
The source detail page shows actual fetch time and visited URLs separately from
the original export snapshot date.

**Cancel refresh** stops fetching without activating partial data. Closing the
reader does not cancel an explicitly requested background refresh; reopening it
shows the saved version. **Source and review → Undo refresh** restores only that
topic, preserving other topic updates. The first Undo returns to the supplied
baseline; later updates can toggle the immediately previous saved version.
Immutable snapshot files remain available for manual recovery; no automatic prune
or whole-library discovery is performed.

Unreadable/login-required/challenge/404/server-error pages, failed pagination,
timeouts, cancellation, layout changes and invalid data retain stored lore. Missing
replies are retained and marked previously captured/not returned, since partial
access is not evidence of deletion. Explicit forum breadcrumb/title-prefix status
can record moves into shelving/pending/supersession; body keywords do not infer it.

## Bounds and storage

- Official HTTPS forum hosts and the same numeric topic ID only. Redirects/pagination
  are checked before requests; login/search/discovery/profile/media URLs are not fetched.
- Sequential requests at least one second apart, 30 seconds between attempts on
  the same topic, HTTP Retry-After backoff, one active refresh and cancellation.
- 20 pages, 1000 posts, 4 MiB per response, 16 MiB per topic, bounded request timeouts
  and overall pagination deadline. Oversized/incomplete results fail as a unit.
- `updates/snapshots/<sha256>.json` is immutable; `updates/active.json` is the atomic
  per-topic pointer, with `active.previous.json` retained for repair. Pointer entries
  also bind to the original record fingerprint, so another baseline does not inherit
  unrelated overlays. The source pack and `personal/` files are never rewritten.
- Snapshot checksums, strict schema validation, external-pointer edit detection and
  index-before-activation protect the last good state. Orphan staged files are safe
  and not automatically deleted. Aggregate active snapshot input is bounded to 64 MiB.

No login credentials are requested/stored. This implementation supports public
pages; authenticated/restricted forum sources need a future supported access path.
The Java client honors configured HTTPS proxy routing and normal TLS trust. In
this cloud, live CLI verification used the platform CA store explicitly; it did
not disable TLS verification. A normal local Minecraft installation uses Java trust.
The cloud helper also passes that verified platform trust-store path to game
launches, so native refresh uses the same supported proxy/TLS route as the CLI.

Machine contracts: [baseline confirmation](../data/schema/baseline-trust.schema.json),
[topic snapshot](../data/schema/topic-snapshot.schema.json), and
[active pointer](../data/schema/topic-pointer.schema.json). Java additionally checks
topic/host identity, checksums, time/size bounds, duplicate post IDs and base affinity.

## CLI and checks

```sh
mclorehub-importer trust --pack pack.json --output new-trust.json
mclorehub-importer refresh --library path/to/config/mclorehub --document lotc:topic:198584
mclorehub-importer undo --library path/to/config/mclorehub --document lotc:topic:198584
```

`trust` is an explicit local confirmation command, not an automatic import side
effect. It refuses existing output. The packaged private data ZIP supplies the
manifest for this user's already-authorized source database.

Unit/integration tests cover confirmation bindings, HTTP/robots/redirect/pagination
limits, source status, immutable snapshots, atomic activation, per-topic rollback,
unchanged-version history, corrupt/external edits, cancellation, concurrent local
reload and preservation of pack/favorites/characters. Native tests exercise the
refresh button, saved content, restart, Undo, HTTP failure and cancellation.
Real Java HTTPS and an actual Minecraft button flow both fetched Tawkin's **two
pages and 23 replies**, activated the update, verified restart persistence and left
the original pack unchanged. Final verification passed **179 core tests**, the
full build, and all native scenarios; see STATUS.md. The final fixture run did not
repeat the live network request after the last concurrency-only correction.

Remaining: sourced compatibility conditions/exceptions and entity eligibility,
curated character lore selection, optional whole-pack import UX, and actual modpack/
authenticated-server/physical-hardware acceptance. Rich formatting and a new audit
of the supplied baseline are out of scope under the user's latest instructions.
