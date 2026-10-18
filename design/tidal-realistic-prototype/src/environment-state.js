// Environment presentation only. Local clock and artistic tide never select memories.
import { terrainFeatures, tideMaterial } from './environment-material.js';
export { smoothstep } from './environment-material.js';
export const PERIOD_KEYS = ['morning', 'afternoon', 'night'];
// Paused, not deleted. Old tide/atlas helpers remain below for later restoration.
export const HOME_TIDE_PAUSED = true;
export const FROZEN_HOME_LEVEL = 0;
export function canonicalPeriod(value) {
  if (value === 'noon') return 'morning'; // Existing review URLs remain usable.
  return PERIOD_KEYS.includes(value) ? value : null;
}
export const LIGHTING = {
  morning: { brightness: 1.14, saturation: 1, gain: [1.07, 1.025, .985], lift: [.008, .005], contrast: 1.065, direction: [-.88, -.47], beam: .24, tint: [1, .62, .20], glint: .012 },
  // Historical noon field for archived renderer tests, not an active state.
  noon: { brightness: 1.19, saturation: 1.04, gain: [1.02, 1.01, .985], lift: [.004, .004], contrast: 1.08, direction: [.10, -.995], beam: .065, tint: [1, .97, .90], glint: .015 },
  afternoon: { brightness: 1.055, saturation: .97, gain: [1.105, 1.035, .915], lift: [.005, .005], contrast: 1.085, direction: [.89, -.46], beam: .26, tint: [1, .54, .14], glint: .013 },
  night: { brightness: .52, saturation: .82, gain: [.82, .98, 1.23], lift: [.008, .010], contrast: 1.035, direction: [-.71, -.70], beam: .10, tint: [.25, .52, 1], glint: .017 },
};
export function periodForLocalTime(date = new Date()) {
  const hour = date.getHours();
  if (hour >= 6 && hour < 14) return 'morning';
  if (hour >= 14 && hour < 18) return 'afternoon';
  return 'night';
}
export function resolvePeriod({ date = new Date(), forceDark = false, previewPeriod = null } = {}) {
  return forceDark ? 'night' : canonicalPeriod(previewPeriod) || periodForLocalTime(date);
}
export function msUntilNextPeriod(date = new Date()) {
  const next = new Date(date), hour = date.getHours();
  const boundary = hour < 6 ? 6 : hour < 14 ? 14 : hour < 18 ? 18 : 30;
  next.setHours(boundary, 0, 0, 0);
  return Math.max(1, next.getTime() - date.getTime());
}
// Illustrative half-daily rhythm, not a location-based tide forecast.
// The accelerated eight-minute loop is explicit design-workbench preview only.
export const TIDE_CYCLE_MS = 12*3600000+25*60000;
export function artisticTide(now = Date.now(), cycle = TIDE_CYCLE_MS) {
  return .08 + .44 * (.5 - .5 * Math.cos(2 * Math.PI * ((now % cycle) / cycle)));
}
export const TIDE_LEVELS = { low: .08, mid: .30, high: .52 };
export function targetTide(now, preview = 'auto') { return TIDE_LEVELS[preview] ?? artisticTide(now,preview==='accelerated'?480000:TIDE_CYCLE_MS); }
export function homeTideTarget(now, preview = 'auto', paused = HOME_TIDE_PAUSED) {
  return paused ? FROZEN_HOME_LEVEL : targetTide(now, preview);
}
export function introTide(progress, target) { return target + (1 - target) * Math.max(0, Math.min(1, progress)); }
// Row-major matrix shared by CPU fallback and GLSL. No geometry depends on lighting.
export function lightingMatrix(period) {
  const { brightness: b, saturation: s, gain } = LIGHTING[period] || LIGHTING.noon;
  const luma = [.2126, .7152, .0722];
  return gain.flatMap((g, row) => luma.map((l, col) => b * g * ((1 - s) * l + (row === col ? s : 0))));
}
export function applyLight(rgb, matrix) {
  return [0, 1, 2].map(row => Math.min(1, Math.max(0, rgb.reduce((n, v, col) => n + matrix[row * 3 + col] * v, 0))));
}
export function lightingVector(period) {
  const p = LIGHTING[period] || LIGHTING.noon;
  return [...lightingMatrix(period), ...p.lift, p.contrast, ...p.direction, p.beam, ...p.tint, p.glint];
}
// Same raw-source classification and tide constants in CPU and generated GLSL.
export function terrainWater(rgb, u, v, tide) {
  const { height, channel } = terrainFeatures(rgb, u, v);
  return tideMaterial(height, channel, tide).water;
}
