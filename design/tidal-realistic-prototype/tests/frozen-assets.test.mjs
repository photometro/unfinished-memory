import test from 'node:test';
import assert from 'node:assert/strict';
import { loadFrozenImages } from '../src/frozen-assets.js';

const fixture = load => {
  const events = [];
  const pending = loadFrozenImages({ load, shoreUrl: 'shore', seaUrl: 'sea',
    onShore: () => events.push('shore'), onSea: ok => events.push(['sea', ok]),
    onError: () => events.push('error'), onReady: () => events.push('ready') });
  return { ...pending, events };
};

test('sea failure keeps the shore visible and releases the intro', async () => {
  const run = fixture(url => url === 'sea' ? Promise.reject(new Error('injected sea failure')) : Promise.resolve());
  await run.completed;
  assert.ok(run.events.includes('shore'));
  assert.ok(run.events.includes('ready'));
  assert.ok(!run.events.includes('error'));
  assert.deepEqual(run.events.find(Array.isArray), ['sea', false]);
});

test('shore failure releases intro independently of an unresolved sea', async () => {
  const run = fixture(url => url === 'shore' ? Promise.reject(new Error('injected shore failure')) : new Promise(() => {}));
  await new Promise(resolve => setImmediate(resolve));
  assert.deepEqual(run.events, ['error', ['sea', false], 'ready']);
  run.cancel();
});

test('slow sea is rejected at the admission deadline and cannot pop into the entry', async () => {
  const events = []; let arrive;
  const run = loadFrozenImages({ load: url => url === 'shore' ? Promise.resolve('shore-image') : new Promise(resolve => { arrive = resolve; }),
    shoreUrl: 'shore', seaUrl: 'sea', openingWaitMs: 10,
    onShore: image => events.push(image), onSea: ok => events.push(ok), onReady: () => events.push('ready'), onError: () => {} });
  await run.completed;
  assert.deepEqual(events, ['shore-image', false, 'ready']);
  arrive('late-sea'); await new Promise(resolve => setImmediate(resolve));
  assert.deepEqual(events, ['shore-image', false, 'ready']);
});

test('decoded sea is admitted before opening readiness, with its period bundle intact', async () => {
  const events = [], bundle = { image: 'sea', reference: 'daylight' };
  const run = loadFrozenImages({ load: url => Promise.resolve(url === 'sea' ? bundle : 'shore'), shoreUrl: 'shore', seaUrl: 'sea',
    onShore: () => {}, onSea: (ok, image) => events.push([ok, image]), onReady: () => events.push('ready'), onError: () => {} });
  await run.completed; assert.deepEqual(events, [[true, bundle], 'ready']);
});

test('cancelled image loads cannot replace a newer state or report readiness', async () => {
  const resolves = [];
  const run = fixture(() => new Promise(resolve => resolves.push(resolve)));
  run.cancel();
  resolves.forEach(resolve => resolve());
  await run.completed;
  assert.deepEqual(run.events, []);
});
