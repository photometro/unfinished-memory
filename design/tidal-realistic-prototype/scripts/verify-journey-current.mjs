import assert from 'node:assert/strict';
import { isCurrentActive } from '../src/journey-state.js';

for (const phase of ['withdrawing', 'lifting', 'folding', 'forming']) {
  assert.equal(isCurrentActive(phase), false, `${phase}: current starts after the paper opens`);
}
for (const phase of ['opening', 'ready', 'drifting', 'complete']) {
  assert.equal(isCurrentActive(phase), true, `${phase}: surrounding current must keep flowing`);
}
assert.equal(isCurrentActive('unknown'), false);
console.log('PASS: current remains active from opening through completion.');
