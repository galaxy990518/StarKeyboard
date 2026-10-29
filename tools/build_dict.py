#!/usr/bin/env python3
"""把小麥注音的 data.txt 轉成鍵盤用的 SQLite 字典。

用法：python3 tools/build_dict.py <McBopomofo/Source/Data/data.txt> <輸出.sqlite>

data.txt 每行是「讀音 字詞 分數」，讀音的音節用 - 連接，例如：ㄋㄧˇ-ㄏㄠˇ 你好 -4.38
資料表格式和 Core/Sources/KeyboardCore/ZhuyinEngine.swift 的 createSchema 一樣。
"""
import os
import re
import sqlite3
import sys

SYLLABLE = "[ㄅ-ㄩˊˇˋ˙]+"
READING = re.compile("^%s(-%s)*$" % (SYLLABLE, SYLLABLE))


def main(src, dst):
    best = {}
    with open(src, encoding="utf-8") as f:
        for line in f:
            if line.startswith("#") or not line.strip():
                continue
            parts = line.rstrip("\n").split(" ")
            if len(parts) != 3:
                continue
            reading, phrase, score = parts
            if not READING.match(reading) or phrase.startswith("MACRO@"):
                continue
            try:
                score = float(score)
            except ValueError:
                continue
            key = (reading, phrase)
            if key not in best or best[key] < score:
                best[key] = score

    if os.path.exists(dst):
        os.remove(dst)
    os.makedirs(os.path.dirname(os.path.abspath(dst)), exist_ok=True)
    db = sqlite3.connect(dst)
    db.execute("PRAGMA page_size=4096")
    db.execute("CREATE TABLE phrases(reading TEXT NOT NULL, phrase TEXT NOT NULL, score REAL NOT NULL)")
    db.executemany("INSERT INTO phrases VALUES (?, ?, ?)",
                   ((r, p, round(s, 4)) for (r, p), s in sorted(best.items())))
    db.execute("CREATE INDEX idx_reading ON phrases(reading)")
    db.execute("CREATE INDEX idx_phrase ON phrases(phrase)")
    db.commit()
    db.execute("VACUUM")
    db.close()

    singles = sum(1 for (r, _) in best if "-" not in r)
    print("字詞 %d 筆（單字 %d）、檔案 %.1f MB" % (len(best), singles, os.path.getsize(dst) / 1e6))

    # 簡單檢查：常用字要查得到
    db = sqlite3.connect(dst)
    for reading, phrase in [("ㄋㄧˇ-ㄏㄠˇ", "你好"), ("ㄉㄜ˙", "的"), ("ㄊㄞˊ-ㄨㄢ", "台灣")]:
        rows = [r[0] for r in db.execute(
            "SELECT phrase FROM phrases WHERE reading=? ORDER BY score DESC LIMIT 5", (reading,))]
        print(reading, rows)
        if phrase not in rows:
            sys.exit("字典檢查失敗：%s 找不到 %s" % (reading, phrase))


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2])
