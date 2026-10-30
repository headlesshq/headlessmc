"""Downloads the jars of the locked headlessmc-lwjgl release into the layout of the HeadlessMc cache,
<cache>/lwjgl-patched/<version>/<file name>, and verifies their size and SHA-256 against the lock file, like
PatchedLwjglJars does. CI downloads them once and hands them to every Minecraft run, instead of every run downloading
the release.

Usage: download_patched_lwjgl.py <lock file> <cache>/lwjgl-patched
"""

import hashlib
import json
import sys
import urllib.request
from pathlib import Path


def main():
    lock = json.loads(Path(sys.argv[1]).read_text())
    directory = Path(sys.argv[2]) / lock["version"]
    directory.mkdir(parents=True, exist_ok=True)
    errors = []
    for name, jar in sorted(lock["jars"].items()):
        target = directory / jar["fileName"]
        if target.is_file() and target.stat().st_size == jar["size"] \
                and hashlib.sha256(target.read_bytes()).hexdigest() == jar["sha256"]:
            continue

        with urllib.request.urlopen(jar["url"], timeout=120) as response:
            data = response.read()

        digest = hashlib.sha256(data).hexdigest()
        if len(data) != jar["size"] or digest != jar["sha256"]:
            errors.append(f"{name}: {jar['url']} has {len(data)} bytes and SHA-256 {digest}, "
                          f"the lock expects {jar['size']} bytes and {jar['sha256']}")
            continue

        target.write_bytes(data)

    for error in errors:
        print(f"::error::{error}")

    print(f"headlessmc-lwjgl {lock['version']}: {len(lock['jars']) - len(errors)} of {len(lock['jars'])} jars in {directory}")
    sys.exit(1 if errors else 0)


if __name__ == "__main__":
    main()
