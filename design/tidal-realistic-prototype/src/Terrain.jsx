'use client';
import React, { memo } from 'react';
import { RealisticSurface } from './RealisticSurface.jsx';

export const Terrain = memo(function Terrain({ variant, period, active, reduced }) {
  return <RealisticSurface {...{ variant,period,active,reduced }} level={0} />;
});
