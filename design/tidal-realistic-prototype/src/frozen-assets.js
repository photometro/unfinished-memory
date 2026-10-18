// Shore and introductory sea are independent: losing the sea cannot hide a shore.
export function loadFrozenImages({ load, shoreUrl, seaUrl, onShore, onSea, onError, onReady, openingWaitMs = 1800 }) {
  let cancelled = false;
  let timeout, cancelWait;
  const shore = load(shoreUrl).then(image => {
    if (!cancelled) onShore(image);
    return true;
  }).catch(error => {
    if (!cancelled) onError(error);
    return false;
  });
  // A late sea cannot pop into an already-running intro. Wait a bounded time,
  // then seal the decision; missing shore still releases navigation immediately.
  const sea = load(seaUrl).then(image => ({ ok: true, image }), () => ({ ok: false }));
  const completed = shore.then(async ok => {
    if (cancelled) return;
    const result = ok ? await Promise.race([sea, new Promise(resolve => {
      cancelWait = () => resolve({ ok: false });
      timeout = setTimeout(() => resolve({ ok: false }), openingWaitMs);
    })]) : { ok: false };
    clearTimeout(timeout);
    if (!cancelled) { onSea(result.ok, result.image); onReady(); }
  });
  return { cancel: () => { cancelled = true; clearTimeout(timeout); cancelWait?.(); }, completed };
}
