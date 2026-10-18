'use client';

// PROTOTYPE ONLY: three new-home compositions, switchable via ?variant=A|B|C.
// Question: can an incised drainage landscape read as tidal creeks, rather than a botanical tree?
// All records are fictional. User input lives only in React memory. No API, storage or real data.
import React, { useEffect, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { AnimatePresence, motion, useMotionValue, useReducedMotion, useSpring } from 'framer-motion';
import { ArrowLeft, ArrowRight, ArrowUpRight, Check, IconContext, Moon, Play, Plus, SlidersHorizontal, Sun, X } from '@phosphor-icons/react';
import { Terrain } from './Terrain.jsx';
import { ShoreCurrents, TideReveal } from './Ocean.jsx';
import './styles.css';

const VARIANTS = {
  A: { name: '横向潮滩', caption: '地形在下，留白在上。', detail: '不同滩面区域的细沟逐级汇合，越靠近海口越宽。地貌横向铺开，而不是把一棵树摆在首页。' },
  B: { name: '纵向潮沟', caption: '水汇向海，岸边留白。', detail: '狭长潮滩形成纵向汇流关系，陆侧末梢与弯曲主沟共同切开滩面。左岸留给文字。' },
  C: { name: '留白海湾', caption: '从海域，走到岸边。', detail: '海口在远处，支沟汇入海湾。近处保留完整滩面，让水道成为地形的负空间。' },
};

const DEMO_MEMORY = {
  id: 'fictional-station', year: '2021', season: '夏天', title: '雨停以后，老车站',
  content: '那天的雨停得很晚。我们没有赶上最后一班车，就在老车站旁边坐了一会儿。\n\n铁皮屋檐还在滴水，远处有一盏灯。我不记得后来聊了些什么，只记得那段时间忽然变得很慢。',
  source: '虚构演示记录',
};
const INITIAL_MEMORIES = [DEMO_MEMORY, {
  id: 'fictional-window', year: '2023', season: '秋天', title: '搬走之前的窗台',
  content: '收拾完最后一只纸箱，窗台上只剩一圈淡淡的杯印。\n\n房间空了以后，反而听见了平时没有注意过的声音。', source: '虚构演示记录',
}, {
  id: 'fictional-road', year: '2024', season: '春天', title: '绕远的一小段路',
  content: '今天没有走熟悉的那条路。拐角的树刚刚长出新叶，路边有人搬着一盆很大的植物。\n\n绕远了一点，但并不想赶时间。', source: '虚构演示记录',
}];

const spring = { type: 'spring', stiffness: 230, damping: 28 };

function MagneticButton({ children, className = '', reduced = false, ...props }) {
  const x = useMotionValue(0);
  const y = useMotionValue(0);
  const softX = useSpring(x, { stiffness: 220, damping: 24 });
  const softY = useSpring(y, { stiffness: 220, damping: 24 });
  return <motion.button
    type="button" {...props} className={`pressable ${className}`} style={{ x: softX, y: softY }}
    whileTap={reduced ? {} : { scale: 0.975 }} transition={spring}
    onPointerMove={event => {
      if (reduced || event.pointerType !== 'mouse') return;
      const bounds = event.currentTarget.getBoundingClientRect();
      x.set((event.clientX - bounds.left - bounds.width / 2) * 0.055);
      y.set((event.clientY - bounds.top - bounds.height / 2) * 0.055);
    }}
    onPointerLeave={() => { x.set(0); y.set(0); }}
  >{children}</motion.button>;
}

function HomeMessage({ scene, cueCount, onAction, reduced, compact = false }) {
  if (scene === 'quiet') return <>
    <p className="eyebrow">此刻，没有需要赶赴的过去</p>
    <h2 className={`home-title ${compact ? 'compact' : ''}`}>今天海面<br />很安静。</h2>
    <p className="mt-4 text-[13px] leading-[1.9] text-[var(--muted)]">留下一点，或者只是停一会儿。</p>
  </>;
  return <>
    <p className="eyebrow"><span className="signal-dot mr-2 inline-block" />有一段过去，今天回来了</p>
    <h2 className={`home-title ${compact ? 'compact' : ''}`}>一段夏天，<br />正在靠岸。</h2>
    <p className="mt-4 flex items-center gap-3 text-[13px] text-[var(--muted)]"><span>2021</span><span className="h-2.5 w-px bg-[var(--line)]" /><span>夏天</span></p>
    {cueCount > 0 && <motion.p initial={{ opacity: 0, y: reduced ? 0 : 5 }} animate={{ opacity: 1, y: 0 }} className="mt-3 text-[13px] leading-6 text-[var(--ink)]">{cueCount === 1 ? '一点线索：一处老车站。' : '屋檐下，还有没有滴完的雨。'}</motion.p>}
    <div className="encounter-actions mt-5 grid grid-cols-2 gap-x-3 gap-y-2">
      <MagneticButton reduced={reduced} className="encounter-primary" onClick={() => onAction('recall')}>想一会儿<ArrowUpRight size={15} /></MagneticButton>
      <MagneticButton reduced={reduced} className="encounter-secondary" onClick={() => onAction('cue')}>{cueCount < 2 ? '再给一点' : '线索到这里'}<Plus size={14} /></MagneticButton>
      <MagneticButton reduced={reduced} className="encounter-secondary" onClick={() => onAction('read')}>直接看看<ArrowRight size={14} /></MagneticButton>
      <MagneticButton reduced={reduced} className="encounter-secondary" onClick={() => onAction('defer')}>让它再待一会</MagneticButton>
    </div>
  </>;
}

function VariantA({ scene, cueCount, onAction, reduced }) {
  return <>
    <section className="message-a absolute left-[8%] right-[8%] top-[19%]">
      <HomeMessage {...{ scene, cueCount, onAction, reduced }} />
    </section>
    <div className="absolute bottom-[20%] left-[8%] flex items-center gap-2 text-[10px] tracking-[.14em] text-[var(--terrain-label)]" aria-hidden="true"><span className="h-px w-7 bg-current opacity-60" />潮水退去，痕迹留下</div>
    <nav className="home-nav absolute bottom-[6.5%] left-[8%] right-[8%] flex items-center justify-between" aria-label="主要操作">
      <MagneticButton reduced={reduced} className="leave-action" onClick={() => onAction('leave')}><Plus size={19} /><span>留下一段</span></MagneticButton>
      <MagneticButton reduced={reduced} className="past-action" onClick={() => onAction('past')}><span>过去</span><ArrowUpRight size={17} /></MagneticButton>
    </nav>
  </>;
}

function VariantB({ scene, cueCount, onAction, reduced }) {
  return <>
    <section className="message-b absolute left-[8%] right-[15%] top-[14%]">
      <HomeMessage {...{ scene, cueCount, onAction, reduced }} compact />
    </section>
    <nav className="absolute bottom-[8%] left-[8%] flex flex-col items-start gap-5" aria-label="主要操作">
      <MagneticButton reduced={reduced} className="vertical-leave flex items-center gap-3" onClick={() => onAction('leave')}><span className="plus-circle"><Plus size={26} /></span><span className="text-[14px]">留下一段</span></MagneticButton>
      <MagneticButton reduced={reduced} className="past-action pl-1" onClick={() => onAction('past')}><span>走进过去</span><ArrowUpRight size={17} /></MagneticButton>
    </nav>
    <p className="absolute bottom-[13%] right-[7%] text-[10px] tracking-[.22em] text-[var(--terrain-label)] [writing-mode:vertical-rl]" aria-hidden="true">时间经过的地方</p>
  </>;
}

function VariantC({ scene, cueCount, onAction, reduced }) {
  return <>
    <section className={`message-c absolute left-[8%] right-[8%] ${scene === 'quiet' ? 'top-[55%]' : 'top-[43%]'}`}>
      <HomeMessage {...{ scene, cueCount, onAction, reduced }} compact />
      {scene === 'quiet' && <MagneticButton reduced={reduced} className="inline-leave mt-6 flex items-center gap-4 text-[14px]" onClick={() => onAction('leave')}><span>留下一段新的记忆</span><ArrowUpRight size={18} /></MagneticButton>}
    </section>
    <nav className="absolute bottom-[5.5%] left-[8%] right-[8%] flex items-center justify-between" aria-label="主要操作">
      {scene === 'quiet' ? <span className="eyebrow opacity-75">现在 · 岸边</span> : <MagneticButton reduced={reduced} className="past-action" onClick={() => onAction('leave')}><Plus size={17} /><span>留下</span></MagneticButton>}
      <MagneticButton reduced={reduced} className="past-action" onClick={() => onAction('past')}><span>过去</span><ArrowUpRight size={17} /></MagneticButton>
    </nav>
  </>;
}

function PanelHeader({ title, onBack, children }) {
  return <header className="panel-header flex shrink-0 items-center justify-between gap-3 px-6 py-5">
    <div className="flex items-center gap-2"><MagneticButton className="icon-button -ml-2" aria-label="返回岸边" onClick={onBack}><ArrowLeft size={20} /></MagneticButton><h2 className="text-[14px] font-[500]">{title}</h2></div>{children}
  </header>;
}

function LeavePage({ draft, setDraft, onBack, onSave, onSeal }) {
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const timer = useRef(null);
  useEffect(() => () => clearTimeout(timer.current), []);
  const submit = action => {
    if (!draft.trim()) { setError('先写下一点内容，再试一次。'); return; }
    setError(''); setSaving(true);
    timer.current = setTimeout(() => { setSaving(false); action(); }, 550);
  };
  return <section className="function-page absolute inset-0 flex flex-col">
    <PanelHeader title="留下" onBack={onBack}><button type="button" className="text-button text-[12px]" disabled={saving} onClick={() => submit(onSave)}>{saving ? '暂存中…' : '暂存草稿'}</button></PanelHeader>
    <div className="min-h-0 flex-1 overflow-y-auto px-8 pb-5 pt-7">
      <p className="eyebrow">不必整理得很完整</p><h3 className="mt-4 text-[27px] leading-[1.55] tracking-[.04em]">把这一刻，<br />留给以后。</h3>
      <label htmlFor="memory-input" className="mt-8 block text-[12px] text-[var(--muted)]">想留下什么？</label>
      <textarea id="memory-input" className="writing-input mt-3 min-h-[220px] w-full resize-none bg-transparent text-[16px] leading-[2] outline-none" value={draft} onChange={event => { setDraft(event.target.value); setError(''); }} placeholder="一个画面，一句话，或者还没想明白的事。" aria-describedby="draft-help draft-error" />
      <p id="draft-error" role="alert" className="min-h-6 text-[12px] text-[#9b5943]">{error}</p>
      <p id="draft-help" className="mt-2 text-[11px] leading-6 text-[var(--muted)]">仅体验书写与封存动效。输入不联网、不写入文件，刷新后清空。</p>
    </div>
    <footer className="shrink-0 px-8 pb-8 pt-3"><MagneticButton className="solid-action w-full" disabled={saving} onClick={() => submit(onSeal)}>{saving ? '准备封存…' : '封存 · 演示'}<ArrowUpRight size={18} /></MagneticButton></footer>
  </section>;
}

function PastPage({ memories, onBack, onRead }) {
  const [query, setQuery] = useState('');
  const filtered = memories.filter(memory => `${memory.title} ${memory.content} ${memory.year}`.includes(query));
  return <section className="function-page absolute inset-0 flex flex-col">
    <PanelHeader title="过去" onBack={onBack} />
    <div className="min-h-0 flex-1 overflow-y-auto px-8 pb-10 pt-5">
      <h3 className="text-[27px] leading-[1.6] tracking-[.04em]">时间经过，<br />一些东西留下。</h3>
      <label htmlFor="memory-search" className="mt-7 block text-[11px] text-[var(--muted)]">寻找一段记忆</label><input id="memory-search" className="search-input mt-2 w-full border-b border-[var(--line)] bg-transparent py-3 text-[14px] outline-none" placeholder="搜索文字或年份" value={query} onChange={event => setQuery(event.target.value)} />
      <p className="mt-3 text-[10px] leading-5 text-[var(--muted)]">预置内容均为虚构示例；新输入只保留在本次演示中。</p>
      <div className="mt-7">{filtered.length ? filtered.map((memory, index) => <motion.button key={memory.id} type="button" onClick={() => onRead(memory)} initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: index * 0.07, duration: 0.22 }} className="past-row flex w-full items-center justify-between gap-3 border-b border-[var(--line)] py-5 text-left"><span><span className="block text-[10px] tracking-[.15em] text-[var(--muted)]">{memory.year} · {memory.season}</span><span className="mt-2 block text-[15px]">{memory.title}</span></span><ArrowUpRight size={18} className="shrink-0 text-[var(--muted)]" /></motion.button>) : <div className="py-14"><p className="text-[18px]">这里还没有这段痕迹。</p><p className="mt-3 text-[12px] leading-6 text-[var(--muted)]">试试别的文字，或清空搜索看看。</p><button className="text-button mt-5 text-[13px]" type="button" onClick={() => setQuery('')}>清空搜索</button></div>}</div>
    </div>
  </section>;
}

function ReadPage({ memory, recall, onBack }) {
  const [responding, setResponding] = useState(false);
  const [response, setResponse] = useState('');
  const [saved, setSaved] = useState(false);
  return <section className="function-page reading-page absolute inset-0 flex flex-col">
    <PanelHeader title="靠岸之后" onBack={onBack} />
    <article className="min-h-0 flex-1 overflow-y-auto px-8 pb-10 pt-6">
      <p className="eyebrow">{memory.year} · {memory.season}</p><h3 className="mt-4 text-[26px] leading-[1.55] tracking-[.03em]">{memory.title}</h3>
      <p className="mt-3 text-[10px] text-[var(--muted)]">{memory.source}{memory.id.startsWith('fictional-') ? ' · 不是你的真实记忆' : ' · 刷新后清空'}</p>
      <div className="mt-8 whitespace-pre-line text-[15px] leading-[2.05]">{memory.content}</div>
      {recall.trim() && <section className="mt-8 border-t border-[var(--line)] pt-6"><p className="eyebrow">刚刚想起的</p><p className="mt-3 whitespace-pre-line text-[14px] leading-7">{recall}</p></section>}
      <section className="mt-9 border-t border-[var(--line)] pt-6">{!responding ? <button type="button" className="text-button flex items-center gap-2 text-[13px]" onClick={() => setResponding(true)}>写下现在的回应<Plus size={15} /></button> : <><label htmlFor="reflection" className="block text-[12px] text-[var(--muted)]">现在想说的话 · 可留空</label><textarea id="reflection" className="mt-3 min-h-24 w-full resize-none bg-transparent text-[14px] leading-7 outline-none" placeholder="不需要得出结论。" value={response} onChange={event => { setResponse(event.target.value); setSaved(false); }} /><button type="button" className="text-button text-[12px]" onClick={() => setSaved(true)}>{saved ? '仅在本次页面暂存' : '暂存回应 · 演示'}</button></>}</section>
    </article>
  </section>;
}

function RecallPage({ recall, setRecall, cueCount, onCue, onRead, onBack, onDefer }) {
  return <section className="function-page absolute inset-0 flex flex-col">
    <PanelHeader title="想一会儿" onBack={onBack} />
    <div className="min-h-0 flex-1 overflow-y-auto px-8 pt-7"><p className="eyebrow">2021 · 夏天 · 虚构示例</p><h3 className="mt-5 text-[27px] leading-[1.6]">你还能<br />想起什么？</h3>
      {cueCount > 0 && <p className="mt-4 text-[13px] leading-6 text-[var(--muted)]">{cueCount === 1 ? '一点线索：一处老车站。' : '屋檐下，还有没有滴完的雨。'}</p>}
      <label htmlFor="recall-input" className="mt-7 block text-[12px] text-[var(--muted)]">现在的回忆 · 可以留空</label><textarea id="recall-input" className="mt-3 min-h-[180px] w-full resize-none bg-transparent text-[16px] leading-[1.9] outline-none" value={recall} onChange={event => setRecall(event.target.value)} placeholder="想到多少，就写多少。" />
      <p className="text-[11px] leading-6 text-[var(--muted)]">这不是一次记忆测试。可以直接查看，也可以停在这里。</p>
    </div>
    <footer className="shrink-0 px-8 pb-7 pt-5"><MagneticButton className="solid-action w-full" onClick={onRead}>看看那时候留下的<ArrowRight size={18} /></MagneticButton><div className="mt-3 flex items-center justify-between gap-2"><button type="button" className="text-button py-3 text-[12px]" onClick={onCue}>{cueCount < 2 ? '再给一点线索' : '线索到这里'}</button><button type="button" className="text-button py-3 text-[12px]" onClick={onDefer}>让它再待一会</button></div></footer>
  </section>;
}

function SettingsPage({ night, setNight, reduced, setManualReduced, systemReduced, onBack }) {
  return <section className="function-page absolute inset-0 flex flex-col"><PanelHeader title="这一片海域" onBack={onBack} /><div className="px-8 pt-7"><p className="eyebrow">视觉原型设置</p><h3 className="mt-4 text-[26px] leading-[1.6]">按你的节奏。</h3><div className="mt-8"><ToggleRow label="深海模式" hint="查看同一构图的夜间配色" checked={night} onChange={() => setNight(!night)} /><ToggleRow label="减少动画" hint={systemReduced ? '系统已开启减少动画，优先遵循系统设置' : '直接抵达岸边，不播放退潮和流动'} checked={reduced} disabled={systemReduced} onChange={() => setManualReduced(!reduced)} /></div><p className="mt-8 text-[11px] leading-7 text-[var(--muted)]">当前仅验证地形、构图和动效。没有登录、AI 请求、定位、真实存储或核心算法执行。</p></div></section>;
}

function ToggleRow({ label, hint, checked, onChange, disabled = false }) {
  return <div className="flex items-center justify-between gap-4 border-b border-[var(--line)] py-5"><div><p className="text-[13px]">{label}</p><p className="mt-1 text-[10px] leading-5 text-[var(--muted)]">{hint}</p></div><button type="button" role="switch" aria-label={label} aria-checked={checked} disabled={disabled} onClick={onChange} className={`toggle shrink-0 ${checked ? 'on' : ''}`}><motion.span animate={{ x: checked ? 16 : 0 }} transition={spring} /></button></div>;
}

function SealDemo({ reduced, onDone }) {
  useEffect(() => { const timer = setTimeout(onDone, reduced ? 200 : 1550); return () => clearTimeout(timer); }, [onDone, reduced]);
  return <motion.div className="seal-screen absolute inset-0 flex flex-col justify-center px-8" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}><motion.div initial={{ y: 0, scale: 1, opacity: 1 }} animate={reduced ? { opacity: 0 } : { y: 160, scale: 0.05, opacity: 0 }} transition={{ duration: reduced ? .1 : 1.15, ease: [.25, .1, .18, 1] }}><p className="eyebrow">这一段，暂时交给时间。</p><div className="mt-5 h-px w-28 bg-[var(--muted)] opacity-30" /></motion.div><motion.p className="mt-12 text-[13px] text-[var(--muted)]" initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: reduced ? 0 : .75 }}>封存动效演示<br /><span className="text-[10px]">只保留在本次预览，刷新后清空</span></motion.p></motion.div>;
}

function PrototypeSwitcher({ variant, changeVariant, reduced }) {
  if (!import.meta.env.DEV) return null;
  const keys = Object.keys(VARIANTS);
  const cycle = direction => changeVariant(keys[(keys.indexOf(variant) + direction + keys.length) % keys.length]);
  useEffect(() => {
    const listener = event => {
      if (event.target.closest('input,textarea,select,[contenteditable="true"],[role="textbox"]')) return;
      if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') { event.preventDefault(); cycle(event.key === 'ArrowLeft' ? -1 : 1); }
    };
    window.addEventListener('keydown', listener);
    return () => window.removeEventListener('keydown', listener);
  }, [variant, changeVariant]);
  return <aside className="prototype-switcher fixed bottom-5 left-1/2 flex -translate-x-1/2 items-center gap-2 rounded-full px-2 py-1.5" aria-label="原型构图切换"><MagneticButton reduced={reduced} className="switcher-arrow" aria-label="上一种构图" onClick={() => cycle(-1)}><ArrowLeft size={17} /></MagneticButton><span className="min-w-[154px] px-2 text-center text-[12px]"><span className="mr-2 text-[#d6b18e]">{variant}</span>{VARIANTS[variant].name}<span className="ml-2 text-[9px] opacity-50">原型</span></span><MagneticButton reduced={reduced} className="switcher-arrow" aria-label="下一种构图" onClick={() => cycle(1)}><ArrowRight size={17} /></MagneticButton></aside>;
}

function App() {
  const validVariant = () => {
    const fromUrl = new URLSearchParams(location.search).get('variant');
    return VARIANTS[fromUrl] ? fromUrl : 'A';
  };
  const [variant, setVariant] = useState(validVariant);
  const [scene, setScene] = useState('quiet');
  const [route, setRoute] = useState('home');
  const [intro, setIntro] = useState(true);
  const [stage, setStage] = useState('sea');
  const [replayKey, setReplayKey] = useState(0);
  const [night, setNight] = useState(false);
  const [manualReduced, setManualReduced] = useState(false);
  const systemReduced = !!useReducedMotion();
  const reduced = systemReduced || manualReduced;
  const [cueCount, setCueCount] = useState(0);
  const [draft, setDraft] = useState('');
  const [recall, setRecall] = useState('');
  const [memories, setMemories] = useState(INITIAL_MEMORIES);
  const [reading, setReading] = useState(DEMO_MEMORY);
  const [readBack, setReadBack] = useState('home');
  const [toast, setToast] = useState('');
  const skipRef = useRef(null);
  const restoreRef = useRef(null);

  useEffect(() => { if (!toast) return; const timer = setTimeout(() => setToast(''), 3200); return () => clearTimeout(timer); }, [toast]);
  useEffect(() => { const listener = () => { setVariant(validVariant()); setIntro(false); setRoute('home'); }; window.addEventListener('popstate', listener); return () => window.removeEventListener('popstate', listener); }, []);
  useEffect(() => {
    const listener = event => { if (event.key === 'Escape') { if (intro) skipRef.current?.(); else setRoute('home'); } };
    window.addEventListener('keydown', listener);
    return () => window.removeEventListener('keydown', listener);
  }, [intro]);

  const replay = () => { setToast(''); setRoute('home'); setReplayKey(value => value + 1); setIntro(true); setStage('sea'); };
  const changeVariant = next => {
    if (!VARIANTS[next]) return;
    const url = new URL(location.href); url.searchParams.set('variant', next); history.replaceState(null, '', url);
    setVariant(next); setRoute('home'); setIntro(false); setStage('shore'); setToast('');
  };
  const chooseScene = next => { skipRef.current?.(); setToast(''); setScene(next); setCueCount(0); setRoute('home'); };
  const goBack = () => { setRoute('home'); restoreRef.current?.focus(); };
  const action = type => {
    if (type === 'cue') { if (cueCount < 2) setCueCount(value => value + 1); else setToast('线索就到这里。可以查看，也可以稍后再遇见。'); return; }
    if (type === 'defer') { setScene('quiet'); setCueCount(0); setRoute('home'); setToast('这次先到这里。暂缓仅为原型演示。'); return; }
    setToast('');
    if (type === 'read') { setReading(DEMO_MEMORY); setReadBack('home'); setRoute('read'); return; }
    setRoute(type);
  };
  const finishSeal = () => {
    setMemories(list => [{ id: `draft-${Date.now()}`, year: '本次演示', season: '未设置发生时间', title: draft.trim().slice(0, 16), content: draft.trim(), source: '本次输入 · 仅内存暂存' }, ...list]);
    setDraft(''); setRoute('home'); setScene('quiet'); setToast('演示封存完成；内容只在本次预览中保留。');
  };
  const HomeVariant = variant === 'B' ? VariantB : variant === 'C' ? VariantC : VariantA;
  const routeLabels = { home: '首页', leave: '留下', past: '过去', read: '阅读', recall: '回忆', settings: '设置', seal: '封存演示' };

  return <IconContext.Provider value={{ weight: 'light' }}>
    <main className="studio min-h-[100dvh] px-5 pb-28 pt-10 md:px-12 md:pb-24 md:pt-12">
      <header className="mx-auto flex max-w-[1180px] items-center justify-between gap-5"><p className="text-[13px] font-[500] tracking-[.08em]">未完成<span className="ml-0.5 text-[#a5aca4]">_</span><span className="ml-5 text-[10px] font-normal tracking-[.13em] text-[#7b8782]">ABSTRACT TIDAL STUDY / 03</span></p><p className="hidden text-[10px] tracking-[.12em] text-[#87918a] sm:block">视觉实验 · 非正式 App</p></header>
      <div className="studio-grid mx-auto grid max-w-[1120px] items-center gap-12 pt-12 md:grid-cols-[minmax(260px,1fr)_minmax(320px,420px)] md:gap-20 md:pt-9">
        <section className="study-panel max-w-[430px]">
          <p className="text-[10px] tracking-[.18em] text-[#7b8782]">洋流 / 退潮 / 潮汐树</p>
          <h1 className="mt-5 text-[clamp(30px,3.4vw,44px)] font-[450] leading-[1.4] tracking-[.025em] text-[#2b3938]">水退去，<br />时间留下轮廓。</h1>
          <p className="mt-5 max-w-[31ch] text-[13px] leading-[2] text-[#687873]">不是一棵长出分枝的树，<br />而是一片被潮水切开的滩面。</p>
          <div className="mt-9 flex items-center gap-3"><span className="flex h-7 w-7 items-center justify-center rounded-full border border-[#d7ddd4] text-[11px] text-[#657871]">{variant}</span><div><h2 className="text-[15px] font-[500] text-[#34463f]">{VARIANTS[variant].name}</h2><p className="mt-1 text-[11px] text-[#78867d]">{VARIANTS[variant].caption}</p></div></div>
          <p className="mt-4 max-w-[32ch] text-[12px] leading-[1.95] text-[#738178]">{VARIANTS[variant].detail}</p>
          <ol className="mt-6 space-y-2 text-[11px] leading-6 text-[#738178]" aria-label="潮沟生成逻辑"><li>01 · 不规则滩面与海口边界</li><li>02 · 排水路径汇合，集中水量</li><li>03 · 达到示意阈值后，形成宽窄潮沟</li><li>04 · 水位下降，显露同一片地形</li></ol>
          <div className="mt-8 flex flex-wrap gap-2"><MagneticButton reduced={reduced} className="studio-button primary" disabled={reduced} onClick={replay}><Play size={15} />重播退潮</MagneticButton><MagneticButton reduced={reduced} className="studio-button" onClick={() => { skipRef.current?.(); setNight(value => !value); }}>{night ? <Sun size={15} /> : <Moon size={15} />}{night ? '浅海模式' : '深海模式'}</MagneticButton></div>
          <div className="mt-5 flex flex-wrap gap-2" aria-label="首页演示状态"><button className={`scene-pill ${scene === 'quiet' ? 'selected' : ''}`} type="button" aria-pressed={scene === 'quiet'} onClick={() => chooseScene('quiet')}>海面安静</button><button className={`scene-pill ${scene === 'encounter' ? 'selected' : ''}`} type="button" aria-pressed={scene === 'encounter'} onClick={() => chooseScene('encounter')}>有记忆靠岸</button></div>
          <label className="mt-6 flex cursor-pointer items-center gap-2.5 text-[11px] text-[#687a6f]"><input type="checkbox" checked={reduced} disabled={systemReduced} onChange={event => setManualReduced(event.target.checked)} className="h-3.5 w-3.5 accent-[#788c7a]" />减少动画{systemReduced && <span>（系统已开启）</span>}</label>
          <div className="mt-8 border-t border-[#dce1d7] pt-4 text-[10px] leading-6 text-[#829083]" role="status"><div className="flex flex-wrap gap-x-5"><span>阶段：{intro ? stage === 'sea' ? '满海' : '正在退潮' : '岸边'}</span><span>页面：{routeLabels[route]}</span><span>构图：{variant}</span></div><p>演示放慢了退潮节奏，非最终启动时长。<br />潮沟为机制启发的示意，不是物理模拟。<br />输入仅保留在本次预览；没有 AI 或记忆算法运行。</p></div>
        </section>
        <section className="preview-column flex min-w-0 flex-col items-center">
          <div className={`app-window variant-${variant} ${night ? 'night' : ''}`} data-route={route} data-scene={scene} data-stage={intro ? stage : 'shore'} aria-label={`${VARIANTS[variant].name}手机原型`}>
            <Terrain variant={variant} night={night} />
            {!intro && route === 'home' && <ShoreCurrents variant={variant} active={scene === 'quiet'} reduced={reduced} />}
            <motion.div className="home-interface absolute inset-0" animate={{ opacity: intro || route !== 'home' ? 0 : 1 }} transition={{ duration: reduced ? 0 : .4 }} inert={intro || route !== 'home' ? true : undefined} aria-hidden={intro || route !== 'home'}>
              <header className="absolute left-[8%] right-[6%] top-[4%] flex items-center justify-between"><p className="text-[10px] tracking-[.16em] text-[var(--muted)]">今日 · 岸边</p><MagneticButton reduced={reduced} className="settings-icon icon-button" aria-label="打开设置" onClick={() => { setToast(''); setRoute('settings'); }}><SlidersHorizontal size={19} /></MagneticButton></header>
              <HomeVariant {...{ scene, cueCount, reduced }} onAction={action} />
              <p className="absolute bottom-[2.2%] left-[8%] text-[8px] tracking-[.06em] text-[var(--terrain-label)]">{scene === 'encounter' ? '虚构记忆示例 · 仅供体验' : '潮汐树视觉原型 · 内容不持久保存'}</p>
            </motion.div>
            {intro && <TideReveal key={`${variant}-${replayKey}`} variant={variant} night={night} replayKey={replayKey} reduced={reduced} skipRef={skipRef} onStage={setStage} onComplete={() => setIntro(false)} />}
            {intro && <button type="button" className="skip-intro absolute right-5 top-6 rounded-full px-3 py-2 text-[11px] text-[#f0f1e8]" onClick={() => skipRef.current?.()}>抵达岸边<ArrowRight size={14} /></button>}
            <AnimatePresence mode="wait">{route !== 'home' && <motion.div key={route} className="panel-layer absolute inset-0" initial={{ opacity: 0, y: reduced ? 0 : 12 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: reduced ? 0 : 8 }} transition={{ duration: reduced ? 0 : .24, ease: [.16, 1, .3, 1] }}>
              {route === 'leave' && <LeavePage draft={draft} setDraft={setDraft} onBack={goBack} onSave={() => { setToast('草稿仅在本次预览中暂存，刷新后清空。'); goBack(); }} onSeal={() => setRoute('seal')} />}
              {route === 'past' && <PastPage memories={memories} onBack={goBack} onRead={memory => { setReading(memory); setReadBack('past'); setRoute('read'); }} />}
              {route === 'read' && <ReadPage memory={reading} recall={reading.id === DEMO_MEMORY.id ? recall : ''} onBack={() => setRoute(readBack)} />}
              {route === 'recall' && <RecallPage recall={recall} setRecall={setRecall} cueCount={cueCount} onCue={() => action('cue')} onRead={() => action('read')} onBack={goBack} onDefer={() => action('defer')} />}
              {route === 'settings' && <SettingsPage {...{ night, setNight, reduced, setManualReduced, systemReduced }} onBack={goBack} />}
              {route === 'seal' && <SealDemo reduced={reduced} onDone={finishSeal} />}
            </motion.div>}</AnimatePresence>
            <AnimatePresence>{toast && <motion.div className="toast absolute bottom-6 left-6 right-6 rounded-xl px-4 py-3 text-[11px] leading-6" role="status" initial={{ opacity: 0, y: reduced ? 0 : 6 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}>{toast}</motion.div>}</AnimatePresence>
          </div>
          <p className="mt-4 text-[10px] tracking-[.08em] text-[#859184]">点击界面体验 · 左右方向键切换构图</p>
        </section>
      </div>
      <PrototypeSwitcher {...{ variant, changeVariant, reduced }} />
    </main>
  </IconContext.Provider>;
}

createRoot(document.getElementById('root')).render(<App />);
