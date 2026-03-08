"""
Simple ES module bundler for the spikes game.
Resolves import/export and concatenates into a single IIFE script.
Run: python bundle.py
Output: spikes.bundle.js
"""
import re
from pathlib import Path

ROOT = Path(__file__).parent

# Define files in dependency order (leaves first)
FILES_IN_ORDER = [
    "AndroidBridge.js",
    "objects/spike.js",
    "objects/bird.js",
    "objects/wall.js",
    "scenes/gameScene.js",
    "spikes.js",
]

# Map from filename stem to the variable name for its default export
MODULE_NAMES = {
    "AndroidBridge.js": "Bridge",
    "objects/spike.js": "Spike",
    "objects/bird.js": "Bird",
    "objects/wall.js": "Wall",
    "scenes/gameScene.js": "GameScene",
}

def strip_imports(code):
    """Remove all import lines."""
    return re.sub(r"^\s*import\s+.+?['\"];?\s*$", "", code, flags=re.MULTILINE)

def strip_exports(code):
    """Remove 'export default' from class/const declarations."""
    code = re.sub(r"^export\s+default\s+", "", code, flags=re.MULTILINE)
    return code

def bundle():
    parts = ["// spikes.bundle.js - auto-generated, do not edit", "(function() {", ""]
    for filepath in FILES_IN_ORDER:
        full = ROOT / filepath
        code = full.read_text(encoding="utf-8")
        code = strip_imports(code)
        code = strip_exports(code)
        parts.append(f"// === {filepath} ===")
        parts.append(code.strip())
        parts.append("")
    parts.append("})();")
    output = ROOT / "spikes.bundle.js"
    output.write_text("\n".join(parts), encoding="utf-8")
    print(f"Bundled {len(FILES_IN_ORDER)} files -> {output.name}")

if __name__ == "__main__":
    bundle()
