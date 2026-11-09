# 小狐狸水彩素材：生圖記錄

使用內建 image_gen 工具產生，沒有使用 API／CLI fallback。使用者提供的「小狐狸的楓葉信」鍵盤圖為主要美術參考。

最終素材存於 `Keyboard/Resources/FoxLetter/`：

- `keycaps-watercolor-v2.png`：九款透明鍵帽圖集。
- `wide-keycaps-watercolor-v2.png`：空白鍵與換行鍵的獨立寬版構圖。
- `background-watercolor-v2.png`：秋日山景、郵筒、信封、楓葉與深紫紙感背景。

文字不在圖片中，仍由程式繪製。素材是本輪修正版，尚未取得使用者美術定稿確認。

## 鍵帽圖集

```text
Use case: illustration-story.
Asset type: production transparent sprite atlas for an iPhone illustrated keyboard theme, not a UI mockup.
Input image 1 is the user's REQUIRED visual/style reference. Reproduce its specific watercolor and colored-pencil-on-grainy-paper appearance, fox head silhouettes, tiny low-set faces, warm autumn color combinations, scarves, leaves, berets and envelopes. The previous flat vector approximation was rejected.
Create ONE precisely regular 3 by 3 sprite atlas, square 1536x1536 if possible, with TRANSPARENT background, no grid lines. Every cell is exactly the same size. All nine isolated fox HEAD keycaps have identical scale and placement, wholly inside their own cell with generous transparent gutters. No overlapping cells. No cast shadow outside the heads. Each fox head should occupy approximately 78% of cell width and 85% of cell height, centered. The two ears are part of the head silhouette; round gently irregular cheeks and a squat round chin. Cream muzzle covers the bottom fifth, tiny two dark dot eyes and tiny oval/triangular nose sitting close to the lower edge exactly like the reference. There is NO body, no neck, no legs, no arms.
Across the upper-middle 60% of EACH head leave a quiet uninterrupted fur area where live keyboard letters will be overlaid by code. Do NOT draw any letters, symbols, numbers, logos or text anywhere.
Nine designs in exact reading order:
row 1: burnt orange fox with burgundy wool beret on upper-left; cream/beige fox with a red maple leaf upper-right and blush cheeks; deep cocoa fox with green ochre checked scarf along bottom-left.
row 2: orange fox with a few little brown decorative stitches upper-right; maple red fox with small maple leaf upper-right; cream fox with olive beret upper-left and rosy cheeks.
row 3: orange fox with burgundy red tartan scarf at lower-right; cocoa fox with a small cream envelope and red heart seal upper-right; muted olive fox with orange maple leaf upper-right.
Each accessory is painted with tactile irregular edges and fine details, not simplistic geometric clipart. Show visible fine paper fibers, mottled pigment, dry brush and pencil grain within the fur, ears, muzzle and fabrics, like the attached reference. Soft warm brown pencil edges, subtle painted shading. These must look like illustrated paper fox keycaps from that exact autumn keyboard, not corporate vector icons or glossy 3D buttons. No rectangular cards or colored cell backgrounds. True alpha outside silhouettes.
```

## 透明邊緣修正

```text
Use case: precise-object-edit. Edit the supplied sprite atlas, preserve its nine watercolor fox-head designs, colors, soft paper grain, proportions, cell placement and exact regular 3x3 layout.
Fix ONLY the alpha/background cleanup: remove ALL bright red and yellow fringe, glow, scattered pixels and colored speckles outside the silhouettes. Outside the nine heads must be perfectly transparent. Each head must have a clean soft warm-brown antialiased edge with its watercolor texture preserved inside. Do not add any shadow or any background. No black, white, red or yellow matte. Keep the isolated silhouettes separated by transparent gutters. No text or symbols. Also make the two tiny dot eyes visible above each scarf, and leave the large upper-middle forehead blank for live keyboard labels. Everything else stays the same.
```

## 秋日背景

```text
Use case: illustration-story.
Asset type: one production background painting for the "小狐狸的楓葉信" iPhone keyboard, NO keys, NO toolbar, NO phone, NO text.
Input image 1 is the user's required aesthetic reference. Match its watercolor, hand-drawn colored pencil and delicate grainy paper texture closely.
Make a portrait canvas approximately 1254 by 1254. The TOP 18% of the canvas is a pale dusty rose and warm cream watercolor autumn landscape banner: soft layered terracotta/mauve hills, a burgundy rustic mailbox with a small cream envelope and red heart seal at the upper-right, a loose cream envelope upper-left, scattered red-orange maple leaves near the two corners. Keep the central area of this pale banner quiet and light, blank for live title text. The banner must fill edge to edge.
The LOWER 82% is mostly quiet, richly tactile dark plum-brown paper (#443741), subtly mottled with visible fine watercolor/paper fibers, close to the dark keyboard bed in the reference. A very subtle irregular painted boundary below the landscape, then quiet uninterrupted dark plum surface all the way to the bottom. Keep all bright autumn decorations only in the top banner; keyboard keys will be composited later over the lower area, so do not draw any foxes, keys, letters, controls, circles, grids or objects there. Cohesive premium Japanese illustrated stationery atmosphere, soft dry-brushed paper fibers, warm and delicate. No flat vector gradients, no glossy 3D. No text anywhere.
```

## 空白鍵／換行鍵寬版

```text
Use case: illustration-story. Asset type: ONE transparent sprite sheet containing TWO dedicated WIDE fox-head keyboard keycaps. Input image is the style reference (existing watercolor fox atlas). Keep this exact autumn watercolor/paper/colored-pencil style and warm tactile pigment grain.
Canvas: wide LANDSCAPE aspect 3:2, ideally 1536 by 1024. Two rows only, one keycap per row, plenty of transparent gutter. Each long fox head occupies x=5% to95% of the canvas and only y=10% to40% for the upper row, y=60% to90% for the lower row. Thus every head is about THREE TIMES AS WIDE AS TALL. Keep the same dimensions and placement in both rows.
Upper: a long burnt-orange fox FACE keycap, two upright ears at far left and far right, rounded lower edge, cream muzzle following the whole lower edge. Small dot eyes and tiny nose grouped tightly at bottom CENTER, normal tiny proportions. Small cream envelope with a red heart seal at the far lower-left CORNER and a red maple leaf cluster at the far lower-right CORNER.
Lower: same elongated fox FACE keycap in deep cocoa brown, cream lower muzzle and tiny grouped eyes/nose bottom CENTER, a gold/orange/red maple leaf cluster only at the far lower-left CORNER.
IMPORTANT: the entire broad UPPER-MIDDLE surface and the vertical bands at 25% and 75% of each head must contain ONLY QUIET FUR TEXTURE, without hats, scarves, leaves, lines, ears or objects there. These quiet bands will stretch horizontally to fit different keyboard widths. Keep all ears/decorations in the outer 15% of the head, and the facial features within the middle 15% along the bottom edge.
No letters, numbers, words, keyboard labels, UI, mockup, rectangular backgrounds or grid. No bodies or limbs. True transparent background, clean warm-brown antialiased silhouette, NO red/yellow glow, NO scattered background pixels. Real painted paper texture, no vector-looking shapes, no 3D glossy plastic.
```
