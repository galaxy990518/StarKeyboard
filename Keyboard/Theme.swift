import UIKit

extension UIColor {
    convenience init(hex: UInt32, alpha: CGFloat = 1) {
        self.init(red: CGFloat((hex >> 16) & 0xFF) / 255,
                  green: CGFloat((hex >> 8) & 0xFF) / 255,
                  blue: CGFloat(hex & 0xFF) / 255,
                  alpha: alpha)
    }
}

extension UIFont {
    /// 圓體，看起來比較可愛
    static func rounded(_ size: CGFloat, _ weight: UIFont.Weight = .regular) -> UIFont {
        let base = UIFont.systemFont(ofSize: size, weight: weight)
        guard let d = base.fontDescriptor.withDesign(.rounded) else { return base }
        return UIFont(descriptor: d, size: size)
    }
}

struct KeyboardTheme {
    let id: String
    let name: String
    let backgroundTop: UIColor
    let backgroundBottom: UIColor
    let key: UIColor
    let keyText: UIColor
    let specialKey: UIColor
    let specialText: UIColor
    let accent: UIColor
    let accentText: UIColor
    let shadow: UIColor
    let decoration: String
    let isDark: Bool

    static let all: [KeyboardTheme] = [
        KeyboardTheme(id: "strawberry", name: "草莓牛奶",
                      backgroundTop: UIColor(hex: 0xFFE4EC), backgroundBottom: UIColor(hex: 0xFFD0DE),
                      key: .white, keyText: UIColor(hex: 0x8A4B5C),
                      specialKey: UIColor(hex: 0xFFC2D4), specialText: UIColor(hex: 0x7A3045),
                      accent: UIColor(hex: 0xFF7FA6), accentText: .white,
                      shadow: UIColor(hex: 0xE8A2B6), decoration: "🍓", isDark: false),
        KeyboardTheme(id: "mint", name: "薄荷蘇打",
                      backgroundTop: UIColor(hex: 0xDDF7EF), backgroundBottom: UIColor(hex: 0xC5EFE3),
                      key: .white, keyText: UIColor(hex: 0x2F6B5E),
                      specialKey: UIColor(hex: 0xA7E3D2), specialText: UIColor(hex: 0x2F6B5E),
                      accent: UIColor(hex: 0x3CC5A0), accentText: .white,
                      shadow: UIColor(hex: 0x9FD8C7), decoration: "🌿", isDark: false),
        KeyboardTheme(id: "milktea", name: "奶茶布丁",
                      backgroundTop: UIColor(hex: 0xF3E6D6), backgroundBottom: UIColor(hex: 0xE9D5BD),
                      key: UIColor(hex: 0xFFF8EE), keyText: UIColor(hex: 0x6B4A33),
                      specialKey: UIColor(hex: 0xD9B994), specialText: UIColor(hex: 0x5A3B24),
                      accent: UIColor(hex: 0xB9835A), accentText: .white,
                      shadow: UIColor(hex: 0xCBAE8C), decoration: "🍮", isDark: false),
        KeyboardTheme(id: "lavender", name: "薰衣草",
                      backgroundTop: UIColor(hex: 0xECE4FF), backgroundBottom: UIColor(hex: 0xDCD0FF),
                      key: .white, keyText: UIColor(hex: 0x5B4B8A),
                      specialKey: UIColor(hex: 0xC7B8F5), specialText: UIColor(hex: 0x4A3B78),
                      accent: UIColor(hex: 0x8D74E8), accentText: .white,
                      shadow: UIColor(hex: 0xB9A9EC), decoration: "💜", isDark: false),
        KeyboardTheme(id: "lemon", name: "檸檬小雞",
                      backgroundTop: UIColor(hex: 0xFFF6CC), backgroundBottom: UIColor(hex: 0xFFEC9E),
                      key: .white, keyText: UIColor(hex: 0x7A5B12),
                      specialKey: UIColor(hex: 0xFFDD6B), specialText: UIColor(hex: 0x6B4E0A),
                      accent: UIColor(hex: 0xFFA928), accentText: .white,
                      shadow: UIColor(hex: 0xE9CB6A), decoration: "🐥", isDark: false),
        KeyboardTheme(id: "night", name: "星空夜",
                      backgroundTop: UIColor(hex: 0x1E2245), backgroundBottom: UIColor(hex: 0x2B2F63),
                      key: UIColor(hex: 0x3A3F7A), keyText: UIColor(hex: 0xF3F0FF),
                      specialKey: UIColor(hex: 0x2A2E5E), specialText: UIColor(hex: 0xCFCBFF),
                      accent: UIColor(hex: 0xFFD65C), accentText: UIColor(hex: 0x2B2F63),
                      shadow: UIColor(hex: 0x14173A), decoration: "⭐", isDark: true),
        KeyboardTheme(id: "light", name: "經典淺色",
                      backgroundTop: UIColor(hex: 0xD3D6DC), backgroundBottom: UIColor(hex: 0xCDD0D6),
                      key: .white, keyText: .black,
                      specialKey: UIColor(hex: 0xABB0BA), specialText: .black,
                      accent: UIColor(hex: 0x2F7BF6), accentText: .white,
                      shadow: UIColor(hex: 0x8E9196), decoration: "", isDark: false),
        KeyboardTheme(id: "dark", name: "經典深色",
                      backgroundTop: UIColor(hex: 0x2B2B2D), backgroundBottom: UIColor(hex: 0x232325),
                      key: UIColor(hex: 0x6B6B6E), keyText: .white,
                      specialKey: UIColor(hex: 0x474749), specialText: .white,
                      accent: UIColor(hex: 0x2F7BF6), accentText: .white,
                      shadow: UIColor(hex: 0x111111), decoration: "", isDark: true),
    ]

    static func named(_ id: String) -> KeyboardTheme {
        all.first { $0.id == id } ?? all[0]
    }
}

/// 按鍵造型
enum KeyShape: String, CaseIterable {
    case rounded, jelly, catEar, cloud

    var title: String {
        switch self {
        case .rounded: return "圓角"
        case .jelly: return "果凍"
        case .catEar: return "貓耳"
        case .cloud: return "雲朵"
        }
    }

    private func earSize(_ r: CGRect) -> CGFloat { min(r.width * 0.24, r.height * 0.24, 9) }
    private func bumpSize(_ r: CGRect) -> CGFloat { min(r.height * 0.16, 6) }

    /// 按鍵本體（文字畫在這裡面）
    func bodyRect(in r: CGRect) -> CGRect {
        switch self {
        case .catEar:
            let ear = earSize(r) * 0.6
            return CGRect(x: r.minX, y: r.minY + ear, width: r.width, height: r.height - ear)
        case .cloud:
            let b = bumpSize(r)
            return CGRect(x: r.minX, y: r.minY + b, width: r.width, height: r.height - b)
        default:
            return r
        }
    }

    func path(in r: CGRect) -> CGPath {
        let body = bodyRect(in: r)
        switch self {
        case .rounded:
            return UIBezierPath(roundedRect: body, cornerRadius: min(7, body.height * 0.22)).cgPath
        case .jelly:
            return UIBezierPath(roundedRect: body, cornerRadius: min(body.width, body.height) * 0.42).cgPath
        case .catEar:
            let ear = earSize(r)
            var path = UIBezierPath(roundedRect: body, cornerRadius: min(body.width, body.height) * 0.34).cgPath
            let left = UIBezierPath()
            left.move(to: CGPoint(x: body.minX + ear * 0.2, y: body.minY + ear * 1.1))
            left.addLine(to: CGPoint(x: body.minX + ear * 0.45, y: r.minY))
            left.addLine(to: CGPoint(x: body.minX + ear * 1.7, y: body.minY + ear * 0.35))
            left.close()
            let right = UIBezierPath()
            right.move(to: CGPoint(x: body.maxX - ear * 1.7, y: body.minY + ear * 0.35))
            right.addLine(to: CGPoint(x: body.maxX - ear * 0.45, y: r.minY))
            right.addLine(to: CGPoint(x: body.maxX - ear * 0.2, y: body.minY + ear * 1.1))
            right.close()
            path = path.union(left.cgPath).union(right.cgPath)
            return path
        case .cloud:
            let b = bumpSize(r)
            var path = UIBezierPath(roundedRect: body, cornerRadius: min(body.width, body.height) * 0.38).cgPath
            let count = max(2, Int(body.width / 20))
            let step = body.width / CGFloat(count + 1)
            for i in 1...count {
                let c = CGPoint(x: body.minX + step * CGFloat(i), y: body.minY + b * 0.5)
                let circle = UIBezierPath(arcCenter: c, radius: b * 1.5, startAngle: 0, endAngle: .pi * 2, clockwise: true)
                path = path.union(circle.cgPath)
            }
            return path
        }
    }
}

struct KeyboardStyle {
    var theme: KeyboardTheme
    var shape: KeyShape
    var keyAlpha: CGFloat
    var fontScale: CGFloat = 1   // 按鍵上的字放大縮小
    var gapScale: CGFloat = 1    // 按鍵之間的空隙（越小按鍵越大）
}

/// 鍵盤自己的設定（存在鍵盤擴充功能自己的空間，不需要 App Group）
final class KeyboardSettings {
    static let shared = KeyboardSettings()
    private let d = UserDefaults.standard

    private func bool(_ key: String, _ fallback: Bool) -> Bool { d.object(forKey: key) as? Bool ?? fallback }
    private func double(_ key: String, _ fallback: Double) -> Double { d.object(forKey: key) as? Double ?? fallback }

    var themeID: String {
        get { d.string(forKey: "themeID") ?? "strawberry" }
        set { d.set(newValue, forKey: "themeID") }
    }
    var keyShape: KeyShape {
        get { KeyShape(rawValue: d.string(forKey: "keyShape") ?? "") ?? .jelly }
        set { d.set(newValue.rawValue, forKey: "keyShape") }
    }
    var keyAlpha: CGFloat {
        get { CGFloat(double("keyAlpha", 1)) }
        set { d.set(Double(newValue), forKey: "keyAlpha") }
    }
    /// 背景圖片上蓋一層主題色，0 = 完全看得到圖
    var backgroundFade: CGFloat {
        get { CGFloat(double("backgroundFade", 0.25)) }
        set { d.set(Double(newValue), forKey: "backgroundFade") }
    }
    var hasBackground: Bool {
        get { bool("hasBackground", false) }
        set { d.set(newValue, forKey: "hasBackground") }
    }
    var keySound: Bool {
        get { bool("keySound", true) }
        set { d.set(newValue, forKey: "keySound") }
    }
    var haptics: Bool {
        get { bool("haptics", true) }
        set { d.set(newValue, forKey: "haptics") }
    }
    var associations: Bool {
        get { bool("associations", true) }
        set { d.set(newValue, forKey: "associations") }
    }
    var enabledModes: [InputMode] {
        get {
            let raw = d.stringArray(forKey: "modes") ?? [InputMode.zhuyin, .english, .handwriting].map { $0.rawValue }
            let modes = InputMode.allCases.filter { raw.contains($0.rawValue) }
            return modes.isEmpty ? [.zhuyin] : modes
        }
        set { d.set(newValue.map { $0.rawValue }, forKey: "modes") }
    }
    var lastMode: InputMode {
        get { InputMode(rawValue: d.string(forKey: "lastMode") ?? "") ?? .zhuyin }
        set { d.set(newValue.rawValue, forKey: "lastMode") }
    }
    var autocorrect: Bool {
        get { bool("autocorrect", true) }
        set { d.set(newValue, forKey: "autocorrect") }
    }
    var fuzzyKeys: Bool {
        get { bool("fuzzyKeys", true) }
        set { d.set(newValue, forKey: "fuzzyKeys") }
    }
    var fuzzyTones: Bool {
        get { bool("fuzzyTones", true) }
        set { d.set(newValue, forKey: "fuzzyTones") }
    }
    var fuzzyPhonetic: Bool {
        get { bool("fuzzyPhonetic", false) }
        set { d.set(newValue, forKey: "fuzzyPhonetic") }
    }
    /// 鍵盤高度倍率（0.8～1.3）
    var heightScale: CGFloat {
        get { CGFloat(double("heightScale", 1)) }
        set { d.set(Double(newValue), forKey: "heightScale") }
    }
    /// 按鍵字的大小倍率（0.8～1.4）
    var fontScale: CGFloat {
        get { CGFloat(double("fontScale", 1)) }
        set { d.set(Double(newValue), forKey: "fontScale") }
    }
    /// 按鍵間距倍率（0.3～1.6）
    var gapScale: CGFloat {
        get { CGFloat(double("gapScale", 1)) }
        set { d.set(Double(newValue), forKey: "gapScale") }
    }
    // 新版提醒
    var lastUpdateCheck: Double {
        get { d.double(forKey: "lastUpdateCheck") }
        set { d.set(newValue, forKey: "lastUpdateCheck") }
    }
    var latestBuild: Int {
        get { d.integer(forKey: "latestBuild") }
        set { d.set(newValue, forKey: "latestBuild") }
    }
    var notifiedBuild: Int {
        get { d.integer(forKey: "notifiedBuild") }
        set { d.set(newValue, forKey: "notifiedBuild") }
    }
    var translateTarget: String {
        get { d.string(forKey: "translateTarget") ?? "en" }
        set { d.set(newValue, forKey: "translateTarget") }
    }
    var clipLimit: Int {
        get { d.object(forKey: "clipLimit") as? Int ?? 200 }
        set { d.set(newValue, forKey: "clipLimit") }
    }
    var lastPasteboardChange: Int {
        get { d.integer(forKey: "lastPasteboardChange") }
        set { d.set(newValue, forKey: "lastPasteboardChange") }
    }
}

enum Storage {
    static let directory: URL = {
        let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("StarKeyboard", isDirectory: true)
        try? FileManager.default.createDirectory(at: base, withIntermediateDirectories: true)
        return base
    }()

    static func url(_ name: String) -> URL {
        directory.appendingPathComponent(name)
    }
}

extension UIImage {
    func resized(maxSide: CGFloat) -> UIImage {
        let longest = max(size.width, size.height)
        guard longest > maxSide else { return self }
        let scale = maxSide / longest
        let newSize = CGSize(width: floor(size.width * scale), height: floor(size.height * scale))
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        return UIGraphicsImageRenderer(size: newSize, format: format).image { _ in
            draw(in: CGRect(origin: .zero, size: newSize))
        }
    }
}
