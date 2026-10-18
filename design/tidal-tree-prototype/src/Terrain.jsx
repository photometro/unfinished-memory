import React, { memo, useMemo } from 'react';
import { buildTerrain } from './channel-geometry.js';

export const Terrain = memo(function Terrain({ variant, night }) {
  const { layers, sea, stats } = useMemo(() => buildTerrain(variant), [variant]);
  const suffix = `${variant}-${night ? 'night' : 'day'}`;
  return (
    <svg viewBox="0 0 720 1440" preserveAspectRatio="none" className="terrain absolute inset-0 h-full w-full pointer-events-none" data-channels={stats.reaches} data-morphology="tidal-creeks" data-model={stats.model} data-disconnected={stats.disconnected} data-flux-errors={stats.reverseFlux} aria-hidden="true">
      <defs>
        <linearGradient id={`sand-${suffix}`} x1="0" y1="0" x2="0.9" y2="1">
          <stop offset="0%" stopColor={night ? '#14272d' : '#f5f2e9'} />
          <stop offset="68%" stopColor={night ? '#152c31' : '#e8e4d9'} />
          <stop offset="100%" stopColor={night ? '#17363d' : '#dce5e1'} />
        </linearGradient>
        <linearGradient id={`water-${suffix}`} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor={night ? '#43616a' : '#b7cdd0'} />
          <stop offset="100%" stopColor={night ? '#2d4a56' : '#92b0b8'} />
        </linearGradient>
        <filter id={`edge-${suffix}`} x="-10%" y="-10%" width="120%" height="120%" colorInterpolationFilters="sRGB">
          <feOffset in="SourceAlpha" dx=".5" dy=".8" result="shifted" />
          <feComposite in="SourceAlpha" in2="shifted" operator="out" result="edge" />
          <feFlood floodColor={night ? '#91abaa' : '#faf5e8'} floodOpacity={night ? '.2' : '.46'} />
          <feComposite in2="edge" operator="in" result="highlight" />
          <feMerge><feMergeNode in="SourceGraphic" /><feMergeNode in="highlight" /></feMerge>
        </filter>
        <filter id={`silt-${suffix}`} x="0" y="0" width="100%" height="100%" colorInterpolationFilters="sRGB">
          <feTurbulence type="fractalNoise" baseFrequency=".56" numOctaves="2" seed="431" />
          <feColorMatrix type="saturate" values="0" />
        </filter>
      </defs>
      <rect width="720" height="1440" fill={`url(#sand-${suffix})`} />
      <rect width="720" height="1440" filter={`url(#silt-${suffix})`} opacity={night ? '.024' : '.032'} />
      <path d={layers.wet} fill={night ? '#78948b' : '#999b8d'} opacity={night ? '.075' : '.10'} />
      <path d={sea} fill={night ? '#81978c' : '#9da798'} opacity=".14" transform="translate(-7 -5)" />
      <path d={sea} fill={`url(#water-${suffix})`} opacity={night ? '.72' : '.85'} />
      <path d={layers.bank} fill={night ? '#7f8d7d' : '#a59e89'} opacity={night ? '.22' : '.32'} />
      <path d={layers.rim} fill={night ? '#182d34' : '#718080'} opacity={night ? '.60' : '.45'} transform="translate(.4 .7)" />
      <path d={layers.water} fill={`url(#water-${suffix})`} opacity={night ? '.89' : '.96'} filter={`url(#edge-${suffix})`} />
      <path d={layers.fine} fill={night ? '#809b9c' : '#879f9f'} opacity={night ? '.48' : '.46'} />
    </svg>
  );
});
