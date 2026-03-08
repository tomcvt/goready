"""
Simple ES module bundler for the spikes game (WebGL variant).
Run: python bundle2.py
Output: spikes2.bundle.js
"""
import re
from pathlib import Path

ROOT = Path(__file__).parent

FILES_IN_ORDER = [
    "AndroidBridge.js",
    "objects/spike.js",
    "objects/bird.js",
    "objects/wall.js",
    "scenes/gameScene.js",
    "spikes2.js",
]

def strip_imports(code):
    return re.sub(r"^\s*import\s+.+?['\"];?\s*$", "", code, flags=re.MULTILINE)

def strip_exports(code):
    code = re.sub(r"^export\s+default\s+", "", code, flags=re.MULTILINE)
    return code

def bundle():
    parts = ["// spikes2.bundle.js - auto-generated, do not edit", "(function() {", ""]
    for filepath in FILES_IN_ORDER:
        full = ROOT / filepath
        code = full.read_text(encoding="utf-8")
        code = strip_imports(code)
        code = strip_exports(code)
        parts.append(f"// === {filepath} ===")
        parts.append(code.strip())
        parts.append("")
    parts.append("})();")
    output = ROOT / "spikes2.bundle.js"
    output.write_text("\n".join(parts), encoding="utf-8")
    print(f"Bundled {len(FILES_IN_ORDER)} files -> {output.name}")

if __name__ == "__main__":
    bundle()
