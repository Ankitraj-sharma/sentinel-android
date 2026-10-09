"""Read-only hashes of the main app's private files; never print file contents."""
import hashlib
import json
import subprocess
import sys
from pathlib import Path

root = Path(__file__).resolve().parents[1]
adb = root / ".tools/android-sdk/platform-tools/adb.exe"
package = "com.phishshield.sentinel"
result = {}
for folder in ("files/datastore", "no_backup"):
    listing = subprocess.run([str(adb), "shell", "run-as", package, "ls", folder], capture_output=True)
    if listing.returncode:
        continue
    for name in listing.stdout.decode().splitlines():
        if name in (".", "..") or "/" in name:
            continue
        path = folder + "/" + name
        content = subprocess.run([str(adb), "exec-out", "run-as", package, "cat", path], capture_output=True, check=True).stdout
        result[path] = {"bytes": len(content), "sha256": hashlib.sha256(content).hexdigest()}
output = Path(sys.argv[1])
output.write_text(json.dumps(result, indent=2) + "\n")
print("Saved read-only storage hashes:", len(result), "files")
