import Foundation

enum BopomofoKind {
    case initial, medial, rhyme, tone
}

enum Bopomofo {
    static let initials: Set<Character> = Set("ㄅㄆㄇㄈㄉㄊㄋㄌㄍㄎㄏㄐㄑㄒㄓㄔㄕㄖㄗㄘㄙ")
    static let medials: Set<Character> = Set("ㄧㄨㄩ")
    static let finals: Set<Character> = Set("ㄚㄛㄜㄝㄞㄟㄠㄡㄢㄣㄤㄥㄦ")
    static let tones: Set<Character> = Set("ˊˇˋ˙")

    static func kind(of c: Character) -> BopomofoKind? {
        if initials.contains(c) { return .initial }
        if medials.contains(c) { return .medial }
        if finals.contains(c) { return .rhyme }
        if tones.contains(c) { return .tone }
        return nil
    }

    /// 大千排列：和電腦鍵盤的 1qaz、2wsx… 直排一樣
    static let dachenRows: [String] = [
        "ㄅㄉˇˋㄓˊ˙ㄚㄞㄢㄦ",
        "ㄆㄊㄍㄐㄔㄗㄧㄛㄟㄣ",
        "ㄇㄋㄎㄑㄕㄘㄨㄜㄠㄤ",
        "ㄈㄌㄏㄒㄖㄙㄩㄝㄡㄥ",
    ]
}

/// 一個字的注音：聲母＋介音＋韻母＋聲調（一聲不標）
struct Syllable: Equatable {
    var initial: Character?
    var medial: Character?
    var rhyme: Character?   // 韻母
    var tone: Character?

    var isEmpty: Bool { initial == nil && medial == nil && rhyme == nil && tone == nil }
    var hasSound: Bool { initial != nil || medial != nil || rhyme != nil }

    /// 和字典一樣的寫法，例如「ㄋㄧˇ」「ㄇㄚ」「ㄉㄜ˙」
    var reading: String {
        var s = ""
        for c in [initial, medial, rhyme, tone] {
            if let c = c { s.append(c) }
        }
        return s
    }

    /// 大千的打法：同一類的符號再打一次就取代
    mutating func put(_ c: Character) {
        switch Bopomofo.kind(of: c) {
        case .initial: initial = c
        case .medial: medial = c
        case .rhyme: rhyme = c
        case .tone: tone = c
        case nil: break
        }
    }

    mutating func removeLast() {
        if tone != nil { tone = nil }
        else if rhyme != nil { rhyme = nil }
        else if medial != nil { medial = nil }
        else { initial = nil }
    }
}

/// 組字區：已經打完的音節＋正在打的音節
struct ZhuyinComposer {
    private(set) var syllables: [Syllable] = []
    private(set) var current = Syllable()

    var readings: [String] { syllables.map { $0.reading } }
    var isComposing: Bool { !syllables.isEmpty || !current.isEmpty }
    var hasPendingSyllable: Bool { !current.isEmpty }

    /// 回傳 false 表示這個符號沒被組字用掉（例如還沒打音就先打聲調）
    @discardableResult
    mutating func input(_ c: Character) -> Bool {
        guard let kind = Bopomofo.kind(of: c) else { return false }
        if kind == .tone {
            guard current.hasSound else { return false }
            current.tone = c
            finishSyllable()
            return true
        }
        current.put(c)
        return true
    }

    /// 空白鍵＝一聲
    @discardableResult
    mutating func confirmSyllable() -> Bool {
        guard current.hasSound else { return false }
        finishSyllable()
        return true
    }

    @discardableResult
    mutating func backspace() -> Bool {
        if !current.isEmpty {
            current.removeLast()
            return true
        }
        if !syllables.isEmpty {
            syllables.removeLast()
            return true
        }
        return false
    }

    mutating func removeFirst(_ n: Int) {
        syllables.removeFirst(min(max(n, 0), syllables.count))
    }

    mutating func clear() {
        syllables = []
        current = Syllable()
    }

    private mutating func finishSyllable() {
        syllables.append(current)
        current = Syllable()
    }
}
