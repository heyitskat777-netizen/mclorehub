#!/usr/bin/env python3
"""Rebuild an offline LoreDB export from validated main-post captures; never activates user data."""
import argparse,copy,csv,datetime,hashlib,json,sqlite3,subprocess,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('--baseline',type=Path,required=True);p.add_argument('--references',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args()
if a.output.exists():raise SystemExit('Choose a new output directory; previous databases are preserved')
a.output.mkdir(parents=True)
with zipfile.ZipFile(a.baseline) as z:old=json.loads(z.read('lotc_lore.json'))
records={str(r['topic_id']):copy.deepcopy(r) for r in old['records']};at=datetime.datetime.now(datetime.timezone.utc).isoformat();statuses={};counts={'public_main_posts':0,'retained_restricted':0}
CATEGORIES={'559':('world','world_general'),'910':('world','starting_locations'),'1773':('world','maplore'),'578':('world','legends'),'564':('deity','deity_lore'),'565':('deity','aenguls'),'566':('deity','daemons'),'567':('deity','patrons_deities'),'913':('deity','dragonkin'),'914':('creature','event_creatures'),'1570':('creature','bestiary')}
for file in sorted(a.references.glob('*.json')):
 d=json.loads(file.read_text());tid=d['id']
 if d.get('parse_error') or d['http'].get('http_status')!=200 or not d.get('body'):raise ValueError('Incomplete source capture '+tid)
 row=records.get(tid);members=d['listing']['memberships'];scopes={x['scope'] for x in members};labels=[b['title'].lower() for b in d.get('breadcrumbs',[])]
 state='shelved' if any('outdated lore' in s or s in ('denied lore','shelved lore') for s in labels) else 'legacy' if 'legacy lore' in labels or 'legacy' in scopes else 'accepted_current'
 if row is None:
  family,category=CATEGORIES.get(members[0]['forum_id'],('reference','lore_policy' if 'policy_reference' in scopes else 'legacy_lore'))
  if tid=='263819':family,category='magic','deity'
  row={'topic_id':tid,'title':'','url':d['http']['final_url'],'author':None,'published_text':d.get('main_times',[''])[0] if d.get('main_times') else None,'family':family,'category':category,'tags':[],'status_note':'','scrape_error':'','search_aliases':[],'audit_role':'player-use candidate; requires source review' if tid=='263819' else 'background/context; not a player option'}
 row.update(title=d['title'],url=d['http']['final_url'],author=d.get('author') or None,content_text=d['body'],content_sha256=hashlib.sha256(d['body'].encode()).hexdigest(),word_count=len(d['body'].split()),source_snapshot_at=d['http']['checked_at'],source_outline=d.get('source_outline',[]),scrape_error='',verification='Public main post captured over official HTTPS; directory/source provenance recorded; no independent staff decision or reply/image audit',source_location=[b for b in d.get('breadcrumbs',[]) if '/forums/forum/' in b['url']])
 row['coverage_notes']=['main_post_only','images_and_external_documents_not_transcribed','archived_replies_not_refreshed']
 if state!='accepted_current':row['status_note']='Legacy Lore: accepted but no longer obtainable' if state=='legacy' else 'Source is in Recently Outdated Lore/archived noncurrent material'
 records[tid]=row;statuses[tid]={'status':state,'url':row['url'],'evidence':'Legacy Lore — accepted but no longer obtainable' if state=='legacy' else 'Breadcrumb: Recently Outdated Lore; Outdated Lore Archives' if state=='shelved' else 'Official implemented/policy directory source'};counts['public_main_posts']+=1
for tid,row in records.items():
 if row.get('category')=='magical_artifacts' and tid not in statuses:
  row['source_snapshot_at']=old['metadata'].get('updated_utc',old['metadata']['generated_utc']);row['coverage_notes']=['retained_restricted_snapshot_not_reverified','last_known_approved_archive_membership','images_and_external_documents_not_transcribed'];counts['retained_restricted']+=1
metadata=copy.deepcopy(old['metadata']);metadata.update(generated_utc=at,updated_utc=at,record_count=len(records),scope='Rebuilt public current/legacy/policy main posts with retained restricted MArt snapshot. Directory membership is provenance, not player eligibility or independent staff approval.',rebuild_counts=counts,previous_database_sha256=hashlib.sha256(a.baseline.read_bytes()).hexdigest(),retained_restricted_snapshot_at=old['metadata'].get('updated_utc'),coverage_limits=['Restricted MArts not live verified','Image-contained rules/external documents/reply amendments not transcribed','Legacy and archived entries require explicit noncurrent overrides'])
export={**{k:v for k,v in old.items() if k not in ('metadata','records')},'metadata':metadata,'records':sorted(records.values(),key=lambda r:int(r['topic_id']))}
source=a.output/'lotc_lore.json';source.write_text(json.dumps(export,ensure_ascii=False,indent=2)+'\n');(a.output/'source-status.json').write_text(json.dumps(statuses,ensure_ascii=False,indent=2)+'\n')
# Keep the original SQL column names and add source/outline provenance and structured references.
with sqlite3.connect(a.output/'lotc_lore.sqlite') as db:
 db.executescript('CREATE TABLE metadata(key TEXT PRIMARY KEY,value TEXT);CREATE TABLE lore(id INTEGER PRIMARY KEY,title TEXT,url TEXT UNIQUE,author TEXT,published_text TEXT,family TEXT,category TEXT,tags TEXT,status_note TEXT,content_text TEXT,word_count INTEGER,scrape_error TEXT,topic_id TEXT UNIQUE,artifact_type TEXT,verification TEXT,source_snapshot_at TEXT,source_outline TEXT,coverage_notes TEXT,observed_status TEXT);CREATE VIRTUAL TABLE lore_fts USING fts5(title,content_text,topic_id UNINDEXED);CREATE TABLE thread_posts(post_id TEXT PRIMARY KEY,topic_id TEXT,author TEXT,published TEXT,url TEXT,content_text TEXT,is_original INTEGER,links_json TEXT,images_json TEXT);CREATE TABLE artifact_sources(artifact_type TEXT PRIMARY KEY,archive_url TEXT,status TEXT,checked_date TEXT,indexed_topics INTEGER,coverage_json TEXT);')
 db.executemany('INSERT INTO metadata VALUES(?,?)',[(k,json.dumps(v,ensure_ascii=False)) for k,v in metadata.items()])
 columns=['title','url','author','published_text','family','category','tags','status_note','content_text','word_count','scrape_error','topic_id','artifact_type','verification','source_snapshot_at','source_outline','coverage_notes','observed_status']
 for r in export['records']:
  row={**r,'observed_status':statuses.get(str(r['topic_id']),{}).get('status','retained_last_known_accepted')}
  values=[json.dumps(row.get(c,[]),ensure_ascii=False) if c in ('tags','source_outline','coverage_notes') else row.get(c) for c in columns];db.execute('INSERT INTO lore('+','.join(columns)+') VALUES('+','.join('?' for _ in columns)+')',values)
  db.execute('INSERT INTO lore_fts(title,content_text,topic_id) VALUES(?,?,?)',(r['title'],r['content_text'],r['topic_id']))
  for post in r.get('thread_posts',[]):db.execute('INSERT OR IGNORE INTO thread_posts VALUES(?,?,?,?,?,?,?,?,?)',(str(post['post_id']),str(r['topic_id']),post.get('author'),post.get('published'),post.get('url'),post.get('content_text'),post.get('is_original'),json.dumps(post.get('links',[]),ensure_ascii=False),json.dumps(post.get('images',[]),ensure_ascii=False)))
 for x in metadata.get('artifact_sources',[]):db.execute('INSERT INTO artifact_sources VALUES(?,?,?,?,?,?)',(x['artifact_type'],x['archive_url'],x['status'],x['checked_date'],x['indexed_topics'],json.dumps(x,ensure_ascii=False)))
 assert db.execute('PRAGMA integrity_check').fetchone()[0]=='ok'
 assert db.execute('SELECT COUNT(*) FROM lore').fetchone()[0]==len(records)
print('Database export created:',len(records),'records;',counts,flush=True)
