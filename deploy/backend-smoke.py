#!/usr/bin/env python3
"""Runs only inside an isolated staging directory; never installs a service."""
import json
import pathlib
import re
import select
import signal
import subprocess
import sys
import time
import urllib.error
import urllib.request

def interrupted(signum, frame):
    raise SystemExit(128 + signum)

for signum in (signal.SIGTERM, signal.SIGINT, signal.SIGHUP):
    signal.signal(signum, interrupted)

stage = pathlib.Path(sys.argv[1]).resolve()
java = stage / 'runtime/bin/java'
backend = stage / 'backend/lib/*'
start = time.monotonic()
process = subprocess.Popen([str(java), '-cp', str(backend), 'com.mealspire.backend.BackendServer'],
                           env={'MEALSPIRE_PORT': '0'}, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
try:
    if not select.select([process.stdout], [], [], 15)[0]:
        raise RuntimeError('Backend startup timed out')
    line = process.stdout.readline()
    match = re.fullmatch(r'Mealspire backend listening on loopback port (\d+)\n', line)
    if not match:
        raise RuntimeError('Backend failed to start')
    port = int(match.group(1))
    base = f'http://127.0.0.1:{port}'
    def request(path, expected, body=None, token=None, method=None):
        headers = {'Content-Type': 'application/json'}
        if token:
            headers['Authorization'] = 'Bearer ' + token
        req = urllib.request.Request(base + path, data=None if body is None else body.encode(), headers=headers, method=method)
        try:
            response = urllib.request.urlopen(req, timeout=10)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            assert response.status == expected, (path, response.status, expected)
            assert response.headers['Cache-Control'] == 'no-store'
            data = json.load(response)
            assert data['apiVersion'] == 1
            return data
    assert request('/health', 200)['status'] == 'ok'
    catalog = request('/v1/catalog', 200)
    assert len(catalog['meals']) == 3
    assert all(len(meal) > 10 for meal in catalog['meals'])
    assert all(recipe['title'] and recipe['details'] for meal in catalog['meals'] for recipe in meal)
    request('/v2/catalog', 404)
    request('/v1/catalog', 405, '{}')
    request('/v1/recipe', 401, '{}')
    request('/v1/recipe', 400, 'broken', 'synthetic-invalid-token')
    request('/v1/proposals', 400, '{"model":"gpt-6-luna","request":{"household":{"exclusions":["UNKNOWN"]}}}', 'synthetic-invalid-token')
    request('/v1/recipe', 413, 'x' * 270000, 'synthetic-invalid-token')
    print(f'PASS: 8 loopback HTTP checks; catalog={sum(map(len, catalog["meals"]))} recipes; port={port}')
finally:
    process.terminate()
    try:
        process.wait(timeout=10)
    except subprocess.TimeoutExpired:
        process.kill()
        process.wait(timeout=5)
    print(f'Process stopped; actual elapsed: {time.monotonic()-start:.1f}s')
