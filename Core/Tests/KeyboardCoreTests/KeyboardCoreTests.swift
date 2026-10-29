import XCTest
@testable import KeyboardCore

final class ZhuyinComposerTests: XCTestCase {
    func testTonesFinishSyllables() {
        var c = ZhuyinComposer()
        for ch in "ㄋㄧˇㄏㄠˇ" { XCTAssertTrue(c.input(ch)) }
        XCTAssertEqual(c.readings, ["ㄋㄧˇ", "ㄏㄠˇ"])
        XCTAssertFalse(c.hasPendingSyllable)
    }

    func testSpaceIsFirstTone() {
        var c = ZhuyinComposer()
        c.input("ㄇ")
        c.input("ㄚ")
        XCTAssertTrue(c.confirmSyllable())
        XCTAssertEqual(c.readings, ["ㄇㄚ"])
    }

    func testSameKindReplaces() {
        var c = ZhuyinComposer()
        c.input("ㄅ")
        c.input("ㄆ")
        c.input("ㄚ")
        c.input("ˋ")
        XCTAssertEqual(c.readings, ["ㄆㄚˋ"])
    }

    func testNeutralToneAtEnd() {
        var c = ZhuyinComposer()
        for ch in "ㄉㄜ˙" { c.input(ch) }
        XCTAssertEqual(c.readings, ["ㄉㄜ˙"])
    }

    func testToneWithoutSoundIsNotUsed() {
        var c = ZhuyinComposer()
        XCTAssertFalse(c.input("ˇ"))
        XCTAssertFalse(c.isComposing)
    }

    func testBackspace() {
        var c = ZhuyinComposer()
        for ch in "ㄋㄧˇㄏㄠ" { c.input(ch) }
        XCTAssertTrue(c.backspace())          // 刪 ㄠ
        XCTAssertEqual(c.current.reading, "ㄏ")
        XCTAssertTrue(c.backspace())          // 刪 ㄏ
        XCTAssertTrue(c.backspace())          // 刪整個 ㄋㄧˇ
        XCTAssertFalse(c.isComposing)
        XCTAssertFalse(c.backspace())
    }
}

final class ZhuyinEngineTests: XCTestCase {
    private func makeEngine() -> ZhuyinEngine {
        let db = SQLiteDB.inMemory()!
        ZhuyinEngine.createSchema(in: db)
        let rows: [(String, String, Double)] = [
            ("ㄋㄧˇ", "你", -3.0), ("ㄋㄧˇ", "妳", -4.0), ("ㄏㄠˇ", "好", -3.0),
            ("ㄋㄧˇ-ㄏㄠˇ", "你好", -4.0), ("ㄋㄧˇ-ㄇㄣ˙", "你們", -4.1),
            ("ㄕˋ", "是", -2.5), ("ㄕˋ", "事", -3.2), ("ㄕˋ", "世", -3.6),
            ("ㄐㄧㄝˋ", "界", -4.2), ("ㄕˋ-ㄐㄧㄝˋ", "世界", -4.5),
        ]
        for r in rows { ZhuyinEngine.insert(into: db, reading: r.0, phrase: r.1, score: r.2) }
        return ZhuyinEngine(db: db, userModel: UserPhraseModel(fileURL: nil))
    }

    func testPhraseBeatsSingles() {
        let e = makeEngine()
        XCTAssertEqual(e.bestSegments(["ㄋㄧˇ", "ㄏㄠˇ"]).map { $0.text }.joined(), "你好")
    }

    func testSentence() {
        let e = makeEngine()
        let s = e.bestSegments(["ㄋㄧˇ", "ㄏㄠˇ", "ㄕˋ", "ㄐㄧㄝˋ"]).map { $0.text }.joined()
        XCTAssertEqual(s, "你好世界")
    }

    func testCandidates() {
        let e = makeEngine()
        let c = e.candidates(for: ["ㄋㄧˇ", "ㄏㄠˇ"])
        XCTAssertEqual(c.first, Candidate(text: "你好", span: 2, kind: .sentence))
        XCTAssertTrue(c.contains(Candidate(text: "妳", span: 1, kind: .phrase)))
        let single = e.candidates(for: ["ㄕˋ"])
        XCTAssertEqual(single.map { $0.text }, ["是", "事", "世"])
    }

    func testUnknownSyllableStaysAsSymbols() {
        let e = makeEngine()
        XCTAssertEqual(e.bestSegments(["ㄅㄧㄤ"]).map { $0.text }, ["ㄅㄧㄤ"])
        XCTAssertEqual(e.candidates(for: ["ㄅㄧㄤ"]).first?.kind, .literal)
    }

    func testLearning() {
        let e = makeEngine()
        XCTAssertEqual(e.phrases(["ㄋㄧˇ"]).first?.text, "你")
        e.learn(readings: ["ㄋㄧˇ"], text: "妳")
        e.learn(readings: ["ㄋㄧˇ"], text: "妳")
        XCTAssertEqual(e.phrases(["ㄋㄧˇ"]).first?.text, "妳")
        e.resetLearning()
        XCTAssertEqual(e.phrases(["ㄋㄧˇ"]).first?.text, "你")
    }

    func testLearnedNewPhrase() {
        let e = makeEngine()
        e.learn(readings: ["ㄏㄠˇ", "ㄕˋ"], text: "好事")
        XCTAssertEqual(e.phrases(["ㄏㄠˇ", "ㄕˋ"]).first?.text, "好事")
    }

    func testAssociations() {
        let e = makeEngine()
        let a = e.associations(after: "你")
        XCTAssertTrue(a.contains("好"))
        XCTAssertTrue(a.contains("們"))
        XCTAssertEqual(e.associations(after: "世"), ["界"])
    }
}

final class StoreTests: XCTestCase {
    func testClipboardDedupeAndLimit() {
        let s = ClipboardStore(fileURL: nil, limit: 3)
        s.add("a"); s.add("b"); s.add("c")
        s.add("a")
        XCTAssertEqual(s.ordered.map { $0.text }, ["a", "c", "b"])
        s.togglePin(s.items.last!.id)       // 釘選 b
        s.add("d"); s.add("e"); s.add("f")
        XCTAssertEqual(s.ordered.map { $0.text }, ["b", "f", "e", "d"])
        XCTAssertFalse(s.add("   "))
        s.clearUnpinned()
        XCTAssertEqual(s.ordered.map { $0.text }, ["b"])
    }

    func testEmoticonStore() throws {
        let dir = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        let s = EmoticonStore(directory: dir)
        s.addCustom("(=^･ω･^=)")
        XCTAssertEqual(s.custom.first, "(=^･ω･^=)")
        let ref = s.addSticker(pngData: Data([1, 2, 3]))
        XCTAssertNotNil(ref)
        s.pushRecent("😀", kind: "emoji")
        s.pushRecent("😂", kind: "emoji")
        s.pushRecent("😀", kind: "emoji")
        XCTAssertEqual(s.recents("emoji"), ["😀", "😂"])

        let reloaded = EmoticonStore(directory: dir)
        XCTAssertEqual(reloaded.custom.first, "(=^･ω･^=)")
        XCTAssertEqual(reloaded.stickers.count, 1)
        reloaded.removeSticker(ref!)
        XCTAssertEqual(reloaded.stickers.count, 0)
    }

    func testSymbolDataIsClean() {
        for group in [SymbolData.emoji, SymbolData.kaomoji, SymbolData.symbols] {
            for cat in group {
                XCTAssertFalse(cat.items.isEmpty, cat.title)
                XCTAssertFalse(cat.items.contains(""), cat.title)
            }
        }
    }
}
