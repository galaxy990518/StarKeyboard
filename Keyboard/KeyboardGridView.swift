import UIKit

/// 一顆按鍵（只負責畫，觸控由 KeyboardGridView 統一處理）
final class KeyButton: UIView {
    var spec: KeySpec {
        didSet {
            updateFace()
            applyColors()
        }
    }
    var style: KeyboardStyle {
        didSet {
            updateFace()
            applyColors()
            setNeedsLayout()
        }
    }
    var isPressed = false {
        didSet { if oldValue != isPressed { pressChanged() } }
    }

    private let content = UIView()
    private let shadowLayer = CAShapeLayer()
    private let bodyLayer = CAShapeLayer()
    private let glossLayer = CAShapeLayer()
    private let label = UILabel()
    private let icon = UIImageView()

    init(spec: KeySpec, style: KeyboardStyle) {
        self.spec = spec
        self.style = style
        super.init(frame: .zero)
        isUserInteractionEnabled = false
        addSubview(content)
        content.layer.addSublayer(shadowLayer)
        content.layer.addSublayer(bodyLayer)
        content.layer.addSublayer(glossLayer)
        label.textAlignment = .center
        label.adjustsFontSizeToFitWidth = true
        label.minimumScaleFactor = 0.5
        label.baselineAdjustment = .alignCenters
        content.addSubview(label)
        icon.contentMode = .center
        content.addSubview(icon)
        updateFace()
        applyColors()
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    override func layoutSubviews() {
        super.layoutSubviews()
        content.bounds = bounds
        content.center = CGPoint(x: bounds.midX, y: bounds.midY)
        let outer = bounds.insetBy(dx: 3 * style.gapScale, dy: 4.5 * style.gapScale)
        guard outer.width > 4, outer.height > 4 else { return }
        let path = style.shape.path(in: outer)
        var shift = CGAffineTransform(translationX: 0, y: 1.5)
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        bodyLayer.path = path
        shadowLayer.path = path.copy(using: &shift)
        if style.shape == .jelly {
            let body = style.shape.bodyRect(in: outer)
            let gloss = CGRect(x: body.minX + body.width * 0.2, y: body.minY + 2.5,
                               width: body.width * 0.6, height: max(2.5, body.height * 0.16))
            glossLayer.path = UIBezierPath(roundedRect: gloss, cornerRadius: gloss.height / 2).cgPath
        } else {
            glossLayer.path = nil
        }
        CATransaction.commit()
        let textRect = style.shape.bodyRect(in: outer)
        label.frame = textRect.insetBy(dx: 2, dy: 0)
        icon.frame = textRect
    }

    private func updateFace() {
        switch spec.face {
        case .text(let s):
            label.isHidden = false
            icon.isHidden = true
            label.text = s
            label.font = KeyFonts.font(style.fontID, size: spec.fontSize * style.fontScale, weight: spec.kind == .normal ? .regular : .semibold)
        case .symbol(let name):
            label.isHidden = true
            icon.isHidden = false
            icon.image = UIImage(systemName: name, withConfiguration: UIImage.SymbolConfiguration(pointSize: 18 * style.fontScale, weight: .medium))
        }
    }

    private func applyColors() {
        let t = style.theme
        var fill: UIColor
        let text: UIColor
        switch spec.kind {
        case .normal:
            fill = t.key
            text = t.keyText
        case .special:
            fill = t.specialKey
            text = t.specialText
        case .accent:
            fill = t.accent
            text = t.accentText
        }
        if isPressed {
            fill = spec.kind == .normal ? t.specialKey : t.key
        }
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        bodyLayer.fillColor = fill.withAlphaComponent(style.keyAlpha).cgColor
        shadowLayer.fillColor = t.shadow.withAlphaComponent(min(1, style.keyAlpha + 0.1)).cgColor
        glossLayer.fillColor = UIColor.white.withAlphaComponent(t.isDark ? 0.18 : 0.55).cgColor
        CATransaction.commit()
        label.textColor = text
        icon.tintColor = text
    }

    private func pressChanged() {
        applyColors()
        if isPressed {
            UIView.animate(withDuration: 0.05, delay: 0, options: [.allowUserInteraction, .beginFromCurrentState]) {
                self.content.transform = CGAffineTransform(scaleX: 0.9, y: 0.9)
            }
        } else {
            UIView.animate(withDuration: 0.3, delay: 0, usingSpringWithDamping: 0.45, initialSpringVelocity: 3,
                           options: [.allowUserInteraction, .beginFromCurrentState]) {
                self.content.transform = .identity
            }
        }
    }
}

/// 一整片按鍵：處理多指連打、滑動修正、刪除鍵連續刪、空白鍵拖曳移動游標
final class KeyboardGridView: UIView {
    var onKey: ((KeySpec) -> Void)?
    var onGlobe: ((UIView, UIEvent?) -> Void)?
    var onCursorMove: ((Int) -> Void)?
    var style: KeyboardStyle {
        didSet { keys.forEach { $0.style = style } }
    }

    private(set) var keys: [KeyButton] = []
    private var layout = KeyboardLayout(rows: [], columns: 10)
    private var tracks: [UITouch: Track] = [:]
    private var repeatTimer: Timer?

    private struct Track {
        var key: KeyButton
        let action: KeyAction   // 按下時的功能（版面中途換掉也照這個處理）
        var fired = false
        var origin: CGPoint
        var cursorX: CGFloat = 0
        var cursorMode = false
    }

    init(style: KeyboardStyle) {
        self.style = style
        super.init(frame: .zero)
        isMultipleTouchEnabled = true
        backgroundColor = .clear
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    // 按鍵物件會重用，所以換版面時手指還按著也不會出錯
    func setLayout(_ newLayout: KeyboardLayout) {
        layout = newLayout
        let specs = newLayout.rows.flatMap { $0 }
        while keys.count > specs.count {
            keys.removeLast().removeFromSuperview()
        }
        for (i, spec) in specs.enumerated() {
            if i < keys.count {
                keys[i].spec = spec
            } else {
                let key = KeyButton(spec: spec, style: style)
                addSubview(key)
                keys.append(key)
            }
        }
        setNeedsLayout()
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        guard !layout.rows.isEmpty else { return }
        let padX: CGFloat = 2, padY: CGFloat = 3
        let rowH = (bounds.height - padY * 2) / CGFloat(layout.rows.count)
        let unit = (bounds.width - padX * 2) / layout.columns
        var index = 0
        for (r, row) in layout.rows.enumerated() {
            let fixed = row.reduce(CGFloat(0)) { $0 + max($1.width, 0) }
            let flexCount = row.filter { $0.width == 0 }.count
            let flexW = flexCount > 0 ? max(1, (layout.columns - fixed) / CGFloat(flexCount)) : 0
            let total = fixed + flexW * CGFloat(flexCount)
            var x = padX + max(0, layout.columns - total) * unit / 2
            let y = padY + rowH * CGFloat(r)
            for spec in row {
                guard index < keys.count else { return }
                let w = (spec.width > 0 ? spec.width : flexW) * unit
                keys[index].frame = CGRect(x: x, y: y, width: w, height: rowH)
                x += w
                index += 1
            }
        }
    }

    // MARK: 觸控

    private func key(at p: CGPoint) -> KeyButton? {
        if let k = keys.first(where: { $0.frame.contains(p) }) { return k }
        return keys.min { distance($0.frame, p) < distance($1.frame, p) }
    }

    private func distance(_ r: CGRect, _ p: CGPoint) -> CGFloat {
        let dx = max(r.minX - p.x, 0, p.x - r.maxX)
        let dy = max(r.minY - p.y, 0, p.y - r.maxY)
        return dx * dx + dy * dy
    }

    private func fire(_ key: KeyButton) {
        onKey?(key.spec)
    }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        for touch in touches {
            // 打很快時，新的手指按下就先送出前一個還沒放開的字
            for (other, var track) in tracks where !track.fired && !track.cursorMode
                && (track.key.spec.action.isCharacter || track.key.spec.action == .space) {
                track.fired = true
                track.key.isPressed = false
                tracks[other] = track
                fire(track.key)
            }
            let p = touch.location(in: self)
            guard let key = key(at: p) else { continue }
            var track = Track(key: key, action: key.spec.action, origin: p)
            key.isPressed = true
            tracks[touch] = track
            switch key.spec.action {
            case .backspace:
                track.fired = true
                tracks[touch] = track
                fire(key)
                startRepeat(key)
            case .shift:
                track.fired = true
                tracks[touch] = track
                fire(key)
            case .globe:
                onGlobe?(self, event)
            default:
                break
            }
        }
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        for touch in touches {
            guard var track = tracks[touch], !track.fired else { continue }
            let p = touch.location(in: self)
            if track.key.spec.action == .space {
                if !track.cursorMode && abs(p.x - track.origin.x) > 16 {
                    track.cursorMode = true
                    track.cursorX = p.x
                }
                if track.cursorMode {
                    let steps = Int((p.x - track.cursorX) / 9)
                    if steps != 0 {
                        onCursorMove?(steps)
                        track.cursorX += CGFloat(steps) * 9
                    }
                }
            } else if track.key.spec.action.isCharacter, let k = key(at: p), k !== track.key, k.spec.action.isCharacter {
                track.key.isPressed = false
                k.isPressed = true
                track.key = k
            }
            tracks[touch] = track
        }
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        for touch in touches {
            guard let track = tracks.removeValue(forKey: touch) else { continue }
            track.key.isPressed = false
            switch track.action {
            case .backspace:
                stopRepeat()
            case .globe:
                onGlobe?(self, event)
            default:
                if !track.fired && !track.cursorMode { fire(track.key) }
            }
        }
    }

    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        for touch in touches {
            guard let track = tracks.removeValue(forKey: touch) else { continue }
            track.key.isPressed = false
            if track.action == .backspace { stopRepeat() }
        }
    }

    private func startRepeat(_ key: KeyButton) {
        stopRepeat()
        repeatTimer = Timer.scheduledTimer(withTimeInterval: 0.45, repeats: false) { [weak self] _ in
            guard let self = self else { return }
            self.repeatTimer = Timer.scheduledTimer(withTimeInterval: 0.08, repeats: true) { [weak self] _ in
                self?.fire(key)
            }
        }
    }

    private func stopRepeat() {
        repeatTimer?.invalidate()
        repeatTimer = nil
    }
}
