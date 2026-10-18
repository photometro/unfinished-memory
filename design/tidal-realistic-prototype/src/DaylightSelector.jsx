'use client';
import React from 'react';
import { Sun, Moon } from '@phosphor-icons/react';
import { DAYLIGHT, PERIODS } from './daylight.js';

// Shared leaf component; the selectors change only lighting, never route, tide or fictional memories.
export function DaylightSelector({ period, onChange }) {
  return <div className="daylight-selector grid grid-cols-3 gap-1" role="group" aria-label="海域时段">
    {PERIODS.map(key => {
      const Icon = key === 'night' ? Moon : Sun;
      return <button key={key} type="button" className={`daylight-option ${period === key ? 'selected' : ''}`} data-period={key} aria-pressed={period === key} onClick={() => onChange(key)}>
        <Icon size={17} weight="light" /><span>{DAYLIGHT[key].label}</span>
      </button>;
    })}
  </div>;
}

export function AssetComparison() {
  return <details className="asset-comparison mt-7">
    <summary>三态素材 · 固定岸线对照</summary>
    <p className="mt-2">指定日光图局部平静水面，斜阳与夜色保留各自光影；开屏使用同态配套海面。只播放一次退潮入场，首页岸线固定，图片不叠加调色。</p>
    <div className="mt-4 grid grid-cols-3 gap-2">
      {['shore', 'sea'].map(kind => PERIODS.map(period => <figure key={`${kind}-${period}`}>
        <img src={DAYLIGHT[period][kind]} alt={`${DAYLIGHT[period].label}${kind === 'shore' ? '平静潮滩候选' : '配套海面候选'}`} loading="lazy" />
        <figcaption>{DAYLIGHT[period].label} · {kind === 'shore' ? '潮滩' : '海面'}</figcaption>
      </figure>))}
    </div>
  </details>;
}
