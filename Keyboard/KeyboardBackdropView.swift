import UIKit

/// 只柔化自訂背景的邊界，讓 iOS 26 鍵盤容器的底色透出。
/// 按鍵、文字與觸控區域放在此視圖外，避免一起被遮罩淡化。
final class KeyboardBackdropView: UIView {
    var softensEdges = true {
        didSet { if softensEdges != oldValue { setNeedsLayout() } }
    }
    private let edgeMask = CAGradientLayer()
    private let fadesBottom: Bool

    init(fadesBottom: Bool = true) {
        self.fadesBottom = fadesBottom
        super.init(frame: .zero)
        isUserInteractionEnabled = false
        isOpaque = false
        clipsToBounds = true
        if #available(iOS 26.0, *) {
            layer.mask = edgeMask
        }
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    override func layoutSubviews() {
        super.layoutSubviews()
        guard #available(iOS 26.0, *) else { return }
        layer.mask = softensEdges ? edgeMask : nil
        guard softensEdges, bounds.height > 0 else { return }
        // 使用點數而非固定比例：調整鍵盤高度時，過渡帶不會跟著變厚。
        let top = min(24, bounds.height / 3) / bounds.height
        let bottom = min(20, bounds.height / 3) / bounds.height
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        edgeMask.frame = bounds
        edgeMask.startPoint = CGPoint(x: 0.5, y: 0)
        edgeMask.endPoint = CGPoint(x: 0.5, y: 1)
        edgeMask.colors = [UIColor.clear.cgColor, UIColor.black.cgColor,
                           UIColor.black.cgColor, (fadesBottom ? UIColor.clear : UIColor.black).cgColor]
        edgeMask.locations = [0, NSNumber(value: Double(top)),
                              NSNumber(value: Double(fadesBottom ? 1 - bottom : 1)), 1]
        CATransaction.commit()
    }
}
