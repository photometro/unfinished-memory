const variants = [
  { id: "A", label: "A 垂直叠页" },
  { id: "B", label: "B 错位索引" },
  { id: "C", label: "C 扇形卡组" },
];

const slotLayouts = {
  A: [
    { left: 29, top: 138, rotate: -0.5, z: 3, scale: 1 },
    { left: 25, top: 70, rotate: 1.2, z: 2, scale: 0.97 },
    { left: 31, top: 4, rotate: -1.6, z: 1, scale: 0.94 },
  ],
  B: [
    { left: 22, top: 142, rotate: -1, z: 3, scale: 1 },
    { left: 49, top: 72, rotate: 2.2, z: 2, scale: 0.97 },
    { left: 12, top: 4, rotate: -3.5, z: 1, scale: 0.94 },
  ],
  C: [
    { left: 27, top: 126, rotate: 0, z: 3, scale: 1 },
    { left: 18, top: 78, rotate: -4, z: 2, scale: 0.97 },
    { left: 36, top: 28, rotate: 4, z: 1, scale: 0.94 },
  ],
};

const params = new URLSearchParams(window.location.search);
const initialVariant = params.get("variant");
const initialSurface = params.get("state");

const state = {
  variant: variants.some((item) => item.id === initialVariant) ? initialVariant : "A",
  surface: initialSurface === "returned" ? "returned" : "quiet",
  deck: [],
  panel: null,
  cueLevel: 1,
  recallState: "PARTIAL",
  leaveStep: "compose",
  leavePolicy: "SERENDIPITY",
  memoryDraft: "",
  memoryTitle: "",
  cues: [
    { id: "time", label: "时间", value: "2022 年夏天", source: "AI 建议" },
    { id: "place", label: "地点", value: "杭州", source: "AI 建议" },
    { id: "person", label: "人物或关键词", value: "大学朋友，晚饭", source: "AI 建议" },
  ],
  suppressClickUntil: 0,
};

const app = document.querySelector("#app");
const panel = document.querySelector("#functionPanel");
const panelInner = document.querySelector("#panelInner");
const homeLine = document.querySelector("#homeLine");
const returnSignal = document.querySelector("#returnSignal");
const gestureHint = document.querySelector("#gestureHint");
const encounterTab = document.querySelector("#encounterTab");
const encounterMeta = document.querySelector("#encounterMeta");
const encounterTitle = document.querySelector("#encounterTitle");
const encounterDescription = document.querySelector("#encounterDescription");
const encounterAction = document.querySelector("#encounterAction");
const variantLabel = document.querySelector("#variantLabel");
const stateReadout = document.querySelector("#stateReadout");
const toast = document.querySelector("#toast");
let toastTimer;
let gesture = null;

function defaultDeck() {
  return state.surface === "returned" ? ["encounter", "leave", "past"] : ["leave", "encounter", "past"];
}

function resetDeck() {
  state.deck = defaultDeck();
}

function setSurface(surface) {
  state.surface = surface;
  resetDeck();
  renderState();
}

function updateUrl() {
  const next = new URL(window.location.href);
  next.searchParams.set("variant", state.variant);
  next.searchParams.set("state", state.surface);
  window.history.replaceState({}, "", next);
}

function applySlot(card, slotIndex, progress = 0) {
  const slot = slotLayouts[state.variant][slotIndex];
  card.dataset.slot = String(slotIndex);
  card.style.setProperty("--slot-left", `${slot.left}px`);
  card.style.setProperty("--slot-top", `${slot.top}px`);
  card.style.setProperty("--slot-rotate", `${slot.rotate}deg`);
  card.style.setProperty("--slot-z", String(slot.z));
  card.style.setProperty("--slot-scale", String(slot.scale));
  card.style.setProperty("--gesture-lift", `${slotIndex === 1 ? progress * 16 : slotIndex === 2 ? progress * 8 : 0}px`);
  card.style.setProperty("--gesture-scale", `${slotIndex === 1 ? progress * 0.03 : slotIndex === 2 ? progress * 0.02 : 0}`);
}

function renderDeck(progress = 0) {
  document.querySelectorAll(".home-card").forEach((card) => {
    const slotIndex = state.deck.indexOf(card.dataset.card);
    applySlot(card, slotIndex, progress);
    if (!card.classList.contains("is-dragging")) {
      card.style.removeProperty("--drag-x");
      card.style.removeProperty("--drag-y");
      card.style.removeProperty("--drag-rotate");
    }
    card.style.removeProperty("opacity");
    card.style.removeProperty("pointer-events");
  });
}

function renderState() {
  app.dataset.variant = state.variant;
  app.dataset.surface = state.surface;
  variantLabel.textContent = variants.find((item) => item.id === state.variant).label;
  stateReadout.textContent = state.surface === "returned" ? "一页已回来" : "安静状态";

  if (state.surface === "returned") {
    homeLine.textContent = "有一页回来";
    encounterTab.textContent = "回来的一页";
    encounterMeta.textContent = "等待打开";
    encounterTitle.textContent = "有一页，今天回来了。";
    encounterDescription.textContent = "先别看以前。看看你现在还记得什么。";
    encounterAction.textContent = "打开";
  } else {
    homeLine.textContent = "有一页回来";
    encounterTab.textContent = "遇见";
    encounterMeta.textContent = "随机";
    encounterTitle.textContent = "遇见过去";
    encounterDescription.textContent = "不知道会遇见什么。";
    encounterAction.textContent = "遇见";
  }

  renderDeck();
  updateUrl();
}

function panelHeader(name, progress = "") {
  return `
    <div class="panel-head">
      <button class="back-button" type="button" data-action="close" aria-label="返回主页">← 返回</button>
      <span class="panel-name">${name}${progress ? ` <span class="panel-progress">${progress}</span>` : ""}</span>
    </div>
  `;
}

function escapeHtml(value = "") {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function leaveComposeTemplate() {
  return `
    ${panelHeader("留下")}
    <h2 class="panel-title">想留下些什么？</h2>
    <p class="panel-copy">不需要完整。只留下现在不想失去的部分。写下的内容会自动保存。</p>
    <div class="field">
      <label for="memoryDraft">记忆内容</label>
      <textarea id="memoryDraft" placeholder="从此刻开始写……">${state.memoryDraft}</textarea>
    </div>
    <div class="field compact-field">
      <label for="memoryTitle">给它一个名字（可选）</label>
      <input id="memoryTitle" type="text" value="${state.memoryTitle}" placeholder="例如：夏天的晚饭" />
    </div>
    <div class="save-status" id="saveStatus" role="status">随时自动保存</div>
    <div class="button-row">
      <button class="primary-button" type="button" data-action="leave-cues">下一步：添加线索</button>
      <button class="secondary-button" type="button" data-action="save-exit">保存并退出</button>
    </div>
  `;
}

function leaveDraftTemplate() {
  return `
    ${panelHeader("留下")}
    <h2 class="panel-title">继续完成这段记忆。</h2>
    <p class="panel-copy">草稿已经保存。你可以继续修改，或从线索开始整理它。</p>
    <div class="draft-preview">
      <span>草稿内容</span>
      <p>${state.memoryDraft || "还没有写下内容。"}</p>
    </div>
    <div class="button-row">
      <button class="primary-button" type="button" data-action="leave-cues">添加线索</button>
      <button class="secondary-button" type="button" data-action="leave-compose">继续修改</button>
    </div>
  `;
}

function captureCueEdits() {
  panelInner.querySelectorAll("[data-cue-input]").forEach((input) => {
    const cue = state.cues.find((item) => item.id === input.dataset.cueInput);
    if (cue) cue.value = input.value;
  });
}

function getRecallCues() {
  const values = state.cues.map((cue) => cue.value.trim()).filter(Boolean);
  return values.length ? values : ["还没有设置线索。"];
}

function leaveCuesTemplate() {
  return `
    ${panelHeader("留下", "线索")}
    <h2 class="panel-title">我找到了几条可能的线索。</h2>
    <p class="panel-copy">这些内容由 AI 从你的原文中提取，只是建议。你可以修改、移除或自己添加。</p>
    <div class="cue-suggestions">
      ${state.cues.length ? state.cues.map((cue) => `
        <article class="cue-suggestion">
          <div class="cue-suggestion-head"><span class="cue-type">${escapeHtml(cue.label)}</span><span class="cue-source">${escapeHtml(cue.source)}</span></div>
          <input data-cue-input="${escapeHtml(cue.id)}" type="text" value="${escapeHtml(cue.value)}" placeholder="写下一条线索" />
          <button class="cue-remove text-action" type="button" data-action="remove-cue" data-cue-id="${escapeHtml(cue.id)}">移除</button>
        </article>
      `).join("") : `<div class="cue-empty">还没有线索，你可以自己添加一条。</div>`}
    </div>
    <button class="text-action add-cue-action" type="button" data-action="add-cue">＋ 自己添加一条</button>
    <div class="button-row">
      <button class="primary-button" type="button" data-action="leave-policy">确认线索并继续</button>
      <button class="text-action" type="button" data-action="leave-draft">稍后再说</button>
    </div>
  `;
}

function leavePolicyTemplate() {
  return `
    ${panelHeader("留下", "浮现方式")}
    <h2 class="panel-title">它以后要怎么回来？</h2>
    <div class="choice-list">
      <button class="choice-card ${state.leavePolicy === "QUIET" ? "is-selected" : ""}" type="button" data-policy="QUIET">
        <span><strong>静静保存</strong><small>只在你主动寻找时出现</small></span><span class="choice-mark">${state.leavePolicy === "QUIET" ? "已选" : ""}</span>
      </button>
      <button class="choice-card ${state.leavePolicy === "SERENDIPITY" ? "is-selected" : ""}" type="button" data-policy="SERENDIPITY">
        <span><strong>以后偶尔出现</strong><small>在不可预测的某一天回来</small></span><span class="choice-mark">${state.leavePolicy === "SERENDIPITY" ? "已选" : ""}</span>
      </button>
      <button class="choice-card ${state.leavePolicy === "FIXED_DATE" ? "is-selected" : ""}" type="button" data-policy="FIXED_DATE">
        <span><strong>某一天回来</strong><small>指定一个未来日期</small></span><span class="choice-mark">${state.leavePolicy === "FIXED_DATE" ? "已选" : ""}</span>
      </button>
    </div>
    <div class="button-row">
      <button class="primary-button" type="button" data-action="leave-review">查看封存确认</button>
    </div>
  `;
}

function leaveReviewTemplate() {
  const policyLabels = { QUIET: "静静保存", SERENDIPITY: "以后偶尔出现", FIXED_DATE: "某一天回来" };
  const confirmedCues = state.cues.map((cue) => cue.value.trim()).filter(Boolean);
  return `
    ${panelHeader("留下", "确认封存")}
    <h2 class="panel-title">准备好把它交给未来了吗？</h2>
    <p class="panel-copy">封存是一次明确的完成动作。确认后，Original 原文不会在浮现过程中被覆盖。</p>
    <div class="review-summary">
      <div><span>这段记忆</span><strong>${state.memoryTitle || "未命名的一段"}</strong></div>
      <div><span>回来方式</span><strong>${policyLabels[state.leavePolicy]}</strong></div>
      <div><span>浮现线索</span><strong>${confirmedCues.length ? `${confirmedCues.length} 条 · ${escapeHtml(confirmedCues.slice(0, 2).join("、"))}` : "未设置"}</strong></div>
    </div>
    <div class="seal-warning">之后仍可删除，但不能直接修改已经封存的原文。</div>
    <div class="button-row">
      <button class="primary-button" type="button" data-action="seal-memory">确认并封存</button>
      <button class="secondary-button" type="button" data-action="leave-policy">返回修改</button>
    </div>
  `;
}

function leaveDoneTemplate() {
  return `
    ${panelHeader("已封存")}
    <h2 class="panel-title">它会安静地待在那里。</h2>
    <p class="panel-copy">以后是否回来，由你刚刚选择的方式决定。现在不需要再做什么。</p>
    <div class="sealed-mark" aria-hidden="true">原<br />已封存</div>
    <div class="button-row"><button class="primary-button" type="button" data-action="close">回到首页</button></div>
  `;
}

function encounterTemplate() {
  if (state.surface === "returned") {
    return `
      ${panelHeader("回来的一页")}
      <h2 class="panel-title">先别看以前。</h2>
      <p class="panel-copy">打开以后，先记录你现在还能想起的内容。</p>
      <div class="cue-block"><p class="cue-label">第一条线索</p><p class="cue-value">2022 年，夏天。</p></div>
      <div class="button-row"><button class="primary-button" type="button" data-action="start-recall">打开</button><button class="secondary-button" type="button" data-action="defer">再待一会</button></div>
    `;
  }

  return `
    ${panelHeader("遇见过去")}
    <h2 class="panel-title">今天很安静。</h2>
    <p class="panel-copy">没有一页主动回来。你也可以自己走近一段过去。</p>
    <div class="encounter-empty"><span>候选池暂时没有结果</span><strong>今天可以什么都不想起。</strong></div>
    <div class="button-row"><button class="primary-button" type="button" data-action="encounter-now">主动遇见</button><button class="text-action" type="button" data-action="close">改天再说</button></div>
  `;
}

function recallTemplate() {
  const cues = getRecallCues();
  const cue = cues[Math.min(state.cueLevel - 1, cues.length - 1)];
  const cueLabel = state.cueLevel === 1 ? "第一条线索" : state.cueLevel === cues.length ? "最后一条线索" : "再近一点";
  const nextLabel = state.cueLevel < cues.length ? "再给一点" : "完全想不起来";
  return `
    ${panelHeader("你还记得吗？", `${state.cueLevel} / 3`)}
    <h2 class="panel-title">你想起什么了吗？</h2>
    <div class="cue-block"><p class="cue-label">${cueLabel}</p><p class="cue-value">${cue}</p></div>
    <div class="recall-state-row" role="group" aria-label="现在的记忆状态">
      ${["CLEAR", "PARTIAL", "VAGUE", "NONE"].map((item) => `<button class="state-choice ${state.recallState === item ? "is-selected" : ""}" type="button" data-recall-state="${item}">${{ CLEAR: "清楚", PARTIAL: "一部分", VAGUE: "模糊", NONE: "不记得" }[item]}</button>`).join("")}
    </div>
    <div class="field"><label for="recallDraft">现在记得的版本</label><textarea id="recallDraft" placeholder="写下你现在记得的……"></textarea></div>
    <div class="button-row"><button class="primary-button" type="button" data-action="submit-recall">提交回忆</button><button class="secondary-button" type="button" data-action="next-cue">${nextLabel}</button></div>
  `;
}

function revealTemplate() {
  return `
    ${panelHeader("揭晓")}
    <h2 class="panel-title">那时候的你，是这样留下的。</h2>
    <p class="panel-copy">这里没有正确答案。只是让两个时间里的你，见上一面。</p>
    <div class="memory-compare">
      <div class="compare-block"><span>现在记得的</span><p>${state.recallState === "NONE" ? "想不起来。" : "我记得那天应该挺开心，好像还一起吃了饭。"}</p></div>
      <div class="compare-block"><span>2022 年留下的</span><p>今天和他最后一次吃饭。整个晚上我们都没有聊以后。</p></div>
    </div>
    <div class="button-row"><button class="primary-button" type="button" data-action="reflection">现在呢？</button></div>
  `;
}

function reflectionTemplate() {
  return `
    ${panelHeader("回应")}
    <h2 class="panel-title">现在呢？</h2>
    <p class="panel-copy">可以写，也可以不回答。回应只属于现在。</p>
    <div class="field"><label for="reflectionDraft">现在的回应（可选）</label><textarea id="reflectionDraft" placeholder="如果你愿意，可以写一点……"></textarea></div>
    <div class="button-row"><button class="primary-button" type="button" data-action="finish">完成这次相遇</button><button class="text-action" type="button" data-action="finish">不写了</button></div>
  `;
}

function pastTemplate() {
  return `
    ${panelHeader("很久以前")}
    <h2 class="panel-title">你留下的过去。</h2>
    <p class="panel-copy">按年份寻找。这里不会主动替你解释任何一段记忆。</p>
    <div class="search-line"><label for="memorySearch">搜索过去</label><input id="memorySearch" type="search" placeholder="人物、地点或关键词" /></div>
    <div class="year-list">
      <button class="year-item text-button" type="button" data-action="open-memory"><strong>2026</strong><span>6 段</span></button>
      <button class="year-item text-button" type="button" data-action="open-memory"><strong>2024</strong><span>9 段</span></button>
      <button class="year-item text-button" type="button" data-action="open-memory"><strong>2022</strong><span>13 段</span></button>
    </div>
  `;
}

function memoryDetailTemplate() {
  return `
    ${panelHeader("2022")}
    <h2 class="panel-title">那顿没有聊以后的晚饭。</h2>
    <p class="panel-copy">杭州。一个后来很少见到的人。</p>
    <div class="timeline">
      <article class="timeline-event"><span>2022 年 7 月 18 日</span><strong>当时留下</strong><p>今天和他最后一次吃饭。整个晚上我们都没有聊以后。</p></article>
      <article class="timeline-event"><span>2026 年 9 月 7 日</span><strong>后来记得</strong><p>我记得那天应该挺开心，好像还一起吃过饭。</p></article>
      <article class="timeline-event is-current"><span>现在</span><strong>重新看到以后</strong><p>还没有回应。</p></article>
    </div>
    <div class="button-row"><button class="primary-button" type="button" data-action="close">回到过去</button></div>
  `;
}

function settingsTemplate() {
  return `
    ${panelHeader("设置")}
    <h2 class="panel-title">让它保持私人。</h2>
    <p class="panel-copy">你可以随时暂停主动浮现，也可以删除已经封存的记忆。</p>
    <div class="settings-list">
      <button class="setting-row" type="button" data-action="setting-toggle"><span><strong>主动浮现</strong><small>允许被随机选中</small></span><span class="switch is-on" id="surfaceSwitch">开</span></button>
      <button class="setting-row" type="button" data-action="setting-toggle"><span><strong>浮现通知</strong><small>只提醒“有一页回来”</small></span><span class="switch is-on">开</span></button>
      <button class="setting-row" type="button" data-action="setting-toggle"><span><strong>本地锁</strong><small>打开 App 时再确认一次</small></span><span class="switch">关</span></button>
    </div>
    <div class="privacy-note"><strong>关于 Original</strong><p>封存后的原始记录不会被浮现流程提前展示。管理员默认不能查看正文。</p></div>
  `;
}

function renderLeaveStep() {
  panelInner.innerHTML = state.leaveStep === "compose" ? leaveComposeTemplate() : state.leaveStep === "draft" ? leaveDraftTemplate() : state.leaveStep === "cues" ? leaveCuesTemplate() : state.leaveStep === "policy" ? leavePolicyTemplate() : state.leaveStep === "review" ? leaveReviewTemplate() : leaveDoneTemplate();
}

function openPanel(kind) {
  state.panel = kind;
  panel.dataset.kind = kind;
  app.dataset.panel = kind;
  state.cueLevel = 1;
  if (kind === "leave") renderLeaveStep();
  if (kind === "past") panelInner.innerHTML = pastTemplate();
  if (kind === "encounter") panelInner.innerHTML = encounterTemplate();
  if (kind === "settings") panelInner.innerHTML = settingsTemplate();
  panel.classList.add("is-open");
  panel.setAttribute("aria-hidden", "false");
  window.setTimeout(() => panelInner.querySelector("button, textarea, input")?.focus(), 80);
}

function closePanel() {
  panel.classList.remove("is-open");
  panel.setAttribute("aria-hidden", "true");
  state.panel = null;
  app.removeAttribute("data-panel");
  window.setTimeout(() => document.querySelectorAll(".home-card").forEach((card) => card.classList.remove("is-picked")), 240);
}

function showToast(message) {
  window.clearTimeout(toastTimer);
  toast.textContent = message;
  toast.classList.add("is-visible");
  toastTimer = window.setTimeout(() => toast.classList.remove("is-visible"), 1800);
}

function switchVariant(direction) {
  const current = variants.findIndex((item) => item.id === state.variant);
  state.variant = variants[(current + direction + variants.length) % variants.length].id;
  renderState();
}

function hapticConfirm() {
  if (typeof navigator.vibrate === "function") navigator.vibrate(8);
}

function dismissGestureHint() {
  gestureHint.classList.add("is-hidden");
}

function beginGesture(event, card) {
  if (state.panel || card.dataset.slot !== "0" || event.button > 0) return;
  dismissGestureHint();
  gesture = { card, pointerId: event.pointerId, startX: event.clientX, startY: event.clientY, startTime: performance.now(), dx: 0, dy: 0, moved: false, crossed: false };
  card.setPointerCapture?.(event.pointerId);
  card.classList.add("is-dragging");
}

function updateGesture(event) {
  if (!gesture || event.pointerId !== gesture.pointerId) return;
  const { card } = gesture;
  gesture.dx = event.clientX - gesture.startX;
  gesture.dy = event.clientY - gesture.startY;
  if (Math.abs(gesture.dx) > 5 || Math.abs(gesture.dy) > 5) gesture.moved = true;
  if (!gesture.moved) return;
  event.preventDefault();
  const width = card.getBoundingClientRect().width || 320;
  const threshold = Math.max(82, width * 0.28);
  const progress = Math.min(Math.abs(gesture.dx) / threshold, 1);
  const rotation = Math.max(-6, Math.min(6, (gesture.dx / width) * 5));
  card.style.setProperty("--drag-x", `${gesture.dx}px`);
  card.style.setProperty("--drag-y", `${Math.max(-14, Math.min(14, gesture.dy * 0.12))}px`);
  card.style.setProperty("--drag-rotate", `${rotation}deg`);
  renderDeck(progress);
  if (!gesture.crossed && Math.abs(gesture.dx) >= threshold) {
    gesture.crossed = true;
    hapticConfirm();
  }
}

function finishGesture(event) {
  if (!gesture || event.pointerId !== gesture.pointerId) return;
  const current = gesture;
  const card = current.card;
  const elapsed = Math.max(1, performance.now() - current.startTime);
  const velocity = Math.abs(current.dx) / elapsed;
  const width = card.getBoundingClientRect().width || 320;
  const threshold = Math.max(82, width * 0.28);
  const shouldSwipe = current.moved && (Math.abs(current.dx) >= threshold || velocity >= 0.8);
  card.releasePointerCapture?.(event.pointerId);
  card.classList.remove("is-dragging");
  gesture = null;

  if (!shouldSwipe) {
    renderDeck();
    if (current.moved) state.suppressClickUntil = Date.now() + 450;
    return;
  }

  state.suppressClickUntil = Date.now() + 520;
  const direction = current.dx >= 0 ? 1 : -1;
  card.classList.add("is-flinging");
  card.style.setProperty("--drag-x", `${direction * width * 1.35}px`);
  card.style.setProperty("--drag-y", `${Math.max(-22, Math.min(22, current.dy * 0.16))}px`);
  card.style.setProperty("--drag-rotate", `${direction * 10}deg`);
  card.style.opacity = "0";
  card.style.pointerEvents = "none";

  window.setTimeout(() => {
    state.deck.push(state.deck.shift());
    card.classList.remove("is-flinging");
    renderDeck();
  }, 300);
}

document.querySelectorAll(".home-card").forEach((card) => {
  card.addEventListener("pointerdown", (event) => beginGesture(event, card));
  card.addEventListener("pointermove", updateGesture, { passive: false });
  card.addEventListener("pointerup", finishGesture);
  card.addEventListener("pointercancel", finishGesture);
  card.addEventListener("click", () => {
    if (Date.now() < state.suppressClickUntil) return;
    card.classList.add("is-picked");
    const delay = window.matchMedia("(prefers-reduced-motion: reduce)").matches ? 0 : 250;
    window.setTimeout(() => openPanel(card.dataset.card), delay);
  });
});

document.querySelector("#previousVariant").addEventListener("click", () => switchVariant(-1));
document.querySelector("#nextVariant").addEventListener("click", () => switchVariant(1));
document.querySelector("#toggleState").addEventListener("click", () => setSurface(state.surface === "quiet" ? "returned" : "quiet"));

panel.addEventListener("click", (event) => {
  const target = event.target.closest("[data-action], [data-policy], [data-recall-state]");
  if (!target) return;
  const action = target.dataset.action;

  if (target.dataset.policy) {
    state.leavePolicy = target.dataset.policy;
    renderLeaveStep();
    return;
  }

  if (target.dataset.recallState) {
    state.recallState = target.dataset.recallState;
    panelInner.innerHTML = recallTemplate();
    return;
  }

  if (action === "close") closePanel();
  if (action === "leave-compose") { state.leaveStep = "compose"; renderLeaveStep(); }
  if (action === "save-draft") {
    state.memoryDraft = panelInner.querySelector("#memoryDraft")?.value || "";
    state.memoryTitle = panelInner.querySelector("#memoryTitle")?.value || "";
    state.leaveStep = "draft";
    closePanel();
    showToast("草稿已保存");
  }
  if (action === "save-exit") {
    state.memoryDraft = panelInner.querySelector("#memoryDraft")?.value || "";
    state.memoryTitle = panelInner.querySelector("#memoryTitle")?.value || "";
    state.leaveStep = "draft";
    closePanel();
    showToast("草稿已保存");
  }
  if (action === "leave-draft") { captureCueEdits(); state.leaveStep = "draft"; renderLeaveStep(); }
  if (action === "leave-cues") {
    state.memoryDraft = panelInner.querySelector("#memoryDraft")?.value || state.memoryDraft;
    state.memoryTitle = panelInner.querySelector("#memoryTitle")?.value || state.memoryTitle;
    captureCueEdits();
    state.leaveStep = "cues";
    renderLeaveStep();
  }
  if (action === "remove-cue") {
    captureCueEdits();
    state.cues = state.cues.filter((cue) => cue.id !== target.dataset.cueId);
    renderLeaveStep();
  }
  if (action === "add-cue") {
    captureCueEdits();
    const id = `custom-${Date.now()}`;
    state.cues.push({ id, label: "自定义", value: "", source: "你添加的" });
    renderLeaveStep();
    window.setTimeout(() => panelInner.querySelector(`[data-cue-input="${id}"]`)?.focus(), 40);
  }
  if (action === "leave-policy") { state.leaveStep = "policy"; renderLeaveStep(); }
  if (action === "leave-review") { captureCueEdits(); state.leaveStep = "review"; renderLeaveStep(); }
  if (action === "seal-memory") { state.leaveStep = "done"; renderLeaveStep(); showToast("这段记忆已封存"); }

  if (action === "encounter-now") { setSurface("returned"); closePanel(); showToast("有一页被选中了"); }
  if (action === "defer") { setSurface("quiet"); closePanel(); showToast("它重新回到安静里了"); }
  if (action === "start-recall") { state.panel = "recall"; panelInner.innerHTML = recallTemplate(); }
  if (action === "next-cue") {
    const cueTotal = getRecallCues().length;
    if (state.cueLevel < cueTotal) { state.cueLevel += 1; panelInner.innerHTML = recallTemplate(); }
    else { state.recallState = "NONE"; panelInner.innerHTML = revealTemplate(); }
  }
  if (action === "submit-recall") panelInner.innerHTML = revealTemplate();
  if (action === "reflection") panelInner.innerHTML = reflectionTemplate();
  if (action === "finish") { setSurface("quiet"); closePanel(); showToast("这段记忆重新沉下去了"); }
  if (action === "open-memory") { state.panel = "memory-detail"; panelInner.innerHTML = memoryDetailTemplate(); }
  if (action === "setting-toggle") {
    const switchEl = target.querySelector(".switch");
    if (switchEl) { switchEl.classList.toggle("is-on"); switchEl.textContent = switchEl.classList.contains("is-on") ? "开" : "关"; }
  }
});

panel.addEventListener("input", (event) => {
  if (event.target.id === "memoryDraft") {
    state.memoryDraft = event.target.value;
    const status = panelInner.querySelector("#saveStatus");
    if (status) {
      status.textContent = "已自动保存";
      status.classList.add("is-saved");
    }
  }
  if (event.target.id === "memoryTitle") state.memoryTitle = event.target.value;
});

document.addEventListener("keydown", (event) => {
  const tag = document.activeElement?.tagName;
  const isEditing = tag === "INPUT" || tag === "TEXTAREA" || document.activeElement?.isContentEditable;
  if (isEditing) return;
  if (event.key === "Escape" && state.panel) { closePanel(); return; }
  if (state.panel) return;
  if (event.key === "ArrowLeft") switchVariant(-1);
  if (event.key === "ArrowRight") switchVariant(1);
});

document.querySelector(".settings-button").addEventListener("click", () => openPanel("settings"));
returnSignal.addEventListener("click", () => {
  state.deck = ["encounter", ...state.deck.filter((kind) => kind !== "encounter")];
  renderDeck();
  openPanel("encounter");
});

resetDeck();
renderState();
