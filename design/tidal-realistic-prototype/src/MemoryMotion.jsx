'use client';

import React, { memo, useEffect, useLayoutEffect, useRef, useState } from 'react';
import { animate, motion, useMotionValue, useTransform } from 'framer-motion';
import { advanceCurrent, carrierPath, currentVelocity, samplePath } from './current-flow.js';
import { PaperBoat } from './PaperBoat.jsx';
import { PageFold } from './PageFold.jsx';
import { isCurrentActive } from './journey-state.js';

// Paper is a stable visual identity, not an importance score or an arrival timer.
export const MemoryVessel = memo(function MemoryVessel({ memoryId, className = '', layout = false }) {
  return <motion.div className={`memory-vessel ${className}`} layoutId={layout ? `carrier-${memoryId}` : undefined} data-memory-id={memoryId} data-carrier="paper-boat" aria-hidden="true">
    <PaperBoat />
  </motion.div>;
});

export const CurrentField = memo(function CurrentField({ reduced = false, active = true }) {
  const canvasRef = useRef(null);
  useEffect(() => {
    const canvas = canvasRef.current, context = canvas.getContext('2d');
    if (!context) return;
    let width = 1, height = 1, aspect = 2, frame = 0, last = 0, particles = [];
    let seed = 73421;
    const random = () => { seed = (seed * 1664525 + 1013904223) >>> 0; return seed / 4294967296; };
    const spawn = () => {
      const point = [random() * 1.22 - .12, .29 + random() * .61];
      const length = 16 + Math.floor(random() * 31), trail = [point];
      for (let i = 1; i < length; i++) trail.push(advanceCurrent(trail.at(-1), -.045, aspect));
      return { point, trail: trail.reverse(), length, alpha: .14 + random() * .26, width: .45 + random() * .45, age: 0, life: 6 + random() * 12 };
    };
    const draw = () => {
      context.clearRect(0, 0, width, height);
      context.lineCap = 'round';
      for (const particle of particles) {
        const fade = Math.min(1, (particle.life - particle.age) / .8);
        // Short, independent, tapered traces: no full-length fixed ribbon bundles.
        for (let band = 0; band < 3; band++) {
          const start = Math.floor((particle.trail.length - 1) * band / 3);
          const end = Math.ceil((particle.trail.length - 1) * (band + 1) / 3);
          context.beginPath();
          for (let i = start; i <= end; i++) {
            const [x, y] = particle.trail[i];
            if (i === start) context.moveTo(x * width, y * height); else context.lineTo(x * width, y * height);
          }
          context.strokeStyle = `rgba(218,235,234,${particle.alpha * fade * (.25 + band * .3)})`;
          context.lineWidth = particle.width; context.stroke();
        }
      }
    };
    const resize = new ResizeObserver(([entry]) => {
      width = Math.max(1, entry.contentRect.width); height = Math.max(1, entry.contentRect.height); aspect = height / width;
      const ratio = Math.min(window.devicePixelRatio || 1, 1.5);
      canvas.width = Math.round(width * ratio); canvas.height = Math.round(height * ratio);
      context.setTransform(ratio, 0, 0, ratio, 0, 0);
      seed = 73421; particles = Array.from({ length: Math.min(220, Math.max(160, Math.round(width * .52))) }, spawn); draw();
    });
    const tick = now => {
      if (last && now - last < 32) { frame = requestAnimationFrame(tick); return; }
      const dt = last ? Math.min((now - last) / 1000, .065) : .033;
      last = now;
      particles = particles.map(particle => {
        const point = advanceCurrent(particle.point, dt, aspect);
        if (particle.age + dt > particle.life || point[0] < -.3 || point[0] > 1.3 || point[1] < .15 || point[1] > 1.02) return spawn();
        particle.point = point; particle.age += dt; particle.trail.push(point);
        if (particle.trail.length > particle.length) particle.trail.shift();
        return particle;
      });
      draw(); frame = requestAnimationFrame(tick);
    };
    const visibility = () => {
      cancelAnimationFrame(frame); last = 0;
      if (active && !reduced && !document.hidden) frame = requestAnimationFrame(tick);
    };
    resize.observe(canvas); visibility(); document.addEventListener('visibilitychange', visibility);
    return () => { resize.disconnect(); cancelAnimationFrame(frame); document.removeEventListener('visibilitychange', visibility); };
  }, [reduced, active]);
  return <div className="current-field" data-current="open-field" aria-hidden="true"><canvas ref={canvasRef} /></div>;
});

// Resize-aware pixel transforms keep both routes attached to the same field.
// MotionValues update DOM transforms directly, never React state per frame.
function useCarrierMotion(progress, stationary = false, enabled = true, initialSize) {
  const originRef = useRef(null), pathRef = useRef(carrierPath());
  const [measured, setMeasured] = useState(false);
  const width = useMotionValue(initialSize?.width || 1), height = useMotionValue(initialSize?.height || 1);
  const restingY = useMotionValue(.61), minimumY = useMotionValue(.25);
  // Establish derived-value subscriptions BEFORE the first layout measurement.
  const x = useTransform(() => (stationary ? .5 : samplePath(pathRef.current, progress.get())[0]) * width.get());
  const y = useTransform(() => (stationary ? restingY.get() : Math.max(minimumY.get(), samplePath(pathRef.current, progress.get())[1])) * height.get());
  const boundaryOpacity = useTransform(() => stationary ? 1 : Math.max(0, Math.min(1, (samplePath(pathRef.current, progress.get())[1] - minimumY.get()) / .025)));
  const rotate = useTransform(() => {
    if (stationary) return -12;
    const velocity = currentVelocity(samplePath(pathRef.current, progress.get()), height.get() / width.get());
    return Math.atan2(velocity[1] * height.get(), velocity[0] * width.get()) * 180 / Math.PI + 12;
  });
  useLayoutEffect(() => {
    if (!originRef.current) return;
    const origin = originRef.current, scene = origin.closest('.seal-journey');
    const header = scene?.querySelector('.journey-header'), footer = scene?.querySelector('.journey-footer');
    const measure = () => {
      const w = Math.max(1, origin.clientWidth), h = Math.max(1, origin.clientHeight);
      // Reserve the full folded paper/turned boat, not just its unrotated silhouette.
      // Measure the actual content blocks so short screens and wrapping text stay safe.
      const halfSpan = Math.max(96, Math.min(132, w * .29)) * .58;
      const top = header ? (header.offsetTop + header.offsetHeight + halfSpan + 12) / h : .25;
      const bottom = footer ? (footer.offsetTop - halfSpan - 12) / h : .72;
      const min = Math.min(top, bottom), max = Math.max(top, bottom);
      const startY = Math.max(min, Math.min(max, .63));
      restingY.set(Math.max(min, Math.min(max, .61))); minimumY.set(min);
      pathRef.current = carrierPath(h / w, startY); width.set(w); height.set(h);
    };
    // Reduced motion has no page snapshot: measure before the first paint so
    // its static boat never flashes at the temporary 1x1 origin.
    measure();
    // One pre-paint rerender also refreshes derived values in React StrictMode,
    // whose effect replay can cancel the first scheduled MotionValue update.
    // This is initialization state only, never a per-animation-frame update.
    setMeasured(true);
    const observer = new ResizeObserver(measure);
    observer.observe(origin); if (header) observer.observe(header); if (footer) observer.observe(footer);
    return () => observer.disconnect();
  }, [width, height, restingY, minimumY, enabled]);
  return { originRef, x, y, rotate, boundaryOpacity, width, height, measured };
}

export function SealJourney({ memory, reduced, snapshot, onFinish }) {
  const quiet = memory.policy === 'quiet';
  const progress = useMotionValue(0);
  const fold = useMotionValue(reduced ? 1 : 0);
  const { originRef, x, y, rotate: direction, boundaryOpacity, width, height } = useCarrierMotion(progress, quiet || reduced, true, snapshot);
  const rotate = useTransform(() => -6 + (direction.get() + 6) * Math.min(1, progress.get() / .12));
  const scale = useTransform(progress, [0, .6, 1], [1, .8, .6]);
  const fade = useTransform(progress, [0, .63, 1], [1, 1, 0]);
  const opacity = useTransform(() => fade.get() * boundaryOpacity.get());
  const wordsOpacity = useTransform(fold, [.72, .95], [0, 1]);
  const footerOpacity = useTransform(fold, [.6, .8], [0, 1]);
  const [phase, setPhase] = useState(reduced ? 'complete' : 'withdrawing');
  const controlsRef = useRef([]);
  const cancelRef = useRef(() => {}), settledRef = useRef(false);
  useEffect(() => {
    let cancelled = false;
    const timers = [];
    controlsRef.current = [];
    const stop = () => { cancelled = true; timers.forEach(clearTimeout); controlsRef.current.forEach(control => control.stop()); };
    cancelRef.current = stop;
    const staticResult = reduced || settledRef.current || !snapshot;
    progress.set(0); fold.set(staticResult ? 1 : 0);
    if (staticResult) { settledRef.current = true; setPhase('complete'); return stop; }
    const run = (value, target, duration, next, ease = [.22, .61, .36, 1]) => {
      if (cancelled) return;
      controlsRef.current.push(animate(value, target, { duration, ease, onComplete: () => { if (!cancelled) next(); } }));
    };
    setPhase('withdrawing');
    timers.push(setTimeout(() => {
      if (cancelled) return;
      setPhase('lifting');
      run(fold, .44, 1.1, () => {
        setPhase('folding');
        run(fold, .67, .65, () => {
          setPhase('forming');
          run(fold, .84, .55, () => {
            setPhase('opening');
            run(fold, 1, .65, () => {
            if (quiet) settledRef.current = true;
            setPhase(quiet ? 'complete' : 'ready');
            if (quiet) return;
            timers.push(setTimeout(() => {
              if (cancelled) return;
              setPhase('drifting');
              run(progress, 1, 2.1, () => { settledRef.current = true; setPhase('complete'); }, [.25, .1, .3, 1]);
            }, 220));
            }, 'linear');
          }, 'linear');
        }, 'linear');
      }, 'linear');
    }, 180));
    return stop;
  }, [memory.id, quiet, reduced, snapshot, progress, fold]);
  useEffect(() => {
    if (phase === 'opening' || phase === 'complete') originRef.current?.closest('.seal-journey')?.querySelector('[data-page-title]')?.focus({ preventScroll: true });
  }, [phase, originRef]);
  const complete = phase === 'complete';
  const status = { withdrawing: '内容已留下。', lifting: '把这一页，轻轻收好。', folding: '刚才写下的，藏进折痕。', forming: '折起两角，留下船的形状。', opening: '沿着折痕，轻轻展开。', ready: '这一段，折成了一艘纸船。', drifting: '让洋流，带它远一点。' };
  return <section className={`seal-journey ${quiet ? 'quiet-journey' : ''}`} data-phase={phase} data-carrier="paper-boat">
    {!quiet && <motion.div className="journey-current" style={{ opacity: wordsOpacity }}><CurrentField reduced={reduced} active={isCurrentActive(phase)} /></motion.div>}
    <motion.header className="journey-header" style={{ opacity: wordsOpacity }}><p className="eyebrow">{quiet ? '静存' : '交给时间'}</p><h2 data-page-title tabIndex={-1}>{quiet ? '静静留下。' : <>这一段，<br />随时间流动。</>}</h2><p className="journey-description">{quiet ? '不主动上岸。想看的时候，它仍在「过去」。' : '不必知道它去向哪里。\n此刻，先让它离开眼前。'}</p></motion.header>
    <div className="seal-carrier-origin" ref={originRef} />
    <PageFold {...{snapshot,fold,width,height,x,y,rotate,opacity}} driftScale={scale} memoryId={memory.id} />
    <motion.footer className="journey-footer" style={{ opacity: footerOpacity }}><p className="journey-status" role="status" aria-live="polite">{complete ? quiet ? '已静静收存。' : '已封存。它只是离开眼前，仍保留在「过去」。' : status[phase]}</p><button type="button" className="solid-action w-full" onClick={() => { cancelRef.current(); onFinish(); }}>{complete ? '回到岸边' : '跳过动效，回到岸边'}<span aria-hidden="true">→</span></button><p className="journey-demo-note">演示记录已在本次内存保留 · 刷新后清空</p></motion.footer>
  </section>;
}

export const DriftingMemory = memo(function DriftingMemory({ memory, reduced }) {
  const progress = useMotionValue(0);
  const { originRef, x, y, rotate } = useCarrierMotion(progress, reduced, !!memory && memory.policy !== 'quiet');
  const opacity = useTransform(progress, [0, .08, .72, 1], [0, 1, 1, 0]);
  const scale = useTransform(progress, [0, 1], [1, .7]);
  useEffect(() => {
    progress.set(0);
    if (reduced || !memory || memory.policy === 'quiet') return;
    const control = animate(progress, 1, { duration: 14, repeat: Infinity, repeatDelay: 1.4, ease: 'linear' });
    return () => control.stop();
  }, [memory?.id, memory?.policy, reduced, progress]);
  if (!memory || memory.policy === 'quiet') return null;
  return <div className="seal-carrier-origin" ref={originRef}><motion.div className="seal-carrier-motion" style={{ x, y, rotate, opacity: reduced ? 1 : opacity, scale: reduced ? 1 : scale }}><div className="seal-carrier-anchor"><MemoryVessel memoryId={memory.id} /></div></motion.div></div>;
});
