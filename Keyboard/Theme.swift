import UIKit
import CoreText

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
    /// 成套的主題（奶油軟萌系列）：選這個主題時按鍵造型也一起換
    var shape: KeyShape? = nil

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
        // 奶油軟萌系列：空白鍵左邊有造型徽章，所以不放 emoji；陰影比較淡
        KeyboardTheme(id: "bunny", name: "兔兔",
                      backgroundTop: UIColor(hex: 0xFFE3EC), backgroundBottom: UIColor(hex: 0xFFD6E2),
                      key: .white, keyText: UIColor(hex: 0x8A4B5C),
                      specialKey: UIColor(hex: 0xFFC9D9), specialText: UIColor(hex: 0x7A3045),
                      accent: UIColor(hex: 0xFF8FB1), accentText: .white,
                      shadow: UIColor(hex: 0xEBC3CF), decoration: "", isDark: false, shape: .bunny),
        KeyboardTheme(id: "puppy", name: "狗狗",
                      backgroundTop: UIColor(hex: 0xF3E6D7), backgroundBottom: UIColor(hex: 0xEBDAC6),
                      key: UIColor(hex: 0xFFF9F0), keyText: UIColor(hex: 0x6B4A33),
                      specialKey: UIColor(hex: 0xE3CCB2), specialText: UIColor(hex: 0x5A3B24),
                      accent: UIColor(hex: 0xC79A74), accentText: .white,
                      shadow: UIColor(hex: 0xD8C2AA), decoration: "", isDark: false, shape: .puppy),
        KeyboardTheme(id: "pudding", name: "布丁",
                      backgroundTop: UIColor(hex: 0xFFF6CF), backgroundBottom: UIColor(hex: 0xFFEFB0),
                      key: UIColor(hex: 0xFFFCF0), keyText: UIColor(hex: 0x7A5B12),
                      specialKey: UIColor(hex: 0xF5DFA0), specialText: UIColor(hex: 0x6B4E0A),
                      accent: UIColor(hex: 0xD9A441), accentText: .white,
                      shadow: UIColor(hex: 0xE6D3A0), decoration: "", isDark: false, shape: .pudding),
        KeyboardTheme(id: "jellyfish", name: "水母",
                      backgroundTop: UIColor(hex: 0xECE5FF), backgroundBottom: UIColor(hex: 0xE0D6FF),
                      key: .white, keyText: UIColor(hex: 0x5B4B8A),
                      specialKey: UIColor(hex: 0xD3C8F7), specialText: UIColor(hex: 0x4A3B78),
                      accent: UIColor(hex: 0x9C86EC), accentText: .white,
                      shadow: UIColor(hex: 0xC9BEEE), decoration: "", isDark: false, shape: .jellyfish),
        KeyboardTheme(id: "flame", name: "火焰",
                      backgroundTop: UIColor(hex: 0xFFE8DB), backgroundBottom: UIColor(hex: 0xFFDCCB),
                      key: UIColor(hex: 0xFFFAF4), keyText: UIColor(hex: 0x8A5040),
                      specialKey: UIColor(hex: 0xF9CDB4), specialText: UIColor(hex: 0x7A3F2C),
                      accent: UIColor(hex: 0xF59A6B), accentText: .white,
                      shadow: UIColor(hex: 0xEBC6B2), decoration: "", isDark: false, shape: .flame),
        KeyboardTheme(id: "droplet", name: "水滴",
                      backgroundTop: UIColor(hex: 0xDFF7EF), backgroundBottom: UIColor(hex: 0xCDF0E4),
                      key: .white, keyText: UIColor(hex: 0x2F6B5E),
                      specialKey: UIColor(hex: 0xB5E6D8), specialText: UIColor(hex: 0x2F6B5E),
                      accent: UIColor(hex: 0x4CC3A5), accentText: .white,
                      shadow: UIColor(hex: 0xB3DDD0), decoration: "", isDark: false, shape: .droplet),
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
    case rounded, jelly, catEar, cloud, duck
    // 奶油軟萌系列：按鍵上有第二種顏色的小細節，寬的鍵改成長條＋小徽章
    case bunny, puppy, pudding, jellyfish, flame, droplet

    var title: String {
        switch self {
        case .rounded: return "圓角"
        case .jelly: return "果凍"
        case .catEar: return "貓耳"
        case .cloud: return "雲朵"
        case .duck: return "小鴨"
        case .bunny: return "兔兔"
        case .puppy: return "狗狗"
        case .pudding: return "布丁"
        case .jellyfish: return "水母"
        case .flame: return "火焰"
        case .droplet: return "水滴"
        }
    }

    /// 細節的顏色（兔耳內側、焦糖、傘紋…）；nil 表示這個造型沒有細節
    var decorColor: UIColor? {
        switch self {
        case .bunny: return UIColor(hex: 0xFFB8CF)
        case .puppy: return UIColor(hex: 0xD9B79A)
        case .pudding: return UIColor(hex: 0xB5764A)
        case .jellyfish: return UIColor(hex: 0xC9BBF5)
        case .flame: return UIColor(hex: 0xF6B98E)
        case .droplet: return UIColor(hex: 0x9ED3EA)
        default: return nil
        }
    }

    private func earSize(_ r: CGRect) -> CGFloat { min(r.width * 0.24, r.height * 0.24, 9) }
    private func bumpSize(_ r: CGRect) -> CGFloat { min(r.height * 0.16, 6) }
    private func headSize(_ r: CGRect) -> CGFloat { min(r.width * 0.2, r.height * 0.2, 7) }
    private func bunnyEar(_ r: CGRect) -> CGFloat { min(r.height * 0.3, 13) }
    private func puppyEar(_ r: CGRect) -> CGFloat { min(r.width * 0.24, 10) }
    private func jellyHead(_ r: CGRect) -> CGFloat { r.height * 0.74 }

    /// 空白鍵這種很寬的鍵：造型拉長會變形，改畫圓角長條，左邊放一個小徽章
    private func isWide(_ r: CGRect) -> Bool { decorColor != nil && r.width > r.height * 2.4 }

    /// 實際要塗的細節顏色：寬鍵的徽章是整個造型縮小，布丁整塊塗焦糖色太深，改用淺奶茶色
    func decorTint(in r: CGRect) -> UIColor? {
        if self == .pudding && isWide(r) { return UIColor(hex: 0xDDBBA0) }
        return decorColor
    }

    /// 按鍵本體（文字畫在這裡面）
    func bodyRect(in r: CGRect) -> CGRect {
        // 寬鍵左右都讓出徽章的寬度，字才會置中又不壓到徽章
        if isWide(r) { return r.insetBy(dx: r.height * 0.9, dy: 0) }
        switch self {
        case .catEar:
            let ear = earSize(r) * 0.6
            return CGRect(x: r.minX, y: r.minY + ear, width: r.width, height: r.height - ear)
        case .cloud:
            let b = bumpSize(r)
            return CGRect(x: r.minX, y: r.minY + b, width: r.width, height: r.height - b)
        case .duck:
            let h = headSize(r) * 1.2
            return CGRect(x: r.minX, y: r.minY + h, width: r.width, height: r.height - h)
        case .bunny:
            let ear = bunnyEar(r) * 0.7
            return CGRect(x: r.minX, y: r.minY + ear, width: r.width, height: r.height - ear)
        case .puppy:
            let ear = puppyEar(r)
            return CGRect(x: r.minX + ear * 0.5, y: r.minY, width: r.width - ear, height: r.height)
        case .pudding:
            return CGRect(x: r.minX, y: r.minY + r.height * 0.2, width: r.width, height: r.height * 0.8)
        case .jellyfish:
            // 字往下一點，讓開傘頂的條紋
            let head = jellyHead(r)
            return CGRect(x: r.minX, y: r.minY + head * 0.2, width: r.width, height: head * 0.8)
        case .flame:
            return CGRect(x: r.minX, y: r.minY + r.height * 0.28, width: r.width, height: r.height * 0.7)
        case .droplet:
            return CGRect(x: r.minX, y: r.minY + r.height * 0.36, width: r.width, height: r.height * 0.64)
        default:
            return r
        }
    }

    func path(in r: CGRect) -> CGPath {
        if isWide(r) {
            return UIBezierPath(roundedRect: r, cornerRadius: min(r.height * 0.4, 14)).cgPath
        }
        let body = bodyRect(in: r)
        let m = min(r.width, r.height)
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
        case .duck:
            // 左上鴨頭＋往左的嘴，右上翹尾巴
            let h = headSize(r)
            var path = UIBezierPath(roundedRect: body, cornerRadius: min(body.width, body.height) * 0.4).cgPath
            let head = CGPoint(x: body.minX + h * 1.9, y: body.minY + h * 0.1)
            let headCircle = UIBezierPath(arcCenter: head, radius: h, startAngle: 0, endAngle: .pi * 2, clockwise: true)
            let beak = UIBezierPath(ovalIn: CGRect(x: body.minX, y: head.y - h * 0.05, width: h * 1.1, height: h * 0.55))
            let tail = UIBezierPath()
            tail.move(to: CGPoint(x: body.maxX - h * 1.6, y: body.minY + h * 0.4))
            tail.addLine(to: CGPoint(x: body.maxX - h * 0.1, y: body.minY - h * 0.9))
            tail.addLine(to: CGPoint(x: body.maxX - h * 0.3, y: body.minY + h * 1.0))
            tail.close()
            path = path.union(headCircle.cgPath).union(beak.cgPath).union(tail.cgPath)
            return path
        case .bunny:
            var path = UIBezierPath(roundedRect: body, cornerRadius: m * 0.38).cgPath
            for ear in bunnyEars(r) { path = path.union(ear.outer) }
            return path
        case .puppy:
            var path = UIBezierPath(roundedRect: body, cornerRadius: min(body.width, body.height) * 0.42).cgPath
            for ear in puppyEars(r) { path = path.union(ear.outer) }
            return path
        case .pudding:
            // 上窄下寬的布丁杯
            let top = r.width * 0.1
            return roundedPolygon([
                (CGPoint(x: r.minX + top, y: r.minY), m * 0.12),
                (CGPoint(x: r.maxX - top, y: r.minY), m * 0.12),
                (CGPoint(x: r.maxX, y: r.maxY), m * 0.3),
                (CGPoint(x: r.minX, y: r.maxY), m * 0.3),
            ])
        case .jellyfish:
            // 圓傘頭＋四隻短觸手
            let head = CGRect(x: r.minX, y: r.minY, width: r.width, height: jellyHead(r))
            let dome = min(head.width, head.height) * 0.48
            let rim = m * 0.14
            var path = roundedPolygon([
                (CGPoint(x: head.minX, y: head.minY), dome),
                (CGPoint(x: head.maxX, y: head.minY), dome),
                (CGPoint(x: head.maxX, y: head.maxY), rim),
                (CGPoint(x: head.minX, y: head.maxY), rim),
            ])
            let tw = max(2.5, r.width * 0.1)
            let legs: [(x: CGFloat, length: CGFloat, tilt: CGFloat)] = [(0.22, 0.8, 0.18), (0.41, 1, 0), (0.59, 1, 0), (0.78, 0.8, -0.18)]
            for leg in legs {
                let top = CGPoint(x: r.minX + r.width * leg.x, y: head.maxY - 3)
                let rect = CGRect(x: top.x - tw / 2, y: top.y, width: tw, height: (r.maxY - top.y) * leg.length)
                path = path.union(turned(UIBezierPath(roundedRect: rect, cornerRadius: tw / 2), leg.tilt, around: top))
            }
            return path
        case .flame:
            return flamePath(in: r)
        case .droplet:
            return dropletPath(in: r)
        }
    }

    /// 細節（用 decorColor 疊在本體上面畫）
    func decorPath(in r: CGRect) -> CGPath? {
        guard decorColor != nil else { return nil }
        if isWide(r) {
            // 徽章就是這個造型本身縮小
            let s = r.height * 0.56
            return path(in: CGRect(x: r.minX + r.height * 0.32, y: r.midY - s / 2, width: s, height: s))
        }
        let m = min(r.width, r.height)
        func p(_ x: CGFloat, _ y: CGFloat) -> CGPoint { point(r, x, y) }
        switch self {
        case .bunny:
            let path = CGMutablePath()
            for ear in bunnyEars(r) { path.addPath(ear.inner) }
            return path
        case .puppy:
            let path = CGMutablePath()
            for ear in puppyEars(r) { path.addPath(ear.inner) }
            return path
        case .pudding:
            // 頂上一層焦糖，下緣三段波浪（中間那段垂比較深）
            let cap = r.minY + r.height * 0.17
            var caramel = CGPath(rect: CGRect(x: r.minX - 1, y: r.minY - 1, width: r.width + 2, height: cap - r.minY + 1), transform: nil)
            for (x, depth) in [(CGFloat(0.18), CGFloat(0.1)), (0.5, 0.14), (0.82, 0.1)] {
                let drip = CGRect(x: r.minX + r.width * (x - 0.19), y: cap - r.height * depth / 2,
                                  width: r.width * 0.38, height: r.height * depth)
                caramel = caramel.union(UIBezierPath(ovalIn: drip).cgPath)
            }
            return caramel.intersection(path(in: r))
        case .jellyfish:
            // 傘頂一道彎彎的條紋
            let head = jellyHead(r)
            let arc = CGRect(x: r.minX + r.width * 0.16, y: r.minY + head * 0.06, width: r.width * 0.68, height: head * 0.36)
            return UIBezierPath(ovalIn: arc).cgPath
                .subtracting(UIBezierPath(ovalIn: arc.offsetBy(dx: 0, dy: head * 0.08)).cgPath)
        case .flame:
            // 左右兩道內焰
            let line = CGMutablePath()
            line.move(to: p(0.17, 0.5))
            line.addQuadCurve(to: p(0.28, 0.92), control: p(0.08, 0.78))
            line.move(to: p(0.83, 0.5))
            line.addQuadCurve(to: p(0.72, 0.92), control: p(0.92, 0.78))
            return line.copy(strokingWithWidth: m * 0.08, lineCap: .round, lineJoin: .round, miterLimit: 1)
        case .droplet:
            // 左邊一道反光
            let line = CGMutablePath()
            line.move(to: p(0.22, 0.5))
            line.addQuadCurve(to: p(0.2, 0.82), control: p(0.08, 0.64))
            return line.copy(strokingWithWidth: m * 0.08, lineCap: .round, lineJoin: .round, miterLimit: 1)
        default:
            return nil
        }
    }

    // MARK: 畫造型用的小工具

    /// 0～1 的比例座標，按鍵長寬不同也會跟著縮放
    private func point(_ r: CGRect, _ x: CGFloat, _ y: CGFloat) -> CGPoint {
        CGPoint(x: r.minX + r.width * x, y: r.minY + r.height * y)
    }

    /// 繞著某一點轉一個角度（耳朵、觸手往外撇）
    private func turned(_ path: UIBezierPath, _ angle: CGFloat, around c: CGPoint) -> CGPath {
        path.apply(CGAffineTransform(translationX: c.x, y: c.y).rotated(by: angle).translatedBy(x: -c.x, y: -c.y))
        return path.cgPath
    }

    /// 多邊形，每個角有自己的圓角半徑
    private func roundedPolygon(_ corners: [(CGPoint, CGFloat)]) -> CGPath {
        let path = CGMutablePath()
        let first = corners[0].0, last = corners[corners.count - 1].0
        path.move(to: CGPoint(x: (first.x + last.x) / 2, y: (first.y + last.y) / 2))
        for (i, corner) in corners.enumerated() {
            path.addArc(tangent1End: corner.0, tangent2End: corners[(i + 1) % corners.count].0, radius: corner.1)
        }
        path.closeSubpath()
        return path
    }

    /// 兩隻短短的兔耳朵（往外撇），inner 是粉紅色內側
    private func bunnyEars(_ r: CGRect) -> [(outer: CGPath, inner: CGPath)] {
        let h = bunnyEar(r) * 1.4
        let w = min(r.width * 0.24, 9)
        let ears: [(x: CGFloat, tilt: CGFloat)] = [(0.3, -0.18), (0.7, 0.18)]
        return ears.map { ear -> (outer: CGPath, inner: CGPath) in
            let rect = CGRect(x: r.minX + r.width * ear.x - w / 2, y: r.minY, width: w, height: h)
            let base = CGPoint(x: rect.midX, y: rect.maxY)
            let inner = CGRect(x: rect.minX + w * 0.25, y: rect.minY + w * 0.3, width: w * 0.5, height: h * 0.45)
            return (turned(UIBezierPath(roundedRect: rect, cornerRadius: w / 2), ear.tilt, around: base),
                    turned(UIBezierPath(roundedRect: inner, cornerRadius: w * 0.25), ear.tilt, around: base))
        }
    }

    /// 兩側圓圓的垂耳，inner 那塊放在靠外側，才不會蓋到臉
    private func puppyEars(_ r: CGRect) -> [(outer: CGPath, inner: CGPath)] {
        let w = puppyEar(r)
        let h = r.height * 0.5
        let ears: [(x: CGFloat, tilt: CGFloat)] = [(r.minX + 1, 0.12), (r.maxX - 1 - w, -0.12)]
        return ears.map { ear -> (outer: CGPath, inner: CGPath) in
            let rect = CGRect(x: ear.x, y: r.minY + r.height * 0.1, width: w, height: h)
            let top = CGPoint(x: rect.midX, y: rect.minY)
            let innerX = ear.tilt > 0 ? rect.minX + w * 0.15 : rect.maxX - w * 0.55
            let inner = CGRect(x: innerX, y: rect.minY + h * 0.2, width: w * 0.4, height: h * 0.62)
            return (turned(UIBezierPath(ovalIn: rect), ear.tilt, around: top),
                    turned(UIBezierPath(ovalIn: inner), ear.tilt, around: top))
        }
    }

    /// 圓底的小火苗：中間一個主火尖，左右各一個短短的小火尖
    private func flamePath(in r: CGRect) -> CGPath {
        func p(_ x: CGFloat, _ y: CGFloat) -> CGPoint { point(r, x, y) }
        let path = CGMutablePath()
        path.move(to: p(0.5, 1))
        path.addCurve(to: p(0.03, 0.62), control1: p(0.2, 1), control2: p(0.03, 0.82))
        path.addCurve(to: p(0.15, 0.3), control1: p(0.03, 0.48), control2: p(0.08, 0.37))
        path.addCurve(to: p(0.32, 0.42), control1: p(0.22, 0.34), control2: p(0.28, 0.38))
        path.addCurve(to: p(0.57, 0.04), control1: p(0.32, 0.24), control2: p(0.44, 0.1))
        path.addCurve(to: p(0.72, 0.36), control1: p(0.6, 0.17), control2: p(0.66, 0.28))
        path.addCurve(to: p(0.88, 0.25), control1: p(0.78, 0.33), control2: p(0.84, 0.3))
        path.addCurve(to: p(0.97, 0.62), control1: p(0.95, 0.36), control2: p(0.97, 0.5))
        path.addCurve(to: p(0.5, 1), control1: p(0.97, 0.82), control2: p(0.8, 1))
        path.closeSubpath()
        return path
    }

    /// 飽滿的水珠，尖端短短的
    private func dropletPath(in r: CGRect) -> CGPath {
        func p(_ x: CGFloat, _ y: CGFloat) -> CGPoint { point(r, x, y) }
        let path = CGMutablePath()
        path.move(to: p(0.54, 0.06))
        path.addCurve(to: p(0.97, 0.64), control1: p(0.64, 0.24), control2: p(0.97, 0.38))
        path.addCurve(to: p(0.5, 1), control1: p(0.97, 0.86), control2: p(0.77, 1))
        path.addCurve(to: p(0.03, 0.64), control1: p(0.23, 1), control2: p(0.03, 0.86))
        path.addCurve(to: p(0.54, 0.06), control1: p(0.03, 0.38), control2: p(0.44, 0.24))
        path.closeSubpath()
        return path
    }
}

struct KeyboardStyle {
    var theme: KeyboardTheme
    var shape: KeyShape
    var keyAlpha: CGFloat
    var fontScale: CGFloat = 1   // 按鍵上的字放大縮小
    var gapScale: CGFloat = 1    // 按鍵之間的空隙（越小按鍵越大）
    var fontID = KeyFont.system.rawValue
    var keyEffect = KeyEffect.none.rawValue
}

/// 按下按鍵時冒出來的小圖案
enum KeyEffect: String, CaseIterable {
    case none, stars, hearts, sparkles, flowers, bubbles

    var title: String {
        switch self {
        case .none: return "無"
        case .stars: return "星星"
        case .hearts: return "愛心"
        case .sparkles: return "閃亮"
        case .flowers: return "花花"
        case .bubbles: return "泡泡"
        }
    }

    var glyphs: [String] {
        switch self {
        case .none: return []
        case .stars: return ["★", "☆", "✦"]
        case .hearts: return ["♥\u{FE0E}", "♡", "❤\u{FE0E}"]
        case .sparkles: return ["✧", "✦", "⋆"]
        case .flowers: return ["✿", "❀", "❁"]
        case .bubbles: return ["○", "◦", "°"]
        }
    }
}

/// 按鍵音：系統的喀喀聲，或 App 附的合成音（tools/make_sounds.js）
enum KeySound: String, CaseIterable {
    case system, bubble, marimba, bell, pop

    var title: String {
        switch self {
        case .system: return "系統"
        case .bubble: return "泡泡"
        case .marimba: return "木琴"
        case .bell: return "叮鈴"
        case .pop: return "啵啵"
        }
    }
}

/// 按鍵和候選字用的字體（App 裡附的免費字體，SIL OFL 1.1）
enum KeyFont: String, CaseIterable {
    case system, huninn, iansui, wenkai

    var title: String {
        switch self {
        case .system: return "系統"
        case .huninn: return "粉圓"
        case .iansui: return "芫荽"
        case .wenkai: return "文楷"
        }
    }
}

enum KeyFonts {
    private static var names: [String: String] = [:]

    /// 選的字體；系統字體或讀不到時用圓體
    static func font(_ id: String, size: CGFloat, weight: UIFont.Weight = .regular) -> UIFont {
        guard id != KeyFont.system.rawValue, let name = postScriptName(id), let f = UIFont(name: name, size: size) else {
            return .rounded(size, weight)
        }
        return f
    }

    /// 第一次用時把字體檔註冊給系統，記住它的名字
    private static func postScriptName(_ id: String) -> String? {
        if let n = names[id] { return n }
        guard let url = Bundle.main.url(forResource: id, withExtension: "ttf")
                ?? Bundle.main.url(forResource: id, withExtension: "ttf", subdirectory: "Fonts"),
              let provider = CGDataProvider(url: url as CFURL),
              let cg = CGFont(provider),
              let name = cg.postScriptName as String? else { return nil }
        CTFontManagerRegisterGraphicsFont(cg, nil)
        names[id] = name
        return name
    }
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
    var fontID: String {
        get { d.string(forKey: "fontID") ?? KeyFont.system.rawValue }
        set { d.set(newValue, forKey: "fontID") }
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
    // MARK: 背景照片（可以放好幾張輪流換，每張記住自己的位置和大小）

    /// 照片裁切時盡量放在中間的點（0～1，貼上時有找到臉就是臉的位置）、放大倍數（1 = 剛好蓋滿鍵盤）
    struct BgPhoto: Codable, Equatable {
        var file: String
        var fx: Double = 0.5
        var fy: Double = 0.3
        var zoom: Double = 1
    }

    static let photoDirectory: URL = {
        let dir = Storage.directory.appendingPathComponent("backgrounds", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }()

    var photos: [BgPhoto] {
        get {
            guard let data = d.data(forKey: "photos") else { return migrateOldBackground() }
            return (try? JSONDecoder().decode([BgPhoto].self, from: data)) ?? []
        }
        set { d.set(try? JSONEncoder().encode(newValue), forKey: "photos") }
    }

    /// 舊版只有一張 background.jpg：搬進照片清單
    private func migrateOldBackground() -> [BgPhoto] {
        var list: [BgPhoto] = []
        let old = Storage.url("background.jpg")
        if bool("hasBackground", false), FileManager.default.fileExists(atPath: old.path) {
            let name = "bg-old.jpg"
            if (try? FileManager.default.moveItem(at: old, to: Self.photoDirectory.appendingPathComponent(name))) != nil {
                list.append(BgPhoto(file: name, fx: double("backgroundFocusX", 0.5), fy: double("backgroundFocusY", 0.3),
                                    zoom: double("backgroundZoom", 1)))
            }
        }
        photos = list
        return list
    }

    var photoIndex: Int {
        get { d.integer(forKey: "photoIndex") }
        set { d.set(newValue, forKey: "photoIndex") }
    }

    /// 照片怎麼換：fixed 固定、open 每次打開鍵盤換、daily 每天換
    var photoRotation: String {
        get { d.string(forKey: "photoRotation") ?? "fixed" }
        set { d.set(newValue, forKey: "photoRotation") }
    }

    private func wrapped(_ i: Int, _ n: Int) -> Int { ((i % n) + n) % n }

    var currentPhoto: BgPhoto? {
        let list = photos
        return list.isEmpty ? nil : list[wrapped(photoIndex, list.count)]
    }

    var currentPhotoURL: URL? { currentPhoto.map { Self.photoDirectory.appendingPathComponent($0.file) } }
    var hasBackground: Bool { currentPhotoURL.map { FileManager.default.fileExists(atPath: $0.path) } ?? false }

    private func updateCurrent(_ change: (inout BgPhoto) -> Void) {
        var list = photos
        guard !list.isEmpty else { return }
        change(&list[wrapped(photoIndex, list.count)])
        photos = list
    }

    var backgroundFocusX: Double {
        get { currentPhoto?.fx ?? 0.5 }
        set { updateCurrent { $0.fx = newValue } }
    }
    var backgroundFocusY: Double {
        get { currentPhoto?.fy ?? 0.3 }
        set { updateCurrent { $0.fy = newValue } }
    }
    var backgroundZoom: Double {
        get { currentPhoto?.zoom ?? 1 }
        set { updateCurrent { $0.zoom = min(max(newValue, 1), 5) } }
    }

    /// 新增一張照片，並換成這張
    func addPhoto(_ data: Data, fx: Double, fy: Double) throws {
        let name = "bg-\(Int(Date().timeIntervalSince1970 * 1000)).jpg"
        try data.write(to: Self.photoDirectory.appendingPathComponent(name), options: .atomic)
        let list = photos + [BgPhoto(file: name, fx: fx, fy: fy, zoom: 1)]
        photos = list
        photoIndex = list.count - 1
    }

    func removePhoto(at index: Int) {
        var list = photos
        guard list.indices.contains(index) else { return }
        try? FileManager.default.removeItem(at: Self.photoDirectory.appendingPathComponent(list.remove(at: index).file))
        photos = list
        photoIndex = list.isEmpty ? 0 : min(wrapped(photoIndex, list.count), list.count - 1)
    }

    func clearPhotos() {
        for p in photos { try? FileManager.default.removeItem(at: Self.photoDirectory.appendingPathComponent(p.file)) }
        photos = []
        photoIndex = 0
    }

    /// 打開鍵盤時依設定換照片
    func rotatePhoto() {
        let n = photos.count
        guard n >= 2 else { return }
        switch photoRotation {
        case "open": photoIndex = (wrapped(photoIndex, n) + 1) % n
        case "daily":
            let day = Calendar.current.ordinality(of: .day, in: .era, for: Date()) ?? 0
            photoIndex = day % n
        default: break
        }
    }

    /// 照片的小縮圖（設定面板用）
    func thumbnail(_ p: BgPhoto) -> UIImage? {
        UIImage(contentsOfFile: Self.photoDirectory.appendingPathComponent(p.file).path)?
            .preparingThumbnail(of: CGSize(width: 120, height: 120))
    }
    var keyEffect: String {
        get { d.string(forKey: "keyEffect") ?? KeyEffect.none.rawValue }
        set { d.set(newValue, forKey: "keyEffect") }
    }
    var keySoundStyle: String {
        get { d.string(forKey: "keySoundStyle") ?? KeySound.system.rawValue }
        set { d.set(newValue, forKey: "keySoundStyle") }
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
    var glideTyping: Bool {
        get { bool("glideTyping", true) }
        set { d.set(newValue, forKey: "glideTyping") }
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
    /// 拼音模式輸出簡體字（馬來西亞、中國用簡體）
    var pinyinSimplified: Bool {
        get { bool("pinyinSimplified", false) }
        set { d.set(newValue, forKey: "pinyinSimplified") }
    }
    /// 中文符號頁用全形（＃＊）還是半形（# *）；預設半形，Discord 語法才能用
    var fullWidthSymbols: Bool {
        get { bool("fullWidthSymbols", false) }
        set { d.set(newValue, forKey: "fullWidthSymbols") }
    }
    /// 翻譯的原文語言："auto" 是自己猜
    var translateSource: String {
        get { d.string(forKey: "translateSource") ?? "auto" }
        set { d.set(newValue, forKey: "translateSource") }
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
