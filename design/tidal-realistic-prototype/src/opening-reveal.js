// Source-photo space only. This is an artistic entry reveal, NOT tidal simulation.
// The daylight photograph supplies channels in every light state; night hue never
// changes the geography. A distance field exposes higher flats before low creeks.
const clamp = value => Math.max(0, Math.min(1, value));
const smooth = value => { const t = clamp(value); return t * t * (3 - 2 * t); };

export function buildOpeningField(rgba, width, height) {
  const distance = new Float32Array(width * height), field = new Float32Array(distance.length);
  let waterCount = 0;
  for (let i = 0; i < distance.length; i++) {
    const [r, g, b] = rgba.subarray(i * 4, i * 4 + 3);
    const water = g - r > 7 && b - r > 12 && r < 210;
    distance[i] = water ? 0 : width + height;
    if (water) waterCount++;
  }
  if (!waterCount) {
    // Graceful deterministic fallback: marine side remains last. No hard wipe.
    for (let y = 0; y < height; y++) for (let x = 0; x < width; x++)
      distance[y * width + x] = Math.hypot((width - 1 - x) * .7, (height - 1 - y) * .45);
  } else {
    const diagonal = Math.SQRT2;
    for (let y = 0; y < height; y++) for (let x = 0; x < width; x++) {
      const i = y * width + x;
      if (x) distance[i] = Math.min(distance[i], distance[i - 1] + 1);
      if (y) distance[i] = Math.min(distance[i], distance[i - width] + 1);
      if (x && y) distance[i] = Math.min(distance[i], distance[i - width - 1] + diagonal);
      if (x + 1 < width && y) distance[i] = Math.min(distance[i], distance[i - width + 1] + diagonal);
    }
    for (let y = height - 1; y >= 0; y--) for (let x = width - 1; x >= 0; x--) {
      const i = y * width + x;
      if (x + 1 < width) distance[i] = Math.min(distance[i], distance[i + 1] + 1);
      if (y + 1 < height) distance[i] = Math.min(distance[i], distance[i + width] + 1);
      if (x + 1 < width && y + 1 < height) distance[i] = Math.min(distance[i], distance[i + width + 1] + diagonal);
      if (x && y + 1 < height) distance[i] = Math.min(distance[i], distance[i + width - 1] + diagonal);
    }
  }
  for (let y = 0; y < height; y++) for (let x = 0; x < width; x++) {
    const i = y * width + x;
    field[i] = .065 + .80 * Math.pow(clamp(distance[i] / (width * .34)), .52)
      + .04 * (1 - y / Math.max(1, height - 1));
  }
  return field;
}

export function openingAlpha(height, progress) {
  // All pixels opaque at 1 and fully absent at 0. Only boundary pixels soften.
  return smooth((clamp(progress) - height + .025) / .05);
}

export function paintOpeningMask(field, progress, rgba) {
  let coverage = 0;
  for (let i = 0; i < field.length; i++) {
    const alpha = openingAlpha(field[i], progress);
    rgba[i * 4] = rgba[i * 4 + 1] = rgba[i * 4 + 2] = 255;
    rgba[i * 4 + 3] = Math.round(alpha * 255); coverage += alpha;
  }
  return coverage / Math.max(1, field.length);
}
