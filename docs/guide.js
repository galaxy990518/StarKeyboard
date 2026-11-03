// 安裝說明的圖文步驟：用 HTML 畫手機（或電腦）畫面示意圖，粉紅圈和 👆 標出要按的地方
(function () {
  // 每一格畫面：title 標題列、rows 一行一行的內容。
  // 行的種類：row 清單、btn 按鈕、toggle 開關、note 小字、dialog 跳出的對話框、file 檔案、key 鍵盤、big 大標
  // hl: true 表示要按這裡
  var FLOWS = {
    iphone: [
      { cap: '在 iPhone 的 App Store 搜尋 <b>LocalDevVPN</b>，按「取得」安裝。', screen: { title: 'App Store', rows: [
        { k: 'big', t: 'LocalDevVPN' }, { k: 'note', t: '工具程式' }, { k: 'btn', t: '取得', hl: true }] } },
      { cap: '電腦打開 <b>iloader</b>，用 Apple ID 登入，選你的 iPhone，按 <b>Install SideStore (Stable)</b>。', pc: true, screen: { title: 'iloader', rows: [
        { k: 'row', t: 'Apple ID：已登入 ✓' }, { k: 'row', t: '裝置：我的 iPhone' }, { k: 'btn', t: 'Install SideStore (Stable)', hl: true }] } },
      { cap: 'iPhone 打開 <b>設定 › 一般 › VPN 與裝置管理</b>，點你的 Apple ID，按「信任」。', screen: { title: 'VPN 與裝置管理', rows: [
        { k: 'note', t: '開發者 App' }, { k: 'row', t: '你的 Apple ID', hl: true }, { k: 'dialog', t: '要信任這位開發者嗎？', b: '信任' }] } },
      { cap: '打開 <b>LocalDevVPN</b>，按連線。之後每次安裝、更新都要先開著。', screen: { title: 'LocalDevVPN', rows: [
        { k: 'big', t: '未連線' }, { k: 'btn', t: '連線', hl: true }] } },
      { cap: '打開 <b>SideStore</b>，用同一個 Apple ID 登入。到 <b>My Apps</b> 按 SideStore 旁邊的 <b>7 DAYS</b> 續期一次。', screen: { title: 'My Apps', rows: [
        { k: 'row', t: 'SideStore', r: '7 DAYS', hl: true }, { k: 'note', t: '續期後才算設定完成' }] } },
      { cap: '用 iPhone 打開安裝網站，按「下載 iPhone 安裝檔」。', screen: { title: 'Safari', rows: [
        { k: 'big', t: '糯糯鍵盤' }, { k: 'btn', t: '下載 iPhone 安裝檔', hl: true }, { k: 'note', t: '下載的檔案在「檔案」App' }] } },
      { cap: '回到 SideStore，按左上角 <b>＋</b>，選剛剛下載的 <b>StarKeyboard.ipa</b>。', screen: { title: 'My Apps', plus: true, rows: [
        { k: 'note', t: '選擇檔案' }, { k: 'file', t: 'StarKeyboard.ipa', hl: true }] } },
      { cap: '問到延伸功能時，一定要選 <b>Keep App Extensions (Use Main Profile)</b>，鍵盤才會出現。', screen: { title: 'SideStore', rows: [
        { k: 'dialog', t: 'App Extensions', b: 'Keep App Extensions (Use Main Profile)', other: 'Remove App Extensions' }] } },
      { cap: '<b>設定 › 一般 › 鍵盤 › 鍵盤 › 新增鍵盤…</b>，在「第三方鍵盤」選 <b>糯糯鍵盤</b>。', screen: { title: '新增鍵盤', rows: [
        { k: 'note', t: '第三方鍵盤' }, { k: 'row', t: '糯糯鍵盤', hl: true }] } },
      { cap: '回到鍵盤清單點「糯糯鍵盤」，打開 <b>允許完整取用</b>，跳出提醒按「允許」。', screen: { title: '糯糯鍵盤', rows: [
        { k: 'toggle', t: '允許完整取用', on: true, hl: true }, { k: 'note', t: '剪貼簿、聲音、背景照片都需要它' }] } },
      { cap: '在 LINE 或備忘錄打字時，<b>按住</b>左下角的 🌐，選「糯糯鍵盤」，完成！', screen: { title: '備忘錄', rows: [
        { k: 'menu', t: '糯糯鍵盤', hl: true }, { k: 'key', globe: true }] } },
    ],
    android: [
      { cap: '用 Android 手機打開安裝網站，按 <b>下載 Android 安裝檔</b>。問「要下載嗎？」按<b>下載</b>。', screen: { title: 'Chrome', rows: [
        { k: 'big', t: '糯糯鍵盤' }, { k: 'btn', t: '下載 Android 安裝檔', hl: true }] } },
      { cap: '下載好後點通知上的 <b>StarKeyboard.apk</b>（或到「檔案」App 的下載資料夾點它）。', screen: { title: '下載', rows: [
        { k: 'file', t: 'StarKeyboard.apk', hl: true }, { k: 'note', t: '有好幾個的話點最新的' }] } },
      { cap: '第一次會說不允許安裝：按「設定」，打開 <b>允許來自這個來源的應用程式</b>，再按返回。', screen: { title: '安裝不明應用程式', rows: [
        { k: 'toggle', t: '允許來自這個來源的應用程式', on: true, hl: true }] } },
      { cap: '按 <b>安裝</b>。跳出 Play 安全防護提醒時，按「更多詳細資料」→「仍要安裝」。', screen: { title: '糯糯鍵盤', rows: [
        { k: 'big', t: '要安裝這個應用程式嗎？' }, { k: 'btn', t: '安裝', hl: true }] } },
      { cap: '打開「糯糯鍵盤」App，按 <b>① 打開鍵盤設定</b>。', screen: { title: '糯糯鍵盤', rows: [
        { k: 'btn', t: '① 打開鍵盤設定', hl: true }, { k: 'btn', t: '② 切換成糯糯鍵盤' }] } },
      { cap: '把 <b>糯糯鍵盤</b> 的開關打開，跳出提醒按「確定」。', screen: { title: '管理鍵盤', rows: [
        { k: 'toggle', t: '原本的鍵盤', on: true }, { k: 'toggle', t: '糯糯鍵盤', on: true, hl: true }] } },
      { cap: '回到 App，按 <b>② 切換成糯糯鍵盤</b>，在清單裡點「糯糯鍵盤」。', screen: { title: '選擇輸入法', rows: [
        { k: 'row', t: '原本的鍵盤' }, { k: 'row', t: '● 糯糯鍵盤', hl: true }] } },
      { cap: '在 App 下面的試打框打幾個字，看到糯糯鍵盤就完成了！', screen: { title: '糯糯鍵盤', rows: [
        { k: 'row', t: '在這裡試試新鍵盤' }, { k: 'key' }] } },
    ],
    'android-update': [
      { cap: '有新版時，打開「糯糯鍵盤」App 按 <b>下載新版</b>。', screen: { title: '糯糯鍵盤', rows: [
        { k: 'big', t: '🎉 有新版' }, { k: 'btn', t: '下載新版', hl: true }] } },
      { cap: '點下載好的 <b>StarKeyboard.apk</b>，按 <b>更新</b>。<b>不要先解除安裝</b>，直接蓋過去，設定、照片和剪貼簿都會留著（v0.7.7 以後）。', screen: { title: '糯糯鍵盤', rows: [
        { k: 'big', t: '要更新這個應用程式嗎？' }, { k: 'btn', t: '更新', hl: true }] } },
    ],
    'iphone-update': [
      { cap: '用 iPhone 打開安裝網站，下載新的 <b>.ipa</b>。', screen: { title: 'Safari', rows: [
        { k: 'btn', t: '下載 iPhone 安裝檔', hl: true }] } },
      { cap: '打開 <b>LocalDevVPN</b>，確認已連上。', screen: { title: 'LocalDevVPN', rows: [
        { k: 'big', t: '已連線 ✓' }] } },
      { cap: 'SideStore 左上角 <b>＋</b>，選新的安裝檔蓋過舊版，延伸功能選 <b>Keep App Extensions</b>。不要先刪舊版。', screen: { title: 'My Apps', plus: true, rows: [
        { k: 'file', t: 'StarKeyboard.ipa', hl: true }] } },
    ],
  };

  function el(tag, cls, html) { var e = document.createElement(tag); if (cls) e.className = cls; if (html != null) e.innerHTML = html; return e; }
  function finger(target) { target.classList.add('hl'); target.appendChild(el('span', 'g-finger', '👆')); }

  function drawRow(r) {
    var e;
    switch (r.k) {
      case 'big': e = el('div', 'g-big', r.t); break;
      case 'note': e = el('div', 'g-note', r.t); break;
      case 'btn': e = el('div', 'g-btn', r.t); break;
      case 'file': e = el('div', 'g-file', '<span class="g-ficon">📦</span>' + r.t); break;
      case 'toggle': e = el('div', 'g-row g-toggle', '<span>' + r.t + '</span><i class="' + (r.on ? 'on' : '') + '"></i>'); break;
      case 'menu': e = el('div', 'g-menu', '<div>English</div><div class="g-pick">' + r.t + '</div>'); if (r.hl) finger(e.querySelector('.g-pick')); return e;
      case 'key':
        e = el('div', 'g-kb');
        for (var i = 0; i < 3; i++) { var row = el('div', 'g-kbrow'); for (var j = 0; j < (i === 2 ? 7 : 10); j++) row.appendChild(el('i')); e.appendChild(row); }
        var bottom = el('div', 'g-kbrow g-kbbottom', (r.globe ? '<b class="g-globe">🌐</b>' : '<b>123</b>') + '<i class="g-space"></i><b>⏎</b>');
        e.appendChild(bottom);
        if (r.globe) finger(bottom.querySelector('.g-globe'));
        return e;
      case 'dialog':
        e = el('div', 'g-dialog', '<div class="g-dtitle">' + r.t + '</div>' + (r.other ? '<div class="g-dbtn">' + r.other + '</div>' : '') + '<div class="g-dbtn g-dmain">' + r.b + '</div>');
        finger(e.querySelector('.g-dmain'));
        return e;
      default:
        e = el('div', 'g-row', '<span>' + r.t + '</span>' + (r.r ? '<em>' + r.r + '</em>' : '<em>›</em>'));
        if (r.hl && r.r) { finger(e.querySelector('em')); return e; }
    }
    if (r.hl) finger(e);
    return e;
  }

  function drawScreen(step) {
    var s = step.screen;
    var dev = el('div', step.pc ? 'g-pc' : 'g-phone');
    var scr = el('div', 'g-screen');
    if (!step.pc) scr.appendChild(el('div', 'g-status', '<span>9:41</span><span>▮▮ ◔</span>'));
    var bar = el('div', 'g-bar', (s.plus ? '<b class="g-plus">＋</b>' : '') + '<span>' + s.title + '</span>');
    if (s.plus) finger(bar.querySelector('.g-plus'));
    scr.appendChild(bar);
    var body = el('div', 'g-body');
    s.rows.forEach(function (r) { body.appendChild(drawRow(r)); });
    scr.appendChild(body);
    dev.appendChild(scr);
    return dev;
  }

  document.querySelectorAll('[data-guide]').forEach(function (box) {
    var steps = FLOWS[box.getAttribute('data-guide')];
    if (!steps) return;
    var track = el('ol', 'g-track');
    steps.forEach(function (step, i) {
      var li = el('li', 'g-card');
      li.appendChild(el('div', 'g-num', String(i + 1)));
      li.appendChild(drawScreen(step));
      li.appendChild(el('p', 'g-cap', step.cap));
      track.appendChild(li);
    });
    box.appendChild(track);
    // 左右按鈕（電腦上用；手機直接滑）
    var nav = el('div', 'g-nav');
    var prev = el('button', '', '‹'), next = el('button', '', '›');
    prev.type = next.type = 'button';
    prev.setAttribute('aria-label', '上一步'); next.setAttribute('aria-label', '下一步');
    function go(d) { var card = track.querySelector('.g-card'); track.scrollBy({ left: d * (card ? card.offsetWidth + 14 : 240), behavior: 'smooth' }); }
    prev.onclick = function () { go(-1); }; next.onclick = function () { go(1); };
    nav.appendChild(prev); nav.appendChild(el('span', 'g-count', '共 ' + steps.length + ' 步，左右滑動看下一步')); nav.appendChild(next);
    box.appendChild(nav);
  });
})();
