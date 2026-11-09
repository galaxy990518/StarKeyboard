# Linux 本機驗收記錄

日期：2026-11-09。**前一版平面向量外觀已退回；本記錄更新為水彩素材修正版的資料／版面檢查，外觀仍待使用者確認。**

範圍為向量資料、離線預覽、程式碼檢閱；未執行 UIKit 或 iPhone。這份記錄不表示美術已獲使用者確認。

機器輸出見 [acceptance.json](acceptance.json)，含執行時間、瀏覽器／字體、檢查數量與預覽來源的 SHA-256。若來源改動，需重新檢查再更新記錄。

## 本機結果

| 項目 | 結果與證據 |
|---|---|
| 自動資料測試 | 14 項 Node 測試通過；包含 192 組模式／頁面／地球鍵／注音欄數／Shift 配置 |
| 共用來源 | 兩個產生器的 `--check` 通過；Swift 向量資料與 HTML 均符合 JSON／產生器輸出 |
| 狐狸輪廓 | 已檢視截圖及路徑：臉形、耳朵、下緣五官，沒有獨立身體；美術定稿仍待使用者確認 |
| 寬鍵比例 | 幾何測試通過：增加寬度只延展中段，五官、耳朵端部尺寸保持；已檢視空白鍵與換行鍵截圖 |
| 六種模式與四種頁面 | Chromium 通過 384 場景：4 種寬度 × 4 組高度／字體／間距／造型大小極值 × 6 模式 × 4 頁面 |
| 按鍵與字形分區 | 12,496 顆按鍵邊界、12,016 個文字／提示字邊界檢查通過；文字不超出分區、不碰五官區 |
| 按壓外觀 | 924 次檢查通過：完整透明度滑桿的 14 檔值、一般／按壓狀態、文字及圖示顏色；按壓透明度為設定值的 82% |
| 字色對比策略 | 五色、14 檔透明度、一般／按壓狀態共 140 組字色策略、上下背景共 280 個比較通過。底色對比不足時檢查相反明暗的細邊，不把細邊結果宣稱為文字對整個底色皆達 4.5:1 |
| 預覽互動 | 注音欄數、韓文 Shift、日文羅馬字、地球鍵、全半形、候選列示意、三種面板示意及點按回饋通過；未連接真正輸入引擎 |
| 手機寬度 | `index.html`、`theme.html` 各在 320／390／430 px 下沒有整頁水平溢出；寬版鍵盤可在預覽容器內捲動 |
| JavaScript | 本輪無未處理的瀏覽器例外 |

字色對比項目驗證的是配色模型及字邊策略，沒有逐像素認證水彩材質的全部明暗；紙感版本的實際可讀性仍需視覺與真機確認。

字形檢查使用本機 Chromium 的 `Chiron GoRound TC ExtraLight` 字體，不代表 iPhone 的系統字體或專案所有下載字體均已驗證。極端縮小設定下文字會明顯變小；邊界沒有裁切，閱讀舒適度仍待真機與使用者確認。

## 本輪發現與修正

0. 原先全向量美術未達參考圖目標，改為九款水彩鍵帽圖集、獨立寬鍵與秋日山景背景。三張素材使用內建 image_gen 製作；原生標題區新增 52 pt（橫向 34 pt）。
1. 預覽原本按壓固定為 65% 透明度，會讓低透明度的按鍵反而變深，字色也未同步。已改成套用設定透明度 × 0.82，並重新選擇按壓字色，對齊原生規則。
2. 原本只抽查少數透明度，漏掉奶油色在約 40%～45% 的對比缺口。已加入相反明暗的細邊，並為字邊保留排版空間。原生文字與 SF Symbol 的一般／按壓內容先準備好，按壓時取用；原生實際顯示仍需 Xcode 驗證。
3. 完整預覽增加「造型大小」滑桿，讓本機檢查涵蓋目前有效尺寸範圍。
4. 瀏覽器驗收由 `/tmp` 的一次性腳本整理成專案內可重跑的 `tools/check_fox_letter_browser.js`。

## 個人設定與照片的程式碼檢閱

| 路徑 | 檢閱結果 |
|---|---|
| `SettingsPanelView.foxLetterThemeRow` | 主題卡片只設定 `themeID`，再呼叫既有 `onChange` |
| `KeyboardViewController.settingsChanged` | 沿用設定更新流程，先重新讀取背景選擇 |
| `KeyboardViewController.style` → `FoxLetterTheme.resolved` | 改的是 `KeyboardStyle` 副本的有效造型／質感，沒有回寫個人設定 |
| `KeyboardViewController.loadBackground` | 小狐狸期間不載入照片，只釋放顯示快取；此分支沒有刪除照片或改寫照片設定 |
| `KeyboardViewController.startPhotoAdjust` | 小狐狸期間保留照片，提示切回一般主題後調整 |

以上只確認程式路徑；切回主題、關閉 extension、重新開啟後的保存結果尚未實機驗證。

## 截圖

- [水彩修正版：注音鍵盤](theme-keyboard-zhuyin-v2.png)、[英文鍵盤](theme-keyboard-english-v2.png)

- [單顆版型](preview.png)
- [注音](theme-zhuyin.png)、[英文](theme-english.png)、[手寫](theme-handwriting.png)
- [320 px、80% 高度、最大字體與間距、縮小造型](theme-compact.png)
- [40% 透明度與細字邊](theme-contrast.png)

## 重跑方式

```sh
node tools/tests/fox_letter_prototype.test.js
node tools/tests/fox_letter_theme.test.js
node tools/tests/fox_letter_watercolor.test.js
node tools/build_fox_letter_prototype.js --check
node tools/build_fox_letter_theme.js --check
node tools/check_fox_letter_browser.js --output docs/previews/fox-letter
git diff --check
```

瀏覽器腳本需要 Node 22+、Chromium 和可用的 CJK／注音字體，不需要 npm 套件。可用 `CHROME_BIN` 指定 Chromium、`FONTCONFIG_FILE` 指定本機字體設定；執行環境無法使用 Chromium sandbox 時可設 `FOX_CHROME_NO_SANDBOX=1`。腳本只開啟本機 HTML；預設把報告寫入 `/tmp/fox-letter-acceptance`，加上 `--output` 才指定其他目錄。

## 尚未通過的驗收

美術定稿、所有原生字體、Xcode 編譯與 13 項 UIKit 測試、實際輸入／長按／滑行／游標、候選字及面板功能、照片／設定保存、個人圖片／GIF、真機流暢度與記憶體仍待驗證。接續方式見 [原生驗證說明](../../../KeyboardTests/README.md)。
