import { useEffect, useRef, useState } from 'react';
import { animate, useMotionValue } from 'framer-motion';
import { resolvePeriod, homeTideTarget, msUntilNextPeriod, canonicalPeriod, HOME_TIDE_PAUSED } from './environment-state.js';

export function useEnvironment(intro, reduced, tidePaused = HOME_TIDE_PAUSED) {
  const [now, setNow] = useState(() => new Date());
  const [previewPeriod, setPreviewPeriod] = useState(() => canonicalPeriod(new URLSearchParams(location.search).get('period')));
  const [clockPreview, setClockPreview] = useState('');
  const clockAnchor = useRef(Date.now());
  const [forceDark, setForceDark] = useState(false);
  const [tidePreview, setTidePreview] = useState(() => tidePaused ? 'paused' : new URLSearchParams(location.search).get('tide') || 'auto');
  // In the paused branch this value is used only for intro sea opacity.
  const tideValue = useMotionValue(1), targetValue = useMotionValue(homeTideTarget(Date.now(), tidePreview, tidePaused));
  const introRef = useRef(intro), reducedRef = useRef(reduced), tidePreviewRef = useRef(tidePreview);
  introRef.current = intro; reducedRef.current = reduced; tidePreviewRef.current = tidePreview;
  useEffect(() => {
    let boundaryTimer;
    const sync = () => { const current = new Date(); setNow(current); const simulated = clockPreview ? new Date(new Date(clockPreview).getTime() + current.getTime() - clockAnchor.current) : current; clearTimeout(boundaryTimer); boundaryTimer = setTimeout(sync, msUntilNextPeriod(simulated)); };
    sync();
    const interval = setInterval(sync, 15000);
    const visible = () => { if (!document.hidden) sync(); };
    document.addEventListener('visibilitychange', visible);
    window.addEventListener('focus', sync); window.addEventListener('pageshow', sync);
    return () => { clearInterval(interval); clearTimeout(boundaryTimer); document.removeEventListener('visibilitychange', visible); window.removeEventListener('focus', sync); window.removeEventListener('pageshow', sync); };
  }, [clockPreview]);
  useEffect(() => {
    if (tidePaused) {
      targetValue.set(homeTideTarget(Date.now(), tidePreview, true));
      if (!intro) tideValue.set(targetValue.get());
      return; // No tidal timer, visibility task, wet history or homepage animation.
    }
    let controls;
    const sync = () => {
      if (document.hidden) return;
      const target = homeTideTarget(Date.now(), tidePreviewRef.current, false);
      targetValue.set(target);
      if (!introRef.current) {
        controls?.stop();
        if (reducedRef.current) tideValue.set(target);
        else controls = animate(tideValue, target, { duration: .7, ease: 'easeInOut' });
      }
    };
    const visible = () => { if (document.hidden) controls?.stop(); else sync(); };
    sync(); const interval = setInterval(sync, 1000);
    document.addEventListener('visibilitychange', visible);
    return () => { clearInterval(interval); controls?.stop(); document.removeEventListener('visibilitychange', visible); };
  }, [intro, reduced, tidePreview, tidePaused, tideValue, targetValue]);
  const effectiveDate = clockPreview ? new Date(new Date(clockPreview).getTime() + now.getTime() - clockAnchor.current) : now;
  const period = resolvePeriod({ date: effectiveDate, forceDark, previewPeriod });
  const changePeriod = next => {
    next = canonicalPeriod(next);
    setPreviewPeriod(next); setClockPreview('');
    const url = new URL(location.href);
    if (next) url.searchParams.set('period', next); else url.searchParams.delete('period');
    history.replaceState(null, '', url);
  };
  const changeDark = checked => {
    setForceDark(checked);
    if (!checked) { changePeriod(null); setNow(new Date()); }
  };
  const changeClock = value => { clockAnchor.current = Date.now(); setNow(new Date()); setPreviewPeriod(null); setClockPreview(value); };
  return { period, source: forceDark ? '手动深色' : previewPeriod || clockPreview ? '设计预览' : '当地时间自动', forceDark, changeDark, changePeriod, clockPreview, changeClock, tidePaused, tidePreview, setTidePreview, tideValue, targetValue };
}
