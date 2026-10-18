// One proportion-preserving cover affine for WebGL, CPU and every viewport.
export function coverMapping(width, height, imageWidth, imageHeight, variant = 'A') {
  const rotated = variant === 'B';
  const rw = rotated ? imageHeight : imageWidth, rh = rotated ? imageWidth : imageHeight;
  const scale = Math.max(width / rw, height / rh);
  const sx = width / (rw * scale), sy = height / (rh * scale);
  const ox = (1 - sx) * .5, oy = (1 - sy) * .5;
  if (variant === 'B') return { x: [0, -sy, 1 - oy], y: [sx, 0, ox], scale };
  if (variant === 'C') return { x: [-sx, 0, 1 - ox], y: [0, -sy, 1 - oy], scale };
  return { x: [sx, 0, ox], y: [0, sy, oy], scale };
}
export function sourcePoint(mapping, x, y) {
  return [mapping.x[0] * x + mapping.x[1] * y + mapping.x[2], mapping.y[0] * x + mapping.y[1] * y + mapping.y[2]];
}
// CSS photograph projection matches the existing affine, including B/C rotation.
export function photographPlacement(width, height, imageWidth, imageHeight, variant = 'A') {
  const mapping = coverMapping(width, height, imageWidth, imageHeight, variant);
  return { width: imageWidth * mapping.scale, height: imageHeight * mapping.scale,
    rotation: variant === 'B' ? -90 : variant === 'C' ? 180 : 0, mapping };
}
export function canvasBudget(width, height, dpr = 1) {
  const ratio = Math.min(dpr, 1.25, Math.sqrt(1000000 / Math.max(1, width * height)));
  return { width: Math.max(1, Math.round(width * ratio)), height: Math.max(1, Math.round(height * ratio)) };
}
