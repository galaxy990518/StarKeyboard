import SwiftUI
import PhotosUI

/// PHPicker supports older iPhones without requesting access to the entire library.
struct KeyboardPhotoPicker: UIViewControllerRepresentable {
    var onSelect: (UIImage?) -> Void

    func makeCoordinator() -> Coordinator { Coordinator(onSelect: onSelect) }

    func makeUIViewController(context: Context) -> PHPickerViewController {
        var configuration = PHPickerConfiguration()
        configuration.filter = .images
        configuration.selectionLimit = 1
        let picker = PHPickerViewController(configuration: configuration)
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ controller: PHPickerViewController, context: Context) {}

    final class Coordinator: NSObject, PHPickerViewControllerDelegate {
        private let onSelect: (UIImage?) -> Void
        init(onSelect: @escaping (UIImage?) -> Void) { self.onSelect = onSelect }

        func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
            guard let provider = results.first?.itemProvider,
                  provider.canLoadObject(ofClass: UIImage.self) else {
                onSelect(nil)
                return
            }
            provider.loadObject(ofClass: UIImage.self) { [onSelect] object, _ in
                DispatchQueue.main.async { onSelect(object as? UIImage) }
            }
        }
    }
}
