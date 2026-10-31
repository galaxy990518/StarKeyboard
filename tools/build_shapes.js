// 新的按鍵造型只寫在 tools/key_shapes.txt，這支程式產生三份：
//   iPhone：Keyboard/KeyShapeArt.swift
//   Android：android/.../ime/KeyShapeArt.kt
//   網站試玩：docs/sim-shapes.js
// 用法：node tools/build_shapes.js
const fs = require('fs');
const path = require('path');
const root = path.join(__dirname, '..');
const K = 0.5522847498;

// ---- 讀檔 ----
const shapes = [];
let cur = null;
for (const raw of fs.readFileSync(path.join(__dirname, 'key_shapes.txt'), 'utf8').split(/\r?\n/)) {
  const line = raw.trim();
  if (!line || line.startsWith('//')) continue;
  const head = line.match(/^== (\w+) (.+)$/);
  if (head) { cur = { id: head[1], title: head[2], body: [0, 0, 100, 100], path: '', detail: '', color: null, alpha: 0.4, aspect: 1 }; shapes.push(cur); continue; }
  const [key, ...rest] = line.split(/\s+/);
  const value = rest.join(' ');
  if (key === 'body') cur.body = rest.map(Number);
  else if (key === 'path') cur.path = value;
  else if (key === 'detail') cur.detail = value;
  else if (key === 'color') cur.color = rest[0] === 'accent' ? null : rest.map(h => parseInt(h, 16));
  else if (key === 'alpha') cur.alpha = Number(value);
  else if (key === 'aspect') cur.aspect = Number(value);
  else throw new Error('看不懂：' + line);
}

// ---- 路徑：展開橢圓和星形，每一段都轉成同一個方向（重疊的地方才不會變成洞） ----
function rotate(cx, cy, x, y, deg) {
  const a = deg * Math.PI / 180, dx = x - cx, dy = y - cy;
  return [cx + dx * Math.cos(a) - dy * Math.sin(a), cy + dx * Math.sin(a) + dy * Math.cos(a)];
}

function ellipse(cx, cy, rx, ry, deg) {
  const P = (x, y) => rotate(cx, cy, x, y, deg);
  const kx = rx * K, ky = ry * K;
  // 右 → 下 → 左 → 上，每段四分之一
  const quarters = [
    [[cx + rx, cy + ky], [cx + kx, cy + ry], [cx, cy + ry]],
    [[cx - kx, cy + ry], [cx - rx, cy + ky], [cx - rx, cy]],
    [[cx - rx, cy - ky], [cx - kx, cy - ry], [cx, cy - ry]],
    [[cx + kx, cy - ry], [cx + rx, cy - ky], [cx + rx, cy]],
  ];
  return { start: P(cx + rx, cy), closed: true, segs: quarters.map(q => ({ t: 'C', c1: P(...q[0]), c2: P(...q[1]), p: P(...q[2]) })) };
}

function star(cx, cy, ro, ri, n, deg) {
  const pts = [];
  for (let k = 0; k < n * 2; k++) {
    const a = (deg + k * 180 / n) * Math.PI / 180, r = k % 2 ? ri : ro;
    pts.push([cx + r * Math.cos(a), cy + r * Math.sin(a)]);
  }
  return { start: pts[0], closed: true, segs: pts.slice(1).map(p => ({ t: 'L', p })) };
}

function parse(d) {
  const tok = d.match(/[A-Za-z]|-?\d*\.?\d+/g) || [];
  const subs = [];
  let sub = null, i = 0;
  const num = () => Number(tok[i++]);
  const hasNum = () => i < tok.length && /^-?\d*\.?\d+$/.test(tok[i]);
  while (i < tok.length) {
    const c = tok[i++];
    if (c === 'M') { sub = { start: [num(), num()], segs: [] }; subs.push(sub); }
    else if (c === 'L') sub.segs.push({ t: 'L', p: [num(), num()] });
    else if (c === 'C') sub.segs.push({ t: 'C', c1: [num(), num()], c2: [num(), num()], p: [num(), num()] });
    else if (c === 'Q') sub.segs.push({ t: 'Q', c1: [num(), num()], p: [num(), num()] });
    else if (c === 'Z') sub.closed = true;
    else if (c === 'E') { const a = [num(), num(), num(), num()]; subs.push(ellipse(...a, hasNum() ? num() : 0)); }
    else if (c === 'S') { const a = [num(), num(), num(), num(), num()]; subs.push(star(...a, hasNum() ? num() : 0)); }
    else throw new Error('不支援的指令 ' + c + '：' + d);
  }
  for (const s of subs) if (area(s) < 0) reverse(s);
  return subs;
}

function area(s) {
  const pts = [s.start];
  for (const g of s.segs) { if (g.c1) pts.push(g.c1); if (g.c2) pts.push(g.c2); pts.push(g.p); }
  let a = 0;
  for (let k = 0; k < pts.length; k++) { const p = pts[k], q = pts[(k + 1) % pts.length]; a += p[0] * q[1] - q[0] * p[1]; }
  return a / 2;
}

function reverse(s) {
  const points = [s.start, ...s.segs.map(g => g.p)];
  const segs = [];
  for (let k = s.segs.length - 1; k >= 0; k--) {
    const g = s.segs[k], to = points[k];
    if (g.t === 'L') segs.push({ t: 'L', p: to });
    else if (g.t === 'Q') segs.push({ t: 'Q', c1: g.c1, p: to });
    else segs.push({ t: 'C', c1: g.c2, c2: g.c1, p: to });
  }
  s.start = points[points.length - 1];
  s.segs = segs;
}

const r2 = n => Math.round(n * 100) / 100;

// 給程式用的數字清單：0 = M x y、1 = L x y、2 = C x1 y1 x2 y2 x y、3 = Q x1 y1 x y、4 = Z
function ops(subs) {
  const out = [];
  for (const s of subs) {
    out.push(0, ...s.start);
    for (const g of s.segs) {
      if (g.t === 'L') out.push(1, ...g.p);
      else if (g.t === 'Q') out.push(3, ...g.c1, ...g.p);
      else out.push(2, ...g.c1, ...g.c2, ...g.p);
    }
    if (s.closed) out.push(4);
  }
  return out.map(r2);
}

function svg(subs) {
  const j = a => a.map(r2).join(' ');
  return subs.map(s => 'M' + j(s.start) + s.segs.map(g =>
    g.t === 'L' ? 'L' + j(g.p) : g.t === 'Q' ? 'Q' + j([...g.c1, ...g.p]) : 'C' + j([...g.c1, ...g.c2, ...g.p])
  ).join('') + (s.closed ? 'Z' : '')).join('');
}

for (const s of shapes) {
  const subs = parse(s.path), dsubs = s.detail ? parse(s.detail) : [];
  s.ops = ops(subs); s.dops = ops(dsubs);
  s.svg = svg(subs); s.dsvg = dsubs.length ? svg(dsubs) : '';
}

const note = '由 tools/key_shapes.txt 產生（node tools/build_shapes.js），不要直接改這個檔案';
const hex = n => '0x' + n.toString(16).toUpperCase().padStart(6, '0');
const ids = shapes.map(s => JSON.stringify(s.id)).join(', ');

// ---- iPhone ----
const swiftEntries = shapes.map(s => [
  `        ${JSON.stringify(s.id)}: KeyShapeArt(`,
  `            title: ${JSON.stringify(s.title)}, body: CGRect(x: ${s.body[0]}, y: ${s.body[1]}, width: ${s.body[2]}, height: ${s.body[3]}),`,
  `            path: [${s.ops.join(', ')}],`,
  `            detail: [${s.dops.join(', ')}],`,
  `            detailLight: ${s.color ? hex(s.color[0]) : 'nil'}, detailDark: ${s.color ? hex(s.color[1]) : 'nil'}, detailAlpha: ${s.alpha}, aspect: ${s.aspect}),`,
].join('\n')).join('\n');

const swift = [
  'import UIKit',
  '',
  `// ${note}`,
  '',
  '/// 用線條畫的按鍵造型（動物、像素、拼圖、書本、蝴蝶結…）。座標在 0～100 的方格裡，畫的時候拉伸成按鍵大小。',
  'struct KeyShapeArt {',
  '    let title: String',
  '    /// 字放在這個方框（0～100）的中間',
  '    let body: CGRect',
  '    let path: [CGFloat]',
  '    let detail: [CGFloat]',
  '    /// 小裝飾的顏色；nil = 用主題的強調色',
  '    let detailLight: UInt32?',
  '    let detailDark: UInt32?',
  '    let detailAlpha: CGFloat',
  '    /// 高÷寬：畫的時候保持這個比例（按鍵太瘦時上下留白，太矮時左右留白），才不會被拉長',
  '    let aspect: CGFloat',
  '',
  `    static let order: [String] = [${ids}]`,
  '',
  '    static let shapes: [String: KeyShapeArt] = [',
  swiftEntries,
  '    ]',
  '',
  '    /// 按鍵裡實際畫圖案的方框：保持比例、置中',
  '    func fit(_ r: CGRect) -> CGRect {',
  '        var w = r.width, h = r.width * aspect',
  '        if h > r.height { h = r.height; w = h / aspect }',
  '        return CGRect(x: r.midX - w / 2, y: r.midY - h / 2, width: w, height: h)',
  '    }',
  '    func path(in r: CGRect) -> CGPath { Self.build(path, in: fit(r)) }',
  '    func detailPath(in r: CGRect) -> CGPath? { detail.isEmpty ? nil : Self.build(detail, in: fit(r)) }',
  '    func bodyRect(in rect: CGRect) -> CGRect {',
  '        let r = fit(rect)',
  '        return CGRect(x: r.minX + r.width * body.minX / 100, y: r.minY + r.height * body.minY / 100,',
  '                      width: r.width * body.width / 100, height: r.height * body.height / 100)',
  '    }',
  '    func detailColor(theme: KeyboardTheme) -> UIColor {',
  '        guard let light = detailLight, let dark = detailDark else { return theme.accent }',
  '        return UIColor(hex: theme.isDark ? dark : light)',
  '    }',
  '',
  '    /// 0 = 移到、1 = 直線、2 = 三次曲線、3 = 二次曲線、4 = 收尾',
  '    static func build(_ ops: [CGFloat], in r: CGRect) -> CGPath {',
  '        let p = CGMutablePath()',
  '        func pt(_ k: Int) -> CGPoint { CGPoint(x: r.minX + r.width * ops[k] / 100, y: r.minY + r.height * ops[k + 1] / 100) }',
  '        var i = 0',
  '        while i < ops.count {',
  '            switch Int(ops[i]) {',
  '            case 0: p.move(to: pt(i + 1)); i += 3',
  '            case 1: p.addLine(to: pt(i + 1)); i += 3',
  '            case 2: p.addCurve(to: pt(i + 5), control1: pt(i + 1), control2: pt(i + 3)); i += 7',
  '            case 3: p.addQuadCurve(to: pt(i + 3), control: pt(i + 1)); i += 5',
  '            default: p.closeSubpath(); i += 1',
  '            }',
  '        }',
  '        return p',
  '    }',
  '}',
  '',
].join('\n');
fs.writeFileSync(path.join(root, 'Keyboard/KeyShapeArt.swift'), swift);

// ---- Android ----
const f = n => n + 'f';
const ktEntries = shapes.map(s => [
  `            ${JSON.stringify(s.id)} to KeyShapeArt(`,
  `                ${JSON.stringify(s.title)}, floatArrayOf(${s.body.map(f).join(', ')}),`,
  `                floatArrayOf(${s.ops.map(f).join(', ')}),`,
  `                floatArrayOf(${s.dops.map(f).join(', ')}),`,
  `                ${s.color ? hex(s.color[0]) : 'null'}, ${s.color ? hex(s.color[1]) : 'null'}, ${s.alpha}f, ${s.aspect}f),`,
].join('\n')).join('\n');

const kt = [
  'package com.startown.starkeyboard.ime',
  '',
  'import android.graphics.Color',
  'import android.graphics.Path',
  'import android.graphics.RectF',
  '',
  `// ${note}`,
  '',
  '/** 用線條畫的按鍵造型（動物、像素、拼圖、書本、蝴蝶結…）。座標在 0～100 的方格裡，畫的時候拉伸成按鍵大小。 */',
  'class KeyShapeArt(',
  '    val title: String,',
  '    /** 字放在這個方框（0～100）的中間：x, y, 寬, 高 */',
  '    private val body: FloatArray,',
  '    private val path: FloatArray,',
  '    private val detail: FloatArray,',
  '    /** 小裝飾的顏色；null = 用主題的強調色 */',
  '    private val detailLight: Int?,',
  '    private val detailDark: Int?,',
  '    val detailAlpha: Float,',
  '    /** 高÷寬：畫的時候保持這個比例（按鍵太瘦時上下留白，太矮時左右留白），才不會被拉長 */',
  '    private val aspect: Float,',
  ') {',
  '    /** 按鍵裡實際畫圖案的方框：保持比例、置中 */',
  '    fun fit(r: RectF): RectF {',
  '        var w = r.width(); var h = w * aspect',
  '        if (h > r.height()) { h = r.height(); w = h / aspect }',
  '        return RectF(r.centerX() - w / 2, r.centerY() - h / 2, r.centerX() + w / 2, r.centerY() + h / 2)',
  '    }',
  '    fun path(r: RectF): Path = build(path, fit(r))',
  '    fun detailPath(r: RectF): Path = build(detail, fit(r))',
  '    fun bodyRect(rect: RectF): RectF {',
  '        val r = fit(rect)',
  '        return RectF(r.left + r.width() * body[0] / 100, r.top + r.height() * body[1] / 100,',
  '            r.left + r.width() * (body[0] + body[2]) / 100, r.top + r.height() * (body[1] + body[3]) / 100)',
  '    }',
  '    fun detailColor(theme: KeyboardTheme): Int {',
  '        val light = detailLight ?: return theme.accent',
  '        val c = if (theme.dark) detailDark ?: light else light',
  '        return Color.rgb(c shr 16 and 0xFF, c shr 8 and 0xFF, c and 0xFF)',
  '    }',
  '',
  '    companion object {',
  `        val order = listOf(${ids})`,
  '',
  '        val shapes: Map<String, KeyShapeArt> = mapOf(',
  ktEntries,
  '        )',
  '',
  '        /** 0 = 移到、1 = 直線、2 = 三次曲線、3 = 二次曲線、4 = 收尾 */',
  '        fun build(ops: FloatArray, r: RectF): Path {',
  '            val p = Path()',
  '            fun x(k: Int) = r.left + r.width() * ops[k] / 100',
  '            fun y(k: Int) = r.top + r.height() * ops[k] / 100',
  '            var i = 0',
  '            while (i < ops.size) {',
  '                when (ops[i].toInt()) {',
  '                    0 -> { p.moveTo(x(i + 1), y(i + 2)); i += 3 }',
  '                    1 -> { p.lineTo(x(i + 1), y(i + 2)); i += 3 }',
  '                    2 -> { p.cubicTo(x(i + 1), y(i + 2), x(i + 3), y(i + 4), x(i + 5), y(i + 6)); i += 7 }',
  '                    3 -> { p.quadTo(x(i + 1), y(i + 2), x(i + 3), y(i + 4)); i += 5 }',
  '                    else -> { p.close(); i += 1 }',
  '                }',
  '            }',
  '            return p',
  '        }',
  '    }',
  '}',
  '',
].join('\n');
fs.writeFileSync(path.join(root, 'android/app/src/main/java/com/startown/starkeyboard/ime/KeyShapeArt.kt'), kt);

// ---- 網站試玩 ----
const css = n => '#' + n.toString(16).padStart(6, '0');
const web = shapes.map(s => ({
  id: s.id, title: s.title, center: r2(s.body[1] + s.body[3] / 2), d: s.svg, detail: s.dsvg,
  color: s.color ? s.color.map(css) : null, alpha: s.alpha, aspect: s.aspect,
}));
fs.writeFileSync(path.join(root, 'docs/sim-shapes.js'), `/* ${note} */\nwindow.SIM_ART_SHAPES = ${JSON.stringify(web)};\n`);
console.log('造型 ' + shapes.length + ' 個：' + shapes.map(s => s.title).join('、'));
