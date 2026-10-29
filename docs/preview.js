/* Colors from Keyboard/Theme.swift. */
var previewThemes = {"strawberry": {"name": "草莓牛奶", "top": "#ffe4ec", "bottom": "#ffd0de", "key": "#ffffff", "text": "#8a4b5c", "special": "#ffc2d4", "specialText": "#7a3045", "accent": "#ff7fa6", "accentText": "#ffffff", "shadow": "#e8a2b6"}, "mint": {"name": "薄荷蘇打", "top": "#ddf7ef", "bottom": "#c5efe3", "key": "#ffffff", "text": "#2f6b5e", "special": "#a7e3d2", "specialText": "#2f6b5e", "accent": "#3cc5a0", "accentText": "#ffffff", "shadow": "#9fd8c7"}, "milktea": {"name": "奶茶布丁", "top": "#f3e6d6", "bottom": "#e9d5bd", "key": "#fff8ee", "text": "#6b4a33", "special": "#d9b994", "specialText": "#5a3b24", "accent": "#b9835a", "accentText": "#ffffff", "shadow": "#cbae8c"}, "lavender": {"name": "薰衣草", "top": "#ece4ff", "bottom": "#dcd0ff", "key": "#ffffff", "text": "#5b4b8a", "special": "#c7b8f5", "specialText": "#4a3b78", "accent": "#8d74e8", "accentText": "#ffffff", "shadow": "#b9a9ec"}, "lemon": {"name": "檸檬小雞", "top": "#fff6cc", "bottom": "#ffec9e", "key": "#ffffff", "text": "#7a5b12", "special": "#ffdd6b", "specialText": "#6b4e0a", "accent": "#ffa928", "accentText": "#ffffff", "shadow": "#e9cb6a"}, "night": {"name": "星空夜", "top": "#1e2245", "bottom": "#2b2f63", "key": "#3a3f7a", "text": "#f3f0ff", "special": "#2a2e5e", "specialText": "#cfcbff", "accent": "#ffd65c", "accentText": "#2b2f63", "shadow": "#14173a"}, "light": {"name": "經典淺色", "top": "#d3d6dc", "bottom": "#cdd0d6", "key": "#ffffff", "text": "#000000", "special": "#abb0ba", "specialText": "#000000", "accent": "#2f7bf6", "accentText": "#ffffff", "shadow": "#8e9196"}, "dark": {"name": "經典深色", "top": "#2b2b2d", "bottom": "#232325", "key": "#6b6b6e", "text": "#ffffff", "special": "#474749", "specialText": "#ffffff", "accent": "#2f7bf6", "accentText": "#ffffff", "shadow": "#111111"}};
(function () {
  'use strict';
  var root = document.getElementById('preview');
  if (!root) return;
  var phone = document.getElementById('preview-phone');
  var message = document.getElementById('preview-message');
  var reading = document.getElementById('preview-reading');
  var candidates = document.getElementById('preview-candidates');
  var status = document.getElementById('preview-status');
  var play = document.getElementById('preview-play');
  var sceneName = 'typing', shapeName = '兔兔', themeName = '薰衣草', timer = null, selectedPrefix = null;
  var shapeID = 'bunny', themeID = 'lavender';
  var scenes = {
    typing: { text: '今天想跟你說…', prefix: '今天想跟你說… ', reading: 'ㄋㄧˇ', items: ['你', '你好', '你們', '妳'], title: '按了哪些注音，一眼就看得見', copy: '上排保留注音和聲調，下排選中文字。打一個字也能先看到詞語預測；常選的字詞會在手機上學習。' },
    punctuation: { text: '今天一起吃飯嗎', prefix: '今天一起吃飯嗎', reading: '', items: ['？', '，', '。', '！', '～'], title: '一句話的停頓，順手就能補上', copy: '打完文字後，候選列也能出現常用標點。試著點問號，不用另外切到符號頁。' },
    shortcut: { text: '把常說的話，留給快捷片語', prefix: '', reading: 'ㄨㄢ', items: ['晚', '我晚點回覆你', '玩'], title: '一小段縮寫，帶出整句常用的話', copy: '先在 App 設定自己的縮寫和片語。例如把「ㄨㄢ」對應到「我晚點回覆你」，之後從候選列選一下就好。' }
  };
  function pressed(group, selected) {
    root.querySelectorAll(group).forEach(function (button) { button.setAttribute('aria-pressed', button === selected ? 'true' : 'false'); });
  }
  function clearActiveKey() { root.querySelectorAll('.is-pressed').forEach(function (key) { key.classList.remove('is-pressed'); }); }
  function stop() { clearTimeout(timer); timer = null; play.textContent = '▷ 播放注音示範'; clearActiveKey(); }
  function showCandidates(items, selectable) {
    candidates.replaceChildren();
    items.forEach(function (text) {
      var button = document.createElement('button');
      button.type = 'button'; button.textContent = text;
      button.addEventListener('click', function () {
        stop();
        if (selectable === false) return;
        var scene = scenes[sceneName];
        message.textContent = (selectedPrefix === null ? scene.prefix : selectedPrefix) + text;
        reading.textContent = '';
        status.textContent = '已選「' + text + '」。可以再選其他候選字，或切換上方的功能示範。';
      });
      candidates.appendChild(button);
    });
  }
  function setScene(name) {
    stop(); sceneName = name; selectedPrefix = null;
    var scene = scenes[name];
    message.textContent = scene.text; reading.textContent = scene.reading;
    showCandidates(scene.items);
    document.getElementById('preview-feature-title').textContent = scene.title;
    document.getElementById('preview-feature-copy').textContent = scene.copy;
    pressed('[data-scene]', root.querySelector('[data-scene="' + name + '"]'));
  }
  function setDetail() {
    var colors = themeID === 'night' || themeID === 'dark'
      ? { pudding: '#d7a15f', flame: '#ffd46b', droplet: '#b8efff', duck: '#ffd276' }
      : { pudding: '#a86635', flame: '#ed914a', droplet: '#4aadd5', duck: '#d78a32' };
    phone.style.setProperty('--detail', colors[shapeID] || previewThemes[themeID].accent);
  }
  root.querySelectorAll('[data-shape-option]').forEach(function (button) {
    button.addEventListener('click', function () {
      shapeID = button.dataset.shapeOption; shapeName = button.dataset.name;
      phone.dataset.shape = shapeID;
      phone.style.setProperty('--label-y', button.dataset.center + '%');
      phone.querySelectorAll('use').forEach(function (use) { use.setAttribute('href', '#preview-shape-' + shapeID); });
      pressed('[data-shape-option]', button); setDetail();
      status.textContent = '目前預覽：' + shapeName + ' × ' + themeName;
    });
  });
  root.querySelectorAll('[data-theme-option]').forEach(function (button) {
    button.addEventListener('click', function () {
      themeID = button.dataset.themeOption; themeName = button.dataset.name;
      var theme = previewThemes[themeID];
      var mapping = { top: 'top', bottom: 'bottom', key: 'key', text: 'key-text', special: 'special', specialText: 'special-text', accent: 'key-accent', accentText: 'accent-text', shadow: 'key-shadow' };
      Object.keys(mapping).forEach(function (key) { phone.style.setProperty('--' + mapping[key], theme[key]); });
      setDetail(); pressed('[data-theme-option]', button);
      document.getElementById('preview-theme-name').textContent = themeName;
      status.textContent = '目前預覽：' + shapeName + ' × ' + themeName;
    });
  });
  root.querySelectorAll('[data-scene]').forEach(function (button) {
    button.addEventListener('click', function () { setScene(button.dataset.scene); status.textContent = scenes[sceneName].title + '。點候選字就能試看。'; });
  });
  play.addEventListener('click', function () {
    if (timer !== null) { stop(); status.textContent = '示範已停止，可以直接點選候選字。'; return; }
    setScene('typing');
    play.textContent = '■ 停止示範';
    var frames = [
      ['ㄋ', 'ㄋ', ['你', '妳', '年']], ['ㄧ', 'ㄋㄧ', ['你', '妳', '泥']], ['ˇ', 'ㄋㄧˇ', ['你', '你好', '你們']],
      ['ㄏ', 'ㄋㄧˇ ㄏ', ['你好', '妳好', '你還']], ['ㄠ', 'ㄋㄧˇ ㄏㄠ', ['你好', '妳好']], ['ˇ', 'ㄋㄧˇ ㄏㄠˇ', ['你好', '妳好']]
    ];
    var index = 0;
    function step() {
      clearActiveKey();
      if (index === frames.length) {
        message.textContent = '今天想跟你說… 你好'; reading.textContent = '';
        stop(); selectedPrefix = '今天想跟你說… 你好'; showCandidates(['，', '。', '！']);
        status.textContent = '示範完成：注音逐字出現，選好「你好」後就能加標點。';
        return;
      }
      var frame = frames[index++];
      reading.textContent = frame[1]; showCandidates(frame[2]);
      root.querySelectorAll('[data-key]').forEach(function (key) { if (key.dataset.key === frame[0]) key.classList.add('is-pressed'); });
      timer = setTimeout(step, 450);
    }
    step();
  });
  var audio;
  document.getElementById('preview-sound').addEventListener('click', function () {
    if (!audio) { audio = new Audio('assets/preview-tink.wav'); audio.volume = 0.65; }
    audio.currentTime = 0;
    audio.play().then(function () { status.textContent = '柔和叮叮：App 內的按鍵音範例，音量依裝置設定而異。'; }).catch(function () { status.textContent = '目前無法播放聲音，請確認裝置音量後再試一次。'; });
  });
  document.addEventListener('visibilitychange', function () { if (document.hidden) stop(); });
  setScene('typing');
}());
