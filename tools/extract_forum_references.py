#!/usr/bin/env python3
"""Offline extraction of captured forum pages using the pinned Java/Jsoup parser."""
import argparse,json,hashlib,subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('--captures',type=Path,required=True);p.add_argument('--topics',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args()
if a.output.exists():raise SystemExit('Choose a new output directory')
a.output.mkdir(parents=True);compiled=a.output/'parser';compiled.mkdir()
jdk=ROOT.parent/'.mclorehub-tools/jdk-21.0.13+9/bin';libs=ROOT/'build/install/mclorehub-importer/lib/*'
subprocess.run([str(jdk/'javac'),'-cp',str(libs),'-d',str(compiled),str(ROOT/'tools/ForumReference.java')],check=True)
proc=subprocess.Popen([str(jdk/'java'),'-cp',str(libs)+':'+str(compiled),'ForumReference'],stdin=subprocess.PIPE,stdout=subprocess.PIPE,text=True)
count=outlined=0
try:
 for file in sorted(a.topics.glob('*.json')):
  record=json.loads(file.read_text());tid=record['id'];meta=record['http'];url=meta['requested_url'];key=hashlib.sha256(url.encode()).hexdigest();html=a.captures/(key+'.html')
  if meta.get('http_status')!=200 or hashlib.sha256(html.read_bytes()).hexdigest()!=meta['sha256']:raise ValueError('Invalid HTTP capture: '+tid)
  proc.stdin.write(json.dumps(dict(path=str(html.resolve()),url=meta.get('final_url',url),mode='topic',id=tid))+'\n');proc.stdin.flush();parsed=json.loads(proc.stdout.readline())
  if parsed.get('parse_error') or parsed['body']!=record['body']:raise ValueError('Parsing changed source text: '+tid)
  record.update(parsed);(a.output/(tid+'.json')).write_text(json.dumps(record,ensure_ascii=False,indent=2)+'\n');count+=1;outlined+=bool(parsed['source_outline'])
  if count%50==0:print('STRUCTURED',count,'pages;',outlined,'with headings',flush=True)
finally:
 proc.stdin.close();proc.wait(timeout=20)
print('EXTRACTION_COMPLETE',count,'pages;',outlined,'with headings')
