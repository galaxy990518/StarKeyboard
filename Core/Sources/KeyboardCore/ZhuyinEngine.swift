import Foundation

struct Candidate: Equatable {
    enum Kind: Equatable {
        case sentence   // 整串自動組好的句子
        case phrase     // 字典裡的詞或單字
        case literal    // 查不到，直接用注音符號
    }

    let text: String
    let span: Int       // 用掉前面幾個音節
    let kind: Kind
}

/// 注音選字：查小麥注音的字典（SQLite），整串用動態規劃找分數最高的斷詞
final class ZhuyinEngine {
    struct Scored {
        let text: String
        let score: Double
    }

    static let maxPhraseLength = 6
    /// 查不到的音節的分數（字典分數是 log10 機率，大約 -2 到 -8）
    static let unknownPenalty: Double = -30
    /// 使用者自己學到、字典沒有的詞
    static let userPhraseBase: Double = -6

    private let db: SQLiteDB
    /// 用哪一欄查：注音用 reading（有聲調），拼音用 plain（沒聲調）
    private let column: String
    private var cache: [String: [Scored]] = [:]
    private var fuzzyCache: [String: [Scored]] = [:]
    private var alternativeCache: [String: [(reading: String, penalty: Double)]] = [:]

    /// 容錯：打錯鍵、聲調錯、ㄣㄥ這類模糊音也猜得到
    struct FuzzyOptions: Equatable {
        var keyNeighbors = true   // 打出來的音不存在時，試試旁邊的鍵
        var tones = true          // 聲調打錯或忘了打
        var phonetic = false      // ㄣㄥ、ㄓㄗ、ㄔㄘ、ㄕㄙ 分不清
    }

    var fuzzy = FuzzyOptions() {
        didSet {
            if fuzzy != oldValue {
                fuzzyCache.removeAll()
                alternativeCache.removeAll()
            }
        }
    }

    static let tonePenalty: Double = -2.5
    static let phoneticPenalty: Double = -1.2
    static let neighborPenalty: Double = -2.5
    /// 只打了開頭（ㄋ 當 ㄋㄧ），比打完整的排後面一點
    static let prefixPenalty: Double = -1.0
    let userModel: UserPhraseModel

    init(db: SQLiteDB, userModel: UserPhraseModel, column: String = "reading") {
        self.db = db
        self.userModel = userModel
        self.column = column
    }

    convenience init?(path: String, userModel: UserPhraseModel, column: String = "reading") {
        guard let db = SQLiteDB(path: path) else { return nil }
        self.init(db: db, userModel: userModel, column: column)
    }

    // MARK: 字典格式（tools/build_dict.py 也是用這個）

    static func createSchema(in db: SQLiteDB) {
        db.execute("CREATE TABLE IF NOT EXISTS phrases(reading TEXT NOT NULL, plain TEXT NOT NULL, abbr TEXT NOT NULL, phrase TEXT NOT NULL, score REAL NOT NULL)")
        db.execute("CREATE INDEX IF NOT EXISTS idx_reading ON phrases(reading)")
        db.execute("CREATE INDEX IF NOT EXISTS idx_plain ON phrases(plain)")
        db.execute("CREATE INDEX IF NOT EXISTS idx_abbr ON phrases(abbr)")
        db.execute("CREATE INDEX IF NOT EXISTS idx_phrase ON phrases(phrase)")
    }

    static func insert(into db: SQLiteDB, reading: String, phrase: String, score: Double) {
        let plain = String(reading.filter { !Bopomofo.tones.contains($0) })
        let abbr = String(reading.split(separator: "-").compactMap { $0.first })
        db.run("INSERT INTO phrases(reading, plain, abbr, phrase, score) VALUES (?1, ?2, ?3, ?4, ?5)",
               [.text(reading), .text(plain), .text(abbr), .text(phrase), .double(score)])
    }

    /// 畫面上顯示的注音（拿掉查字典用的 *）
    static func display(_ reading: String) -> String { reading.replacingOccurrences(of: "*", with: "") }

    /// 讀音合不合 pattern：有 * 的音節只要開頭一樣（聲調不限），其他要完全一樣。合的話回傳要扣的分數
    static func matchPenalty(_ pattern: [Substring], _ reading: [Substring]) -> Double? {
        guard pattern.count == reading.count else { return nil }
        var penalty: Double = 0
        for (want, have) in zip(pattern, reading) {
            guard want.hasSuffix("*") else {
                if want != have { return nil }
                continue
            }
            let head = want.dropLast()
            let plain = String(have.filter { !Bopomofo.tones.contains($0) })
            guard plain.hasPrefix(head) else { return nil }
            if plain.count != head.count { penalty += prefixPenalty }
        }
        return penalty
    }

    /// 查字典：讀音裡有沒打完的音節（*）就用開頭注音查，再一個一個比
    private func lookup(_ key: String) -> [(phrase: String, score: Double)] {
        var out: [(phrase: String, score: Double)] = []
        guard key.contains("*") else {
            db.run("SELECT phrase, score FROM phrases WHERE \(column) = ?1 ORDER BY score DESC LIMIT 80", [.text(key)]) { stmt in
                out.append((SQLiteDB.text(stmt, 0), SQLiteDB.double(stmt, 1)))
            }
            return out
        }
        let pattern = key.split(separator: "-")
        let abbr = String(pattern.compactMap { $0.first })
        var best: [String: Double] = [:]
        db.run("SELECT reading, phrase, score FROM phrases WHERE abbr = ?1", [.text(abbr)]) { stmt in
            guard let penalty = Self.matchPenalty(pattern, SQLiteDB.text(stmt, 0).split(separator: "-")) else { return }
            let phrase = SQLiteDB.text(stmt, 1)
            let s = SQLiteDB.double(stmt, 2) + penalty
            if let old = best[phrase], old >= s { return }
            best[phrase] = s
        }
        for (phrase, score) in best.sorted(by: { $0.value != $1.value ? $0.value > $1.value : $0.key < $1.key }).prefix(80) {
            out.append((phrase, score))
        }
        return out
    }

    // MARK: 查詢

    /// 一段注音（例如 ["ㄋㄧˇ", "ㄏㄠˇ"]）對應的所有詞，分數高的在前面
    func phrases(_ readings: ArraySlice<String>) -> [Scored] {
        let key = readings.joined(separator: "-")
        if let cached = cache[key] { return cached }

        var best: [String: Double] = [:]
        var order: [String] = []
        for (phrase, score) in lookup(key) where best[phrase] == nil {
            best[phrase] = score
            order.append(phrase)
        }
        for phrase in userModel.phrases(for: key) where best[phrase] == nil {
            best[phrase] = Self.userPhraseBase
            order.append(phrase)
        }

        var list = order.enumerated().map { (i, phrase) in
            (i, Scored(text: phrase, score: best[phrase]! + userModel.boost(reading: key, phrase: phrase)))
        }
        list.sort { $0.1.score != $1.1.score ? $0.1.score > $1.1.score : $0.0 < $1.0 }
        let result = list.map { $0.1 }
        cache[key] = result
        return result
    }

    /// 和 phrases 一樣，但也算進打錯的可能（每個詞最多猜錯一個音節，分數扣一點）
    func fuzzyPhrases(_ readings: ArraySlice<String>) -> [Scored] {
        let key = readings.joined(separator: "-")
        if let cached = fuzzyCache[key] { return cached }
        var best: [String: Double] = [:]
        var order: [String] = []
        func merge(_ list: [Scored], _ penalty: Double) {
            for p in list {
                let s = p.score + penalty
                if let old = best[p.text] {
                    if s > old { best[p.text] = s }
                } else {
                    best[p.text] = s
                    order.append(p.text)
                }
            }
        }
        merge(phrases(readings), 0)
        let syllables = Array(readings)
        for j in syllables.indices {
            for alt in alternatives(for: syllables[j]) {
                var variant = syllables
                variant[j] = alt.reading
                merge(phrases(variant[...]), alt.penalty)
            }
        }
        var list = order.enumerated().map { (i, text) in (i, Scored(text: text, score: best[text]!)) }
        list.sort { $0.1.score != $1.1.score ? $0.1.score > $1.1.score : $0.0 < $1.0 }
        let result = list.map { $0.1 }
        fuzzyCache[key] = result
        return result
    }

    /// 一個音節可能是哪些音打錯了（只留字典裡真的有的）
    func alternatives(for reading: String) -> [(reading: String, penalty: Double)] {
        if let cached = alternativeCache[reading] { return cached }
        if reading.hasSuffix("*") { return [] }   // 沒打完的音節本來就聲調不限，不用再猜
        var syllable = Syllable()
        for c in reading { syllable.put(c) }
        var out: [(reading: String, penalty: Double)] = []
        var seen: Set<String> = [reading]
        func add(_ s: Syllable, _ penalty: Double) {
            let r = s.reading
            guard s.hasSound, !seen.contains(r) else { return }
            seen.insert(r)
            if !phrases([r][...]).isEmpty { out.append((r, penalty)) }
        }

        if fuzzy.tones {
            for tone in [nil, "ˊ", "ˇ", "ˋ", "˙"] as [Character?] where tone != syllable.tone {
                var s = syllable
                s.tone = tone
                add(s, Self.tonePenalty)
            }
        }
        if fuzzy.phonetic {
            if let i = syllable.initial, let swap = Bopomofo.similar[i] {
                var s = syllable
                s.initial = swap
                add(s, Self.phoneticPenalty)
            }
            if let f = syllable.rhyme, let swap = Bopomofo.similar[f] {
                var s = syllable
                s.rhyme = swap
                add(s, Self.phoneticPenalty)
            }
        }
        // 打出來的音根本不存在：多半是按到旁邊的鍵
        if fuzzy.keyNeighbors && phrases([reading][...]).isEmpty {
            for c in reading {
                for n in Bopomofo.neighbors[c] ?? [] {
                    var s = syllable
                    switch Bopomofo.kind(of: c) {
                    case .initial: s.initial = nil
                    case .medial: s.medial = nil
                    case .rhyme: s.rhyme = nil
                    case .tone: s.tone = nil
                    case nil: continue
                    }
                    s.put(n)
                    add(s, Self.neighborPenalty)
                }
            }
        }
        alternativeCache[reading] = out
        return out
    }

    /// 整串注音分數最高的斷法
    func bestSegments(_ readings: [String]) -> [Scored] {
        let n = readings.count
        guard n > 0 else { return [] }
        var best = [Double](repeating: -.infinity, count: n + 1)
        var back = [(start: Int, text: String, score: Double)](repeating: (0, "", 0), count: n + 1)
        best[0] = 0
        for i in 1...n {
            for k in 1...min(Self.maxPhraseLength, i) {
                let start = i - k
                guard best[start] > -.infinity else { continue }
                if let top = fuzzyPhrases(readings[start..<i]).first {
                    let s = best[start] + top.score
                    if s > best[i] {
                        best[i] = s
                        back[i] = (start, top.text, top.score)
                    }
                } else if k == 1 {
                    let s = best[start] + Self.unknownPenalty
                    if s > best[i] {
                        best[i] = s
                        back[i] = (start, Self.display(readings[start]), Self.unknownPenalty)
                    }
                }
            }
        }
        var segments: [Scored] = []
        var i = n
        while i > 0 {
            let b = back[i]
            segments.append(Scored(text: b.text, score: b.score))
            i = b.start
        }
        return segments.reversed()
    }

    /// 候選字：第一個是整句，接著是從開頭算起最長到最短的詞
    func candidates(for readings: [String]) -> [Candidate] {
        guard !readings.isEmpty else { return [] }
        var out: [Candidate] = []
        var seen = Set<String>()
        if readings.count > 1 {
            let sentence = bestSegments(readings).map { $0.text }.joined()
            out.append(Candidate(text: sentence, span: readings.count, kind: .sentence))
            seen.insert(sentence)
        }
        var hasSingle = false
        for k in stride(from: min(Self.maxPhraseLength, readings.count), through: 1, by: -1) {
            for p in fuzzyPhrases(readings[0..<k]) where !seen.contains(p.text) {
                seen.insert(p.text)
                out.append(Candidate(text: p.text, span: k, kind: .phrase))
                if k == 1 { hasSingle = true }
            }
        }
        if !hasSingle {
            out.append(Candidate(text: Self.display(readings[0]), span: 1, kind: .literal))
        }
        return out
    }

    /// 所有音節（沒聲調）的開頭，例如 ㄋㄧㄠ → ㄋ、ㄋㄧ、ㄋㄧㄠ（給組字用，判斷注音能不能接在一起）
    func syllablePrefixes() -> Set<String> {
        var out = Set<String>()
        db.run("SELECT DISTINCT plain FROM phrases WHERE plain NOT LIKE '%-%'") { stmt in
            let s = SQLiteDB.text(stmt, 0)
            var prefix = ""
            for c in s {
                prefix.append(c)
                out.insert(prefix)
            }
        }
        return out
    }

    /// 聯想詞：打完一個字後，接在它後面常見的字詞
    func associations(after text: String, limit: Int = 30) -> [String] {
        guard let last = text.last else { return [] }
        let head = String(last)
        var out: [String] = []
        db.run("SELECT phrase, MAX(score) AS s FROM phrases WHERE phrase > ?1 AND phrase < ?2 GROUP BY phrase ORDER BY s DESC LIMIT ?3",
               [.text(head), .text(head + "\u{10FFFF}"), .int(limit)]) { stmt in
            let phrase = SQLiteDB.text(stmt, 0)
            guard phrase.count > 1, phrase.hasPrefix(head) else { return }
            out.append(String(phrase.dropFirst()))
        }
        return out
    }

    /// 使用者選了某個詞：之後這個詞會排更前面
    func learn(readings: [String], text: String) {
        guard !readings.isEmpty, readings.count <= Self.maxPhraseLength else { return }
        userModel.record(reading: readings.joined(separator: "-"), phrase: text)
        cache.removeAll()
        fuzzyCache.removeAll()
    }

    func resetLearning() {
        userModel.reset()
        cache.removeAll()
        fuzzyCache.removeAll()
    }
}

/// 記住使用者選過的字詞
final class UserPhraseModel {
    private var counts: [String: [String: Int]] = [:]
    private let fileURL: URL?

    init(fileURL: URL?) {
        self.fileURL = fileURL
        if let url = fileURL, let saved = JSONFile.load([String: [String: Int]].self, from: url) {
            counts = saved
        }
    }

    /// 每選一次加 0.8（log10，大約變成 6 倍常用），最多算 5 次
    func boost(reading: String, phrase: String) -> Double {
        guard let c = counts[reading]?[phrase] else { return 0 }
        return min(Double(c), 5) * 0.8
    }

    func phrases(for reading: String) -> [String] {
        guard let dict = counts[reading] else { return [] }
        return dict.keys.sorted()
    }

    func record(reading: String, phrase: String) {
        counts[reading, default: [:]][phrase, default: 0] += 1
        save()
    }

    func reset() {
        counts = [:]
        save()
    }

    private func save() {
        if let url = fileURL { JSONFile.save(counts, to: url) }
    }
}
