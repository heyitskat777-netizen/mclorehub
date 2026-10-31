#!/usr/bin/env python3
"""Package an already validated private database and add exact-source spell search to SQLite."""
import argparse, collections, hashlib, json, sqlite3, zipfile
from pathlib import Path

p = argparse.ArgumentParser()
p.add_argument('--database', type=Path, required=True)
a = p.parse_args()
root = a.database
index = json.loads((root / 'spell-index.json').read_text())
export = json.loads((root / 'lotc_lore.json').read_text())
pack = json.loads((root / 'normalized/pack.json').read_text())
reviews = json.loads((root / 'normalized/reviews.json').read_text())
records = {str(r['topic_id']): r for r in export['records']}
assert len(records) == len(pack['documents']) == 801
assert len(reviews['decisions']) == 592
assert collections.Counter(d['status'] for d in reviews['decisions']) == dict(accepted_current=564, legacy=27, shelved=1)
assert sum(r.get('category') == 'magical_artifacts' for r in records.values()) == 209

def utf16_slice(text, start, end):
    return text.encode('utf-16-le')[start * 2:end * 2].decode('utf-16-le')

for ability in index['abilities']:
    r = records[ability['sourceId'].split(':')[-1]]
    assert utf16_slice(r['content_text'], ability['start'], ability['end']) == ability['information']
    if ability['parentStart'] >= 0:
        assert utf16_slice(r['content_text'], ability['parentStart'], ability['parentEnd']) == ability['parent_context']
assert len({x['id'] for x in index['abilities']}) == len(index['abilities'])
with sqlite3.connect(root / 'lotc_lore.sqlite') as db:
    assert db.execute('PRAGMA integrity_check').fetchone()[0] == 'ok'
    assert db.execute('SELECT COUNT(*) FROM lore').fetchone()[0] == len(records)
    assert db.execute('SELECT COUNT(*) FROM thread_posts').fetchone()[0] == sum(len(r.get('thread_posts', [])) for r in records.values())
    for tid, body, at, outline in db.execute('SELECT topic_id,content_text,source_snapshot_at,source_outline FROM lore'):
        r = records[tid]
        assert body == r['content_text'] and hashlib.sha256(body.encode()).hexdigest() == r['content_sha256']
        assert at == r['source_snapshot_at'] and json.loads(outline) == r.get('source_outline', [])
    # New-table guard makes an accidental second packaging attempt fail rather than mutate a released DB.
    db.executescript('CREATE TABLE spells(id TEXT PRIMARY KEY,topic_id TEXT,name TEXT,minimum_tier INTEGER,stage TEXT,branch TEXT,extra_investment INTEGER,source_url TEXT,source_snapshot_at TEXT,source_fingerprint TEXT,source_start_utf16 INTEGER,source_end_utf16 INTEGER,tier_evidence TEXT,information TEXT,parent_context TEXT);CREATE VIRTUAL TABLE spells_fts USING fts5(name,information,parent_context,id UNINDEXED,topic_id UNINDEXED);')
    for s in index['abilities']:
        tid = s['sourceId'].split(':')[-1]
        db.execute('INSERT INTO spells VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)',
                   (s['id'], tid, s['name'], s['minimumTier'], s['stage'], s['branch'], int(s['extraInvestment']),
                    s['source_url'], s['source_snapshot_at'], s['sourceFingerprint'], s['start'], s['end'],
                    s['tierEvidence'], s['information'], s['parent_context']))
        db.execute('INSERT INTO spells_fts VALUES(?,?,?,?,?)', (s['name'], s['information'], s['parent_context'], s['id'], tid))
    db.execute('INSERT INTO metadata VALUES(?,?)', ('spell_index_basis', json.dumps(index['basis'])))
    db.execute('INSERT INTO metadata VALUES(?,?)', ('spell_offset_units', json.dumps('UTF-16 code units, matching Java source text')))
    assert db.execute('PRAGMA integrity_check').fetchone()[0] == 'ok'
    assert db.execute('SELECT COUNT(*) FROM spells').fetchone()[0] == len(index['abilities'])
    assert db.execute("SELECT COUNT(*) FROM spells_fts WHERE spells_fts MATCH 'Gust'").fetchone()[0] > 0

notes = f'''MCLoreHub private lore database — 2026-10-30, mod 1.1.0+

801 retained/additional topic records: 592 freshly captured public main posts and
209 restricted MArts retained from the original September 21 snapshot, NOT reverified.
Normal references: 773 (534 implemented + 30 policy/reference + 209 retained MArts).
Audit only: 27 legacy and one outdated Elder Tree. These explicit status overrides
are bound to exact source records. Directory observation is not staff approval or
player eligibility. Archived replies are retained but were not refreshed or used
for the new spell index. Image-contained rules/external documents remain gaps.

Import mclorehub-lore-database-2026-10-30.zip through K → Database, inspect preview,
then Activate import. Supplied status overrides are retained with either trust mode.
Characters, favorites and reading positions remain separate. Database Undo is available.
Keep a backup of the old configuration. This ZIP contains the normalized pack,
matching personal confirmation, directory observations and an offline spell companion.
The mod derives its live spell index from the validated main text/outline.

{len(index['abilities'])} source-bound ability/reference sections from {len(index['sources'])} selected practice sources.
Unresolved tiers/branches/prerequisites are explicit; there is no automatic learning,
approval or guaranteed completeness. Exact spell and parent excerpts plus complete
practice rules are available. The separate SQLite file has lore_fts and spells_fts
for external search; JSON retains the original LoreDB record schema with added provenance.
UTF-16 source offsets match Java. minimum_tier=-1 means unresolved, never highest tier.

Keep this bundle private: it includes retained restricted artifact records.
No player sheets, personal settings, session cookies or credentials are included.
'''
(root / 'README.txt').write_text(notes)
files = ['lotc_lore.json', 'lotc_lore.sqlite', 'spell-index.json']
manifest = dict(schema_version=1, record_count=801, public_main_posts=592, retained_restricted_unverified=209,
                normal_references=773, legacy=27, outdated=1, spell_reference_sections=len(index['abilities']),
                unresolved_tier_sections=sum(s['minimumTier'] < 0 and not s['stage'] for s in index['abilities']),
                source_counts=index['sources'], files={})
for f in files + ['normalized/pack.json', 'normalized/trust.json', 'normalized/reviews.json']:
    data = (root / f).read_bytes()
    manifest['files'][f] = dict(bytes=len(data), sha256=hashlib.sha256(data).hexdigest())
(root / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')
archive = root / 'mclorehub-lore-database-2026-10-30.zip'
with zipfile.ZipFile(archive, 'x', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as z:
    for f in ('pack.json', 'trust.json', 'reviews.json'):
        z.write(root / 'normalized' / f, f)
    for f in ('spell-index.json', 'README.txt', 'manifest.json'):
        z.write(root / f, f)
with zipfile.ZipFile(archive) as z:
    assert z.testzip() is None
    assert sum(f.file_size for f in z.infolist()) <= 64 << 20
manifest['archive'] = dict(bytes=archive.stat().st_size, sha256=hashlib.sha256(archive.read_bytes()).hexdigest())
(root / 'verification.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')
print('Packaged:', archive, json.dumps(manifest['archive']))
print('SQLite integrity/FTS/exact UTF-16 excerpts/IDs/status counts/ZIP limits passed.')
