"""Build historical exact-URL evidence; omit URLs with conflicting benign labels.

Usage: python tools/build_url_index.py path/to/malicious_phish.csv
Raw dataset stays outside source control. No websites are contacted.
"""
import csv
import hashlib
import json
import struct
import sys
from collections import Counter
from pathlib import Path
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parents[1]


def canonical(raw):
    value = raw.strip()
    parsed = urlsplit(value if "://" in value else "https://" + value)
    if parsed.scheme.lower() not in ("http", "https") or parsed.username is not None:
        return None
    host = parsed.hostname
    if not host or not host.isascii() or any(c.isspace() for c in value):
        return None
    host = host.lower().removesuffix(".")
    if ":" in host:
        host = "[" + host + "]"
    port = parsed.port
    suffix = "" if port is None or (parsed.scheme == "http" and port == 80) or (parsed.scheme == "https" and port == 443) else ":" + str(port)
    return host + suffix + (parsed.path or "/") + ("?" + parsed.query if parsed.query else "")


source = Path(sys.argv[1])
labels = {}
counts = Counter()
skipped = 0
with source.open(encoding="utf-8-sig", newline="") as stream:
    for row in csv.DictReader(stream):
        counts[row["type"]] += 1
        try:
            value = canonical(row["url"])
        except ValueError:
            value = None
        if value is None:
            skipped += 1
            continue
        digest = hashlib.sha256(value.encode()).digest()
        labels.setdefault(digest, set()).add(row["type"])
codes = {"phishing": 0, "malware": 1, "defacement": 2}
records = [(digest, min(codes[label] for label in values if label in codes))
           for digest, values in labels.items() if "benign" not in values and any(label in codes for label in values)]
records.sort()
output = ROOT / "app/src/main/assets/url-threats-v1.bin"
output.parent.mkdir(parents=True, exist_ok=True)
payload = struct.pack(">II", 0x53554931, len(records)) + b"".join(digest + bytes([code]) for digest, code in records)
output.write_bytes(payload)
metadata = {"source": "https://www.kaggle.com/datasets/sid321axn/malicious-urls-dataset",
            "attribution": "Malicious URLs dataset by Manu Siddhartha (sid321axn), Kaggle version 1",
            "license": "CC0: Public Domain (Kaggle metadata)",
            "dataset_sha256": hashlib.sha256(source.read_bytes()).hexdigest(),
            "input_labels": dict(counts), "skipped_rows": skipped, "indexed_urls": len(records),
            "conflicting_benign_urls_omitted": sum("benign" in v and len(v) > 1 for v in labels.values()),
            "index_sha256": hashlib.sha256(payload).hexdigest(),
            "scope": "Historical exact URL matches only. Absence does not establish safety. Not independent evaluation."}
(output.parent / "url-threats-v1.json").write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
print(json.dumps(metadata, indent=2))
