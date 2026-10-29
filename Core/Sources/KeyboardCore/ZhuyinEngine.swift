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
    private var cache: [String: [Scored]] = [:]
    let userModel: UserPhraseModel

    init(db: SQLiteDB, userModel: UserPhraseModel) {
        self.db = db
        self.userModel = userModel
    }

    convenience init?(path: String, userModel: UserPhraseModel) {
        guard let db = SQLiteDB(path: path) else { return nil }
        self.init(db: db, userModel: userModel)
    }

    // MARK: 字典格式（tools/build_dict.py 也是用這個）

    static func createSchema(in db: SQLiteDB) {
        db.execute("CREATE TABLE IF NOT EXISTS phrases(reading TEXT NOT NULL, phrase TEXT NOT NULL, score REAL NOT NULL)")
        db.execute("CREATE INDEX IF NOT EXISTS idx_reading ON phrases(reading)")
        db.execute("CREATE INDEX IF NOT EXISTS idx_phrase ON phrases(phrase)")
    }

    static func insert(into db: SQLiteDB, reading: String, phrase: String, score: Double) {
        db.run("INSERT INTO phrases(reading, phrase, score) VALUES (?1, ?2, ?3)",
               [.text(reading), .text(phrase), .double(score)])
    }

    // MARK: 查詢

    /// 一段注音（例如 ["ㄋㄧˇ", "ㄏㄠˇ"]）對應的所有詞，分數高的在前面
    func phrases(_ readings: ArraySlice<String>) -> [Scored] {
        let key = readings.joined(separator: "-")
        if let cached = cache[key] { return cached }

        var best: [String: Double] = [:]
        var order: [String] = []
        db.run("SELECT phrase, score FROM phrases WHERE reading = ?1 ORDER BY score DESC LIMIT 80", [.text(key)]) { stmt in
            let phrase = SQLiteDB.text(stmt, 0)
            if best[phrase] == nil {
                best[phrase] = SQLiteDB.double(stmt, 1)
                order.append(phrase)
            }
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
                if let top = phrases(readings[start..<i]).first {
                    let s = best[start] + top.score
                    if s > best[i] {
                        best[i] = s
                        back[i] = (start, top.text, top.score)
                    }
                } else if k == 1 {
                    let s = best[start] + Self.unknownPenalty
                    if s > best[i] {
                        best[i] = s
                        back[i] = (start, readings[start], Self.unknownPenalty)
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
            for p in phrases(readings[0..<k]) where !seen.contains(p.text) {
                seen.insert(p.text)
                out.append(Candidate(text: p.text, span: k, kind: .phrase))
                if k == 1 { hasSingle = true }
            }
        }
        if !hasSingle {
            out.append(Candidate(text: readings[0], span: 1, kind: .literal))
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
    }

    func resetLearning() {
        userModel.reset()
        cache.removeAll()
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
