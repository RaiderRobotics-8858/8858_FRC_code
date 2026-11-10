"""Compress and split (or rejoin and restore) the .glb robot models.

    python glb_pack.py pack      # model.glb -> model.glb.gz.part000, .part001, ...
    python glb_pack.py unpack    # model.glb.gz.partNNN -> model.glb (after cloning)

Parts are kept under 90 MB so they fit GitHub's 100 MB per-file limit.
Optionally pass a folder or a .glb path after the command; the default is this
script's folder, searched recursively.
"""

import gzip
import shutil
import sys
from pathlib import Path

PART_SIZE = 90 * 1024 * 1024


class SplitWriter:
    """File-like object that writes sequential <base>.partNNN files."""

    def __init__(self, base: Path):
        self.base = base
        self.index = 0
        self.written = 0
        self.fh = None
        self.parts = []

    def _open_next(self):
        if self.fh:
            self.fh.close()
        part = Path(f"{self.base}.part{self.index:03d}")
        self.index += 1
        self.written = 0
        self.parts.append(part)
        self.fh = open(part, "wb")

    def write(self, data):
        view = memoryview(data)
        while len(view):
            if self.fh is None or self.written >= PART_SIZE:
                self._open_next()
            chunk = view[: PART_SIZE - self.written]
            self.fh.write(chunk)
            self.written += len(chunk)
            view = view[len(chunk):]
        return len(data)

    def flush(self):
        if self.fh:
            self.fh.flush()

    def close(self):
        if self.fh:
            self.fh.close()
            self.fh = None


class JoinReader:
    """File-like object that reads several part files back to back."""

    def __init__(self, parts):
        self.parts = list(parts)
        self.fh = None

    def read(self, size=-1):
        out = bytearray()
        while size < 0 or len(out) < size:
            if self.fh is None:
                if not self.parts:
                    break
                self.fh = open(self.parts.pop(0), "rb")
            data = self.fh.read(-1 if size < 0 else size - len(out))
            if not data:
                self.fh.close()
                self.fh = None
                continue
            out += data
        return bytes(out)


def pack(glb: Path) -> None:
    for old in glb.parent.glob(glb.name + ".gz.part*"):
        old.unlink()
    writer = SplitWriter(Path(str(glb) + ".gz"))
    with open(glb, "rb") as fin, gzip.GzipFile(
        fileobj=writer, mode="wb", compresslevel=9, filename=""
    ) as gz:
        shutil.copyfileobj(fin, gz, 1024 * 1024)
    writer.close()
    total = sum(p.stat().st_size for p in writer.parts) / 1e6
    print(f"{glb.name}: {glb.stat().st_size / 1e6:.1f} MB -> {len(writer.parts)} parts, {total:.1f} MB")


def unpack(glb: Path) -> None:
    parts = sorted(glb.parent.glob(glb.name + ".gz.part*"))
    if not parts:
        print(f"No parts found for {glb}")
        return
    with gzip.GzipFile(fileobj=JoinReader(parts), mode="rb") as gz, open(glb, "wb") as fout:
        shutil.copyfileobj(gz, fout, 1024 * 1024)
    print(f"{len(parts)} parts -> {glb.name}: {glb.stat().st_size / 1e6:.1f} MB")


def main() -> int:
    if len(sys.argv) < 2 or sys.argv[1] not in ("pack", "unpack"):
        print(__doc__)
        return 1
    mode = sys.argv[1]
    target = Path(sys.argv[2]) if len(sys.argv) > 2 else Path(__file__).resolve().parent
    if target.is_file():
        glbs = [target]
    elif mode == "pack":
        glbs = sorted(target.rglob("*.glb"))
    else:
        glbs = sorted({Path(str(p).rsplit(".gz.part", 1)[0]) for p in target.rglob("*.glb.gz.part*")})
    if not glbs:
        print(f"No models found under {target}")
        return 1
    for g in glbs:
        (pack if mode == "pack" else unpack)(g)
    return 0


if __name__ == "__main__":
    sys.exit(main())
