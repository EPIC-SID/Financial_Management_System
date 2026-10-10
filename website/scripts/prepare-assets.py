"""Prepare W1 source copy, self-hosted font subsets and art density variants."""

import hashlib
import json
import math
from pathlib import Path
from urllib.request import urlopen

from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from PIL import Image

SITE = Path(__file__).resolve().parents[1]
ROOT = SITE.parent
CACHE = SITE / ".asset-cache"
FONTS = SITE / "public/fonts"
ART = SITE / "public/art"
for folder in (CACHE, FONTS, ART, SITE / "lib"):
    folder.mkdir(parents=True, exist_ok=True)


def download(url, target):
    if not target.exists():
        with urlopen(url, timeout=60) as response:
            target.write_bytes(response.read())
    return target


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


frames = json.loads((ROOT / "docs/figma-source.json").read_text(encoding="utf-8"))
cta = next(frame for frame in frames if frame["Frame"].endswith("v4\\CTA v2.svg"))
foundation = next(frame for frame in frames if frame["Frame"].endswith("v4\\Foundations.svg"))
cta_copy = [item["Copy"] for item in cta["Text"]]
required_copy = [
    "RUPAI / CTA V2 ONLY", "Clear actions.",
    "16 sp labels. 56 dp default height. Controls grow with text.",
    "Default", "Pressed", "Focused", "Disabled", "Loading", "See plan", "Explore demo",
    "Add an account to enable this action.", "Saving…",
    "130% / 200%: wrap labels, stack rows and grow heights.",
]
assert all(value in cta_copy for value in required_copy), "Requested CTA copy missing from extraction"
copy = dict(zip(
    ["eyebrow", "headline", "intro", "default", "pressed", "focused", "disabled", "loading",
     "primary", "outline", "disabledReason", "loadingLabel", "scaleNote"],
    required_copy,
))
copy["foundationHeadline"] = "Calm, clear and considered."
assert any(item["Copy"] == copy["foundationHeadline"] for item in foundation["Text"])
(SITE / "lib/figma-copy.json").write_text(json.dumps(copy, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

font_sources = []
base_url = "https://raw.githubusercontent.com/google/fonts/main/ofl/librebaskerville/"
libre_source = download(base_url + "LibreBaskerville%5Bwght%5D.ttf", CACHE / "libre-full.ttf")
download(base_url + "OFL.txt", FONTS / "libre-baskerville-OFL.txt")
download("https://raw.githubusercontent.com/google/fonts/main/ofl/inter/OFL.txt", FONTS / "inter-OFL.txt")
for license_file in FONTS.glob("*-OFL.txt"):
    license_file.write_text("\n".join(line.rstrip() for line in license_file.read_text(encoding="utf-8").splitlines()) + "\n", encoding="utf-8")

unicodes = set(range(0x20, 0x100)) | set(range(0x100, 0x180))
unicodes |= {0x20B9, 0x2013, 0x2014, 0x2018, 0x2019, 0x201C, 0x201D, 0x2022, 0x2026, 0x2212, 0x2248, 0x2192, 0x2713}
unicodes |= {ord(character) for value in copy.values() for character in value}
sources = [(libre_source, "libre-baskerville-regular.woff2", "RupAI Serif Subset", 400)]
sources += [(ROOT / f"android_app/app/src/main/res/font/inter_{weight}.ttf", f"inter-{weight}.woff2", "RupAI Sans Subset", number)
            for weight, number in (("regular", 400), ("medium", 500), ("semibold", 600))]

for source, filename, family, weight in sources:
    font = TTFont(source)
    if filename.startswith("inter"):
        assert 0x20B9 in font.getBestCmap(), f"Rupee glyph missing from {source}"
    if "fvar" in font:
        font = instantiateVariableFont(font, {"wght": weight}, inplace=True)
    options = subset.Options()
    options.flavor = "woff2"
    options.layout_features = ["*"]
    subsetter = subset.Subsetter(options=options)
    subsetter.populate(unicodes=unicodes)
    subsetter.subset(font)
    # Use a distinct internal family name for modified OFL subsets.
    for record in font["name"].names:
        if record.nameID in (1, 4, 6, 16):
            value = family.replace(" ", "") if record.nameID == 6 else family
            record.string = value.encode(record.getEncoding())
    font.flavor = "woff2"
    target = FONTS / filename
    font.save(target)
    result = TTFont(target)
    has_rupee = 0x20B9 in result.getBestCmap()
    features = [record.FeatureTag for record in result["GSUB"].table.FeatureList.FeatureRecord] if "GSUB" in result else []
    if filename.startswith("inter"):
        assert has_rupee, f"Rupee glyph lost in {filename}"
        assert "tnum" in features, f"Tabular numbers lost in {filename}"
    font_sources.append({"file": filename, "sourceSha256": sha(source), "sha256": sha(target),
                         "bytes": target.stat().st_size, "rupee": has_rupee, "tabular": "tnum" in features})

art_manifest = json.loads((ROOT / "docs/illustration-assets.json").read_text(encoding="utf-8"))
artwork = []
for item in art_manifest["assets"]:
    source = ROOT / item["source"]
    original = Image.open(source).convert("RGBA")
    name = Path(item["android"]).stem.replace("_", "-")
    small = original.resize((math.ceil(original.width / 2), math.ceil(original.height / 2)), Image.Resampling.LANCZOS)
    variants = []
    for density, image in ((1, small), (2, original)):
        target = ART / f"{name}@{density}x.webp"
        image.save(target, format="WEBP", lossless=True, exact=True, method=6)
        decoded = Image.open(target).convert("RGBA")
        assert decoded.tobytes() == image.tobytes(), f"Lossless/alpha roundtrip failed: {target}"
        variants.append({"density": density, "file": target.name, "width": image.width, "height": image.height,
                         "sha256": sha(target), "bytes": target.stat().st_size})
    artwork.append({"name": name, "source": item["source"], "sourceSha256": sha(source), "variants": variants})

manifest = {"phase": "W1", "fontSource": base_url, "fonts": font_sources,
            "artRule": "2x is native source resolution; 1x is half-size rounded up. No added illustration detail.", "art": artwork}
(SITE / "public/asset-manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
print(json.dumps({"fonts": len(font_sources), "interRupeeGlyphsVerified": all(item["rupee"] for item in font_sources if item["file"].startswith("inter")),
                  "libreRupeeFallback": "Inter",
                  "illustrations": len(artwork), "webpVariants": sum(len(item["variants"]) for item in artwork)}, indent=2))
