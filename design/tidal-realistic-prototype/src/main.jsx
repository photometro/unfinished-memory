'use client';

// PROTOTYPE ONLY. A/B/C on the existing route; all transactions and events are in-memory.
// Question: can writing and sealing have a clear default path without a wall of options?
// No algorithm thresholds, cooldown/defer durations, AI requests, database, location or native UI.
import React, { useEffect, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { AnimatePresence, LayoutGroup, motion, useMotionValue, useReducedMotion, useSpring } from 'framer-motion';
import { ArrowLeft, ArrowRight, ArrowUpRight, Check, IconContext, Moon, Plus, SlidersHorizontal, Sun } from '@phosphor-icons/react';
import { TideReveal } from './Ocean.jsx';
import { captureSealPage } from './PageFold.jsx';
import { RealisticSurface } from './RealisticSurface.jsx';
import { DAYLIGHT, SHORE_MASTER_CANDIDATE } from './daylight.js';
import { DaylightSelector, AssetComparison } from './DaylightSelector.jsx';
import { useEnvironment } from './useEnvironment.js';
import { CurrentField, DriftingMemory, MemoryVessel, SealJourney } from './MemoryMotion.jsx';
import { blankDraft, mergeDraft, writingError, hasDraftContent, photoSelectionError } from './writing-state.js';
import { selectedCueTexts } from './cue-state.js';
import { LeavePage, PolicyPage, BackgroundPage, PreferencePage, ReviewPage } from './LeaveFlow.jsx';
import './styles.css';
import './journey.css';
import './leave.css';

const VARIANTS = {
  A: { name: '横向潮滩', caption: '在熟悉的岸边，等待一段过去。' },
  B: { name: '纵向潮沟', caption: '水道在右，操作留在左岸。' },
  C: { name: '留白海湾', caption: '远处是海，近处是现在。' },
};
const POLICIES = {
  serendipity: { label: '偶然遇见', hint: '允许它在未来某个时刻主动回来。' },
  fixed: { label: '留给某一天', hint: '指定日期的界面演示；到期规则尚未接入。' },
  quiet: { label: '静静保存', hint: '不主动回来，仍可在「过去」查看。' },
};
const PREFERENCES = [
  { value: 'light', label: '轻一点', hint: '更突出直接查看，不必先回忆。' },
  { value: 'normal', label: '让我先想想', hint: '给一次自由回忆的机会。' },
  { value: 'deep', label: '我喜欢慢慢想', hint: '把主动请求线索放得更明显。' },
];
const INITIAL_MEMORIES = [
  { id: 'fictional-station', year: '2021', season: '夏天', title: '雨停以后，老车站', occurredAt: '2021 · 夏天', capturedAt: '2021', provenance: 'now', city: '', policy: 'serendipity', preference: 'inherit', source: '虚构演示记录', content: '那天的雨停得很晚。我们没有赶上最后一班车，就在老车站旁边坐了一会儿。\n\n铁皮屋檐还在滴水，远处有一盏灯。我不记得后来聊了些什么，只记得那段时间忽然变得很慢。', cues: ['一处老车站。', '屋檐下，还有没有滴完的雨。'], sessions: [], reflections: [] },
  { id: 'fictional-window', year: '2023', season: '秋天', title: '搬走之前的窗台', occurredAt: '2023 · 秋天', capturedAt: '2026', provenance: 'backfill', city: '', policy: 'quiet', preference: 'inherit', source: '虚构补记示例', content: '收拾完最后一只纸箱，窗台上只剩一圈淡淡的杯印。\n\n房间空了以后，反而听见了平时没有注意过的声音。', cues: ['搬走之前。', '窗台上留下的一圈杯印。'], sessions: [], reflections: [] },
  { id: 'fictional-road', year: '2024', season: '春天', title: '绕远的一小段路', occurredAt: '2024 · 春天', capturedAt: '2024', provenance: 'now', city: '', policy: 'serendipity', preference: 'inherit', source: '虚构演示记录', content: '今天没有走熟悉的那条路。拐角的树刚刚长出新叶，路边有人搬着一盆很大的植物。\n\n绕远了一点，但并不想赶时间。', cues: ['一段绕远的路。', '拐角刚长出新叶的树。'], sessions: [], reflections: [] },
];
const transition = { type: 'spring', stiffness: 230, damping: 29 };
const today = () => new Date().toLocaleDateString('zh-CN');
const newEncounter = memoryId => ({ id: 'demo-session-' + Date.now(), memoryId, stage: 'glimpse', text: '', visibleCues: [], snapshots: [], locked: false, revealed: false, enteredRecall: false, outcome: null });
const takeSnapshot = session => {
  if (!session.text.trim()) return session.snapshots;
  const last = session.snapshots.at(-1);
  if (last?.text === session.text && last?.cues.length === session.visibleCues.length) return session.snapshots;
  return [...session.snapshots, { text: session.text, cues: [...session.visibleCues] }];
};

function MagneticButton({ children, className = '', reduced = false, ...props }) {
  const x = useMotionValue(0), y = useMotionValue(0);
  const softX = useSpring(x, { stiffness: 220, damping: 24 }), softY = useSpring(y, { stiffness: 220, damping: 24 });
  return <motion.button type="button" {...props} className={'pressable ' + className} style={{ x: softX, y: softY }} whileTap={reduced ? {} : { scale: .975 }} transition={transition}
    onPointerMove={event => { if (reduced || event.pointerType !== 'mouse') return; const b = event.currentTarget.getBoundingClientRect(); x.set((event.clientX - b.left - b.width / 2) * .055); y.set((event.clientY - b.top - b.height / 2) * .055); }}
    onPointerLeave={() => { x.set(0); y.set(0); }}>{children}</motion.button>;
}
function PanelHeader({ title, onBack, backLabel = '返回岸边', children, reduced }) {
  return <header className="panel-header flex shrink-0 items-center justify-between gap-3 px-6 py-5">
    <div className="flex min-w-0 items-center gap-2"><MagneticButton reduced={reduced} className="icon-button -ml-2" aria-label={backLabel} disabled={!onBack} onClick={onBack}><ArrowLeft size={20} /></MagneticButton><h2 data-page-title tabIndex={-1} className="truncate text-[14px] font-[500]">{title}</h2></div>{children}
  </header>;
}
function EncounterChoices({ preference, onAction, reduced, exhausted }) {
  return <div className="encounter-actions mt-5 grid grid-cols-2 gap-x-3 gap-y-2">
    <MagneticButton reduced={reduced} className={preference === 'normal' ? 'encounter-primary' : 'encounter-secondary'} onClick={() => onAction('recall')}>想一会儿<ArrowUpRight size={15} /></MagneticButton>
    <MagneticButton reduced={reduced} className={preference === 'deep' ? 'encounter-primary' : 'encounter-secondary'} disabled={exhausted} onClick={() => onAction('cue')}>{exhausted ? '线索到这里' : '再给一点'}<Plus size={14} /></MagneticButton>
    <MagneticButton reduced={reduced} className={preference === 'light' ? 'encounter-primary' : 'encounter-secondary'} onClick={() => onAction('read')}>直接看看<ArrowRight size={14} /></MagneticButton>
    <MagneticButton reduced={reduced} className="encounter-secondary" onClick={() => onAction('defer')}>让它再待一会</MagneticButton>
  </div>;
}
function HomeMessage({ encounter, memory, empty, preference, onAction, reduced, compact = false }) {
  const arriving = encounter && !encounter.revealed && encounter.stage !== 'deferred' && memory;
  if (!arriving) return <><p className="eyebrow">{empty ? '先留下一些东西，剩下的交给时间' : '此刻，没有需要赶赴的过去'}</p><h2 className={'home-title ' + (compact ? 'compact' : '')}>{empty ? <>留下一段，<br />给以后的自己。</> : <>今天海面<br />很安静。</>}</h2><p className="mt-4 text-[13px] leading-[1.9] text-[var(--muted)]">{empty ? '一个现在，一个人，或一件很普通的小事。' : '你可以留下一点，也可以只是停一会儿。'}</p></>;
  return <><p className="eyebrow"><span className="signal-dot mr-2 inline-block" />有一段过去，今天回来了</p><h2 className={'home-title ' + (compact ? 'compact' : '')}>一段过去，<br />抵达岸边。</h2><p className="mt-4 text-[13px] text-[var(--muted)]">{memory.occurredAt || '一段曾经'}{memory.id.startsWith('fictional-') ? ' · 虚构示例' : ' · 本次输入'}</p>
    {encounter.visibleCues.length > 0 && <p className="visible-cue mt-3 text-[13px] leading-6">{encounter.visibleCues.at(-1)}</p>}
    <EncounterChoices {...{ preference, onAction, reduced }} exhausted={encounter.visibleCues.length >= memory.cues.length} />
  </>;
}
function HomeNavigation({ onAction, reduced }) {
  return <nav className="home-nav" aria-label="主要操作"><MagneticButton reduced={reduced} className="leave-action" onClick={() => onAction('leave')}><Plus size={18} /><span>留下一段</span></MagneticButton><MagneticButton reduced={reduced} className="past-action" onClick={() => onAction('past')}><span>过去</span><ArrowUpRight size={16} /></MagneticButton></nav>;
}
function VariantA(props) {
  return <><section className="message-a home-message"><HomeMessage {...props} /></section><p className="terrain-whisper" aria-hidden="true">潮水退去，痕迹留下</p><HomeNavigation {...props} /></>;
}
function VariantB(props) {
  return <><section className="message-b home-message"><HomeMessage {...props} compact /></section><p className="terrain-whisper" aria-hidden="true">时间经过的地方</p><HomeNavigation {...props} /></>;
}
function VariantC(props) {
  return <><section className="message-c home-message"><HomeMessage {...props} compact /></section><HomeNavigation {...props} /></>;
}
function RecallPage({ memory, encounter, onChange, onCue, onRead, onBack, onDefer, onStop, reduced }) {
  const stopped = encounter.stage === 'stopped';
  return <section className="function-page recall-page absolute inset-0 flex flex-col"><PanelHeader title="想一会儿" backLabel="返回岸边，保留进度" {...{ onBack, reduced }} />
    <div className="page-scroll min-h-0 flex-1 overflow-y-auto px-8 pb-5 pt-4"><p className="eyebrow">{memory.occurredAt || '一段曾经'} · 原记录尚未展开</p><h3 className="page-heading">{stopped ? '那就停在这里。' : '你还能想起什么？'}</h3>
      {encounter.visibleCues.length > 0 && <section className="cue-list"><p className="minor-label mb-3">你主动要的线索</p>{encounter.visibleCues.map((cue, i) => <p key={i} className="visible-cue mt-3 text-[14px] leading-7"><span className="mr-2 text-[11px] text-[var(--muted)]">{i + 1}</span>{cue}</p>)}</section>}
      {!stopped && <><label htmlFor="recall-input" className="content-group group-label block">现在的回忆 <span className="optional-label">可以留空</span></label><textarea id="recall-input" className="recall-input mt-4 min-h-[180px] w-full resize-none bg-transparent text-[17px] leading-[1.9] outline-none" value={encounter.text} onChange={e => onChange(e.target.value)} placeholder={'想到多少，就写多少。\n不必记得准确。'} /><button type="button" className="text-button text-[13px]" onClick={onStop}>想不起来，就到这里</button></>}
      <p className="prototype-note mt-4">{stopped ? '不会继续追问。你可以查看，也可以让它再待一会。' : '只有你主动请求，才展示下一条线索。不同提示下的回忆会分别保留在本次演示中。'}</p>
    </div><footer className="page-footer shrink-0 px-8 pb-7 pt-4"><MagneticButton reduced={reduced} className="solid-action w-full" onClick={onRead}>看看那时候留下的<ArrowRight size={17} /></MagneticButton><div className="mt-2 flex justify-between gap-3">{!stopped && <button type="button" className="text-button text-[12px]" disabled={encounter.visibleCues.length >= memory.cues.length} onClick={onCue}>{encounter.visibleCues.length >= memory.cues.length ? '线索到这里' : '再给一点'}</button>}<button type="button" className="text-button ml-auto text-[12px]" onClick={onDefer}>让它再待一会</button></div></footer>
  </section>;
}
function ReadPage({ memory, session, origin, onBack, onEnd, onReflect, onToast, reduced }) {
  const [responding, setResponding] = useState(false), [response, setResponse] = useState('');
  const [saved, setSaved] = useState(false);
  return <section className="function-page reading-page absolute inset-0 flex flex-col"><PanelHeader title="靠岸之后" backLabel={origin === 'past' ? '返回过去' : '返回岸边，结束本次遇见'} reduced={reduced} onBack={onBack} />
    <article className="page-scroll min-h-0 flex-1 overflow-y-auto px-8 pb-8 pt-4"><p className="eyebrow">{origin === 'past' ? '主动查看 · 不是随机浮现' : '原记录已经展开'}</p><p className="mt-5 text-[12px] text-[var(--muted)]">{memory.occurredAt || '发生时间未补充'}</p><h3 className="mt-3 text-[26px] leading-[1.55]">{memory.title}</h3><p className="mt-3 text-[10px] leading-5 text-[var(--muted)]">{memory.provenance === 'backfill' ? memory.capturedAt + ' 记录对 ' + memory.occurredAt + ' 的回忆' : memory.provenance === 'imported' ? '导入的旧记录 · 原始时间仍需确认' : '记录于 ' + memory.capturedAt}<br />{memory.source}</p>
      <section className="record-section record-original mt-7"><h4 className="group-label">当时留下</h4><p className="group-helper">封存原文 · 不被后来的回忆覆盖</p><div className="mt-4 whitespace-pre-line text-[16px] leading-[2.05]">{memory.content}</div>{memory.photos?.length > 0 && <div className="record-photos">{memory.photos.map(photo => <img key={photo.id} src={photo.url} alt={photo.name} />)}</div>}</section>
      {session?.snapshots.length > 0 && <section className="record-section mt-9"><h4 className="group-label">这次想起</h4><p className="group-helper">揭晓前已锁定，与原记录分开保留</p>{session.snapshots.map((snapshot,i) => <div key={i} className="recall-snapshot mt-5"><p className="minor-label">{i === 0 ? '最初写下的' : '获得线索后'} · 当时可见 {snapshot.cues.length} 条线索</p><p className="mt-2 whitespace-pre-line text-[15px] leading-7">{snapshot.text}</p></div>)}</section>}
      {session?.outcome === 'DIRECT_REVEAL' && <p className="prototype-note mt-7">这一次选择直接查看，没有生成空白回忆记录。</p>}
      <section className="record-section mt-9"><h4 className="group-label">现在回应 <span className="optional-label">可跳过</span></h4>{!responding ? <button type="button" className="text-button mt-3 flex items-center gap-2 text-[14px]" onClick={() => { setResponding(true); setSaved(false); }}>{saved ? '再留一点回应' : '写一点现在的回应'}<Plus size={14} /></button> : <><label htmlFor="reflection" className="mt-4 block text-[13px]">现在想说的话</label><textarea id="reflection" className="mt-3 min-h-24 w-full resize-none bg-transparent text-[16px] leading-7 outline-none" value={response} onChange={e => setResponse(e.target.value)} placeholder="不需要得出结论。" /><button type="button" className="text-button text-[13px]" disabled={!response.trim()} onClick={() => { onReflect(response); setSaved(true); setResponding(false); setResponse(''); }}>保留回应 · 演示</button></>}
        {memory.reflections.map((item,i) => <p key={i} className="reflection-record mt-4 whitespace-pre-line text-[15px] leading-7">{item.text}</p>)}
        {saved && <p role="status" className="group-helper mt-3">回应已在本次演示保留。</p>}
      </section>
      {memory.sessions.length > 0 && <details className="record-section mt-8"><summary className="text-[12px]">此前的遇见记录 · {memory.sessions.length} 次演示</summary>{memory.sessions.map(item => <div key={item.id} className="mt-3 text-[11px] leading-6 text-[var(--muted)]">{item.outcome === 'DEFERRED' ? '选择暂缓' : item.outcome === 'DIRECT_REVEAL' ? '直接查看' : '曾经回忆'} · {item.snapshots.length} 份回忆快照</div>)}</details>}
      <button type="button" className="preference-link mt-7 w-full" onClick={() => onToast('AI 差异分析尚未接入。没有发送原文或回忆。')}><span>看看不同时间的表达</span><span className="text-[10px]">AI 分析未接入</span></button>
    </article><footer className="page-footer shrink-0 px-8 pb-7 pt-3"><MagneticButton reduced={reduced} className="solid-action w-full" onClick={onEnd}>{origin === 'past' ? '返回过去' : '就看到这里'}<Check size={17} /></MagneticButton></footer>
  </section>;
}
function PastPage({ memories, query, setQuery, filter, setFilter, scrollRef, onBack, onRead, reduced }) {
  const container = useRef(null);
  useEffect(() => { container.current.scrollTop = scrollRef.current; }, [scrollRef]);
  const list = memories.filter(m => (filter === 'all' || (filter === 'quiet' ? m.policy === 'quiet' : m.policy !== 'quiet')) && (m.title + m.content + m.occurredAt + m.city).includes(query));
  const groups = [...new Set(list.map(m => m.year))];
  return <section className="function-page archive-page absolute inset-0 flex flex-col"><PanelHeader title="过去" {...{ onBack, reduced }} /><div ref={container} className="page-scroll min-h-0 flex-1 overflow-y-auto px-8 pb-10 pt-4" onScroll={e => { scrollRef.current = e.currentTarget.scrollTop; }}><p className="eyebrow">主动走进时间，不必等它回来</p><h3 className="mt-4 text-[26px] leading-[1.6]">一些东西，<br />仍然留在这里。</h3>
    <label htmlFor="memory-search" className="mt-6 block text-[11px] text-[var(--muted)]">寻找一段记忆</label><input id="memory-search" className="search-input mt-2 w-full bg-transparent py-3 text-[14px] outline-none" placeholder="文字、年份或地点" value={query} onChange={e => { setQuery(e.target.value); scrollRef.current = 0; }} />
    <div className="archive-filters mt-4 flex gap-2">{[['all','全部'],['drift','交给时间'],['quiet','静静保存']].map(([key,label]) => <button type="button" key={key} aria-pressed={filter === key} className={filter === key ? 'selected' : ''} onClick={() => setFilter(key)}>{label}</button>)}</div>
    <p className="prototype-note mt-4">查找与主动阅读不需要追逐漂泊物。预置内容均为虚构；新输入刷新后清空。</p>
    {list.length ? groups.map(year => <section key={year} className="mt-7"><p className="archive-year">{year}</p>{list.filter(m => m.year === year).map(memory => <button type="button" key={memory.id} className="past-row flex w-full items-center gap-4 py-5 text-left" onClick={() => onRead(memory)}><MemoryVessel memoryId={memory.id} className="archive-carrier" reduced /><span className="min-w-0 flex-1"><span className="block text-[14px] leading-6">{memory.title}</span><span className="mt-1 block text-[10px] leading-5 text-[var(--muted)]">{memory.occurredAt || '时间未补充'} · {POLICIES[memory.policy].label}{memory.provenance === 'backfill' ? ' · 后来补记' : ''}</span></span><ArrowUpRight size={17} /></button>)}</section>) : <div className="py-12"><p className="text-[19px]">{memories.length ? '没有找到这段痕迹。' : '这里还没有留下的过去。'}</p><p className="mt-3 text-[12px] leading-6 text-[var(--muted)]">{memories.length ? '换一个词，或清空搜索看看。' : '第一段封存之后，会出现在这里。'}</p>{memories.length > 0 && <button type="button" className="text-button mt-4 text-[12px]" onClick={() => { setQuery(''); setFilter('all'); }}>清空搜索</button>}</div>}
  </div></section>;
}
function OceanPage({ memory, reduced, onBack }) {
  return <section className="ocean-page absolute inset-0 flex flex-col"><CurrentField reduced={reduced} /><PanelHeader title="纸船与洋流 · 设计预览" reduced={reduced} onBack={onBack} /><div className="relative px-8 pt-7"><p className="eyebrow">仅供检查视觉，不是 App 的独立入口</p><h3 className="mt-5 text-[28px] leading-[1.6]">纸张收好，<br />时间流动。</h3><p className="mt-5 max-w-[24ch] text-[13px] leading-7 text-[var(--muted)]">一段记忆，折成一艘暖白纸船。船舷与折面留下，原文藏在里面。漂泊不标记重要性，也不表示到岸进度。</p></div><DriftingMemory memory={memory} reduced={reduced} /><div className="ocean-observation-footer mt-auto px-8 pb-9"><p className="text-[13px] leading-6">{memory ? '纸船始终对应本次内存中的同一段记忆。' : '先留下一段，再检查纸船。'}</p><p className="prototype-note mt-2">流迹与纸船共享洋流场；循环仅供设计观察，不运行浮现算法。</p></div></section>;
}
function ToggleRow({ label, hint, checked, onChange, disabled = false, reduced = false }) {
  return <div className="toggle-row flex items-center justify-between gap-4 py-5"><div><p className="text-[15px]">{label}</p><p className="mt-2 text-[12px] leading-5 text-[var(--muted)]">{hint}</p></div><button type="button" role="switch" aria-label={label} aria-checked={checked} disabled={disabled} onClick={onChange} className={'toggle shrink-0 ' + (checked ? 'on' : '')}><motion.span animate={{ x: checked ? 16 : 0 }} transition={{duration: reduced ? 0 : .16}} /></button></div>;
}
function SettingsPage({ forceDark, changeDark, reduced, setManualReduced, systemReduced, lowTransparency, setLowTransparency, preference, setPreference, allowEncounter, setAllowEncounter, onBack }) {
  return <section className="function-page settings-page absolute inset-0 flex flex-col"><PanelHeader title="设置" {...{ onBack, reduced }} /><div className="page-scroll min-h-0 flex-1 overflow-y-auto px-8 pb-9 pt-4"><p className="eyebrow">控制权始终留在你这里</p><h3 className="page-heading">按你的节奏。</h3>
    <fieldset className="settings-section settings-first"><legend className="group-label">怎样遇见过去</legend>{PREFERENCES.map(item => <label className="policy-option" key={item.value}><input type="radio" name="global-preference" checked={preference === item.value} onChange={() => setPreference(item.value)} /><span><span className="option-title">{item.label}</span><span className="option-hint">{item.hint}</span></span></label>)}</fieldset>
    <ToggleRow reduced={reduced} label="允许上岸演示" hint="仅控制预览入口，不运行随机浮现或通知" checked={allowEncounter} onChange={() => setAllowEncounter(!allowEncounter)} />
    <section className="settings-section"><h4 className="group-label">显示与舒适度</h4><ToggleRow reduced={reduced} label="深色模式" hint="开启呈现夜色；关闭恢复当地时间的光照，固定岸线不变" checked={forceDark} onChange={() => changeDark(!forceDark)} /><ToggleRow reduced={reduced} label="减少动画" hint={systemReduced ? '系统设置优先；直接呈现状态' : '减少入场与漂泊，不改变操作结果'} checked={reduced} disabled={systemReduced} onChange={() => setManualReduced(!reduced)} /><ToggleRow reduced={reduced} label="降低透明度" hint="操作层变得更实，方便阅读" checked={lowTransparency} onChange={() => setLowTransparency(!lowTransparency)} /></section>
    <section className="settings-section settings-explanation"><h4 className="minor-label">AI、情境与隐私 · 当前边界</h4><p className="mt-3 text-[12px] leading-7 text-[var(--muted)]">AI 配置和分析暂未接入，线索建议是本地演示文案。原型不定位、不读取通讯录，也不会把书写或回忆发送到外部服务。</p></section>
  </div></section>;
}
function PrototypeSwitcher({ variant, changeVariant, reduced, pending }) {
  useEffect(() => { if (!import.meta.env.DEV) return; const listener = event => { if (pending || event.target.closest('input,textarea,select,[contenteditable],[role="dialog"]')) return; if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') { event.preventDefault(); const keys = Object.keys(VARIANTS), direction = event.key === 'ArrowLeft' ? -1 : 1; changeVariant(keys[(keys.indexOf(variant) + direction + keys.length) % keys.length]); } }; window.addEventListener('keydown', listener); return () => window.removeEventListener('keydown', listener); }, [variant, changeVariant, pending]);
  if (!import.meta.env.DEV) return null;
  const cycle = direction => { const keys = Object.keys(VARIANTS); changeVariant(keys[(keys.indexOf(variant) + direction + keys.length) % keys.length]); };
  return <aside className="prototype-switcher fixed bottom-5 left-1/2 flex -translate-x-1/2 items-center gap-2 rounded-full px-2 py-1.5" aria-label="原型构图切换"><MagneticButton reduced={reduced} className="switcher-arrow" aria-label="上一种构图" disabled={pending} onClick={() => cycle(-1)}><ArrowLeft size={17} /></MagneticButton><span className="min-w-[154px] text-center text-[12px]">{variant} · {VARIANTS[variant].name}</span><MagneticButton reduced={reduced} className="switcher-arrow" aria-label="下一种构图" disabled={pending} onClick={() => cycle(1)}><ArrowRight size={17} /></MagneticButton></aside>;
}

function App() {
  const variantFromUrl = () => { const key = new URLSearchParams(location.search).get('variant'); return VARIANTS[key] ? key : 'A'; };
  const [variant, setVariant] = useState(variantFromUrl);
  const shoreMaster = SHORE_MASTER_CANDIDATE;
  const [routes, setRoutes] = useState(['home']);
  const route = routes.at(-1);
  const [intro, setIntro] = useState(true), [stage, setStage] = useState('sea'), [replayKey, setReplayKey] = useState(0);
  const [lowTransparency, setLowTransparency] = useState(false), [manualReduced, setManualReduced] = useState(false);
  const systemReduced = !!useReducedMotion(), reduced = systemReduced || manualReduced;
  const { period, source: lightSource, forceDark, changeDark, changePeriod, clockPreview, changeClock, tidePaused, tideValue, targetValue } = useEnvironment(intro, reduced);
  const [surfaceReady, setSurfaceReady] = useState(false);
  const [previewSize, setPreviewSize] = useState('elastic');
  const [largeText, setLargeText] = useState(false), [safePreview, setSafePreview] = useState(false);
  const frameDimensions = previewSize === 'elastic' ? {} : { width: previewSize.split('x')[0] + 'px', height: previewSize.split('x')[1] + 'px' };
  const [preference, setPreference] = useState('normal'), [allowEncounter, setAllowEncounter] = useState(true);
  const [draft, setDraft] = useState(blankDraft), [memories, setMemories] = useState(INITIAL_MEMORIES);
  const [encounter, setEncounter] = useState(null), [readingId, setReadingId] = useState(null), [readOrigin, setReadOrigin] = useState('past');
  const [sealedId, setSealedId] = useState(null), [pending, setPending] = useState(false), [commitError, setCommitError] = useState('');
  const [sealSnapshot, setSealSnapshot] = useState(null);
  const [query, setQuery] = useState(''), [filter, setFilter] = useState('all');
  const [toast, setToast] = useState(''), [failCommit, setFailCommit] = useState(false), [emptyPreview, setEmptyPreview] = useState(false);
  const skipRef = useRef(null), commitTimer = useRef(null), commitLock = useRef(false), pastScrollRef = useRef(0), appRef = useRef(null);
  const encounterMemory = memories.find(m => m.id === encounter?.memoryId);
  const reading = memories.find(m => m.id === readingId);
  const sealed = memories.find(m => m.id === sealedId);
  const oceanMemory = [...memories].find(m => m.policy !== 'quiet');
  const visibleEncounter = encounter && !encounter.revealed && encounter.stage !== 'deferred' && encounterMemory;
  const resolvedPreference = encounterMemory?.preference !== 'inherit' && encounterMemory?.preference ? encounterMemory.preference : preference;
  const push = next => { if (commitLock.current) return; setToast(''); setRoutes(list => [...list, next]); };
  const back = () => { if (commitLock.current || pending) return; setRoutes(list => list.length > 1 ? list.slice(0, -1) : list); };
  const home = () => { if (commitLock.current) return; setRoutes(['home']); setSealSnapshot(null); };
  const updateDraft = patch => { setDraft(value => mergeDraft(value, patch)); setCommitError(''); };
  const addPhotos = async files => {
    const error = photoSelectionError(files, draft.photos.length);
    if (error) return error;
    const photos = await Promise.all(files.map(file => new Promise((resolve, reject) => {
      const reader = new FileReader(); reader.onerror = reject;
      reader.onload = () => resolve({id:crypto.randomUUID(), name:file.name, url:reader.result});
      reader.readAsDataURL(file);
    })));
    setDraft(value => mergeDraft(value, {photos:[...value.photos, ...photos]}));
    return '';
  };
  const removePhoto = id => setDraft(value => mergeDraft(value, {photos:value.photos.filter(photo => photo.id !== id)}));
  useEffect(() => () => clearTimeout(commitTimer.current), []);
  useEffect(() => { if (!toast) return; const timer = setTimeout(() => setToast(''), 3800); return () => clearTimeout(timer); }, [toast]);
  const focusPage = () => {
    const page = appRef.current?.querySelector('.content-stage > .panel-layer');
    // Entry completion must not steal focus from a field already being used.
    if (page?.contains(document.activeElement) && document.activeElement.matches('button,input,textarea,select,summary,a[href],[data-page-title]')) return;
    page?.querySelector('[data-page-title]')?.focus({ preventScroll: true });
  };
  useEffect(() => {
    const listener = event => { if (event.key !== 'Escape' || event.target.closest('[role="dialog"]')) return; if (intro) skipRef.current?.(); else if (route !== 'seal' && !pending) back(); };
    window.addEventListener('keydown', listener); return () => window.removeEventListener('keydown', listener);
  }, [intro, route, pending]);
  const changeVariant = next => { if (commitLock.current || !VARIANTS[next]) return; setVariant(next); const url = new URL(location.href); url.searchParams.set('variant', next); history.replaceState(null, '', url); };
  const createDemoEncounter = () => {
    if (commitLock.current) return;
    if (!allowEncounter) { setToast('请先在设置中允许上岸演示。'); return; }
    const memory = memories.find(m => m.policy === 'serendipity');
    if (!memory) { setToast('本次预览没有可用于偶然上岸演示的记忆。静存内容仍可主动查看。'); return; }
    skipRef.current?.(); setIntro(false); setEmptyPreview(false); setEncounter(newEncounter(memory.id)); home();
  };
  const requestCue = () => setEncounter(session => {
    if (!session || session.locked || !encounterMemory || session.visibleCues.length >= encounterMemory.cues.length) return session;
    return { ...session, stage: 'recalling', snapshots: takeSnapshot(session), visibleCues: [...session.visibleCues, encounterMemory.cues[session.visibleCues.length]] };
  });
  const saveSession = session => setMemories(list => list.map(m => m.id !== session.memoryId ? m : ({ ...m, sessions: [...m.sessions.filter(s => s.id !== session.id), session] })));
  const reveal = () => {
    if (!encounter || !encounterMemory) return;
    const updated = { ...encounter, stage: 'revealed', snapshots: takeSnapshot(encounter), locked: true, revealed: true, outcome: encounter.text.trim() ? 'CONTENT_PROVIDED' : encounter.enteredRecall ? 'NO_RECALL' : 'DIRECT_REVEAL' };
    setEncounter(updated); saveSession(updated); setReadingId(encounter.memoryId); setReadOrigin('encounter');
    setRoutes(['home', 'read']); setToast('');
  };
  const defer = () => {
    if (encounter) { const updated = { ...encounter, stage: 'deferred', outcome: 'DEFERRED', snapshots: takeSnapshot(encounter) }; setEncounter(updated); saveSession(updated); }
    home(); setToast('这次先到这里。已保留暂缓的演示结果；没有设置算法时长。');
  };
  const endReading = () => {
    if (readOrigin === 'past') { back(); return; }
    home(); setToast('本次遇见结束。冷却只作为流程状态展示，不运行计时规则。');
  };
  const onAction = type => {
    if (type === 'cue') { requestCue(); return; }
    if (type === 'read') { reveal(); return; }
    if (type === 'defer') { defer(); return; }
    if (type === 'recall') { setEncounter(s => ({ ...s, stage: 'recalling', enteredRecall: true })); push('recall'); return; }
    push(type);
  };
  const commit = () => {
    if (commitLock.current) return;
    const message = writingError('review', draft);
    if (message) { setCommitError(message); return; }
    commitLock.current = true; setPending(true); setCommitError('');
    commitTimer.current = setTimeout(() => {
      commitLock.current = false; setPending(false);
      if (failCommit) { setFailCommit(false); setCommitError('演示提交失败。内容仍在这里，没有封存或播放成功动画。'); return; }
      const id = 'demo-memory-' + Date.now(), occurredAt = draft.occurredAt.trim();
      const cues = selectedCueTexts(draft); // Only explicitly kept/entered cues; never a fabricated fallback.
      const memory = { id, title: draft.text.trim().slice(0,18) || '留下一组照片', content: draft.text.trim(), photos:draft.photos, year: occurredAt.match(/\d{4}/)?.[0] || '本次留下', season: '', occurredAt, capturedAt: today(), provenance: draft.provenance, city: draft.city, people: draft.people, policy: draft.policy, fixedDate: draft.fixedDate, preference: draft.preference, source: '本次输入 · 仅内存保留', cues, sessions: [], reflections: [] };
      setSealSnapshot(reduced ? null : captureSealPage(appRef.current?.querySelector('.review-page')));
      setMemories(list => [memory, ...list]); setSealedId(id); setDraft(blankDraft()); setEmptyPreview(false); setRoutes(['home','seal']);
    }, 600);
  };
  const finishSeal = () => { home(); setToast(sealed?.policy === 'quiet' ? '已在本次预览静存，可在「过去」查看。' : '已交给时间，仍可在「过去」主动查看。'); };
  const HomeVariant = variant === 'B' ? VariantB : variant === 'C' ? VariantC : VariantA;
  const routeLabel = { home:'岸边',leave:'书写',policy:'怎样回来',background:'补充背景',preference:'回忆偏好',review:'封存前',seal:'交给时间',ocean:'洋流',past:'过去',recall:'回忆',read:'原文与回应',settings:'设置' };
  const ritualSurface = route === 'seal' || route === 'ocean';
  const focusSurface = route !== 'home';
  return <IconContext.Provider value={{ weight:'light' }}><main className={'studio min-h-[100dvh] px-5 pb-24 pt-9 md:px-12' + (new URLSearchParams(location.search).get('view') === 'app' ? ' app-only' : '')}>
    <header className="studio-header mx-auto flex max-w-[1180px] items-center justify-between gap-5"><p className="text-[13px] font-[500] tracking-[.08em]">未完成<span className="ml-1 text-[#81918b]">_</span><span className="ml-5 text-[10px] font-normal tracking-[.13em] text-[#7b8782]">LEAVE STUDY / 18</span></p><a className="baseline-link text-[10px] text-[#687b74]" href="http://127.0.0.1:5188/?variant=A" target="_blank" rel="noreferrer">查看抽象原型<ArrowUpRight size={13} /></a></header>
    <div className="studio-grid mx-auto grid max-w-[1120px] items-start gap-12 pt-10 md:grid-cols-[minmax(260px,1fr)_minmax(320px,420px)] md:gap-20">
      <section className="study-panel max-w-[430px]"><fieldset className="contents" disabled={pending}><p className="text-[10px] tracking-[.16em] text-[#7b8782]">真实岸边 / 留下界面研究</p><h1 className="mt-5 text-[clamp(30px,3.4vw,42px)] font-[450] leading-[1.4] text-[#2b3938]">把一段记忆，<br />交给流动的时间。</h1><p className="mt-5 max-w-[32ch] text-[13px] leading-[2] text-[#687873]">写下 → 封存前 → 交给时间。<br />原文优先，线索可选，其他按需调整。<br />底色、折船与洋流保持。</p>
        <div className="study-section mt-7"><p className="mb-3 text-[10px] text-[#6d7e77]">构图候选 · 未定版</p><div className="flex gap-2">{Object.entries(VARIANTS).map(([key,item]) => <button className={'scene-pill ' + (variant === key ? 'selected' : '')} type="button" key={key} aria-pressed={variant === key} onClick={() => changeVariant(key)}>{key} · {item.name}</button>)}</div></div>
        <div className="study-section mt-7"><p className="mb-3 text-[10px] text-[#6d7e77]">三态光照 · 平静岸边</p><p className="text-[11px] leading-6 text-[#687873]">日光 06–14／斜阳 14–18／夜色 18–06。日光以你指定的图片为准，水面局部平静处理；配套开屏只播放一次退潮，首页岸线固定。</p></div>
        <div className="mt-6"><p className="mb-3 text-[10px] text-[#6d7e77]">光照预览 · {lightSource}</p><DaylightSelector period={period} onChange={changePeriod} /><button type="button" className="studio-button mt-3" onClick={() => { changeDark(false); changeClock(''); }}>恢复当地时间自动</button></div>
<details className="environment-test-controls mt-5"><summary>环境与窗口检查 · 仅设计工作台</summary><label>模拟当地时钟 · 持续推进<input aria-label="模拟当地时钟" type="datetime-local" step="1" value={clockPreview} onInput={e => changeClock(e.currentTarget.value)} onChange={e => changeClock(e.target.value)} /></label><p className="mt-3">首页潮汐已暂存 · 固定岸线，不叠加湿边或水域覆盖。</p><label>原型实际窗口<select aria-label="原型实际窗口" value={previewSize} onChange={e => setPreviewSize(e.target.value)}>{['elastic','320x568','360x640','390x844','430x932','768x1024'].map(s => <option key={s} value={s}>{s === 'elastic' ? '弹性 · 当前容器' : s}</option>)}</select></label><label><input type="checkbox" checked={largeText} onChange={e => setLargeText(e.target.checked)} />文字放大 130% · 检查</label><label><input type="checkbox" checked={safePreview} onChange={e => setSafePreview(e.target.checked)} />模拟安全区 · 上 44 / 下 34</label></details>
        <section className="study-section mt-7"><p className="mb-3 text-[10px] text-[#6d7e77]">演示控制 · 不属于 App 功能</p><div className="flex flex-wrap gap-2"><button type="button" className="studio-button" onClick={() => { skipRef.current?.(); setIntro(false); home(); setEncounter(null); }}>安静岸边</button><button type="button" className="studio-button primary" onClick={createDemoEncounter}>演示记忆上岸</button><button type="button" className="studio-button" onClick={() => { skipRef.current?.(); setIntro(false); home(); setEmptyPreview(true); setEncounter(null); }}>空池示例</button><button type="button" className="studio-button" onClick={() => { home(); setIntro(false); push('ocean'); }}>纸船与洋流 · 设计预览</button><button type="button" className="studio-button" disabled={reduced} onClick={() => { home(); setReplayKey(v => v+1); setIntro(true); }}>重播开场</button></div>
          <label className="mt-4 flex items-center gap-2 text-[11px] text-[#687a6f]"><input type="checkbox" checked={failCommit} onChange={e => setFailCommit(e.target.checked)} />下次提交失败 · 演示</label><label className="mt-3 flex items-center gap-2 text-[11px] text-[#687a6f]"><input type="checkbox" checked={reduced} disabled={systemReduced} onChange={e => setManualReduced(e.target.checked)} />减少动画{systemReduced && '（系统优先）'}</label>
        </section>
        <ol className="journey-outline mt-7"><li><span>01</span>书写与照片为主，继续不等于封存</li><li><span>02</span>封存前选择或编辑线索，也可以不留</li><li><span>03</span>保留折船动效，完成后洋流常显</li><li><span>04</span>原文、回忆与现在的回应</li></ol>
        <details className="mt-6 text-[11px] leading-6 text-[#718178]"><summary>边界与素材</summary><p className="mt-3">日光以本轮指定附件为来源，斜阳与夜色保留原光影，三态只局部处理右下水面；开屏使用配套海面与潮沟空间揭露。原图和旧潮汐代码保留，首页不运行涨退潮或叠加调色／顶部全幅遮罩，旧潮位链接也不会开启。功能底色、折船及完成后的洋流保持原样；留下界面重新编排。文字和照片仅在本次内存保留，照片最多 4 张、每张 10 MB，支持 JPG/PNG/WebP。线索候选为有限的本地原文摘词，不是 AI 分析；不自动采用，不足时不生成候选。指定日期不运行提醒。预览控件不属于 App 设置，本版不运行 AI、定位、持久存储或 V3 算法。</p><AssetComparison /><details className="candidate-crops"><summary>指定日光原图构图对照</summary><div>{[['9 / 16','9:16'],['9 / 19.5','9:19.5'],['3 / 4','3:4']].map(([ratio,label]) => <figure key={label}><img src={SHORE_MASTER_CANDIDATE} style={{aspectRatio:ratio}} alt={'潮汐树母图 ' + label + ' 居中裁切'} /><figcaption>{label} · 居中 cover</figcaption></figure>)}</div></details></details>
        <div className="study-state mt-6" role="status"><p>页面：{routeLabel[route]} · 光照：{DAYLIGHT[period].label}</p><p>记录：{memories.length} 条演示 · 当前会话：{encounter?.stage || '无'}</p><p>回忆快照：{encounter?.snapshots.length || 0} · 可见线索：{encounter?.visibleCues.length || 0}</p><p>载体：{sealedId || encounter?.memoryId || '无新封存'} · {pending ? '提交中' : '仅内存'}</p></div>
      </fieldset></section>
      <section className="preview-column flex min-w-0 flex-col items-center"><LayoutGroup id="memory-journey"><div ref={appRef} className={'app-window variant-' + variant + (period === 'night' ? ' night' : '') + (lowTransparency ? ' solid-material' : '')} style={frameDimensions} data-preview-size={previewSize} data-text-scale={largeText ? 'large' : 'normal'} data-safe-preview={safePreview} data-light-source={lightSource} data-period={period} data-reduced={reduced} data-route={route} data-scene={visibleEncounter && !emptyPreview ? 'encounter' : 'quiet'} data-stage={intro ? stage : 'shore'} data-space={ritualSurface ? 'ritual' : focusSurface ? 'focus' : 'shore'} aria-label={VARIANTS[variant].name + '手机原型'}>
        <div className="environment-layer"><RealisticSurface {...{variant,period,shoreMaster,tideValue,tidePaused,reduced}} opening={intro} active={!focusSurface} onReady={() => setSurfaceReady(true)} /></div>
        <div className="content-stage">
          <motion.div className="home-interface absolute inset-0" animate={{ opacity:intro || route !== 'home' ? 0 : 1 }} transition={{duration:reduced ? 0 : .35}} inert={intro || route !== 'home' ? true : undefined} aria-hidden={intro || route !== 'home'}>
            <header className="home-header"><span className="shore-time">{period === 'night' ? <Moon size={13} /> : <Sun size={13} />}{DAYLIGHT[period].label} · 岸边</span><MagneticButton reduced={reduced} className="settings-icon icon-button" aria-label="打开设置" onClick={() => push('settings')}><SlidersHorizontal size={19} /></MagneticButton></header>
            <HomeVariant encounter={emptyPreview ? null : encounter} memory={encounterMemory} empty={emptyPreview} preference={resolvedPreference} onAction={onAction} reduced={reduced} />
            {visibleEncounter && !emptyPreview && <div className="arrived-carrier"><MemoryVessel memoryId={encounter.memoryId} reduced={reduced} layout /><span className="arrival-waterline" /></div>}
            {hasDraftContent(draft) && <button type="button" className="resume-draft" onClick={() => push('leave')}>继续未封存的草稿<ArrowRight size={13} /></button>}
            <p className="home-prototype-note">交互原型 · 数据仅在本次预览</p>
          </motion.div>
          {intro && <TideReveal key={replayKey} {...{reduced,skipRef,tideValue,targetValue,surfaceReady}} onStage={setStage} onComplete={() => setIntro(false)} />}
          {intro && <button className="skip-intro absolute right-5 top-6 rounded-full px-3 py-2 text-[11px]" type="button" onClick={() => skipRef.current?.()}>抵达岸边<ArrowRight size={14} /></button>}
          <AnimatePresence mode="wait">{route !== 'home' && <motion.div key={route === 'review' || route === 'seal' ? 'review-seal' : route + (route === 'read' ? readingId : '')} className={'panel-layer absolute inset-0 route-' + route} initial={{opacity:0,x:reduced || ritualSurface ? 0 : 12}} animate={{opacity:1,x:0}} exit={{opacity:0,x:reduced ? 0 : -6}} onAnimationComplete={focusPage} transition={{duration:reduced ? 0 : .24,ease:[.22,.61,.36,1]}}>
            {route === 'leave' && <LeavePage {...{draft,updateDraft,reduced}} onPhotos={addPhotos} onRemovePhoto={removePhoto} onBack={back} onSave={() => { home(); setToast('草稿已在本次演示保留，刷新后清空。'); }} onNext={() => push('review')} />}
            {route === 'policy' && <PolicyPage {...{draft,updateDraft,reduced}} onBack={back} onNext={back} onPreference={() => push('preference')} />}
            {route === 'background' && <BackgroundPage {...{draft,updateDraft,reduced}} onBack={back} />}
            {route === 'preference' && <PreferencePage {...{draft,updateDraft,reduced}} onBack={back} />}
            {route === 'review' && <ReviewPage {...{draft,updateDraft,pending,reduced}} error={commitError} onBack={back} onEdit={back} onBackground={() => push('background')} onPolicy={() => push('policy')} onConfirm={commit} />}
            {route === 'seal' && sealed && <SealJourney memory={sealed} reduced={reduced} snapshot={sealSnapshot} onFinish={finishSeal} />}
            {route === 'ocean' && <OceanPage memory={emptyPreview ? null : oceanMemory} reduced={reduced} onBack={back} />}
            {route === 'past' && <PastPage memories={emptyPreview ? [] : memories} {...{query,setQuery,filter,setFilter,reduced}} scrollRef={pastScrollRef} onBack={back} onRead={memory => { setReadingId(memory.id); setReadOrigin('past'); push('read'); }} />}
            {route === 'recall' && encounterMemory && <RecallPage memory={encounterMemory} {...{encounter,reduced}} onChange={text => setEncounter(s => ({...s,text}))} onCue={requestCue} onRead={reveal} onBack={back} onDefer={defer} onStop={() => setEncounter(s => ({...s,stage:'stopped'}))} />}
            {route === 'read' && reading && <ReadPage key={reading.id} memory={reading} session={readOrigin === 'encounter' ? encounter : null} origin={readOrigin} reduced={reduced} onBack={endReading} onEnd={endReading} onToast={setToast} onReflect={text => setMemories(list => list.map(m => m.id === reading.id ? {...m,reflections:[...m.reflections,{text,date:today()}]} : m))} />}
            {route === 'settings' && <SettingsPage {...{forceDark,changeDark,reduced,setManualReduced,systemReduced,lowTransparency,setLowTransparency,preference,setPreference,allowEncounter,setAllowEncounter}} onBack={back} />}
          </motion.div>}</AnimatePresence>
        </div>
        <AnimatePresence>{toast && <motion.div className="toast absolute left-6 right-6 px-4 py-3 text-[11px] leading-6" role="status" initial={{opacity:0,y:reduced ? 0 : 5}} animate={{opacity:1,y:0}} exit={{opacity:0}}>{toast}</motion.div>}</AnimatePresence>
      </div></LayoutGroup><p className="mt-4 text-center text-[10px] leading-5 text-[#819184]">真实版原型 18 · 原文 / 可选线索 / 封存 · 底色和动效保持</p></section>
    </div><PrototypeSwitcher {...{variant,changeVariant,reduced,pending}} />
  </main></IconContext.Provider>;
}
createRoot(document.getElementById('root')).render(<App />);
