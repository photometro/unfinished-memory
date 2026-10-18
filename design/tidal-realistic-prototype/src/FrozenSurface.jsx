import React, { useEffect, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { DAYLIGHT, validPeriod } from './daylight.js';
import { photographPlacement } from './environment-geometry.js';
import { loadFrozenImages } from './frozen-assets.js';
import { OpeningWater } from './OpeningWater.jsx';

const images = new Map();
function loadImage(url) {
  if (!images.has(url)) images.set(url, new Promise((resolve, reject) => {
    const image = new Image();
    image.onload = async () => {
      try { await image.decode(); resolve(image); }
      catch { reject(new Error('海岸素材暂不可用')); }
    };
    image.onerror = () => reject(new Error('海岸素材暂不可用'));
    image.src = url;
  }).catch(error => { images.delete(url); throw error; }));
  return images.get(url);
}

// Home stays an untouched static photo. Only the introductory sea has a mask.
export function FrozenSurface({ variant = 'A', period = 'morning', active = true, reduced = false, opening = false, tideValue, onReady }) {
  const rootRef = useRef(null), readyRef = useRef(false), onReadyRef = useRef(onReady);
  onReadyRef.current = onReady;
  const [bounds, setBounds] = useState({ width: 1, height: 2 });
  const [shown, setShown] = useState(null), [error, setError] = useState('');
  const [sea, setSea] = useState(null);
  const [settled, setSettled] = useState(true);
  const requested = validPeriod(period);
  useEffect(() => {
    const root = rootRef.current;
    const resize = rect => setBounds({ width: Math.max(1, rect.width), height: Math.max(1, rect.height) });
    resize(root.getBoundingClientRect());
    const observer = new ResizeObserver(([entry]) => resize(entry.contentRect));
    observer.observe(root);
    return () => observer.disconnect();
  }, []);
  useEffect(() => {
    setError('');
    let shore;
    const seaUrl = DAYLIGHT[requested].sea;
    const load = url => url === seaUrl
      ? Promise.all([loadImage(url), loadImage(DAYLIGHT.morning.shore)]).then(([image, reference]) => ({ image, reference }))
      : loadImage(url);
    const pending = loadFrozenImages({ load, shoreUrl: DAYLIGHT[requested].shore, seaUrl,
      onShore: image => { shore = image; },
      onSea: (ok, bundle) => {
        if (!shore) return; // Keep an existing good frame if the new shore fails.
        setSettled(reduced || !active);
        setShown({ period: requested, shore: DAYLIGHT[requested].shore, width: shore.naturalWidth, height: shore.naturalHeight });
        setSea(ok ? { ...bundle, period: requested } : null);
      },
      onError: err => setError(err.message),
      // Neither missing image can trap the intro or block navigation.
      onReady: () => {
        if (!readyRef.current) { readyRef.current = true; onReadyRef.current?.(); }
      },
    });
    return pending.cancel;
  }, [requested]);
  const placementFor = frame => photographPlacement(bounds.width, bounds.height, frame?.width || 887, frame?.height || 1774, variant);
  const styleFor = frame => { const p = placementFor(frame); return { width: p.width, height: p.height,
    transform: `translate(-50%, -50%) rotate(${p.rotation}deg)` }; };
  const placement = placementFor(shown);
  return <div ref={rootRef} className={`natural-surface natural-${variant} frozen-surface`}
    data-renderer="photo" data-period={requested} data-shown-period={shown?.period || 'loading'}
    data-tide="0.0000" data-tide-paused="true" data-animation-active="false"
    data-opening-sea-ready={!!sea}
    data-light-settled={settled && shown?.period === requested} data-surface-study="16"
    data-mapping={JSON.stringify(placement.mapping)} data-shore-master="user-daylight-calm-v2" aria-hidden="true">
    <AnimatePresence initial={false}>
      {shown && <motion.div key={shown.shore} className="frozen-photo-layer"
        initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
        transition={{ duration: reduced || !active ? 0 : .6 }} onAnimationComplete={() => setSettled(true)}>
        <img className="frozen-photo" src={shown.shore} style={styleFor(shown)} alt="" draggable={false} />
      </motion.div>}
    </AnimatePresence>
    {opening && shown && sea?.period === shown.period && !reduced && <OpeningWater sea={sea.image} reference={sea.reference}
      progress={tideValue} photoStyle={styleFor(shown)} active={active} reduced={reduced} />}
    {!shown && !error && <div className="surface-loading" />}
    {!shown && error && <div className="surface-error">{error}</div>}
  </div>;
}
