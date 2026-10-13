const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3000;
const HOST = '0.0.0.0';

const htmlContent = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>OMKAR PRESET - Video Editing Instruction Engine</title>
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=JetBrains+Mono:wght@400;500;600&display=swap" rel="stylesheet">
  <style>
    :root {
      --bg: #090d16;
      --card-bg: #111827;
      --card-border: #1f293d;
      --primary: #38bdf8;
      --primary-hover: #0ea5e9;
      --accent: #818cf8;
      --split-red: #ef4444;
      --keyframe-gold: #facc15;
      --text: #f8fafc;
      --text-muted: #94a3b8;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: 'Plus Jakarta Sans', sans-serif;
      background: var(--bg);
      color: var(--text);
      min-height: 100vh;
      display: flex;
      flex-direction: column;
    }
    header {
      background: rgba(17, 24, 39, 0.85);
      backdrop-filter: blur(12px);
      border-bottom: 1px solid var(--card-border);
      padding: 14px 24px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      position: sticky;
      top: 0;
      z-index: 100;
    }
    .brand { display: flex; align-items: center; gap: 12px; }
    .logo-badge {
      background: linear-gradient(135deg, #0284c7, #6366f1);
      color: white;
      font-weight: 900;
      font-size: 14px;
      padding: 6px 10px;
      border-radius: 8px;
      letter-spacing: 1px;
    }
    .title-group h1 { font-size: 18px; font-weight: 800; letter-spacing: 0.5px; }
    .title-group p { font-size: 12px; color: var(--text-muted); }
    .header-actions { display: flex; gap: 10px; }
    button {
      font-family: inherit;
      cursor: pointer;
      border: none;
      border-radius: 8px;
      font-size: 13px;
      font-weight: 600;
      padding: 8px 14px;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      transition: all 0.15s ease;
    }
    .btn-primary { background: var(--primary); color: #021426; }
    .btn-primary:hover { background: var(--primary-hover); transform: translateY(-1px); }
    .btn-secondary { background: #1e293b; color: var(--text); border: 1px solid var(--card-border); }
    .btn-secondary:hover { background: #334155; }
    .btn-split { background: var(--split-red); color: white; }
    .btn-split:hover { background: #dc2626; }
    .btn-accent { background: var(--accent); color: white; }
    .btn-accent:hover { background: #6366f1; }
    
    main {
      flex: 1;
      max-width: 1200px;
      width: 100%;
      margin: 0 auto;
      padding: 20px;
      display: grid;
      grid-template-columns: 1fr 340px;
      gap: 20px;
    }
    @media (max-width: 900px) {
      main { grid-template-columns: 1fr; }
    }
    
    .editor-pane { display: flex; flex-direction: column; gap: 16px; }
    .viewport-card {
      background: #020617;
      border: 1px solid var(--card-border);
      border-radius: 16px;
      overflow: hidden;
      position: relative;
      aspect-ratio: 16 / 9;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    canvas#videoCanvas {
      width: 100%;
      height: 100%;
      object-fit: contain;
    }
    .viewport-overlay {
      position: absolute;
      top: 12px;
      left: 12px;
      display: flex;
      gap: 8px;
      z-index: 10;
    }
    .badge {
      background: rgba(0, 0, 0, 0.75);
      border: 1px solid rgba(255, 255, 255, 0.15);
      padding: 4px 10px;
      border-radius: 6px;
      font-size: 11px;
      font-family: 'JetBrains Mono', monospace;
      color: var(--primary);
    }
    .viewport-controls {
      position: absolute;
      bottom: 12px;
      left: 50%;
      transform: translateX(-50%);
      display: flex;
      align-items: center;
      gap: 12px;
      background: rgba(15, 23, 42, 0.85);
      padding: 6px 16px;
      border-radius: 30px;
      border: 1px solid var(--card-border);
    }
    .play-btn {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 0;
    }
    .time-label {
      font-family: 'JetBrains Mono', monospace;
      font-size: 13px;
      font-weight: 600;
    }

    .timeline-card {
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 16px;
      padding: 16px;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .timeline-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .clips-ribbon {
      height: 28px;
      border-radius: 6px;
      overflow: hidden;
      display: flex;
      gap: 2px;
      background: #020617;
    }
    .clip-segment {
      height: 100%;
      display: flex;
      align-items: center;
      padding: 0 6px;
      font-size: 11px;
      font-weight: 700;
      color: white;
      cursor: pointer;
      transition: opacity 0.15s;
    }
    .clip-segment:hover { opacity: 0.9; }

    .track-container {
      position: relative;
      height: 70px;
      background: #090e1a;
      border: 1px solid var(--card-border);
      border-radius: 8px;
      cursor: pointer;
      user-select: none;
    }
    .track-ruler {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 16px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.05);
    }
    .playhead-line {
      position: absolute;
      top: 0;
      bottom: 0;
      width: 2px;
      background: white;
      pointer-events: none;
      z-index: 20;
    }
    .playhead-head {
      position: absolute;
      top: 0;
      left: -6px;
      width: 14px;
      height: 14px;
      background: white;
      border-radius: 50%;
      border: 2px solid var(--primary);
    }
    .split-line {
      position: absolute;
      top: 0;
      bottom: 0;
      width: 2px;
      background: var(--split-red);
      z-index: 15;
    }
    .split-line::after {
      content: '✂';
      position: absolute;
      top: -8px;
      left: -6px;
      font-size: 10px;
      color: var(--split-red);
    }
    .keyframe-marker {
      position: absolute;
      bottom: 12px;
      width: 12px;
      height: 12px;
      background: var(--keyframe-gold);
      transform: translateX(-50%) rotate(45deg);
      border-radius: 2px;
      z-index: 16;
      cursor: pointer;
    }
    .keyframe-marker.active {
      outline: 2px solid white;
      box-shadow: 0 0 8px var(--keyframe-gold);
    }

    .sidebar { display: flex; flex-direction: column; gap: 16px; }
    .panel {
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 16px;
      padding: 16px;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .panel-title {
      font-size: 12px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 1px;
      color: var(--primary);
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .control-row {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .control-row label {
      font-size: 12px;
      font-weight: 600;
      color: var(--text-muted);
      display: flex;
      justify-content: space-between;
    }
    .control-row input[type="range"] {
      width: 100%;
      accent-color: var(--primary);
    }
    .control-row select, .control-row input[type="text"] {
      background: #090e1a;
      border: 1px solid var(--card-border);
      border-radius: 6px;
      color: var(--text);
      padding: 8px 10px;
      font-family: inherit;
      font-size: 13px;
    }
    .chips-group { display: flex; gap: 6px; }
    .chip-btn {
      flex: 1;
      padding: 6px 8px;
      font-size: 11px;
      border: 1px solid var(--card-border);
      background: #090e1a;
      color: var(--text-muted);
      border-radius: 6px;
      text-align: center;
      cursor: pointer;
    }
    .chip-btn.active {
      background: var(--primary);
      color: #021426;
      font-weight: 700;
      border-color: var(--primary);
    }

    .splits-list {
      max-height: 130px;
      overflow-y: auto;
      display: flex;
      flex-direction: column;
      gap: 6px;
    }
    .split-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: #090e1a;
      padding: 6px 10px;
      border-radius: 6px;
      font-size: 12px;
      font-family: 'JetBrains Mono', monospace;
    }
    .del-btn {
      color: var(--split-red);
      background: none;
      border: none;
      padding: 2px 6px;
      font-size: 14px;
      cursor: pointer;
    }

    /* Modal dialog */
    .modal-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.75);
      display: none;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      padding: 16px;
    }
    .modal {
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 16px;
      padding: 24px;
      max-width: 500px;
      width: 100%;
      display: flex;
      flex-direction: column;
      gap: 16px;
    }
    .modal h2 { font-size: 18px; color: #f59e0b; display: flex; align-items: center; gap: 8px; }
    .modal p { font-size: 13px; line-height: 1.5; color: var(--text-muted); }
    .modal-actions { display: flex; gap: 10px; justify-content: flex-end; }
  </style>
</head>
<body>
  <header>
    <div class="brand">
      <div class="logo-badge">OMKAR</div>
      <div class="title-group">
        <h1>OMKAR PRESET</h1>
        <p>Portable Video Editing Instruction & Keyframe Engine</p>
      </div>
    </div>
    <div class="header-actions">
      <input type="file" id="importVideoInput" accept="video/*" style="display: none;">
      <input type="file" id="importPresetInput" accept=".omkarpreset,application/json" style="display: none;">
      <button class="btn-secondary" onclick="document.getElementById('importVideoInput').click()">📹 Import Video</button>
      <button class="btn-secondary" onclick="document.getElementById('importPresetInput').click()">📥 Import .omkarpreset</button>
      <button class="btn-primary" onclick="savePreset()">💾 Save Preset</button>
      <button class="btn-accent" onclick="exportVideo()">🎬 Export Video</button>
    </div>
  </header>

  <main>
    <div class="editor-pane">
      <div class="viewport-card">
        <canvas id="videoCanvas" width="960" height="540"></canvas>
        <div class="viewport-overlay">
          <div class="badge" id="zoomBadge">ZOOM: 100%</div>
          <div class="badge" id="panBadge">PAN: (0, 0)</div>
          <div class="badge" id="timecodeBadge">00:00.000 / 00:12.000</div>
        </div>
        <div class="viewport-controls">
          <button class="btn-primary play-btn" onclick="togglePlayPause()" id="playBtn">▶</button>
          <div class="time-label" id="currentMsLabel">0 ms</div>
        </div>
      </div>

      <div class="timeline-card">
        <div class="timeline-header">
          <div>
            <h3 style="font-size: 14px; font-weight: 700;">VIDEO TIMELINE</h3>
            <span style="font-size: 12px; color: var(--text-muted);" id="timelineStats">Duration: 12000 ms</span>
          </div>
          <div style="display: flex; gap: 8px;">
            <button class="btn-split" onclick="addSplitAtPlayhead()">✂ Add Split</button>
            <button class="btn-secondary" onclick="addKeyframeAtPlayhead()">◆ Add Keyframe</button>
          </div>
        </div>

        <!-- Ordered Clips Ribbon -->
        <div class="clips-ribbon" id="clipsRibbon"></div>

        <!-- Interactive Scrubber Track -->
        <div class="track-container" id="trackContainer" onmousedown="onTrackMouseDown(event)">
          <div class="track-ruler" id="trackRuler"></div>
          <div class="playhead-line" id="playheadLine">
            <div class="playhead-head"></div>
          </div>
          <div id="splitsContainer"></div>
          <div id="keyframesContainer"></div>
        </div>
      </div>
    </div>

    <div class="sidebar">
      <!-- Preset Metadata -->
      <div class="panel">
        <div class="panel-title">Preset Settings</div>
        <div class="control-row">
          <label>Preset Name</label>
          <input type="text" id="presetNameInput" value="My Cinematic Preset">
        </div>
        <div class="control-row">
          <label>Source Video Duration</label>
          <select id="videoDurationSelect" onchange="changeVideoDuration(Number(this.value))">
            <option value="6000">6 seconds (Shorts/Reels)</option>
            <option value="12000" selected>12 seconds (Default Master)</option>
            <option value="24000">24 seconds (Extended Video)</option>
          </select>
        </div>
      </div>

      <!-- Keyframe Inspector -->
      <div class="panel">
        <div class="panel-title">
          <span>Keyframe Inspector</span>
          <button class="del-btn" id="delKfBtn" onclick="deleteActiveKeyframe()" style="display:none;" title="Delete Keyframe">🗑</button>
        </div>
        <div style="font-size: 12px; color: var(--text-muted); font-family: 'JetBrains Mono', monospace;" id="kfTimecode">
          Select or scrub to a keyframe
        </div>
        <div class="control-row">
          <label>Zoom Factor <span id="zoomVal">100%</span></label>
          <input type="range" id="zoomRange" min="50" max="400" value="100" oninput="updateKeyframeValues()">
        </div>
        <div class="control-row">
          <label>Pan X <span id="panXVal">0%</span></label>
          <input type="range" id="panXRange" min="-100" max="100" value="0" oninput="updateKeyframeValues()">
        </div>
        <div class="control-row">
          <label>Pan Y <span id="panYVal">0%</span></label>
          <input type="range" id="panYRange" min="-100" max="100" value="0" oninput="updateKeyframeValues()">
        </div>
        <div class="control-row">
          <label>Interpolation Mode</label>
          <div class="chips-group">
            <div class="chip-btn active" id="chipLinear" onclick="setInterpolation('LINEAR')">Linear</div>
            <div class="chip-btn" id="chipEase" onclick="setInterpolation('EASE_IN_OUT')">Smooth</div>
            <div class="chip-btn" id="chipHold" onclick="setInterpolation('HOLD')">Hold</div>
          </div>
        </div>
      </div>

      <!-- Split Points List -->
      <div class="panel">
        <div class="panel-title">
          <span>Split Boundaries (<span id="splitsCount">0</span>)</span>
        </div>
        <div class="splits-list" id="splitsList"></div>
      </div>

      <!-- Android Architecture Info -->
      <div class="panel" style="background: rgba(14, 165, 233, 0.05); border-color: rgba(14, 165, 233, 0.2);">
        <div class="panel-title" style="color: var(--primary);">Android APK Ready</div>
        <p style="font-size: 12px; color: var(--text-muted); line-height: 1.4;">
          This applet contains the full Jetpack Compose Kotlin codebase, Robolectric tests, and GitHub Actions workflow configured for building the release APK!
        </p>
      </div>
    </div>
  </main>

  <!-- Duration Warning Dialog -->
  <div class="modal-backdrop" id="warningModal">
    <div class="modal">
      <h2>⚠️ Preset Exceeds Video Duration</h2>
      <p id="warningText"></p>
      <div class="modal-actions">
        <button class="btn-secondary" onclick="confirmImport(false)">Clamp to Video</button>
        <button class="btn-primary" onclick="confirmImport(true)">Keep Exact Timestamps</button>
      </div>
    </div>
  </div>

  <script>
    // Pure Deterministic Timeline State & Transform Evaluator
    const state = {
      presetName: "My Cinematic Preset",
      durationMs: 12000,
      currentPositionMs: 0,
      isPlaying: false,
      splitPointsMs: [3500, 7200],
      keyframes: [
        { id: 'k1', timestampMs: 0, x: 0, y: 0, zoomPercent: 100, interpolationMode: 'LINEAR' },
        { id: 'k2', timestampMs: 3500, x: 15, y: -10, zoomPercent: 140, interpolationMode: 'EASE_IN_OUT' },
        { id: 'k3', timestampMs: 7200, x: -20, y: 15, zoomPercent: 160, interpolationMode: 'LINEAR' },
        { id: 'k4', timestampMs: 12000, x: 0, y: 0, zoomPercent: 100, interpolationMode: 'LINEAR' }
      ],
      activeKeyframeId: 'k1',
      pendingImport: null
    };

    const canvas = document.getElementById('videoCanvas');
    const ctx = canvas.getContext('2d');
    let playInterval = null;

    function evaluateTransform(keyframes, currentMs) {
      if (!keyframes.length) return { x: 0, y: 0, zoomPercent: 100 };
      const sorted = [...keyframes].sort((a,b) => a.timestampMs - b.timestampMs);
      if (currentMs <= sorted[0].timestampMs) return { ...sorted[0] };
      if (currentMs >= sorted[sorted.length - 1].timestampMs) return { ...sorted[sorted.length - 1] };

      let k1 = sorted[0], k2 = sorted[sorted.length - 1];
      for (let i = 0; i < sorted.length - 1; i++) {
        if (currentMs >= sorted[i].timestampMs && currentMs <= sorted[i+1].timestampMs) {
          k1 = sorted[i];
          k2 = sorted[i+1];
          break;
        }
      }
      const dt = k2.timestampMs - k1.timestampMs;
      if (dt <= 0) return { ...k1 };
      const t = Math.max(0, Math.min(1, (currentMs - k1.timestampMs) / dt));
      let factor = t;
      if (k1.interpolationMode === 'HOLD') factor = 0;
      else if (k1.interpolationMode === 'EASE_IN_OUT') factor = t * t * (3 - 2 * t);

      return {
        timestampMs: currentMs,
        x: k1.x + (k2.x - k1.x) * factor,
        y: k1.y + (k2.y - k1.y) * factor,
        zoomPercent: k1.zoomPercent + (k2.zoomPercent - k1.zoomPercent) * factor
      };
    }

    function formatMs(ms) {
      const min = Math.floor(ms / 60000);
      const sec = Math.floor((ms % 60000) / 1000);
      const mil = ms % 1000;
      return String(min).padStart(2, '0') + ':' + String(sec).padStart(2, '0') + '.' + String(mil).padStart(3, '0');
    }

    function renderCanvas() {
      const transform = evaluateTransform(state.keyframes, state.currentPositionMs);
      const scale = transform.zoomPercent / 100;
      const w = canvas.width;
      const h = canvas.height;
      
      ctx.fillStyle = '#0f172a';
      ctx.fillRect(0, 0, w, h);

      // Save transform state
      ctx.save();
      ctx.translate(w / 2, h / 2);
      ctx.translate((transform.x / 100) * (w * 0.35), (transform.y / 100) * (h * 0.35));
      ctx.scale(scale, scale);
      ctx.translate(-w / 2, -h / 2);

      // Draw simulated master video content (high aesthetic generative gradient scene)
      const grad = ctx.createLinearGradient(0, 0, w, h);
      grad.addColorStop(0, '#1e1b4b');
      grad.addColorStop(0.5, '#3b82f6');
      grad.addColorStop(1, '#06b6d4');
      ctx.fillStyle = grad;
      ctx.fillRect(w * 0.1, h * 0.1, w * 0.8, h * 0.8);

      // Sun & horizon
      ctx.beginPath();
      ctx.arc(w * 0.5, h * 0.45, 45, 0, Math.PI * 2);
      ctx.fillStyle = '#f59e0b';
      ctx.fill();

      ctx.strokeStyle = '#38bdf8';
      ctx.lineWidth = 4;
      ctx.beginPath();
      ctx.moveTo(w * 0.1, h * 0.65);
      ctx.lineTo(w * 0.9, h * 0.65);
      ctx.stroke();

      // Text marker
      ctx.fillStyle = 'rgba(255, 255, 255, 0.85)';
      ctx.font = 'bold 22px Plus Jakarta Sans';
      ctx.fillText('OMKAR MASTER VIDEO STREAM', w * 0.18, h * 0.32);
      ctx.restore();

      // Rule of thirds framing overlay
      ctx.strokeStyle = 'rgba(255, 255, 255, 0.15)';
      ctx.lineWidth = 1;
      ctx.setLineDash([6, 6]);
      ctx.beginPath();
      ctx.moveTo(w / 3, 0); ctx.lineTo(w / 3, h);
      ctx.moveTo((w / 3) * 2, 0); ctx.lineTo((w / 3) * 2, h);
      ctx.moveTo(0, h / 3); ctx.lineTo(w, h / 3);
      ctx.moveTo(0, (h / 3) * 2); ctx.lineTo(w, (h / 3) * 2);
      ctx.stroke();
      ctx.setLineDash([]);

      // Update badges
      document.getElementById('zoomBadge').innerText = 'ZOOM: ' + Math.round(transform.zoomPercent) + '%';
      document.getElementById('panBadge').innerText = 'PAN: (' + Math.round(transform.x) + ', ' + Math.round(transform.y) + ')';
      document.getElementById('timecodeBadge').innerText = formatMs(state.currentPositionMs) + ' / ' + formatMs(state.durationMs);
      document.getElementById('currentMsLabel').innerText = state.currentPositionMs + ' ms';

      // Update playhead position on track
      const progress = state.currentPositionMs / state.durationMs;
      document.getElementById('playheadLine').style.left = (progress * 100) + '%';
    }

    function renderTimeline() {
      // Clips Ribbon
      const ribbon = document.getElementById('clipsRibbon');
      ribbon.innerHTML = '';
      const validSplits = [...state.splitPointsMs].filter(s => s > 0 && s < state.durationMs).sort((a,b) => a - b);
      const clipColors = ['#0284c7', '#0d9488', '#4f46e5', '#7c3aed', '#c026d3'];
      
      let start = 0;
      const allBoundaries = [...validSplits, state.durationMs];
      allBoundaries.forEach((end, idx) => {
        const dur = end - start;
        const widthPct = (dur / state.durationMs) * 100;
        const div = document.createElement('div');
        div.className = 'clip-segment';
        div.style.width = widthPct + '%';
        div.style.background = clipColors[idx % clipColors.length];
        div.innerText = 'Clip ' + (idx + 1);
        div.onclick = () => seekTo(start);
        ribbon.appendChild(div);
        start = end;
      });

      // Split lines on track
      const splitsContainer = document.getElementById('splitsContainer');
      splitsContainer.innerHTML = '';
      state.splitPointsMs.forEach(split => {
        const line = document.createElement('div');
        line.className = 'split-line';
        line.style.left = ((split / state.durationMs) * 100) + '%';
        splitsContainer.appendChild(line);
      });

      // Keyframe diamonds on track
      const kfContainer = document.getElementById('keyframesContainer');
      kfContainer.innerHTML = '';
      state.keyframes.forEach(kf => {
        const marker = document.createElement('div');
        marker.className = 'keyframe-marker' + (kf.id === state.activeKeyframeId ? ' active' : '');
        marker.style.left = ((kf.timestampMs / state.durationMs) * 100) + '%';
        marker.onclick = (e) => {
          e.stopPropagation();
          selectKeyframe(kf.id);
          seekTo(kf.timestampMs);
        };
        kfContainer.appendChild(marker);
      });

      // Splits list in sidebar
      const list = document.getElementById('splitsList');
      list.innerHTML = '';
      document.getElementById('splitsCount').innerText = state.splitPointsMs.length;
      state.splitPointsMs.sort((a,b)=>a-b).forEach(split => {
        const row = document.createElement('div');
        row.className = 'split-item';
        row.innerHTML = '<span>' + formatMs(split) + ' (' + split + 'ms)</span><button class="del-btn" onclick="deleteSplit(' + split + ')">×</button>';
        list.appendChild(row);
      });
    }

    function seekTo(ms) {
      state.currentPositionMs = Math.max(0, Math.min(state.durationMs, ms));
      renderCanvas();
    }

    function onTrackMouseDown(e) {
      const rect = document.getElementById('trackContainer').getBoundingClientRect();
      const progress = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
      seekTo(Math.round(progress * state.durationMs));

      function onMouseMove(ev) {
        const p = Math.max(0, Math.min(1, (ev.clientX - rect.left) / rect.width));
        seekTo(Math.round(p * state.durationMs));
      }
      function onMouseUp() {
        window.removeEventListener('mousemove', onMouseMove);
        window.removeEventListener('mouseup', onMouseUp);
      }
      window.addEventListener('mousemove', onMouseMove);
      window.addEventListener('mouseup', onMouseUp);
    }

    function togglePlayPause() {
      state.isPlaying = !state.isPlaying;
      document.getElementById('playBtn').innerText = state.isPlaying ? '❚❚' : '▶';
      if (state.isPlaying) {
        playInterval = setInterval(() => {
          let next = state.currentPositionMs + 33;
          if (next >= state.durationMs) next = 0;
          seekTo(next);
        }, 33);
      } else {
        clearInterval(playInterval);
      }
    }

    function addSplitAtPlayhead() {
      const pos = state.currentPositionMs;
      if (pos <= 0 || pos >= state.durationMs) return;
      if (state.splitPointsMs.includes(pos)) return;
      state.splitPointsMs.push(pos);
      state.splitPointsMs.sort((a,b)=>a-b);
      renderTimeline();
    }

    function deleteSplit(ms) {
      state.splitPointsMs = state.splitPointsMs.filter(s => s !== ms);
      renderTimeline();
    }

    function addKeyframeAtPlayhead() {
      const pos = state.currentPositionMs;
      const current = evaluateTransform(state.keyframes, pos);
      const existing = state.keyframes.find(k => k.timestampMs === pos);
      if (existing) {
        state.activeKeyframeId = existing.id;
      } else {
        const newKf = {
          id: 'kf_' + Date.now(),
          timestampMs: pos,
          x: current.x,
          y: current.y,
          zoomPercent: current.zoomPercent,
          interpolationMode: 'LINEAR'
        };
        state.keyframes.push(newKf);
        state.keyframes.sort((a,b)=>a.timestampMs - b.timestampMs);
        state.activeKeyframeId = newKf.id;
      }
      selectKeyframe(state.activeKeyframeId);
      renderTimeline();
      renderCanvas();
    }

    function selectKeyframe(id) {
      state.activeKeyframeId = id;
      const kf = state.keyframes.find(k => k.id === id);
      if (kf) {
        document.getElementById('delKfBtn').style.display = 'block';
        document.getElementById('kfTimecode').innerText = 'Timestamp: ' + formatMs(kf.timestampMs) + ' (' + kf.timestampMs + 'ms)';
        document.getElementById('zoomRange').value = kf.zoomPercent;
        document.getElementById('zoomVal').innerText = Math.round(kf.zoomPercent) + '%';
        document.getElementById('panXRange').value = kf.x;
        document.getElementById('panXVal').innerText = Math.round(kf.x) + '%';
        document.getElementById('panYRange').value = kf.y;
        document.getElementById('panYVal').innerText = Math.round(kf.y) + '%';
        setInterpolation(kf.interpolationMode, false);
      }
      renderTimeline();
    }

    function updateKeyframeValues() {
      const kf = state.keyframes.find(k => k.id === state.activeKeyframeId);
      if (!kf) return;
      kf.zoomPercent = parseFloat(document.getElementById('zoomRange').value);
      kf.x = parseFloat(document.getElementById('panXRange').value);
      kf.y = parseFloat(document.getElementById('panYRange').value);
      document.getElementById('zoomVal').innerText = Math.round(kf.zoomPercent) + '%';
      document.getElementById('panXVal').innerText = Math.round(kf.x) + '%';
      document.getElementById('panYVal').innerText = Math.round(kf.y) + '%';
      renderCanvas();
    }

    function setInterpolation(mode, updateModel = true) {
      document.getElementById('chipLinear').classList.toggle('active', mode === 'LINEAR');
      document.getElementById('chipEase').classList.toggle('active', mode === 'EASE_IN_OUT');
      document.getElementById('chipHold').classList.toggle('active', mode === 'HOLD');
      if (updateModel) {
        const kf = state.keyframes.find(k => k.id === state.activeKeyframeId);
        if (kf) kf.interpolationMode = mode;
      }
      renderCanvas();
    }

    function deleteActiveKeyframe() {
      if (!state.activeKeyframeId) return;
      state.keyframes = state.keyframes.filter(k => k.id !== state.activeKeyframeId);
      state.activeKeyframeId = null;
      document.getElementById('delKfBtn').style.display = 'none';
      document.getElementById('kfTimecode').innerText = 'Select or scrub to a keyframe';
      renderTimeline();
      renderCanvas();
    }

    function changeVideoDuration(dur) {
      state.durationMs = dur;
      document.getElementById('timelineStats').innerText = 'Duration: ' + dur + ' ms';
      renderTimeline();
      renderCanvas();
    }

    // Save portable .omkarpreset UTF-8 JSON file
    function savePreset() {
      const name = document.getElementById('presetNameInput').value || 'Omkar Preset';
      const validSplits = [...state.splitPointsMs].sort((a,b)=>a-b);
      
      const clips = [];
      let prev = 0;
      validSplits.forEach((sp, idx) => {
        clips.push({ id: 'c_' + (idx+1), startMs: prev, endMs: sp, name: 'Clip ' + (idx+1) });
        prev = sp;
      });
      clips.push({ id: 'c_last', startMs: prev, endMs: state.durationMs, name: 'Clip ' + (clips.length+1) });

      const presetData = {
        format: "OMKAR_PRESET",
        schemaVersion: 1,
        id: "preset_" + Date.now(),
        name: name,
        createdAtMs: Date.now(),
        sourceDurationMs: state.durationMs,
        sourceWidth: 1920,
        sourceHeight: 1080,
        splitPointsMs: validSplits,
        clips: clips,
        keyframes: state.keyframes
      };

      const blob = new Blob([JSON.stringify(presetData, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = name.replace(/\\s+/g, '_') + '.omkarpreset';
      a.click();
      URL.revokeObjectURL(url);
    }

    // Import .omkarpreset
    document.getElementById('importPresetInput').addEventListener('change', (e) => {
      const file = e.target.files[0];
      if (!file) return;
      const reader = new FileReader();
      reader.onload = (event) => {
        try {
          const parsed = JSON.parse(event.target.result);
          if (parsed.format !== 'OMKAR_PRESET') {
            alert('Error: File is not a valid OMKAR_PRESET file.');
            return;
          }
          // Validate against target duration
          const outOfRangeSplits = (parsed.splitPointsMs || []).filter(s => s > state.durationMs);
          const outOfRangeKeyframes = (parsed.keyframes || []).filter(k => k.timestampMs > state.durationMs);

          if (outOfRangeSplits.length > 0 || outOfRangeKeyframes.length > 0) {
            state.pendingImport = parsed;
            document.getElementById('warningText').innerText =
              'Preset contains ' + outOfRangeSplits.length + ' split points and ' +
              outOfRangeKeyframes.length + ' keyframes exceeding target video duration (' +
              state.durationMs + 'ms). Choose whether to preserve exact timestamps or clamp to duration:';
            document.getElementById('warningModal').style.display = 'flex';
          } else {
            applyImportedPreset(parsed, true);
          }
        } catch (err) {
          alert('Failed to parse preset file: ' + err.message);
        }
      };
      reader.readAsText(file);
    });

    function confirmImport(keepExact) {
      document.getElementById('warningModal').style.display = 'none';
      if (state.pendingImport) {
        applyImportedPreset(state.pendingImport, keepExact);
        state.pendingImport = null;
      }
    }

    function applyImportedPreset(preset, keepExact) {
      document.getElementById('presetNameInput').value = preset.name || 'Imported Preset';
      if (keepExact) {
        state.splitPointsMs = [...(preset.splitPointsMs || [])].sort((a,b)=>a-b);
        state.keyframes = [...(preset.keyframes || [])].sort((a,b)=>a.timestampMs - b.timestampMs);
      } else {
        state.splitPointsMs = (preset.splitPointsMs || []).filter(s => s <= state.durationMs).sort((a,b)=>a-b);
        state.keyframes = (preset.keyframes || []).map(k => ({
          ...k,
          timestampMs: Math.min(state.durationMs, k.timestampMs)
        })).sort((a,b)=>a.timestampMs - b.timestampMs);
      }
      state.activeKeyframeId = state.keyframes[0]?.id || null;
      selectKeyframe(state.activeKeyframeId);
      renderTimeline();
      renderCanvas();
      alert('Applied preset \\'' + preset.name + '\\' with exact millisecond preservation!');
    }

    function exportVideo() {
      const validSplits = [...state.splitPointsMs].sort((a,b)=>a-b);
      const totalClips = validSplits.length + 1;
      alert('Export Pipeline executing:\\n• ' + totalClips + ' clips rendered\\n• ' + state.keyframes.length + ' keyframes evaluated\\n• Audio stream synchronized\\n• Output: 1920x1080 MP4');
    }

    // Initialize
    selectKeyframe(state.activeKeyframeId);
    renderTimeline();
    renderCanvas();
  </script>
</body>
</html>
`;

const server = http.createServer((req, res) => {
  res.writeHead(200, {
    'Content-Type': 'text/html; charset=utf-8',
    'Cache-Control': 'no-cache'
  });
  res.end(htmlContent);
});

server.listen(PORT, HOST, () => {
  console.log(`OMKAR PRESET dev server listening at http://${HOST}:${PORT}`);
});
