import Foundation

struct SymbolCategory {
    let title: String
    let items: [String]
}

/// 表符、顏文字、特殊符號的內建清單
enum SymbolData {
    private static func words(_ s: String) -> [String] {
        s.split(whereSeparator: { $0 == " " || $0 == "\n" }).map(String.init)
    }

    private static func lines(_ s: String) -> [String] {
        s.split(separator: "\n").map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
    }

    // MARK: 表情符號

    static let emoji: [SymbolCategory] = [
        SymbolCategory(title: "😀", items: words("""
        😀 😃 😄 😁 😆 😅 🤣 😂 🙂 🙃 😉 😊 😇 🥰 😍 🤩 😘 😗 😚 😙 😋 😛 😜 🤪 😝 🤑 🤗 🤭 🤫 🤔
        🤐 🤨 😐 😑 😶 😏 😒 🙄 😬 😌 😔 😪 🤤 😴 😷 🤒 🤕 🤢 🤮 🥵 🥶 🥴 😵 🤯 🤠 🥳 😎 🤓 🧐 😕
        😟 🙁 😮 😯 😲 😳 🥺 😦 😧 😨 😰 😥 😢 😭 😱 😖 😣 😞 😓 😩 😫 🥱 😤 😡 😠 🤬 😈 👿 💀 💩
        🤡 👻 👽 🤖 😺 😸 😹 😻 😼 😽 🙀 😿 😾 🙈 🙉 🙊
        """)),
        SymbolCategory(title: "👋", items: words("""
        👋 🤚 🖐️ ✋ 🖖 👌 🤌 🤏 ✌️ 🤞 🤟 🤘 🤙 👈 👉 👆 👇 ☝️ 👍 👎 ✊ 👊 🤛 🤜 👏 🙌 👐 🤲 🤝 🙏
        💪 👀 👅 👄 💋 🧠 👶 🧒 👦 👧 🧑 👨 👩 🧓 👴 👵 🙇 💁 🙅 🙆 🙋 🤦 🤷 💃 🕺 👯 🧘 🛀 🛌 👫
        👭 👬 💏 💑 👪
        """)),
        SymbolCategory(title: "🐶", items: words("""
        🐶 🐱 🐭 🐹 🐰 🦊 🐻 🐼 🐨 🐯 🦁 🐮 🐷 🐸 🐵 🐔 🐧 🐦 🐤 🦆 🦅 🦉 🦇 🐺 🐗 🐴 🦄 🐝 🐛 🦋
        🐌 🐞 🐢 🐍 🦖 🐙 🦑 🦐 🦀 🐡 🐠 🐟 🐬 🐳 🐋 🦈 🐊 🐅 🐆 🦓 🦍 🐘 🦛 🦏 🐪 🦒 🦘 🐄 🐎 🐖
        🐑 🦙 🐐 🦌 🐕 🐩 🐈 🐓 🦃 🦚 🦜 🦢 🕊️ 🐇 🦝 🦦 🦥 🐁 🐿️ 🦔 🐾 🌸 🌷 🌹 🌺 🌻 🌼 💐 🍀 🌿
        🌱 🌵 🌴 🍁 🍂 🍄
        """)),
        SymbolCategory(title: "🍓", items: words("""
        🍏 🍎 🍐 🍊 🍋 🍌 🍉 🍇 🍓 🫐 🍈 🍒 🍑 🥭 🍍 🥥 🥝 🍅 🍆 🥑 🥦 🥬 🥒 🌶️ 🌽 🥕 🧄 🧅 🥔 🍠
        🥐 🍞 🥖 🥨 🧀 🥚 🍳 🧈 🥞 🧇 🥓 🥩 🍗 🍖 🌭 🍔 🍟 🍕 🥪 🌮 🌯 🥗 🍝 🍜 🍲 🍛 🍣 🍱 🥟 🍤
        🍙 🍚 🍘 🍥 🥮 🍢 🍡 🍧 🍨 🍦 🥧 🧁 🍰 🎂 🍮 🍭 🍬 🍫 🍿 🍩 🍪 🥛 🍼 ☕ 🍵 🧋 🧃 🥤 🍶 🍺
        🍻 🥂 🍷 🍸 🍹
        """)),
        SymbolCategory(title: "🎀", items: words("""
        ⚽ 🏀 🏈 ⚾ 🎾 🏐 🏓 🏸 🥊 🎯 🎳 🎮 🕹️ 🎲 🧩 🧸 🎨 🎬 🎤 🎧 🎹 🥁 🎷 🎺 🎸 🎻 🎁 🎀 🎈 🎉
        🎊 🎄 🎃 🧧 🏆 🥇 🥈 🥉 🏅 📱 💻 ⌚ 📷 📚 ✏️ 📝 📌 📎 ✂️ 🔑 💡 🕯️ 💰 💎 🛒 🧹 🛁 👗 👚 👕
        👖 👟 👠 👑 🎩 🧢 👓 🕶️ 💍 👜 🎒 ☂️
        """)),
        SymbolCategory(title: "🚗", items: words("""
        🚗 🚕 🚙 🚌 🚎 🏎️ 🚓 🚑 🚒 🚐 🚚 🚲 🛵 🏍️ 🚨 🚃 🚄 🚅 🚆 🚇 🚉 ✈️ 🛫 🛬 🚀 🛸 🚁 ⛵ 🚤 🚢
        ⚓ 🗼 🏰 🏯 🎡 🎢 🎠 ⛲ 🏖️ 🏝️ 🏔️ ⛰️ 🌋 🗻 🏕️ 🏠 🏡 🏢 🏫 🏥 🏪 ⛩️ 🌃 🌆 🌇 🌉 🌌 🎆 🎇 🌠
        🗺️ 🧭
        """)),
        SymbolCategory(title: "❤️", items: words("""
        ❤️ 🧡 💛 💚 💙 💜 🤎 🖤 🤍 💔 ❣️ 💕 💞 💓 💗 💖 💘 💝 💟 ♥️ 💯 💢 💥 💫 💦 💨 💬 💭 💤 ✨
        ⭐ 🌟 ⚡ 🔥 🌈 ☀️ 🌙 ☁️ ❄️ 🎵 🎶 ✅ ❌ ⭕ ❗ ❓ ‼️ ⁉️ 🆗 🆕 🆒 🔴 🟠 🟡 🟢 🔵 🟣
        """)),
    ]

    // MARK: 日式顏文字

    static let kaomoji: [SymbolCategory] = [
        SymbolCategory(title: "開心", items: lines(#"""
        (≧▽≦)
        (*´▽`*)
        ヽ(✿ﾟ▽ﾟ)ノ
        (๑˃ᴗ˂)ﻭ
        ٩(◕‿◕｡)۶
        (ﾉ◕ヮ◕)ﾉ*:･ﾟ✧
        (＾▽＾)
        (´∀`)
        o(〃＾▽＾〃)o
        ＼(^o^)／
        (*≧ω≦)
        (•‿•)
        (◍•ᴗ•◍)
        ヾ(＾∇＾)
        (灬ºωº灬)
        ╰(*°▽°*)╯
        """#)),
        SymbolCategory(title: "愛心", items: lines(#"""
        (♡˙︶˙♡)
        (´｡• ᵕ •｡`) ♡
        (づ￣ ³￣)づ
        (っ´▽`)っ♡
        ♡(ӦｖӦ｡)
        (*˘︶˘*).｡.:*♡
        (｡♥‿♥｡)
        (灬♥ω♥灬)
        (´ε｀ )♡
        (っ˘з(˘⌣˘ )
        ♡＾▽＾♡
        ( ˘ ³˘)♥
        (人 •͈ᴗ•͈)
        ʚ♡ɞ
        (⁄ ⁄•⁄ω⁄•⁄ ⁄)
        """#)),
        SymbolCategory(title: "難過", items: lines(#"""
        (´;ω;`)
        (╥﹏╥)
        (｡•́︿•̀｡)
        (ಥ_ಥ)
        (つД`)
        ｡ﾟ(ﾟ´Д｀ﾟ)ﾟ｡
        (ノД`)・゜・。
        (T▽T)
        (；´д｀)
        (◞‸◟)
        (´-ω-`)
        ( ´•̥̥̥ω•̥̥̥` )
        (∩︵∩)
        (ㄒoㄒ)
        """#)),
        SymbolCategory(title: "生氣", items: lines(#"""
        (╬ Ò﹏Ó)
        (｀へ´)
        ヽ(`Д´)ﾉ
        (╯°□°）╯︵ ┻━┻
        (#`皿´)
        (ㆆ_ㆆ)
        (¬_¬)
        (눈_눈)
        ┻━┻ ︵ヽ(`Д´)ﾉ︵ ┻━┻
        (╬ಠ益ಠ)
        (＃｀д´)ﾉ
        (ノಠ益ಠ)ノ彡┻━┻
        (◣_◢)
        (-_-メ)
        """#)),
        SymbolCategory(title: "驚訝", items: lines(#"""
        (⊙_⊙)
        Σ(ﾟДﾟ)
        (°ロ°) !
        (ﾟoﾟ;)
        w(ﾟДﾟ)w
        (O_O;)
        (⊙ˍ⊙)
        (；ﾟдﾟ)
        Σ(°△°|||)
        (・□・;)
        (゜-゜)
        ∑(O_O；)
        (ﾟ⊿ﾟ)
        """#)),
        SymbolCategory(title: "害羞", items: lines(#"""
        (〃▽〃)
        (*/ω＼*)
        (⁄ ⁄>⁄ ▽ ⁄<⁄ ⁄)
        (///▽///)
        (〃∀〃)
        (〃ω〃)
        (｡>﹏<｡)
        (*ﾉωﾉ)
        (/ω＼)
        (✿◡‿◡)
        (⸝⸝⸝°_°⸝⸝⸝)
        (´,,•ω•,,)
        """#)),
        SymbolCategory(title: "動物", items: lines(#"""
        ʕ•ᴥ•ʔ
        ʕ￫ᴥ￩ʔ
        (=^･ω･^=)
        (=①ω①=)
        ฅ^•ﻌ•^ฅ
        (=ↀωↀ=)
        U・ᴥ・U
        ∪･ω･∪
        (・(ｪ)・)
        (ᵔᴥᵔ)
        /ᐠ｡ꞈ｡ᐟ\
        (・θ・)
        (∵)
        >°)))彡
        くコ:彡
        ฅ(^ω^ฅ)
        """#)),
        SymbolCategory(title: "打招呼", items: lines(#"""
        (｡･ω･)ﾉﾞ
        ヾ(•ω•`)o
        ( ´ ▽ ` )ﾉ
        (*・ω・)ﾉ
        ヾ(^▽^*)))
        (・ω・)ノ
        (^_^)/
        o(^▽^)o
        (∠・ω< )⌒☆
        (｀・ω・´)ゞ
        ヽ(・∀・)ﾉ
        (￣▽￣)ノ
        m(_ _)m
        <(_ _)>
        """#)),
        SymbolCategory(title: "其他", items: lines(#"""
        ¯\_(ツ)_/¯
        (￣ー￣)
        ( ˘•ω•˘ )
        (´-ι_-｀)
        (ーー;)
        (￣▽￣*)ゞ
        (¬‿¬)
        (ง •̀_•́)ง
        ( •_•)>⌐■-■
        (⌐■_■)
        ᕕ( ᐛ )ᕗ
        (っ˘ڡ˘ς)
        (´～`)
        zzz(￣o￣)
        """#)),
    ]

    // MARK: 特殊符號

    static let symbols: [SymbolCategory] = [
        SymbolCategory(title: "常用", items: words("""
        ♡ ♥ ❤ ☆ ★ ✦ ✧ ✩ ✪ ✿ ❀ ❁ ✾ ♪ ♫ ♬ ☀ ☁ ☂ ☃ ❄ ☾ ☽ ✓ ✔ ✗ ✘ ※ ○ ●
        ◎ ◇ ◆ □ ■ △ ▲ ▽ ▼ ♢ ♤ ♧ ☺ ☻ ✌ ☞ ☜ ♨ ✉ ☎ ⌛
        """)),
        SymbolCategory(title: "括號", items: words("""
        「 」 『 』 （ ） 【 】 《 》 〈 〉 〔 〕 ［ ］ ｛ ｝ 〖 〗 〘 〙 〚 〛 ‹ › « » “ ” ‘ ’ 〝 〞
        ︵ ︶ ︷ ︸
        """)),
        SymbolCategory(title: "標點", items: words("""
        ， 。 、 ； ： ？ ！ … ‥ ‧ · ～ — ― ﹏ ＿ ﹍ ＂ ＇ ＆ ＠ ＃ ＊ ％ ＄ ＾ ｜ ＼ ／ ¡ ¿ § ¶ † ‡
        ° ′ ″
        """)),
        SymbolCategory(title: "箭頭", items: words("""
        ← → ↑ ↓ ↖ ↗ ↘ ↙ ↔ ↕ ⇐ ⇒ ⇑ ⇓ ⇔ ➔ ➜ ➤ ↩ ↪ ⤴ ⤵ ↺ ↻ ⟵ ⟶ ▶ ◀ ▸ ◂ ▹ ◃
        """)),
        SymbolCategory(title: "數學", items: words("""
        ＋ － × ÷ ＝ ≠ ≈ ≒ ≡ ＜ ＞ ≤ ≥ ± ∓ √ ∛ ∞ ∝ ∑ ∏ ∫ ∮ ∂ ∇ ∈ ∉ ⊂ ⊃ ⊆ ⊇ ∩ ∪ ∧ ∨ ¬ ∀ ∃ ∴ ∵
        ∠ ⊥ ∥ ‰ ½ ⅓ ¼ ¾ ² ³ ¹ ⁰ ⁿ π
        """)),
        SymbolCategory(title: "單位", items: words("""
        ℃ ℉ ㎜ ㎝ ㎞ ㎡ ㏄ ㎎ ㎏ ㏕ ㎖ ℓ ¥ ￥ $ ￠ ￡ € ₩ ₿ № ℡ ™ © ®
        """)),
        SymbolCategory(title: "數字", items: words("""
        ① ② ③ ④ ⑤ ⑥ ⑦ ⑧ ⑨ ⑩ ⑪ ⑫ ⑬ ⑭ ⑮ ⑯ ⑰ ⑱ ⑲ ⑳ ❶ ❷ ❸ ❹ ❺ ❻ ❼ ❽ ❾ ❿ ⑴ ⑵ ⑶ ⑷ ⑸ ⑹ ⑺ ⑻ ⑼ ⑽
        Ⅰ Ⅱ Ⅲ Ⅳ Ⅴ Ⅵ Ⅶ Ⅷ Ⅸ Ⅹ ㈠ ㈡ ㈢ ㈣ ㈤ ㈥ ㈦ ㈧ ㈨ ㈩
        """)),
        SymbolCategory(title: "希臘", items: words("""
        α β γ δ ε ζ η θ ι κ λ μ ν ξ ο π ρ σ τ υ φ χ ψ ω Α Β Γ Δ Ε Ζ Η Θ Ι Κ Λ Μ Ν Ξ Ο Π Ρ Σ Τ Υ
        Φ Χ Ψ Ω
        """)),
        SymbolCategory(title: "框線", items: words("""
        ─ │ ┌ ┐ └ ┘ ├ ┤ ┬ ┴ ┼ ═ ║ ╔ ╗ ╚ ╝ ╠ ╣ ╦ ╩ ╬ ╭ ╮ ╰ ╯ ▁ ▂ ▃ ▄ ▅ ▆ ▇ █ ▏ ▎ ▍ ▌ ▋ ▊ ▉ ░ ▒
        ▓
        """)),
        SymbolCategory(title: "日文", items: words("""
        あ い う え お か き く け こ さ し す せ そ た ち つ て と な に ぬ ね の は ひ ふ へ ほ ま み む め も
        や ゆ よ ら り る れ ろ わ を ん ゃ ゅ ょ っ ア イ ウ エ オ カ キ ク ケ コ サ シ ス セ ソ タ チ ツ テ ト
        ナ ニ ヌ ネ ノ ハ ヒ フ ヘ ホ マ ミ ム メ モ ヤ ユ ヨ ラ リ ル レ ロ ワ ヲ ン ー ッ
        """)),
        SymbolCategory(title: "注音", items: words("""
        ㄅ ㄆ ㄇ ㄈ ㄉ ㄊ ㄋ ㄌ ㄍ ㄎ ㄏ ㄐ ㄑ ㄒ ㄓ ㄔ ㄕ ㄖ ㄗ ㄘ ㄙ ㄧ ㄨ ㄩ ㄚ ㄛ ㄜ ㄝ ㄞ ㄟ ㄠ ㄡ ㄢ ㄣ
        ㄤ ㄥ ㄦ ˊ ˇ ˋ ˙
        """)),
    ]
}
