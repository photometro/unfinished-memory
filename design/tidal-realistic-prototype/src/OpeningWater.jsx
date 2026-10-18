import React, { useEffect, useRef } from 'react';
import { buildOpeningField, paintOpeningMask } from './opening-reveal.js';

// Mounted ONLY during intro. MotionValue's existing entry clock owns updates;
// this component has no RAF, timer, tide model or ongoing homepage animation.
export function OpeningWater({ sea, reference, progress, photoStyle, active, reduced }) {
  const canvasRef = useRef(null);
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas || !sea || !reference || reduced) return;
    const width = Math.min(384, reference.naturalWidth);
    const height = Math.round(width * reference.naturalHeight / reference.naturalWidth);
    canvas.width = width; canvas.height = height;
    const context = canvas.getContext('2d');
    const mask = document.createElement('canvas'); mask.width = width; mask.height = height;
    const maskContext = mask.getContext('2d', { willReadFrequently: true });
    maskContext.drawImage(reference, 0, 0, width, height);
    const field = buildOpeningField(maskContext.getImageData(0, 0, width, height).data, width, height);
    const pixels = maskContext.createImageData(width, height);
    const draw = value => {
      if (document.hidden || !active) return;
      const coverage = paintOpeningMask(field, value, pixels.data);
      maskContext.putImageData(pixels, 0, 0);
      context.clearRect(0, 0, width, height);
      context.globalCompositeOperation = 'source-over';
      context.drawImage(sea, 0, 0, width, height);
      context.globalCompositeOperation = 'destination-in';
      context.drawImage(mask, 0, 0);
      context.globalCompositeOperation = 'source-over';
      canvas.dataset.progress = value.toFixed(4);
      canvas.dataset.coverage = coverage.toFixed(4);
    };
    draw(progress.get());
    const off = progress.on('change', draw);
    const visible = () => { if (!document.hidden) draw(progress.get()); };
    document.addEventListener('visibilitychange', visible);
    return () => { off(); document.removeEventListener('visibilitychange', visible); canvas.width = canvas.height = mask.width = mask.height = 1; };
  }, [sea, reference, progress, active, reduced]);
  return <canvas ref={canvasRef} className="frozen-photo opening-water" style={photoStyle}
    data-reveal="channel-distance-mask" data-home-tide="paused" aria-hidden="true" />;
}
