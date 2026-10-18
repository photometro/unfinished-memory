'use client';

import React, { memo, useEffect, useMemo, useRef } from 'react';
import { animate, motion, useMotionValue, useTransform } from 'framer-motion';
import { buildTerrain } from './channel-geometry.js';

// Continuous drawing stays inside this leaf: no frame-by-frame React state updates.
export const OceanCurrents = memo(function OceanCurrents({ active, reduced, strength = 1 }) {
  const canvasRef = useRef(null);
  useEffect(() => {
    const canvas = canvasRef.current;
    const context = canvas.getContext('2d');
    let width = 1;
    let height = 1;
    let animationFrame = 0;
    let frameCount = 0;
    let previousTime = 0;
    let elapsed = 0;
    const resize = new ResizeObserver(([entry]) => {
      width = entry.contentRect.width;
      height = entry.contentRect.height;
      const ratio = Math.min(window.devicePixelRatio || 1, 1.5);
      canvas.width = width * ratio;
      canvas.height = height * ratio;
      context.setTransform(ratio, 0, 0, ratio, 0, 0);
      draw(elapsed);
    });

    function flowPoint(t, lane, time) {
      const x = t * width * 1.38 - width * 0.19;
      const base = height * (0.07 + lane * 0.026);
      const bend = Math.sin(t * 4.6 + lane * 0.035) * height * 0.095;
      const eddy = Math.exp(-Math.pow((t - 0.73) * 4.2, 2)) * Math.sin(t * 13 + lane * 0.12) * height * 0.095;
      return [x, base + bend + eddy + Math.sin(time * 0.08 + lane * 0.16) * 3];
    }

    function draw(time) {
      context.clearRect(0, 0, width, height);
      for (let lane = 0; lane < 39; lane++) {
        context.beginPath();
        for (let step = 0; step <= 76; step++) {
          const [x, y] = flowPoint(step / 76, lane, time);
          if (!step) context.moveTo(x, y);
          else context.lineTo(x, y);
        }
        context.strokeStyle = `rgba(227, 241, 237, ${strength * (0.06 + (lane % 4) * 0.026)})`;
        context.lineWidth = lane % 7 === 0 ? 1.15 : 0.6;
        context.stroke();
        if (!reduced) {
          for (let streak = 0; streak < 2; streak++) {
            const t = ((time * 0.014 + lane * 0.079 + streak * 0.49) % 1);
            context.beginPath();
            for (let segment = 0; segment <= 8; segment++) {
              const [x, y] = flowPoint(Math.min(t + segment * 0.0018, 1), lane, time);
              if (!segment) context.moveTo(x, y);
              else context.lineTo(x, y);
            }
            context.strokeStyle = `rgba(237, 246, 240, ${strength * 0.39})`;
            context.lineWidth = 0.9;
            context.stroke();
          }
        }
      }
    }

    function tick(now) {
      if (!document.hidden && active && !reduced) {
        if (previousTime) elapsed += Math.min(now - previousTime, 80) / 1000;
        if (frameCount++ % 2 === 0) draw(elapsed);
      }
      previousTime = now;
      animationFrame = requestAnimationFrame(tick);
    }

    resize.observe(canvas);
    draw(0);
    if (active && !reduced) animationFrame = requestAnimationFrame(tick);
    return () => {
      resize.disconnect();
      cancelAnimationFrame(animationFrame);
    };
  }, [active, reduced, strength]);
  return <canvas ref={canvasRef} className="absolute inset-0 h-full w-full pointer-events-none" aria-hidden="true" />;
});

export const ShoreCurrents = memo(function ShoreCurrents({ variant, active, reduced }) {
  const mask = useMemo(() => {
    const model = buildTerrain(variant);
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 720 1440" preserveAspectRatio="none"><path fill="white" d="${model.sea} ${model.layers.water}"/></svg>`;
    return `url("data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}")`;
  }, [variant]);
  return <div className="shore-current absolute inset-0 pointer-events-none" style={{ maskImage: mask, WebkitMaskImage: mask, maskSize: '100% 100%', WebkitMaskSize: '100% 100%', maskRepeat: 'no-repeat' }}><OceanCurrents active={active} reduced={reduced} strength={.32} /></div>;
});

export function TideReveal({ variant = 'A', night = false, replayKey, reduced, onStage, onComplete, skipRef }) {
  const tide = useMotionValue(1);
  const canvasRef = useRef(null);
  const wordsOpacity = useTransform(tide, [0.58, 0.76, 1], [0, 1, 1]);
  const currentsOpacity = useTransform(tide, [.48, .80, 1], [0, .55, 1]);
  const completeRef = useRef(onComplete);
  const stageRef = useRef(onStage);
  completeRef.current = onComplete;
  stageRef.current = onStage;

  useEffect(() => {
    const canvas = canvasRef.current;
    const context = canvas.getContext('2d', { alpha: true });
    const width = canvas.width, height = canvas.height;
    const heights = new Float32Array(width * height);
    const image = context.createImageData(width, height);
    const model = buildTerrain(variant);
    // Same mature incised field as the home. Lower a level; don't draw branches over time.
    for (let y = 0; y < height; y++) {
      for (let x = 0; x < width; x++) heights[x + y * width] = model.heightAt(x / width * 720, y / height * 1440);
    }
    const draw = progress => {
      const level = progress * 1.10 - .025;
      const finalFade = Math.min(1, progress * 12);
      for (let index = 0; index < heights.length; index++) {
        const difference = level - heights[index];
        const wet = Math.max(0, Math.min(1, difference / .018 + .5));
        const edge = Math.max(0, 1 - Math.abs(difference) / .015);
        const vertical = Math.floor(index / width) / height;
        const offset = index * 4;
        image.data[offset] = (night ? 38 : 100) + vertical * (night ? 24 : 47) + edge * 26;
        image.data[offset + 1] = (night ? 67 : 133) + vertical * (night ? 30 : 47) + edge * 24;
        image.data[offset + 2] = (night ? 78 : 140) + vertical * (night ? 30 : 41) + edge * 19;
        image.data[offset + 3] = wet * 255 * finalFade;
      }
      context.putImageData(image, 0, 0);
    };
    draw(tide.get());
    return tide.on('change', draw);
  }, [variant, night, tide]);

  useEffect(() => {
    let timer, controls;
    let cancelled = false;
    const finish = () => {
      if (cancelled) return;
      clearTimeout(timer); controls?.stop(); tide.set(0);
      stageRef.current('shore'); completeRef.current();
    };
    skipRef.current = finish;
    if (reduced) finish();
    else {
      tide.set(1); stageRef.current('sea');
      timer = setTimeout(() => {
        stageRef.current('ebb');
        controls = animate(tide, 0, { duration: 4.6, ease: [.4, .05, .18, 1], onComplete: finish });
      }, 750);
    }
    return () => { cancelled = true; clearTimeout(timer); controls?.stop(); skipRef.current = null; };
  }, [replayKey, reduced, tide, skipRef]);

  return (
    <div className="tide-layer pointer-events-none absolute inset-0" data-retreat-direction={variant === 'B' ? 'right' : variant === 'C' ? 'top' : 'bottom'} data-reveal-model="incised-height-field" aria-hidden="true">
      <canvas ref={canvasRef} width="216" height="432" className="absolute inset-0 h-full w-full" />
      <motion.div className="absolute inset-0" style={{ opacity: currentsOpacity }}><OceanCurrents active={!reduced} reduced={reduced} /></motion.div>
      <motion.div className="absolute left-8 right-8 top-[23%] text-[#f1f1e8]" style={{ opacity: wordsOpacity }}>
        <p className="text-[11px] tracking-[0.28em] opacity-80">一片承载记忆的时间海域</p>
        <p className="mt-5 text-[39px] font-[450] tracking-[0.08em]">未完成<span className="ml-1 opacity-60">_</span></p>
        <p className="mt-5 text-[14px] leading-7 opacity-85">过去不能修改，<br />但可以重新想起。</p>
      </motion.div>
    </div>
  );
}
