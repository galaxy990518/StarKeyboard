import UIKit
import AudioToolbox

final class KeyboardViewController: UIInputViewController {
    private enum Metrics {
        static let barHeight: CGFloat = 46
    }

    private enum SuggestionSource { case association, handwriting }

    private let settings = KeyboardSettings.shared
    private lazy var clipStore = ClipboardStore(fileURL: Storage.url("clipboard.json"), limit: settings.clipLimit)
    private lazy var emoStore = EmoticonStore(directory: Storage.directory)
    private lazy var engine: ZhuyinEngine? = {
        guard let path = Bundle.main.path(forResource: "zhuyin", ofType: "sqlite") else { return nil }
        return ZhuyinEngine(path: path, userModel: UserPhraseModel(fileURL: Storage.url("learned.json")))
    }()

    // 狀態
    private var mode: InputMode = .zhuyin
    private var layer: KeyboardLayer = .letters
    private var shift: ShiftState = .off
    private var lastShiftTap = Date.distantPast
    private var lastSpaceTime = Date.distantPast
    private var composer = ZhuyinComposer()
    private var candidates: [Candidate] = []
    private var suggestions: [String] = []
    private var suggestionSource: SuggestionSource = .association
    private var panel: PanelKind?
    private var editorText: String?          // 正在新增表符
    private var lastReturnTitle = ""
    private var backgroundImage: UIImage?
    private var clipboardTimer: Timer?
    private var heightConstraint: NSLayoutConstraint?
    private var toastToken = 0
    private lazy var haptic = UIImpactFeedbackGenerator(style: .light)

    // 畫面
    private let gradientLayer = CAGradientLayer()
    private let backgroundImageView = UIImageView()
    private let tintOverlay = UIView()
    private var topBar: TopBarView!
    private let contentView = UIView()
    private var grid: KeyboardGridView!
    private var panelView: UIView?
    private var candidatePanel: CandidatePanelView?
    private var handwritingView: HandwritingView?
    private let toastLabel = UILabel()

    private var style: KeyboardStyle {
        KeyboardStyle(theme: KeyboardTheme.named(settings.themeID), shape: settings.keyShape, keyAlpha: settings.keyAlpha)
    }

    // MARK: - 生命週期

    override func viewDidLoad() {
        super.viewDidLoad()
        let modes = settings.enabledModes
        mode = modes.contains(settings.lastMode) ? settings.lastMode : modes[0]

        view.layer.insertSublayer(gradientLayer, at: 0)
        backgroundImageView.contentMode = .scaleAspectFill
        backgroundImageView.clipsToBounds = true
        view.addSubview(backgroundImageView)
        view.addSubview(tintOverlay)

        topBar = TopBarView(style: style)
        view.addSubview(topBar)
        view.addSubview(contentView)
        grid = KeyboardGridView(style: style)
        contentView.addSubview(grid)

        toastLabel.font = .rounded(14, .semibold)
        toastLabel.textAlignment = .center
        toastLabel.layer.cornerRadius = 14
        toastLabel.clipsToBounds = true
        toastLabel.alpha = 0
        toastLabel.numberOfLines = 2
        view.addSubview(toastLabel)

        wireCallbacks()
        loadBackground()
        applyStyle()
        rebuildKeys()
        refreshTopBar()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        captureClipboard()
        clipboardTimer?.invalidate()
        clipboardTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            self?.captureClipboard()
        }
        rebuildKeys()
        updateAutoShift()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        clipboardTimer?.invalidate()
        clipboardTimer = nil
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        let b = view.bounds
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        gradientLayer.frame = b
        CATransaction.commit()
        backgroundImageView.frame = b
        tintOverlay.frame = b
        topBar.frame = CGRect(x: 0, y: 0, width: b.width, height: Metrics.barHeight)
        contentView.frame = CGRect(x: 0, y: Metrics.barHeight, width: b.width, height: max(0, b.height - Metrics.barHeight))
        grid.frame = contentView.bounds
        panelView?.frame = contentView.bounds
        candidatePanel?.frame = contentView.bounds
        handwritingView?.frame = contentView.bounds
        let toastW = min(b.width - 40, 300)
        toastLabel.frame = CGRect(x: (b.width - toastW) / 2, y: Metrics.barHeight + 8, width: toastW, height: 44)
    }

    override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
        super.viewWillTransition(to: size, with: coordinator)
        coordinator.animate(alongsideTransition: nil) { [weak self] _ in self?.updateHeight() }
    }

    override func textDidChange(_ textInput: UITextInput?) {
        super.textDidChange(textInput)
        refreshReturnTitle()
        updateAutoShift()
    }

    // MARK: - 接線

    private func wireCallbacks() {
        grid.onKey = { [weak self] spec in self?.handle(spec) }
        grid.onGlobe = { [weak self] view, event in
            guard let event = event else { return }
            self?.handleInputModeList(from: view, with: event)
        }
        grid.onCursorMove = { [weak self] steps in
            guard let self = self, !self.composer.isComposing else { return }
            self.textDocumentProxy.adjustTextPosition(byCharacterOffset: steps)
        }
        topBar.onToolbar = { [weak self] item in self?.toolbarTapped(item) }
        topBar.onCandidate = { [weak self] i in self?.selectCandidate(at: i, learn: true) }
        topBar.onTrailing = { [weak self] trailing in
            guard let self = self else { return }
            switch trailing {
            case .expand:
                self.toggleCandidatePanel()
            case .close:
                self.clearSuggestions()
                if self.suggestionSource == .handwriting { self.handwritingView?.clear() }
            }
        }
        topBar.onEditor = { [weak self] save in
            if save { self?.saveEditor() } else { self?.cancelEditor() }
        }
        topBar.onEditorPanel = { [weak self] in self?.togglePanel(.kaomoji) }
    }

    // MARK: - 按鍵

    private func handle(_ spec: KeySpec) {
        feedback()
        switch spec.action {
        case .text(let s): typeText(s)
        case .zhuyin(let c): typeZhuyin(c)
        case .backspace: backspace()
        case .space: space()
        case .returnKey: returnKey()
        case .shift: toggleShift()
        case .globe: break
        case .nextMode: switchMode()
        case .layer(let l):
            layer = l
            rebuildKeys()
        case .panel(let p): openPanel(p)
        case .clearCanvas:
            handwritingView?.clear()
            clearSuggestions()
        }
    }

    private func typeText(_ s: String) {
        if composer.isComposing { commitAll() }
        clearSuggestions()
        output(s)
        if shift == .on {
            shift = .off
            rebuildKeys()
        }
    }

    private func typeZhuyin(_ c: Character) {
        clearSuggestions(refresh: false)
        guard composer.input(c) else {
            output(String(c))
            refreshTopBar()
            return
        }
        if composer.syllables.count >= 20 {
            commitAll()
            return
        }
        updateCandidates()
    }

    private func backspace() {
        if composer.isComposing {
            composer.backspace()
            updateCandidates()
            return
        }
        clearSuggestions()
        deleteOutput()
        updateAutoShift()
    }

    private func space() {
        if mode == .zhuyin && layer == .letters && composer.hasPendingSyllable {
            if composer.confirmSyllable() { updateCandidates() }
            return
        }
        if composer.isComposing {
            if candidates.isEmpty { commitAll() } else { selectCandidate(at: 0, learn: false) }
            return
        }
        if suggestionSource == .handwriting && !suggestions.isEmpty {
            selectCandidate(at: 0, learn: false)
            return
        }
        clearSuggestions()
        // 英文：快速按兩下空白變成句號
        if mode == .english, editorText == nil, Date().timeIntervalSince(lastSpaceTime) < 0.4,
           let before = textDocumentProxy.documentContextBeforeInput, before.hasSuffix(" "),
           let prev = before.dropLast().last, prev.isLetter || prev.isNumber {
            deleteOutput()
            output(". ")
            lastSpaceTime = .distantPast
            updateAutoShift()
            return
        }
        lastSpaceTime = Date()
        output(" ")
        updateAutoShift()
    }

    private func returnKey() {
        if composer.isComposing {
            commitAll()
            return
        }
        if editorText != nil {
            saveEditor()
            return
        }
        clearSuggestions()
        output("\n")
        updateAutoShift()
    }

    private func toggleShift() {
        let now = Date()
        let quick = now.timeIntervalSince(lastShiftTap) < 0.35
        switch shift {
        case .off: shift = .on
        case .on: shift = quick ? .locked : .off
        case .locked: shift = .off
        }
        lastShiftTap = now
        rebuildKeys()
    }

    private func updateAutoShift() {
        guard mode == .english, layer == .letters, shift != .locked, editorText == nil else { return }
        let type = textDocumentProxy.autocapitalizationType ?? .sentences
        let before = textDocumentProxy.documentContextBeforeInput ?? ""
        var want = false
        switch type {
        case .allCharacters:
            want = true
        case .words:
            want = before.isEmpty || before.hasSuffix(" ") || before.hasSuffix("\n")
        case .sentences:
            let trimmed = before.trimmingCharacters(in: .whitespaces)
            want = before.isEmpty || before.hasSuffix("\n")
                || (before.hasSuffix(" ") && (trimmed.last.map { ".!?".contains($0) } ?? false))
        default:
            want = false
        }
        let newShift: ShiftState = want ? .on : .off
        if newShift != shift {
            shift = newShift
            rebuildKeys()
        }
    }

    private func switchMode() {
        commitAll()
        clearSuggestions()
        let modes = settings.enabledModes
        if let i = modes.firstIndex(of: mode) {
            mode = modes[(i + 1) % modes.count]
        } else {
            mode = modes[0]
        }
        settings.lastMode = mode
        layer = .letters
        shift = .off
        rebuildKeys()
        updateAutoShift()
    }

    private var nextModeTitle: String {
        let modes = settings.enabledModes
        guard modes.count > 1 else { return mode.shortTitle }
        let i = modes.firstIndex(of: mode) ?? 0
        return modes[(i + 1) % modes.count].shortTitle
    }

    // MARK: - 輸出（新增表符時打到編輯框，平常打到 App）

    private func output(_ s: String) {
        guard !s.isEmpty else { return }
        if editorText != nil {
            editorText! += s
            refreshTopBar()
            return
        }
        textDocumentProxy.insertText(s)
    }

    private func deleteOutput() {
        if var t = editorText {
            if !t.isEmpty { t.removeLast() }
            editorText = t
            refreshTopBar()
            return
        }
        textDocumentProxy.deleteBackward()
    }

    // MARK: - 注音選字

    private func updateCandidates() {
        if composer.syllables.isEmpty {
            candidates = []
        } else if let engine = engine {
            candidates = engine.candidates(for: composer.readings)
        } else {
            candidates = [Candidate(text: composer.readings.joined(), span: composer.syllables.count, kind: .literal)]
        }
        if !composer.isComposing { closeCandidatePanel() }
        candidatePanel?.setCandidates(candidates.map { $0.text })
        refreshTopBar()
        refreshReturnTitle()
    }

    private var compositionText: String {
        let head = composer.syllables.isEmpty ? "" : (candidates.first?.text ?? composer.readings.joined())
        return head + composer.current.reading
    }

    private func selectCandidate(at i: Int, learn: Bool) {
        if composer.isComposing {
            guard i < candidates.count else { return }
            let c = candidates[i]
            if learn && c.kind != .literal {
                engine?.learn(readings: Array(composer.readings.prefix(c.span)), text: c.text)
            }
            output(c.text)
            composer.removeFirst(c.span)
            if composer.isComposing {
                updateCandidates()
            } else {
                candidates = []
                closeCandidatePanel()
                showAssociations(after: c.text)
                refreshReturnTitle()
            }
        } else if i < suggestions.count {
            let s = suggestions[i]
            output(s)
            if suggestionSource == .handwriting { handwritingView?.clear() }
            showAssociations(after: s)
        }
    }

    private func commitAll() {
        guard composer.isComposing else { return }
        var text = ""
        if !composer.syllables.isEmpty {
            text = engine?.bestSegments(composer.readings).map { $0.text }.joined() ?? composer.readings.joined()
        }
        text += composer.current.reading
        composer.clear()
        candidates = []
        closeCandidatePanel()
        output(text)
        refreshTopBar()
        refreshReturnTitle()
    }

    private func showAssociations(after text: String) {
        if settings.associations, mode != .english, let engine = engine {
            suggestions = engine.associations(after: text)
        } else {
            suggestions = []
        }
        suggestionSource = .association
        refreshTopBar()
    }

    private func clearSuggestions(refresh: Bool = true) {
        guard !suggestions.isEmpty else { return }
        suggestions = []
        if refresh { refreshTopBar() }
    }

    private func refreshTopBar() {
        guard topBar != nil else { return }
        if let t = editorText {
            topBar.show(.editor(text: t))
        } else if composer.isComposing {
            topBar.show(.candidates(composition: compositionText, items: candidates.map { $0.text }, trailing: .expand))
        } else if !suggestions.isEmpty {
            topBar.show(.candidates(composition: "", items: suggestions, trailing: .close))
        } else {
            topBar.show(.toolbar)
        }
    }

    private func toggleCandidatePanel() {
        if candidatePanel != nil {
            closeCandidatePanel()
            return
        }
        let p = CandidatePanelView(style: style)
        p.onSelect = { [weak self] i in self?.selectCandidate(at: i, learn: true) }
        p.onBack = { [weak self] in self?.closeCandidatePanel() }
        p.onBackspace = { [weak self] in self?.backspace() }
        p.setCandidates(candidates.map { $0.text })
        p.frame = contentView.bounds
        contentView.addSubview(p)
        candidatePanel = p
    }

    private func closeCandidatePanel() {
        candidatePanel?.removeFromSuperview()
        candidatePanel = nil
    }

    // MARK: - 工具列與面板

    private func toolbarTapped(_ item: ToolbarItem) {
        feedback()
        switch item {
        case .clipboard: togglePanel(.clipboard)
        case .emoji: togglePanel(.emoji)
        case .kaomoji: togglePanel(.kaomoji)
        case .symbols: togglePanel(.symbols)
        case .settings: togglePanel(.settings)
        case .handwriting:
            closePanel()
            if mode == .handwriting {
                switchMode()
            } else {
                if !settings.enabledModes.contains(.handwriting) {
                    settings.enabledModes = settings.enabledModes + [.handwriting]
                }
                commitAll()
                mode = .handwriting
                settings.lastMode = mode
                layer = .letters
                rebuildKeys()
            }
        case .dismiss:
            commitAll()
            dismissKeyboard()
        }
    }

    private func togglePanel(_ kind: PanelKind) {
        if panel == kind { closePanel() } else { openPanel(kind) }
    }

    private func openPanel(_ kind: PanelKind) {
        commitAll()
        closePanel()
        panel = kind
        let v: UIView
        switch kind {
        case .emoji, .kaomoji, .symbols:
            let panelKind: EmojiPanelView.Kind = kind == .emoji ? .emoji : (kind == .kaomoji ? .kaomoji : .symbols)
            let p = EmojiPanelView(kind: panelKind, store: emoStore, style: style)
            p.onInsert = { [weak self] s in
                self?.feedback()
                self?.output(s)
            }
            p.onCopyImage = { [weak self] img in self?.copySticker(img) }
            p.onAdd = { [weak self] option in self?.addEmoticon(option) }
            p.onBack = { [weak self] in self?.closePanel() }
            p.onBackspace = { [weak self] in self?.deleteOutput() }
            v = p
        case .clipboard:
            captureClipboard()
            let p = ClipboardPanelView(store: clipStore, hasFullAccess: hasFullAccess, style: style)
            p.onInsert = { [weak self] s in
                self?.feedback()
                self?.output(s)
            }
            p.onAddCurrent = { [weak self] in self?.addCurrentClipboard() }
            p.onBack = { [weak self] in self?.closePanel() }
            p.onBackspace = { [weak self] in self?.deleteOutput() }
            v = p
        case .settings:
            let p = SettingsPanelView(settings: settings, hasFullAccess: hasFullAccess, style: style)
            p.onChange = { [weak self] in self?.settingsChanged() }
            p.onPasteBackground = { [weak self] in self?.pasteBackground() }
            p.onClearBackground = { [weak self] in self?.clearBackground() }
            p.onResetLearning = { [weak self] in
                self?.engine?.resetLearning()
                self?.toast("已清除選字學習紀錄")
            }
            p.onBack = { [weak self] in self?.closePanel() }
            v = p
        }
        v.frame = contentView.bounds
        contentView.addSubview(v)
        panelView = v
        grid.isHidden = true
        handwritingView?.isHidden = true
    }

    private func closePanel() {
        panelView?.removeFromSuperview()
        panelView = nil
        panel = nil
        let showHandwriting = mode == .handwriting && layer == .letters
        grid.isHidden = showHandwriting
        handwritingView?.isHidden = !showHandwriting
    }

    private func settingsChanged() {
        clipStore.limit = settings.clipLimit
        let modes = settings.enabledModes
        if !modes.contains(mode) {
            mode = modes[0]
            settings.lastMode = mode
            layer = .letters
        }
        applyStyle()
        rebuildKeys()
    }

    // MARK: - 剪貼簿

    private func captureClipboard() {
        guard hasFullAccess else { return }
        let pb = UIPasteboard.general
        guard pb.changeCount != settings.lastPasteboardChange else { return }
        settings.lastPasteboardChange = pb.changeCount
        guard pb.hasStrings, let s = pb.string else { return }
        if clipStore.add(s) {
            (panelView as? ClipboardPanelView)?.reload()
        }
    }

    private func addCurrentClipboard() {
        guard hasFullAccess else {
            toast("要先開啟「允許完整取用」")
            return
        }
        guard let s = UIPasteboard.general.string, clipStore.add(s) else {
            toast("剪貼簿裡沒有文字")
            return
        }
        (panelView as? ClipboardPanelView)?.reload()
    }

    // MARK: - 我的表符、貼圖

    private func addEmoticon(_ option: EmojiPanelView.AddOption) {
        switch option {
        case .type:
            let returnTo = panel
            closePanel()
            if mode == .handwriting {
                mode = settings.enabledModes.first { $0 != .handwriting } ?? .zhuyin
            }
            layer = .letters
            editorReturnPanel = returnTo
            editorText = ""
            rebuildKeys()
            refreshTopBar()
            toast("用鍵盤打字，或按 (•ᴗ•) 挑顏文字零件")
        case .clipboardText:
            guard hasFullAccess else {
                toast("要先開啟「允許完整取用」")
                return
            }
            guard let s = UIPasteboard.general.string, !s.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
                toast("剪貼簿裡沒有文字")
                return
            }
            emoStore.addCustom(s)
            (panelView as? EmojiPanelView)?.reload()
            toast("已加入我的表符")
        case .clipboardImage:
            guard hasFullAccess else {
                toast("要先開啟「允許完整取用」")
                return
            }
            guard let img = UIPasteboard.general.image, let data = img.resized(maxSide: 512).pngData() else {
                toast("剪貼簿裡沒有圖片，先到相簿「拷貝」一張")
                return
            }
            emoStore.addSticker(pngData: data)
            (panelView as? EmojiPanelView)?.reload()
            toast("已做成貼圖，點一下就能複製")
        }
    }

    private var editorReturnPanel: PanelKind?

    private func saveEditor() {
        commitAll()
        if let t = editorText, !t.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            emoStore.addCustom(t)
            toast("已加入我的表符")
        }
        editorText = nil
        refreshTopBar()
        rebuildKeys()
        if let p = editorReturnPanel { openPanel(p) }
        editorReturnPanel = nil
    }

    private func cancelEditor() {
        composer.clear()
        candidates = []
        editorText = nil
        editorReturnPanel = nil
        refreshTopBar()
        rebuildKeys()
    }

    private func copySticker(_ image: UIImage) {
        guard hasFullAccess else {
            toast("要先開啟「允許完整取用」")
            return
        }
        UIPasteboard.general.image = image
        settings.lastPasteboardChange = UIPasteboard.general.changeCount
        toast("已複製貼圖！在輸入框長按「貼上」")
    }

    // MARK: - 主題與背景

    private func loadBackground() {
        backgroundImage = settings.hasBackground ? UIImage(contentsOfFile: Storage.url("background.jpg").path) : nil
    }

    private func pasteBackground() {
        guard hasFullAccess else {
            toast("要先開啟「允許完整取用」")
            return
        }
        guard let img = UIPasteboard.general.image else {
            toast("剪貼簿裡沒有圖片，先到相簿「拷貝」一張")
            return
        }
        guard let data = img.resized(maxSide: 1000).jpegData(compressionQuality: 0.85) else { return }
        do {
            try data.write(to: Storage.url("background.jpg"), options: .atomic)
            settings.hasBackground = true
            loadBackground()
            applyStyle()
            toast("換好背景了")
        } catch {
            toast("存檔失敗")
        }
    }

    private func clearBackground() {
        try? FileManager.default.removeItem(at: Storage.url("background.jpg"))
        settings.hasBackground = false
        backgroundImage = nil
        applyStyle()
    }

    private func applyStyle() {
        let s = style
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        gradientLayer.colors = [s.theme.backgroundTop.cgColor, s.theme.backgroundBottom.cgColor]
        CATransaction.commit()
        if let img = backgroundImage {
            backgroundImageView.image = img
            backgroundImageView.isHidden = false
            tintOverlay.isHidden = false
            tintOverlay.backgroundColor = s.theme.backgroundTop.withAlphaComponent(settings.backgroundFade)
        } else {
            backgroundImageView.isHidden = true
            tintOverlay.isHidden = true
        }
        topBar.style = s
        grid.style = s
        handwritingView?.style = s
        candidatePanel?.style = s
        if let p = panelView as? EmojiPanelView { p.style = s }
        if let p = panelView as? ClipboardPanelView { p.style = s }
        if let p = panelView as? SettingsPanelView { p.style = s }
        toastLabel.backgroundColor = s.theme.accent.withAlphaComponent(0.95)
        toastLabel.textColor = s.theme.accentText
    }

    // MARK: - 版面

    private var returnTitle: String {
        if composer.isComposing { return "確認" }
        if editorText != nil { return "儲存" }
        switch textDocumentProxy.returnKeyType ?? .default {
        case .go: return "前往"
        case .google, .yahoo, .search: return "搜尋"
        case .join: return "加入"
        case .next: return "下一項"
        case .route: return "路線"
        case .send: return "傳送"
        case .done: return "完成"
        case .continue: return "繼續"
        default: return "換行"
        }
    }

    private var spaceTitle: String {
        let deco = style.theme.decoration
        return deco.isEmpty ? mode.title : "\(deco) \(mode.title)"
    }

    private func refreshReturnTitle() {
        if returnTitle != lastReturnTitle { rebuildKeys() }
    }

    private func rebuildKeys() {
        guard grid != nil else { return }
        lastReturnTitle = returnTitle
        let context = KeyLayouts.Context(mode: mode, layer: layer, shift: shift, needsGlobe: needsInputModeSwitchKey,
                                         returnTitle: returnTitle, spaceTitle: spaceTitle, nextModeTitle: nextModeTitle)
        grid.setLayout(KeyLayouts.layout(context))

        let showHandwriting = mode == .handwriting && layer == .letters
        if showHandwriting {
            let hw: HandwritingView
            if let existing = handwritingView {
                hw = existing
            } else {
                hw = HandwritingView(style: style)
                hw.sideGrid.onKey = { [weak self] spec in self?.handle(spec) }
                hw.onResult = { [weak self] results in
                    guard let self = self else { return }
                    self.suggestions = results
                    self.suggestionSource = .handwriting
                    self.refreshTopBar()
                }
                hw.onAutoCommit = { [weak self] in
                    guard let self = self, self.suggestionSource == .handwriting, let first = self.suggestions.first else { return }
                    self.output(first)
                    self.suggestions = []
                    self.refreshTopBar()
                }
                hw.frame = contentView.bounds
                contentView.addSubview(hw)
                handwritingView = hw
            }
            hw.sideGrid.setLayout(KeyLayouts.handwritingSide(returnTitle: returnTitle, nextModeTitle: nextModeTitle))
        } else if let hw = handwritingView {
            hw.removeFromSuperview()
            handwritingView = nil
            if suggestionSource == .handwriting { clearSuggestions() }
        }
        if panelView == nil {
            grid.isHidden = showHandwriting
        } else {
            handwritingView?.isHidden = true
        }
        if let p = candidatePanel { contentView.bringSubviewToFront(p) }
        updateHeight()
    }

    private var isLandscape: Bool {
        let screen = UIScreen.main.bounds
        return screen.width > screen.height
    }

    private func updateHeight() {
        let fiveRows = mode == .zhuyin && layer == .letters
        let content: CGFloat
        if isLandscape {
            content = fiveRows ? 196 : 164
        } else {
            content = fiveRows ? 252 : 216
        }
        let h = Metrics.barHeight + content
        if let c = heightConstraint {
            if c.constant != h { c.constant = h }
        } else {
            let c = view.heightAnchor.constraint(equalToConstant: h)
            c.priority = UILayoutPriority(999)
            c.isActive = true
            heightConstraint = c
        }
    }

    // MARK: - 回饋

    private func feedback() {
        guard hasFullAccess else { return }
        if settings.keySound { AudioServicesPlaySystemSound(1104) }
        if settings.haptics { haptic.impactOccurred() }
    }

    private func toast(_ text: String) {
        toastToken += 1
        let token = toastToken
        toastLabel.text = text
        view.bringSubviewToFront(toastLabel)
        UIView.animate(withDuration: 0.15) { self.toastLabel.alpha = 1 }
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.8) { [weak self] in
            guard let self = self, self.toastToken == token else { return }
            UIView.animate(withDuration: 0.3) { self.toastLabel.alpha = 0 }
        }
    }
}
