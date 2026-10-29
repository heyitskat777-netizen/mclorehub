#!/usr/bin/env python3
"""Verified Java 21 bootstrap and Gradle activation for Linux x86_64 cloud tasks."""
import hashlib
import os
from pathlib import Path
import platform
import subprocess
import sys
import tarfile
import tempfile
import urllib.parse
import urllib.request

REPO = Path(__file__).resolve().parents[1]
TOOLS = REPO.parent / '.mclorehub-tools'
GRADLE_HOME = REPO.parent / '.mclorehub-gradle'
JDK = TOOLS / 'jdk-21.0.13+9'
NAME = 'OpenJDK21U-jdk_x64_linux_hotspot_21.0.13_9.tar.gz'
SHA256 = 'd7f5d5cf865a2902b70ff549b97586a7d3f234e016f041ab447ea740c7d66d1c'
URL = 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.13%2B9/' + NAME

def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()

def prepare():
    if platform.system() != 'Linux' or platform.machine() not in ('x86_64', 'amd64'):
        raise RuntimeError('Use ./gradlew with your own Java 21 JDK on this platform')
    TOOLS.mkdir(exist_ok=True)
    archive = TOOLS / NAME
    recovered = TOOLS / 'jdk.tar.gz'
    if not archive.exists() and recovered.exists() and digest(recovered) == SHA256:
        recovered.rename(archive)
    if not archive.exists():
        fd, name = tempfile.mkstemp(prefix='.jdk-download-', dir=TOOLS)
        temporary = Path(name)
        try:
            with os.fdopen(fd, 'wb') as output, urllib.request.urlopen(URL, timeout=60) as source:
                total = 0
                while chunk := source.read(1024 * 1024):
                    total += len(chunk)
                    if total > 512 * 1024 * 1024:
                        raise RuntimeError('JDK download exceeds size budget')
                    output.write(chunk)
            if digest(temporary) != SHA256:
                raise RuntimeError('JDK checksum mismatch; refusing installation')
            temporary.rename(archive)
        finally:
            temporary.unlink(missing_ok=True)
    if digest(archive) != SHA256:
        raise RuntimeError('Cached JDK checksum mismatch')
    if not JDK.exists():
        with tempfile.TemporaryDirectory(prefix='.jdk-extract-', dir=TOOLS) as stage:
            with tarfile.open(archive) as source:
                source.extractall(stage, filter='data')
            (Path(stage) / JDK.name).rename(JDK)
    subprocess.run([str(JDK / 'bin/javac'), '-version'], check=True)
    GRADLE_HOME.mkdir(exist_ok=True)
    config = GRADLE_HOME / 'gradle.properties'
    lines = config.read_text().splitlines() if config.exists() else []
    updates = {}
    proxy = urllib.parse.urlsplit(os.environ.get('HTTPS_PROXY', os.environ.get('https_proxy', '')))
    if proxy.hostname:
        for scheme in ('http', 'https'):
            updates[f'systemProp.{scheme}.proxyHost'] = proxy.hostname
            updates[f'systemProp.{scheme}.proxyPort'] = str(proxy.port or 80)
    trust = Path('/etc/ssl/certs/java/cacerts')
    if trust.is_file():
        updates['systemProp.javax.net.ssl.trustStore'] = str(trust)
    for key, value in updates.items():
        if any(line.split('=', 1)[0].strip() == key for line in lines):
            lines = [f'{key}={value}' if line.split('=', 1)[0].strip() == key else line for line in lines]
        else:
            lines.append(f'{key}={value}')
    config.write_text('\n'.join(lines) + '\n')

if __name__ == '__main__':
    try:
        prepare()
        child = os.environ.copy()
        child['JAVA_HOME'] = str(JDK)
        child['GRADLE_USER_HOME'] = str(GRADLE_HOME)
        platform_ca = Path('/etc/ssl/certs/java/cacerts')
        if platform_ca.is_file():
            child['MCLOREHUB_TRUSTSTORE'] = str(platform_ca)
        os.chdir(REPO)
        os.execvpe(str(REPO / 'gradlew'), [str(REPO / 'gradlew'), *sys.argv[1:]], child)
    except (OSError, RuntimeError, subprocess.CalledProcessError) as error:
        sys.exit(f'Cloud toolchain setup failed: {error}')
