#!/usr/bin/env python3
"""Bind captured directory observations to the normalized export; never certify staff approval."""
import argparse, json
from pathlib import Path

p = argparse.ArgumentParser()
p.add_argument('--database', type=Path, required=True)
a = p.parse_args()
root = a.database
catalogue = json.loads((root / 'normalized/initial-catalogue.json').read_text())
statuses = json.loads((root / 'source-status.json').read_text())
export = json.loads((root / 'lotc_lore.json').read_text())
raw = {str(r['topic_id']): r for r in export['records']}
decisions = []
for d in catalogue['review_queue']:
    tid = d['id'].split(':')[-1]
    if tid not in statuses:
        continue  # Restricted snapshots retain their original confirmation and dates.
    s = statuses[tid]
    locations = '; '.join(x['title'] + ' (' + x['url'] + ')' for x in raw[tid]['source_location'])
    decisions.append(dict(document_id=d['id'], revision_sha256=d['revision_sha256'],
                          review_fingerprint=d['review_fingerprint'], status=s['status'],
                          reviewer='Automated official-directory observation; not independent staff approval',
                          reviewed_at=raw[tid]['source_snapshot_at'],
                          evidence=[dict(source_url=s['url'], passage=s['evidence'] + '. Observed location: ' + locations)]))
assert len(decisions) == len(statuses), 'Every captured public source must receive its bound status'
out = root / 'normalized/reviews.json'
with out.open('x') as f:
    json.dump(dict(schema_version=1, decisions=decisions), f, ensure_ascii=False, indent=2)
    f.write('\n')
print('Bound', len(decisions), 'directory observations; restricted records were not reverified.')
