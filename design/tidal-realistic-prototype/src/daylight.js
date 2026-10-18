import { canonicalPeriod } from './environment-state.js';
// Reviewed bitmap variants, not a neutral master with a second color filter.
const SHORE_BASE = `${import.meta.env.BASE_URL}environment/three-state-v2/`;
export const SHORE_MASTER_CANDIDATE = `${SHORE_BASE}shore-daylight-source.png`;
// Paired photographic seas; no old color matrix applied a second time.
export const SEA_BASE = `${SHORE_BASE}sea-daylight.png`;
export const DAYLIGHT = {
  morning: { label: '日光', caption: '明亮柔和，留给上午与中午。', shore: `${SHORE_BASE}shore-daylight.png`, sea: SEA_BASE },
  afternoon: { label: '斜阳', caption: '低角度暖光，照过同一片潮滩。', shore: `${SHORE_BASE}shore-slant.png`, sea: `${SHORE_BASE}sea-slant.png` },
  night: { label: '夜色', caption: '银蓝夜色，潮沟仍然可见。', shore: `${SHORE_BASE}shore-night.png`, sea: `${SHORE_BASE}sea-night.png` },
};
export const PERIODS = Object.keys(DAYLIGHT);
export const validPeriod = value => canonicalPeriod(value) || 'morning';
