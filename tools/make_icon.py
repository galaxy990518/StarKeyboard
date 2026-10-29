#!/usr/bin/env python3
"""產生 iOS App 圖示（粉紅底＋白色星星，跟 Android 的 ic_launcher 一樣）。

不用 PIL，自己算星星蓋到每個像素多少（4 條子掃描線 × 精確的水平覆蓋），寫成不透明的 RGB PNG。
用法：python3 tools/make_icon.py  → App/Assets.xcassets/AppIcon.appiconset/AppIcon.png
"""
import os
import struct
import zlib

SIZE = 1024
SUB = 4  # 每個像素切幾條子掃描線
BG = (0xFF, 0x8F, 0xB0)  # 跟 android/app/src/main/res/values/colors.xml 的 icon_bg 一樣
FG = (0xFF, 0xFF, 0xFF)

# android/app/src/main/res/drawable/ic_star.xml 的星星（108×108 畫布）
STAR = [(54, 28), (60.47, 45.1), (78.73, 45.97), (64.46, 57.4), (69.28, 75.03),
        (54, 65), (38.72, 75.03), (43.54, 57.4), (29.27, 45.97), (47.53, 45.1)]
STAR_SCALE = 1.3  # iOS 圖示沒有 Android 的裁切安全區，星星放大一點

OUT = os.path.join(os.path.dirname(__file__), "..", "App", "Assets.xcassets",
                   "AppIcon.appiconset", "AppIcon.png")


def star_points():
    k = SIZE / 108 * STAR_SCALE
    return [((x - 54) * k + SIZE / 2, (y - 52) * k + SIZE / 2) for x, y in STAR]


def coverage():
    pts = star_points()
    edges = list(zip(pts, pts[1:] + pts[:1]))
    cov = [[0.0] * SIZE for _ in range(SIZE)]
    for row in range(SIZE):
        line = cov[row]
        for s in range(SUB):
            y = row + (s + 0.5) / SUB
            xs = sorted(x0 + (y - y0) * (x1 - x0) / (y1 - y0)
                        for (x0, y0), (x1, y1) in edges if (y0 <= y) != (y1 <= y))
            for a, b in zip(xs[::2], xs[1::2]):
                a, b = max(a, 0.0), min(b, float(SIZE))
                ia, ib = int(a), int(b)
                if ia == ib:
                    if ia < SIZE:
                        line[ia] += (b - a) / SUB
                    continue
                line[ia] += (ia + 1 - a) / SUB
                for i in range(ia + 1, ib):
                    line[i] += 1 / SUB
                if ib < SIZE:
                    line[ib] += (b - ib) / SUB
    return cov


def png(rows):
    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data))
    raw = b"".join(b"\x00" + bytes(r) for r in rows)
    return (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", SIZE, SIZE, 8, 2, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9))
            + chunk(b"IEND", b""))


def main():
    rows = []
    for line in coverage():
        r = bytearray()
        for c in line:
            c = min(c, 1.0)
            r += bytes(round(bg + (fg - bg) * c) for bg, fg in zip(BG, FG))
        rows.append(r)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "wb") as f:
        f.write(png(rows))
    print("寫好了", os.path.normpath(OUT))


if __name__ == "__main__":
    main()
