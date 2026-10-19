"""Bundle the official Inter static fonts used by the Compose typography.

Run with Python 3.11+: python scripts/sync_inter_fonts.py
An existing official release ZIP can be supplied with --archive <path>.
"""

import argparse
import hashlib
import io
from pathlib import Path
import tomllib
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "core/designsystem/src/main/res"
RELEASE_SHA256 = {
    "4.1": "9883fdd4a49d4fb66bd8177ba6625ef9a64aa45899767dde3d36aa425756b11e",
}
FONTS = {
    "inter_regular": "Inter-Regular.ttf",
    "inter_medium": "Inter-Medium.ttf",
    "inter_semibold": "Inter-SemiBold.ttf",
    "inter_bold": "Inter-Bold.ttf",
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--archive", type=Path)
    args = parser.parse_args()
    version = tomllib.loads((ROOT / "gradle/libs.versions.toml").read_text())["versions"]["inter-font"]
    source = f"https://github.com/rsms/inter/releases/download/v{version}/Inter-{version}.zip"
    if args.archive:
        data = args.archive.read_bytes()
    else:
        with urllib.request.urlopen(source, timeout=60) as response:
            data = response.read()
    if hashlib.sha256(data).hexdigest() != RELEASE_SHA256[version]:
        raise ValueError("Inter release checksum mismatch")
    # Read only the selected official files, without extracting ZIP paths or changing the fonts.
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        fonts = {resource: archive.read(f"extras/ttf/{name}") for resource, name in FONTS.items()}
        license_text = archive.read("LICENSE.txt").decode()
    if any(font[:4] != b"\x00\x01\x00\x00" for font in fonts.values()):
        raise ValueError("Expected static TrueType fonts")
    (RES / "font").mkdir(parents=True, exist_ok=True)
    (RES / "raw").mkdir(parents=True, exist_ok=True)
    for resource, font in fonts.items():
        (RES / "font" / f"{resource}.ttf").write_bytes(font)
    (RES / "raw/inter_font_license.txt").write_text(
        f"Inter {version}\nSource: {source}\n\n" + license_text,
        encoding="utf-8",
    )
    print(f"Bundled {len(fonts)} official Inter {version} fonts and their license.")


if __name__ == "__main__":
    main()
