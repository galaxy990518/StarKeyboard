import XCTest
@testable import KeyboardCore

final class HangulTests: XCTestCase {
    /// 模擬輸入框：每次刪掉上次還在拼的字，換成新的（和鍵盤的做法一樣）
    private func type(_ jamo: String) -> String {
        var h = HangulComposer()
        var doc = ""
        var shown = ""
        for c in jamo {
            let out = h.input(c)
            doc.removeLast(shown.count)
            doc += out.commit + out.composing
            shown = out.composing
        }
        return doc
    }

    func testBasicWords() {
        XCTAssertEqual(type("ㅎㅏㄴㄱㅡㄹ"), "한글")
        XCTAssertEqual(type("ㅇㅏㄴㄴㅕㅇ"), "안녕")
        XCTAssertEqual(type("ㅅㅏㄹㅏㅇㅎㅐ"), "사랑해")
    }

    func testCompoundVowelAndFinal() {
        XCTAssertEqual(type("ㅇㅘ"), "와")
        XCTAssertEqual(type("ㄷㅏㄹㄱ"), "닭")
        XCTAssertEqual(type("ㄷㅏㄹㄱㅏ"), "달가")
        XCTAssertEqual(type("ㅇㅣㄹㄱㅇㅓ"), "읽어")
    }

    func testLoneJamo() {
        XCTAssertEqual(type("ㅋㅋㅋ"), "ㅋㅋㅋ")
        XCTAssertEqual(type("ㅏㅏ"), "ㅏㅏ")
    }

    func testBackspace() {
        var h = HangulComposer()
        for c in "ㅎㅏㄴ" { _ = h.input(c) }
        XCTAssertEqual(h.composing, "한")
        h.backspace()
        XCTAssertEqual(h.composing, "하")
        h.backspace()
        XCTAssertEqual(h.composing, "ㅎ")
        h.backspace()
        XCTAssertTrue(h.isEmpty)
        XCTAssertFalse(h.backspace())
    }
}

final class RomajiTests: XCTestCase {
    func testWords() {
        XCTAssertEqual(Romaji.finalize("sakura"), "さくら")
        XCTAssertEqual(Romaji.finalize("konnichiha"), "こんにちは")
        XCTAssertEqual(Romaji.finalize("arigatou"), "ありがとう")
        XCTAssertEqual(Romaji.finalize("kitte"), "きって")
        XCTAssertEqual(Romaji.finalize("kyou"), "きょう")
        XCTAssertEqual(Romaji.finalize("shinbun"), "しんぶん")
        XCTAssertEqual(Romaji.finalize("onna"), "おんな")
        XCTAssertEqual(Romaji.finalize("ra-men"), "らーめん")
        XCTAssertEqual(Romaji.finalize("tsukue"), "つくえ")
        XCTAssertEqual(Romaji.finalize("jaa"), "じゃあ")
    }

    func testUnfinished() {
        XCTAssertEqual(Romaji.convert("sak").kana, "さ")
        XCTAssertEqual(Romaji.convert("sak").rest, "k")
        XCTAssertEqual(Romaji.convert("ky").rest, "ky")
        XCTAssertEqual(Romaji.convert("kan").rest, "n")
    }

    func testKatakana() {
        XCTAssertEqual(Romaji.katakana("さくら"), "サクラ")
        XCTAssertEqual(Romaji.katakana(Romaji.finalize("ko-hi-")), "コーヒー")
    }
}

final class ReleaseTests: XCTestCase {
    func testBuildNumber() {
        XCTAssertEqual(ReleaseChecker.buildNumber(fromTag: "build-12"), 12)
        XCTAssertNil(ReleaseChecker.buildNumber(fromTag: "v1.0"))
    }

    func testParse() {
        let json = #"{"tag_name":"build-7","name":"星星鍵盤 第 7 版","body":"這次更新：\n- 修好按鍵","html_url":"https://github.com/z3332111-cloud/StarKeyboard/releases/tag/build-7"}"#
        let info = ReleaseChecker.parse(Data(json.utf8))
        XCTAssertEqual(info?.build, 7)
        XCTAssertEqual(info?.title, "星星鍵盤 第 7 版")
        XCTAssertTrue(info?.notes.contains("修好按鍵") ?? false)
    }
}
