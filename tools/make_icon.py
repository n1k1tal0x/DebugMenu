#!/usr/bin/env python3
"""Render the Debug Menu button icon from pixel art defined below.

The icon is a beetle - the conventional "debug" symbol - drawn to read clearly
at 16x16 against Minecraft's stone-grey button background.

Usage:
    python tools/make_icon.py
"""

import pathlib
import struct
import zlib

OUT = pathlib.Path(__file__).resolve().parents[1] / "src/main/resources/assets/debugmenu/textures/gui/sprites/icon/bug.png"

# . transparent  K outline  G shell  H shell highlight  W eye
ART = """
................
................
.....K....K.....
......K..K......
......KKKK......
.....KWKKWK.....
....KKKKKKKK....
..KKKGGKKGGKKK..
....KGGKKGGK....
....KGHKKHGK....
..KKKGGKKGGKKK..
....KGGKKGGK....
....KGHKKHGK....
..KKKGGKKGGKKK..
.....KKKKKK.....
................
"""

PALETTE = {
    ".": (0, 0, 0, 0),
    "K": (20, 24, 16, 255),
    "G": (90, 143, 60, 255),
    "H": (127, 183, 86, 255),
    "W": (238, 238, 238, 255),
}


def chunk(tag: bytes, data: bytes) -> bytes:
    return (struct.pack(">I", len(data)) + tag + data
            + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))


def main() -> None:
    rows = [line for line in ART.strip("\n").splitlines() if line]
    size = len(rows)
    assert all(len(r) == size for r in rows), "art must be square"

    raw = b""
    for row in rows:
        raw += b"\x00"  # filter type: none
        for pixel in row:
            raw += bytes(PALETTE[pixel])

    png = (b"\x89PNG\r\n\x1a\n"
           + chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
           + chunk(b"IDAT", zlib.compress(raw, 9))
           + chunk(b"IEND", b""))

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_bytes(png)
    print(f"wrote {OUT.relative_to(pathlib.Path(__file__).resolve().parents[1])} ({size}x{size}, {len(png)} bytes)")


if __name__ == "__main__":
    main()
