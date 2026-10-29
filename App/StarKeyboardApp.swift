import SwiftUI
import PhotosUI
import UIKit

@main
struct StarKeyboardApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

struct ContentView: View {
    @State private var testText = ""
    @State private var photoItem: PhotosPickerItem?
    @State private var message: String?

    private let pink = Color(red: 1.0, green: 0.5, blue: 0.65)

    var body: some View {
        NavigationStack {
            List {
                Section {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("⭐️ 星星鍵盤")
                            .font(.system(.largeTitle, design: .rounded).bold())
                        Text("注音、手寫、英文、表符、顏文字、剪貼簿，還有可愛的主題。")
                            .font(.system(.subheadline, design: .rounded))
                            .foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 6)
                }

                Section("第一次使用：打開鍵盤") {
                    step(1, "打開「設定」App › 一般 › 鍵盤 › 鍵盤 › 新增鍵盤…")
                    step(2, "在「第三方鍵盤」選「星星鍵盤」")
                    step(3, "點「星星鍵盤」，打開「允許完整取用」（剪貼簿、聲音、震動、貼圖、背景圖都需要）")
                    step(4, "打字時長按 🌐 地球鍵，切換到星星鍵盤")
                    Button("打開本 App 的設定頁") {
                        if let url = URL(string: UIApplication.openSettingsURLString) {
                            UIApplication.shared.open(url)
                        }
                    }
                }

                Section("試打看看") {
                    TextField("在這裡試試新鍵盤", text: $testText, axis: .vertical)
                        .lineLimit(3...8)
                }

                Section {
                    PhotosPicker(selection: $photoItem, matching: .images) {
                        Label("挑一張照片", systemImage: "photo.on.rectangle.angled")
                    }
                    Text("照片會被複製到剪貼簿。接著打開鍵盤：\n• 當背景：⚙️ 設定 › 背景圖片 › 從剪貼簿貼上圖片\n• 做成貼圖：☺ 表符 › ⭐ 我的 › ＋ › 把剪貼簿的圖片做成貼圖")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                } header: {
                    Text("背景圖片與自製貼圖")
                }

                Section("使用小提醒") {
                    tip("剪貼簿", "iOS 不讓鍵盤在背景偷看剪貼簿，所以是「打開鍵盤的時候」才把剛複製的東西記下來。複製完切到要打字的地方就會記到。")
                    tip("注音", "大千排列。空白鍵＝一聲；打完按空白直接送出整句，或點上面的候選字一段一段選。點過的字下次會排前面。")
                    tip("手寫", "寫完停一下就會辨識，點候選字送出；直接寫下一個字會自動送出第一個候選字。")
                    tip("韓文、日文", "到鍵盤 ⚙️ 設定 › 輸入法 打開。韓文照兩排式自動拼字；日文打羅馬拼音（sakura）變成假名，可選平假名或片假名。")
                    tip("翻譯", "工具列的「譯」：先打中文，按「→英文」換語言，再按「翻譯」，點結果就送出。")
                    tip("自製表符", "在 ⭐ 我的 按 ＋ 就能新增；長按可以刪除。")
                    tip("空白鍵", "按住空白鍵左右滑，可以移動游標。")
                    tip("免費 Apple ID", "用免費帳號安裝的話，每 7 天要用 Sideloadly 重新安裝一次（資料不會不見）。")
                }
            }
            .navigationTitle("星星鍵盤")
            .navigationBarTitleDisplayMode(.inline)
            .tint(pink)
            .onChange(of: photoItem) { item in
                guard let item = item else { return }
                Task { await copyPhoto(item) }
            }
            .alert(message ?? "", isPresented: Binding(get: { message != nil }, set: { if !$0 { message = nil } })) {
                Button("好") {}
            }
        }
    }

    private func step(_ n: Int, _ text: String) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Text("\(n)")
                .font(.system(.callout, design: .rounded).bold())
                .foregroundStyle(.white)
                .frame(width: 24, height: 24)
                .background(Circle().fill(pink))
            Text(text)
                .font(.system(.callout, design: .rounded))
        }
    }

    private func tip(_ title: String, _ text: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title).font(.system(.callout, design: .rounded).bold())
            Text(text).font(.footnote).foregroundStyle(.secondary)
        }
        .padding(.vertical, 2)
    }

    @MainActor
    private func copyPhoto(_ item: PhotosPickerItem) async {
        guard let data = try? await item.loadTransferable(type: Data.self), let image = UIImage(data: data) else {
            message = "讀不到這張照片"
            return
        }
        let longest = max(image.size.width, image.size.height)
        let scale = min(1, 1000 / longest)
        let size = CGSize(width: image.size.width * scale, height: image.size.height * scale)
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        let resized = UIGraphicsImageRenderer(size: size, format: format).image { _ in
            image.draw(in: CGRect(origin: .zero, size: size))
        }
        UIPasteboard.general.image = resized
        photoItem = nil
        message = "已經複製好了！現在打開鍵盤，到 ⚙️ 設定貼上當背景，或到 ⭐ 我的 做成貼圖。"
    }
}
