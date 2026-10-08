#!/usr/bin/env python3
"""Uruchamiaj na VPS jako root po weryfikacji podpisu APK (RUNBOOK.md)."""
import argparse
import fcntl
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import tempfile
from datetime import datetime, timezone


def sha(data):
    return hashlib.sha256(data).hexdigest()


def atomic_write(path, data, mode=0o644):
    fd, name = tempfile.mkstemp(prefix='.mealspire-', dir=path.parent)
    try:
        with os.fdopen(fd, 'wb') as stream:
            stream.write(data)
            stream.flush()
            os.fsync(stream.fileno())
        os.chmod(name, mode)
        os.replace(name, path)
    finally:
        Path(name).unlink(missing_ok=True)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('stage', type=Path)
    parser.add_argument('--expected-index-sha', required=True)
    parser.add_argument('--dry-run', action='store_true')
    args = parser.parse_args()
    root = Path('/var/www/sochiera')
    with Path('/var/lock/mealspire-publish.lock').open('w') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        apk = (args.stage / 'mealspire-debug.apk').read_bytes()
        metadata = (args.stage / 'wersja.json').read_bytes()
        version = json.loads(metadata)
        name = version['versionName']
        if not isinstance(name, str) or not re.fullmatch(r'[0-9]+\.[0-9]+', name):
            raise SystemExit('Nieprawidłowy versionName')
        if type(version['versionCode']) is not int or version['versionCode'] < 4:
            raise SystemExit('Nieprawidłowy versionCode')
        if version['sha256'] != sha(apk):
            raise SystemExit('Hash APK niezgodny z metadanymi')
        if version['apkUrl'] != 'https://raw.githubusercontent.com/sochiera/mealspire/main/dist/mealspire-debug.apk':
            raise SystemExit('Nieoczekiwany kanał aktualizacji')
        index = root / 'index.html'
        original = index.read_bytes()
        if sha(original) != args.expected_index_sha:
            raise SystemExit('Strona zmieniła się — pobierz ponownie i sprawdź przed publikacją')
        # Bajtowy wzorzec UTF-8: nie przepisywać reszty wspólnej strony.
        pattern = '<a href="/pobierz/mealspire-([0-9]+\\.[0-9]+)\\.apk" download>Mealspire — aplikacja na Androida \\(APK \\1\\)</a>'.encode()
        replacement = f'<a href="/pobierz/mealspire-{name}.apk" download>Mealspire — aplikacja na Androida (APK {name})</a>'.encode()
        updated, count = re.subn(pattern, lambda _: replacement, original)
        if count != 1:
            raise SystemExit('Oczekiwano dokładnie jednego linku Mealspire')
        dest = root / 'pobierz' / f'mealspire-{name}.apk'
        if dest.exists() and dest.read_bytes() != apk:
            raise SystemExit('Wersjonowany URL istnieje z innymi bajtami — nie nadpisuj wydania')
        if args.dry_run:
            print(f'DRY RUN OK: {dest}; versionCode={version["versionCode"]}; sha256={sha(apk)}')
            return
        backup = Path('/var/backups/mealspire') / datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
        backup.mkdir(parents=True, mode=0o700)
        shutil.copy2(index, backup / 'index.html')
        mirror = root / 'pobierz' / 'mealspire-wersja.json'
        if mirror.exists():
            shutil.copy2(mirror, backup / 'mealspire-wersja.json')
        # Najpierw APK, następnie metadane, na końcu link na stronie.
        atomic_write(dest, apk)
        atomic_write(mirror, metadata)
        atomic_write(index, updated, index.stat().st_mode & 0o777)
        print(f'PUBLISHED: versionCode={version["versionCode"]}; sha256={sha(apk)}; backup={backup}')


if __name__ == '__main__':
    main()
