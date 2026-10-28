#!/usr/bin/env python3
"""Independent source-preservation and catalogue checks; all output is private local-data."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
EXPECTED = '7bf60a188c0be27871fc09460002dc0921dd5bebf36f77ed129966542d33495b'

def check(value, message):
    if not value: raise RuntimeError(message)

def digest(path):
    with path.open('rb') as source: return hashlib.file_digest(source, 'sha256').hexdigest()

def verify(source, launcher, output):
    check(digest(source) == EXPECTED, 'Input is not the recorded baseline ZIP')
    with zipfile.ZipFile(source) as archive: original = json.loads(archive.read('lotc_lore.json'))
    output.mkdir(parents=True, exist_ok=False)
    first, second = output / 'first', output / 'second'
    def command(*args, expected=0):
        result = subprocess.run([str(launcher), *map(str, args)], capture_output=True, text=True)
        check(result.returncode == expected, f'CLI exited {result.returncode}: {result.stderr}')
    for target in [first, second]: command('--input', source, '--output', target)
    pack = json.loads((first / 'pack.json').read_text()); report = json.loads((first / 'report.json').read_text())
    check(report['documents'] == report['verified_content_hashes'] == report['requires_review'] == 488, 'Record/hash count mismatch')
    check(report['thread_posts'] == 1290 and report['artifact_documents'] == 243, 'Post/artifact counts mismatch')
    check(report['accepted_current'] == 0, 'Import certified unreviewed lore')
    records = {f"lotc:topic:{r['topic_id']}": r for r in original['records']}
    check(set(records) == {d['id'] for d in pack['documents']}, 'Stable ID mismatch')
    for doc in pack['documents']:
        raw = records[doc['id']]
        check(doc['source_record'] == raw, 'Lost/changed source fields')
        check(doc['blocks'][0]['text'] == raw['content_text'], 'Changed source body')
        check(doc['revision_sha256'] == hashlib.sha256(raw['content_text'].encode()).hexdigest(), 'Wrong body hash')
        check(doc['status']['value'] == 'unknown', 'Incorrect imported status')
    check(pack['source']['export_provenance'] == {k: v for k, v in original.items() if k != 'records'}, 'Lost export metadata')
    check((first / 'pack.json').read_bytes() == (second / 'pack.json').read_bytes(), 'Import is not repeatable')
    catalogue_path = output / 'catalogue.json'
    command('catalogue', '--pack', first / 'pack.json', '--output', catalogue_path)
    catalogue = json.loads(catalogue_path.read_text())
    check(catalogue['document_count'] == catalogue['needs_review_count'] == 488 and catalogue['current_count'] == 0,
          'Catalogue exposed unreviewed current lore')
    expected_roles = dict(player_option_candidate=233, navigation_hub=2, core_and_hub=1, background_context=8,
                          disconnection_notice=1, character_artifact=34, magical_artifact=209, unclassified=0)
    check(catalogue['role_counts'] == expected_roles, 'Audit role classification mismatch')
    check(len({r['id'] for r in catalogue['review_queue']}) == 488, 'Incomplete review queue')
    check(all(len(r['review_fingerprint']) == 64 for r in catalogue['review_queue']), 'Missing review binding')
    before = digest(first / 'pack.json'); sentinel = first / 'characters.json'; sentinel.write_text('personal sentinel')
    command('--input', source, '--output', first, expected=1)
    check(digest(first / 'pack.json') == before and sentinel.read_text() == 'personal sentinel', 'Existing data changed')
    bad = output / 'invalid.json'; bad.write_text('{}'); failed = output / 'invalid-candidate'
    command('--input', bad, '--output', failed, expected=1)
    check(not failed.exists() and digest(source) == EXPECTED, 'Invalid output/source mutation')
    command('catalogue', '--pack', first / 'pack.json', '--output', catalogue_path, expected=1)
    print('PASS: 488 complete records and 1,290 posts preserved; identical repeated packs')
    print('PASS: 488 review candidates, zero current records, exact source-audit role counts')
    print('PASS: existing output/personal sentinel/source ZIP unchanged; invalid input writes no candidate')
    print('Private evidence:', output)

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--input', type=Path, default=ROOT / 'local-data/imports/lotc_current_lore_database.zip')
    parser.add_argument('--launcher', type=Path, default=ROOT / 'build/install/mclorehub-importer/bin/mclorehub-importer')
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args(); verify(args.input.resolve(), args.launcher.resolve(), args.output.resolve())
