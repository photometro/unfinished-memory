'use client';
import React, { useEffect, useRef } from 'react';
import { animate, motion, useMotionValue, useTransform } from 'framer-motion';
import { introTide } from './environment-state.js';

// Intro text and progress only. App owns the persistent surface and target tide.
export function TideReveal({ reduced, skipRef, onStage, onComplete, tideValue, targetValue, surfaceReady }) {
  const progress = useMotionValue(1);
  const opacity = useTransform(progress, [.12, .7, 1], [0, 1, 1]);
  const completeRef = useRef(onComplete), stageRef = useRef(onStage), finishRef = useRef(null);
  completeRef.current = onComplete; stageRef.current = onStage;
  useEffect(() => {
    let cancelled = false, finished = false, timer, controls;
    const update = () => tideValue.set(introTide(progress.get(), targetValue.get()));
    const finish = () => {
      if (cancelled || finished) return;
      finished = true; clearTimeout(timer); controls?.stop(); progress.set(0);
      tideValue.set(targetValue.get()); stageRef.current('shore'); completeRef.current();
    };
    finishRef.current = finish; skipRef.current = finish;
    progress.set(1); update(); stageRef.current('sea');
    const offProgress = progress.on('change', update), offTarget = targetValue.on('change', update);
    const begin = () => {
      if (cancelled || finished || document.hidden) return;
      stageRef.current('ebb');
      controls = animate(progress, 0, { duration: 2.4, ease: [.32, .04, .22, 1], onComplete: finish });
    };
    const visible = () => { if (document.hidden) { clearTimeout(timer); controls?.pause(); } else if (!finished && surfaceReady) { if (controls) controls.play(); else timer = setTimeout(begin, 350); } };
    if (surfaceReady) timer = setTimeout(begin, 350);
    document.addEventListener('visibilitychange', visible);
    return () => { cancelled = true; clearTimeout(timer); controls?.stop(); offProgress(); offTarget(); document.removeEventListener('visibilitychange', visible); if (skipRef.current === finish) skipRef.current = null; finishRef.current = null; };
  }, [surfaceReady, progress, tideValue, targetValue, skipRef]);
  useEffect(() => { if (reduced) finishRef.current?.(); }, [reduced, surfaceReady]);
  return <div className="absolute inset-0 pointer-events-none" aria-hidden="true">
    <motion.div className="ocean-copy absolute left-8 right-8 top-[23%] text-[#f5f3e9]" style={{ opacity }}>
      <p className="text-[11px] tracking-[.22em]">一片承载记忆的时间海域</p>
      <p className="mt-5 text-[39px] font-[450] tracking-[.08em]">未完成<span className="ml-1 opacity-60">_</span></p>
      <p className="mt-5 text-[14px] leading-7">过去不能修改，<br />但可以重新想起。</p>
    </motion.div>
  </div>;
}
