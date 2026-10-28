#!/usr/bin/env python3
"""Run the actual client test; create and stop only our own local Xorg if headless."""
import os
from pathlib import Path
import shutil
import subprocess
import sys
import time
REPO = Path(__file__).resolve().parents[1]

def run():
    env = os.environ.copy(); server = None; log = None
    try:
        if not env.get('DISPLAY'):
            for command in ('Xorg', 'xset'):
                if not shutil.which(command): raise RuntimeError(f'Missing {command}; graphical support required')
            number = next((n for n in range(99, 120) if not Path(f'/tmp/.X{n}-lock').exists()
                           and not Path(f'/tmp/.X11-unix/X{n}').exists()), None)
            if number is None: raise RuntimeError('No unused display; existing displays were left untouched')
            logs = REPO / 'client/build/graphics'; logs.mkdir(parents=True, exist_ok=True)
            log = (logs / f'Xorg.{number}.process.log').open('w')
            server = subprocess.Popen(['Xorg', f':{number}', '-config', str(REPO / 'tools/xorg-headless.conf'),
                                       '-logfile', str(logs / f'Xorg.{number}.log'), '-nolisten', 'tcp',
                                       '-noreset', '-novtswitch', '-sharevts'], stdout=log, stderr=subprocess.STDOUT)
            env['DISPLAY'] = f':{number}'; env['LIBGL_ALWAYS_SOFTWARE'] = 'true'
            deadline = time.monotonic() + 30
            while True:
                if server.poll() is not None: raise RuntimeError(f'Xorg exited; inspect {logs}')
                ready = subprocess.run(['xset', '-display', env['DISPLAY'], 'q'],
                                       stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=2)
                if ready.returncode == 0: break
                if time.monotonic() >= deadline: raise RuntimeError(f'Xorg startup timed out; inspect {logs}')
                time.sleep(.25)
        return subprocess.run([sys.executable, str(REPO / 'tools/cloud_gradle.py'), ':client:runClientGameTest',
                               '--console=plain', *sys.argv[1:]], cwd=REPO, env=env).returncode
    finally:
        if server is not None:
            server.terminate()
            try: server.wait(timeout=10)
            except subprocess.TimeoutExpired: server.kill(); server.wait()
        if log is not None: log.close()

if __name__ == '__main__':
    try: sys.exit(run())
    except (OSError, RuntimeError, subprocess.TimeoutExpired) as error: sys.exit(f'Client test setup failed: {error}')
