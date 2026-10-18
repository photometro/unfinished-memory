import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { buildOpeningField, openingAlpha, paintOpeningMask } from '../src/opening-reveal.js';

test('water withdraws spatially: dry flats before channel-connected low ground', () => {
  const width = 12, height = 24, pixels = new Uint8ClampedArray(width * height * 4);
  for (let y = 0; y < height; y++) for (let x = 0; x < width; x++) {
    const water = x > 8 || (x === 6 && y > 9);
    pixels.set(water ? [110, 163, 177, 255] : [227, 224, 213, 255], (y * width + x) * 4);
  }
  const field = buildOpeningField(pixels, width, height);
  const inland = field[2 * width], channel = field[16 * width + 6], sea = field[22 * width + 11];
  assert.ok(inland > channel && inland > sea);
  assert.equal(openingAlpha(inland, .3), 0);
  assert.equal(openingAlpha(channel, .3), 1);
  assert.equal(openingAlpha(sea, .3), 1);
  for (const h of field) {
    assert.equal(openingAlpha(h, 1), 1);
    assert.equal(openingAlpha(h, 0), 0);
    assert.ok(openingAlpha(h, .6) >= openingAlpha(h, .4));
  }
});

test('mask has simultaneously transparent land and opaque water, not a full-frame fade', () => {
  const field = Float32Array.from([.87, .68, .48, .28, .065]);
  const rgba = new Uint8ClampedArray(field.length * 4);
  const coverage = paintOpeningMask(field, .5, rgba);
  const alphas = [...rgba].filter((_, i) => i % 4 === 3);
  assert.equal(alphas[0], 0); assert.equal(alphas.at(-1), 255);
  assert.ok(coverage > 0 && coverage < 1);
  assert.equal(paintOpeningMask(field, 0, rgba), 0);
  assert.equal(paintOpeningMask(field, 1, rgba), 1);
});

test('empty or featureless reference still gives a safe finite spatial fallback', () => {
  const field = buildOpeningField(new Uint8ClampedArray(8 * 16 * 4).fill(220), 8, 16);
  assert.ok(field.every(Number.isFinite));
  assert.ok(new Set(field).size > 1);
});

test('home is static; opening uses its own masked layer and no old light filter', () => {
  const surface = readFileSync(new URL('../src/FrozenSurface.jsx', import.meta.url), 'utf8');
  assert.match(surface, /opening &&.*OpeningWater/s);
  assert.doesNotMatch(surface, /opacity: tideValue|lightingMatrix|feColorMatrix/);
  assert.match(surface, /data-animation-active="false"/);
  const styles = ['styles.css', 'journey.css'].map(file => readFileSync(new URL('../src/' + file, import.meta.url), 'utf8')).join('\n');
  for (const rule of styles.matchAll(/[^{}]*home-interface::before[^{}]*\{([^}]*)\}/g)) {
    assert.doesNotMatch(rule[1], /linear-gradient|rgba\(/);
  }
});
