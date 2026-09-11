#!/usr/bin/env python3
import json, os, signal, subprocess, sys, time
from datetime import datetime, timedelta
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CFG = ROOT / 'backend' / 'programacao.json'
MEDIA = ROOT / 'media' / 'programas'
STATE = ROOT / 'automation' / 'status.json'
HOST = os.getenv('CASTER_HOST', 'sapircast.caster.fm')
PORT = os.getenv('CASTER_PORT', '19513')
MOUNT = os.getenv('CASTER_MOUNT', 'QKnuH').lstrip('/')
PASSWORD = os.getenv('CASTER_SOURCE_PASSWORD', '')
USER = os.getenv('CASTER_SOURCE_USER', 'source')
BITRATE = os.getenv('CASTER_BITRATE', '96k')

proc = None
current = None


def load_schedule():
    with open(CFG, encoding='utf-8') as f:
        return json.load(f)


def minutes(hm):
    h, m = map(int, hm.split(':'))
    return h*60+m


def slot_now(now=None):
    now = now or datetime.now()
    n = now.hour*60 + now.minute
    for item in load_schedule():
        start, end = minutes(item['inicio']), minutes(item['fim'])
        if start <= n < end:
            return item
    return None


def write_state(**extra):
    STATE.parent.mkdir(parents=True, exist_ok=True)
    data = {'running': proc is not None and proc.poll() is None, 'updated_at': datetime.now().isoformat(timespec='seconds')}
    if current: data.update(current)
    data.update(extra)
    STATE.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding='utf-8')


def stop_proc():
    global proc
    if proc and proc.poll() is None:
        proc.terminate()
        try: proc.wait(timeout=3)
        except subprocess.TimeoutExpired: proc.kill()
    proc = None


def start_slot(item):
    global proc, current
    stop_proc()
    filename = item.get('arquivo','')
    source = MEDIA / filename
    if not source.exists():
        write_state(error=f'Arquivo não encontrado: {source}', running=False)
        return False
    if not PASSWORD:
        write_state(error='CASTER_SOURCE_PASSWORD não configurado', running=False)
        return False
    url = f'icecast://{USER}:{PASSWORD}@{HOST}:{PORT}/{MOUNT}'
    cmd = ['ffmpeg','-hide_banner','-loglevel','warning','-re','-stream_loop','-1','-i',str(source),'-vn','-ac','2','-ar','44100','-c:a','libmp3lame','-b:a',BITRATE,'-content_type','audio/mpeg','-f','mp3',url]
    proc = subprocess.Popen(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, text=True)
    current = {'programa': item.get('programa',''), 'arquivo': filename, 'inicio': item.get('inicio'), 'fim': item.get('fim')}
    write_state(error=None)
    return True


def handle(sig, frame):
    stop_proc(); write_state(running=False); sys.exit(0)

signal.signal(signal.SIGINT, handle); signal.signal(signal.SIGTERM, handle)

while True:
    item = slot_now()
    if item:
        if not current or current.get('programa') != item.get('programa') or not proc or proc.poll() is not None:
            start_slot(item)
        else:
            write_state(error=None)
    else:
        # 23:55–00:00 (and any uncovered time): keep the stream alive with silence.
        if not PASSWORD:
            write_state(running=False, error='CASTER_SOURCE_PASSWORD não configurado')
        elif not proc or proc.poll() is not None or current and current.get('programa') != 'INTERVALO / SILÊNCIO':
            stop_proc()
            url = f'icecast://{USER}:{PASSWORD}@{HOST}:{PORT}/{MOUNT}'
            cmd = ['ffmpeg','-hide_banner','-loglevel','warning','-re','-f','lavfi','-i','anullsrc=r=44100:cl=stereo','-c:a','libmp3lame','-b:a',BITRATE,'-content_type','audio/mpeg','-f','mp3',url]
            proc = subprocess.Popen(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, text=True)
            current = {'programa':'INTERVALO / SILÊNCIO','arquivo':'anullsrc','inicio':'23:55','fim':'00:00'}
            write_state(error=None)
    time.sleep(2)
