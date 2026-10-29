import UIKit

/// 鍵盤裡的設定：主題、按鍵造型、背景圖片、輸入法、聲音震動
final class SettingsPanelView: UIView {
    var onChange: (() -> Void)?
    var onPasteBackground: (() -> Void)?
    var onClearBackground: (() -> Void)?
    var onResetLearning: (() -> Void)?
    var onBack: (() -> Void)?
    var style: KeyboardStyle {
        didSet {
            // 拖滑桿時不要重建（不然滑桿會被拿掉），只有換主題或造型才重建
            if oldValue.theme.id != style.theme.id || oldValue.shape != style.shape {
                rebuild()
            } else {
                bar.style = style
            }
        }
    }

    private let settings: KeyboardSettings
    private let hasFullAccess: Bool
    private let scroll = UIScrollView()
    private let stack = UIStackView()
    private let bar: PanelBottomBar

    init(settings: KeyboardSettings, hasFullAccess: Bool, style: KeyboardStyle) {
        self.settings = settings
        self.hasFullAccess = hasFullAccess
        self.style = style
        bar = PanelBottomBar(style: style)
        super.init(frame: .zero)
        stack.axis = .vertical
        stack.spacing = 10
        scroll.addSubview(stack)
        scroll.alwaysBounceVertical = true
        addSubview(scroll)
        bar.showsBackspace = false
        bar.setTabs(["設定"])
        bar.onBack = { [weak self] in self?.onBack?() }
        addSubview(bar)
        rebuild()
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    override func layoutSubviews() {
        super.layoutSubviews()
        let barH: CGFloat = 40
        scroll.frame = CGRect(x: 0, y: 0, width: bounds.width, height: bounds.height - barH)
        bar.frame = CGRect(x: 0, y: bounds.height - barH, width: bounds.width, height: barH)
        let w = bounds.width - 24
        let size = stack.systemLayoutSizeFitting(CGSize(width: w, height: 0),
                                                 withHorizontalFittingPriority: .required,
                                                 verticalFittingPriority: .fittingSizeLevel)
        stack.frame = CGRect(x: 12, y: 8, width: w, height: size.height)
        scroll.contentSize = CGSize(width: bounds.width, height: size.height + 16)
    }

    // MARK: 建立內容

    private func rebuild() {
        bar.style = style
        let offset = scroll.contentOffset
        stack.arrangedSubviews.forEach { $0.removeFromSuperview() }

        stack.addArrangedSubview(header("主題"))
        stack.addArrangedSubview(themeRow())

        stack.addArrangedSubview(header("按鍵造型"))
        stack.addArrangedSubview(shapeRow())

        stack.addArrangedSubview(header("背景圖片"))
        stack.addArrangedSubview(note("先在相簿把照片「拷貝」，再按下面的按鈕。也可以用「星星鍵盤」App 挑照片。"))
        stack.addArrangedSubview(buttonRow([
            ("從剪貼簿貼上圖片", { [weak self] in self?.onPasteBackground?() }),
            ("清除背景", { [weak self] in self?.onClearBackground?() }),
        ]))
        stack.addArrangedSubview(sliderRow("圖片淡化", value: Float(settings.backgroundFade), range: 0...0.9) { [weak self] v in
            self?.settings.backgroundFade = CGFloat(v)
            self?.onChange?()
        })
        stack.addArrangedSubview(sliderRow("按鍵透明度", value: Float(settings.keyAlpha), range: 0.35...1) { [weak self] v in
            self?.settings.keyAlpha = CGFloat(v)
            self?.onChange?()
        })

        stack.addArrangedSubview(header("輸入法（按「中／英」鍵輪流切換）"))
        for mode in InputMode.allCases {
            stack.addArrangedSubview(switchRow(mode.title, on: settings.enabledModes.contains(mode)) { [weak self] on in
                guard let self = self else { return }
                var modes = self.settings.enabledModes
                if on {
                    if !modes.contains(mode) { modes.append(mode) }
                } else {
                    modes.removeAll { $0 == mode }
                }
                self.settings.enabledModes = InputMode.allCases.filter { modes.contains($0) }
                self.onChange?()
            })
        }
        stack.addArrangedSubview(switchRow("聯想詞（打完字後推薦下一個字）", on: settings.associations) { [weak self] on in
            self?.settings.associations = on
        })

        stack.addArrangedSubview(header("聲音與震動"))
        stack.addArrangedSubview(switchRow("按鍵聲音", on: settings.keySound) { [weak self] on in
            self?.settings.keySound = on
        })
        stack.addArrangedSubview(switchRow("按鍵震動", on: settings.haptics) { [weak self] on in
            self?.settings.haptics = on
        })

        stack.addArrangedSubview(header("剪貼簿最多記幾筆"))
        stack.addArrangedSubview(clipLimitRow())

        stack.addArrangedSubview(header("其他"))
        stack.addArrangedSubview(buttonRow([
            ("清除選字學習紀錄", { [weak self] in self?.onResetLearning?() }),
        ]))
        if !hasFullAccess {
            stack.addArrangedSubview(note("⚠️ 還沒開啟「允許完整取用」：剪貼簿、聲音、震動、貼圖、背景圖片都需要它。\n設定 › 一般 › 鍵盤 › 鍵盤 › 星星鍵盤 › 允許完整取用"))
        }
        stack.addArrangedSubview(note("星星鍵盤 0.1 ・ 注音字典來自小麥注音（MIT 授權）"))

        setNeedsLayout()
        layoutIfNeeded()
        scroll.contentOffset = CGPoint(x: 0, y: min(offset.y, max(0, scroll.contentSize.height - scroll.bounds.height)))
    }

    private var textColor: UIColor { style.theme.keyText }

    private func header(_ text: String) -> UILabel {
        let l = UILabel()
        l.text = text
        l.font = .rounded(14, .bold)
        l.textColor = textColor.withAlphaComponent(0.8)
        return l
    }

    private func note(_ text: String) -> UILabel {
        let l = UILabel()
        l.text = text
        l.numberOfLines = 0
        l.font = .rounded(12)
        l.textColor = textColor.withAlphaComponent(0.65)
        return l
    }

    private func card() -> UIView {
        let v = UIView()
        v.backgroundColor = style.theme.key.withAlphaComponent(0.55)
        v.layer.cornerRadius = 12
        return v
    }

    private func themeRow() -> UIView {
        let scrollRow = UIScrollView()
        scrollRow.showsHorizontalScrollIndicator = false
        scrollRow.heightAnchor.constraint(equalToConstant: 78).isActive = true
        var x: CGFloat = 0
        for theme in KeyboardTheme.all {
            let selected = theme.id == settings.themeID
            let b = UIButton(type: .custom)
            b.frame = CGRect(x: x, y: 0, width: 64, height: 76)
            let swatch = UIView(frame: CGRect(x: 8, y: 2, width: 48, height: 48))
            swatch.isUserInteractionEnabled = false
            swatch.backgroundColor = theme.backgroundTop
            swatch.layer.cornerRadius = 24
            swatch.layer.borderWidth = selected ? 3 : 1
            swatch.layer.borderColor = (selected ? theme.accent : theme.shadow).cgColor
            let dot = UIView(frame: CGRect(x: 12, y: 14, width: 24, height: 20))
            dot.backgroundColor = theme.key
            dot.layer.cornerRadius = 7
            let accent = UIView(frame: CGRect(x: 30, y: 30, width: 12, height: 10))
            accent.backgroundColor = theme.accent
            accent.layer.cornerRadius = 4
            swatch.addSubview(dot)
            swatch.addSubview(accent)
            b.addSubview(swatch)
            let name = UILabel(frame: CGRect(x: 0, y: 54, width: 64, height: 18))
            name.text = theme.name
            name.font = .rounded(11, selected ? .bold : .regular)
            name.textAlignment = .center
            name.textColor = textColor
            b.addSubview(name)
            b.addAction(UIAction { [weak self] _ in
                self?.settings.themeID = theme.id
                self?.onChange?()
            }, for: .touchUpInside)
            scrollRow.addSubview(b)
            x += 68
        }
        scrollRow.contentSize = CGSize(width: x, height: 76)
        return scrollRow
    }

    private func shapeRow() -> UIView {
        let row = UIStackView()
        row.axis = .horizontal
        row.distribution = .fillEqually
        row.spacing = 8
        row.heightAnchor.constraint(equalToConstant: 64).isActive = true
        for shape in KeyShape.allCases {
            let selected = shape == settings.keyShape
            let b = UIButton(type: .custom)
            b.layer.cornerRadius = 12
            b.backgroundColor = selected ? style.theme.specialKey.withAlphaComponent(0.8) : .clear
            let preview = CAShapeLayer()
            preview.path = shape.path(in: CGRect(x: 0, y: 0, width: 40, height: 34))
            preview.fillColor = style.theme.key.cgColor
            preview.strokeColor = style.theme.shadow.cgColor
            preview.lineWidth = 1
            let holder = UIView(frame: CGRect(x: 0, y: 0, width: 40, height: 34))
            holder.isUserInteractionEnabled = false
            holder.layer.addSublayer(preview)
            holder.translatesAutoresizingMaskIntoConstraints = false
            b.addSubview(holder)
            NSLayoutConstraint.activate([
                holder.centerXAnchor.constraint(equalTo: b.centerXAnchor),
                holder.topAnchor.constraint(equalTo: b.topAnchor, constant: 4),
                holder.widthAnchor.constraint(equalToConstant: 40),
                holder.heightAnchor.constraint(equalToConstant: 34),
            ])
            let name = UILabel()
            name.text = shape.title
            name.font = .rounded(12, selected ? .bold : .regular)
            name.textColor = textColor
            name.translatesAutoresizingMaskIntoConstraints = false
            b.addSubview(name)
            NSLayoutConstraint.activate([
                name.centerXAnchor.constraint(equalTo: b.centerXAnchor),
                name.bottomAnchor.constraint(equalTo: b.bottomAnchor, constant: -4),
            ])
            b.addAction(UIAction { [weak self] _ in
                self?.settings.keyShape = shape
                self?.onChange?()
            }, for: .touchUpInside)
            row.addArrangedSubview(b)
        }
        return row
    }

    private func buttonRow(_ buttons: [(String, () -> Void)]) -> UIView {
        let row = UIStackView()
        row.axis = .horizontal
        row.distribution = .fillEqually
        row.spacing = 8
        row.heightAnchor.constraint(equalToConstant: 40).isActive = true
        for (title, action) in buttons {
            let b = UIButton(type: .system)
            b.setTitle(title, for: .normal)
            b.titleLabel?.font = .rounded(14, .semibold)
            b.setTitleColor(style.theme.specialText, for: .normal)
            b.backgroundColor = style.theme.specialKey
            b.layer.cornerRadius = 10
            b.addAction(UIAction { _ in action() }, for: .touchUpInside)
            row.addArrangedSubview(b)
        }
        return row
    }

    private func sliderRow(_ title: String, value: Float, range: ClosedRange<Float>, onChange: @escaping (Float) -> Void) -> UIView {
        let row = UIStackView()
        row.axis = .horizontal
        row.spacing = 10
        let l = UILabel()
        l.text = title
        l.font = .rounded(14)
        l.textColor = textColor
        l.widthAnchor.constraint(equalToConstant: 86).isActive = true
        let s = UISlider()
        s.minimumValue = range.lowerBound
        s.maximumValue = range.upperBound
        s.value = value
        s.minimumTrackTintColor = style.theme.accent
        s.addAction(UIAction { [weak s] _ in if let s = s { onChange(s.value) } }, for: .valueChanged)
        row.addArrangedSubview(l)
        row.addArrangedSubview(s)
        return row
    }

    private func switchRow(_ title: String, on: Bool, onChange: @escaping (Bool) -> Void) -> UIView {
        let row = UIStackView()
        row.axis = .horizontal
        row.spacing = 10
        let l = UILabel()
        l.text = title
        l.font = .rounded(15)
        l.textColor = textColor
        l.numberOfLines = 0
        let sw = UISwitch()
        sw.isOn = on
        sw.onTintColor = style.theme.accent
        sw.setContentHuggingPriority(.required, for: .horizontal)
        sw.addAction(UIAction { [weak sw] _ in if let sw = sw { onChange(sw.isOn) } }, for: .valueChanged)
        row.addArrangedSubview(l)
        row.addArrangedSubview(sw)
        return row
    }

    private func clipLimitRow() -> UIView {
        let options = [50, 100, 200, 500]
        let seg = UISegmentedControl(items: options.map { "\($0)" })
        seg.selectedSegmentIndex = options.firstIndex(of: settings.clipLimit) ?? 2
        seg.selectedSegmentTintColor = style.theme.key
        seg.setTitleTextAttributes([.foregroundColor: textColor], for: .normal)
        seg.addAction(UIAction { [weak self, weak seg] _ in
            guard let seg = seg else { return }
            self?.settings.clipLimit = options[seg.selectedSegmentIndex]
            self?.onChange?()
        }, for: .valueChanged)
        return seg
    }
}
