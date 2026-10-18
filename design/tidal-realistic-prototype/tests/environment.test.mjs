import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { periodForLocalTime, resolvePeriod, artisticTide, targetTide, introTide, lightingMatrix, lightingVector, applyLight, msUntilNextPeriod,TIDE_CYCLE_MS, PERIOD_KEYS, canonicalPeriod, HOME_TIDE_PAUSED, FROZEN_HOME_LEVEL, homeTideTarget } from '../src/environment-state.js';
import { MATERIAL, MATERIAL_GLSL, terrainFeatures, tideMaterial, illuminate, illuminateInto,wetHistory } from '../src/environment-material.js';
import { coverMapping, sourcePoint, canvasBudget, photographPlacement } from '../src/environment-geometry.js';
import { connectedLevels,calibratedTerrain,createTerrainAtlas,bankDistances,SHORE_MAP_VERSION } from '../src/shore-terrain.js';

for (const [h, m, expected] of [[5,59,'night'],[6,0,'morning'],[10,59,'morning'],[11,0,'morning'],[13,59,'morning'],[14,0,'afternoon'],[17,59,'afternoon'],[18,0,'night'],[23,59,'night'],[0,0,'night']]) {
  test(`local time ${h}:${m} → ${expected}`, () => assert.equal(periodForLocalTime(new Date(2026, 9, 4, h, m)), expected));
}
test('dark priority, design preview, clearing override uses then-current local time', () => {
  for (const h of [7,12,16,23]) {
    const date = new Date(2026, 9, 4, h);
    assert.equal(resolvePeriod({ date, forceDark: true, previewPeriod: 'afternoon' }), 'night');
    assert.equal(resolvePeriod({ date }), periodForLocalTime(date));
  }
  assert.equal(resolvePeriod({ date: new Date(2026,9,4,23), previewPeriod: 'noon' }), 'morning');
  assert.equal(resolvePeriod({ date: new Date(2026,9,4,18), previewPeriod: null }), 'night');
  assert.equal(resolvePeriod({ date: new Date(2026,9,4,14), previewPeriod: 'invalid' }), 'afternoon');
});
test('foreground boundary scheduler targets exact next local boundary', () => {
  for (const h of [5,13,17]) assert.equal(msUntilNextPeriod(new Date(2026,9,4,h,59,59)), 1000);
  assert.equal(msUntilNextPeriod(new Date(2026,9,4,10,59,59)), 3*3600000+1000);
  assert.equal(msUntilNextPeriod(new Date(2026,9,4,23,59,59)), 6*3600000+1000);
});
test('artistic tide range, smooth phase and light-independent targets', () => {
  for (let n = 0; n <= 480000; n += 1000) {
    assert.ok(artisticTide(n) >= .08 - 1e-10 && artisticTide(n) <= .52 + 1e-10);
    assert.ok(Math.abs(artisticTide(n + 1) - artisticTide(n)) < .00001);
  }
  assert.equal(targetTide(0, 'high'), .52);
  assert.equal(artisticTide(0), artisticTide(TIDE_CYCLE_MS));
  assert.equal(targetTide(0,'accelerated'),targetTide(480000,'accelerated'));
  assert.ok(TIDE_CYCLE_MS > 12*3600000);
});
test('intro endpoints, skip and changing targets remain same environment', () => {
  for (const target of [.08,.3,.52]) {
    assert.equal(introTide(1, target), 1); assert.equal(introTide(0, target), target);
    assert.equal(introTide(-1, target), target);
    for (let p=0;p<=1;p+=.01) assert.ok(introTide(p,target) >= target && introTide(p,target) <= 1);
  }
  assert.equal(introTide(0, targetTide(120000)), targetTide(120000));
});
test('archived four light fields remain finite and complete for later restoration', () => {
  const colors = ['morning','noon','afternoon','night'].map(p => {
    const light = lightingVector(p);
    assert.equal(light.length, 19); assert.ok(light.every(Number.isFinite));
    assert.ok(applyLight([.2,.5,.8], lightingMatrix(p)).every(c => c >= 0 && c <= 1));
    return illuminate([.68,.67,.64], light, .2, .15, 0);
  });
  assert.equal(new Set(colors.map(c => c.map(n => n.toFixed(3)).join(','))).size, 4);
  const sunlit=illuminate([.68,.67,.64],lightingVector('morning'),.2,.15,0,-.6,-.2);
  const shadow=illuminate([.68,.67,.64],lightingVector('morning'),.2,.15,0,.6,.2);
  assert.ok(sunlit[0]>shadow[0], 'Morning has local sunlit banks and cool shadows');
  assert.ok(colors[1].reduce((a,b)=>a+b) > colors[0].reduce((a,b)=>a+b), 'Noon is visibly more luminous');
  assert.ok(colors[2][0] - colors[2][2] > colors[0][0] - colors[0][2], 'Afternoon warmer than morning');
  assert.ok(colors[3][2] > colors[3][0], 'Night is blue, not a black veil');
  assert.ok(colors[3].every(c => c > .15 && c < .5), 'Night terrain remains legible');
});
test('full-sea intro and exposed high sand use calibrated terrain, not a vertical wipe', () => {
  assert.equal(tideMaterial(.94,0,1).intro,1);
  assert.equal(tideMaterial(.80,0,.52).water,0);
  assert.equal(tideMaterial(.05,1,.08).water,1);
  assert.equal(tideMaterial(.05,1,.08).flood,0,'Original channel colors are retained');
  assert.ok(calibratedTerrain([.7,.68,.65],.23,.655).height > .52);
  assert.ok(calibratedTerrain([.7,.68,.65],.42,.58).height > .52,'A centerline cannot invent water on photographed sand');
  assert.ok(calibratedTerrain([.43,.52,.55],.42,.58).height < .1);
});
test('water is monotone and continuous through rising and falling tide, independent of light', () => {
  for (let v=0;v<=1;v+=.025) for (const channel of [0,.25,.5,1]) {
    const { height } = terrainFeatures([.6,.61,.6 + channel*.1], .5, v);
    let previous = tideMaterial(height,channel,0).water;
    for (let tide=.001;tide<=1;tide+=.001) {
      const current = tideMaterial(height,channel,tide).water;
      assert.ok(current >= previous - 1e-12);
      assert.ok(Math.abs(current - previous) < .055);
      previous = current;
    }
  }
});
test('low/high tide changes exposed sand and produces a localized damp edge', () => {
  const dry = {height:.30,channel:0};
  const low = tideMaterial(dry.height,dry.channel,.08), high = tideMaterial(dry.height,dry.channel,.52);
  assert.ok(high.water - low.water > .75);
  const edge = tideMaterial(dry.height,dry.channel,dry.height - .015);
  assert.ok(edge.wet > .2 && edge.water < .4);
  assert.ok(tideMaterial(dry.height,dry.channel,dry.height - .2).wet < .001);
  assert.equal(tideMaterial(dry.height,dry.channel,1).wet,0);
  assert.ok(tideMaterial(.3,0,.08,{},.32).wet>.8,'Recently receded water leaves damp sand');
});
test('shallow water retains original photo while old sea appears only in opening', () => {
  const original=tideMaterial(.03,1,.08);
  assert.equal(original.flood,0); assert.equal(original.intro,0);
  assert.equal(tideMaterial(.03,1,.52).intro,0);
  assert.ok(MATERIAL.ripple < .001);
});
test('opening normal/skip/reduced initializes identical wet history; drying does not depend on frame rate',()=>{
  let recent=1;
  for(let q=1;q<=100;q++) recent=wetHistory(recent,1-.92*q/100,.05,true,true);
  const normal=wetHistory(recent,.08,.05,false,true);
  const skipped=wetHistory(1,.08,.05,false,true);
  assert.equal(normal,skipped); assert.equal(normal,.08);
  let fast=.52; for(let i=0;i<120;i++)fast=wetHistory(fast,.08,1/60);
  assert.ok(Math.abs(fast-wetHistory(.52,.08,2))<1e-12);
  assert.ok(wetHistory(.52,.08,2)<.52);
  assert.equal(wetHistory(.52,.08,600),.08);
});
test('sea-connected minimax flood cannot fill a basin through an unpassed ridge', () => {
  const heights=Float32Array.from([.02,.10,.60,.15,.70]),seeds=Uint8Array.from([1,0,0,0,0]);
  const levels=connectedLevels(heights,5,1,seeds);
  assert.ok(Math.abs(levels[1]-.10)<1e-6);
  assert.ok(Math.abs(levels[3]-.60)<1e-6,'Low basin requires ridge threshold');
  assert.equal(tideMaterial(levels[3],0,.52).water,0);
  assert.equal(tideMaterial(levels[4],0,.52).water,0);
  assert.throws(()=>connectedLevels(heights,5,1,new Uint8Array(5)),/seeds/);
});
test('source atlas quantization, connectivity and local normals are independent of lighting', () => {
  const w=40,h=80,pixels=new Uint8ClampedArray(w*h*4); pixels.fill(180);
  const a=createTerrainAtlas(pixels,w,h);
  assert.equal(a.version,SHORE_MAP_VERSION); assert.equal(a.data.length,w*h*4);
  assert.ok(a.levels.every(Number.isFinite));
  for(let q=0;q<w*h;q++) assert.equal(a.levels[q],Math.fround(a.data[q*4]/255));
  assert.ok(new Set(a.data.filter((_,i)=>i%4===2)).size>4,'Bank-facing local normals');
});
test('bank distance follows actual photo-water samples rather than creek polylines',()=>{
  const d=bankDistances(Float32Array.from([0,0,1,0,0,0,0,0,0]),3,3);
  assert.equal(d[2],0); assert.ok(Math.abs(d[1]-1/3)<1e-6);
  assert.ok(d[6]>d[5]);
});
test('CPU in-place illumination is the same helper as pure reference without array churn', () => {
  for (const p of ['morning','noon','afternoon','night']) for (const w of [0,.5,1]) {
    const light = lightingVector(p), color = [.64,.68,.70], out = new Float64Array(7);
    illuminateInto(...color,light,.3,.8,w,out,2,255);
    illuminate(color,light,.3,.8,w).forEach((n,i)=>assert.ok(Math.abs(n-out[i+2]/255)<1e-12));
  }
});
test('both rendered paths use calibrated atlas, complete local lighting and restored opening source', () => {
  const shader = readFileSync(new URL('../src/water-shader.js',import.meta.url),'utf8');
  const renderer = readFileSync(new URL('../src/RealisticSurface.jsx',import.meta.url),'utf8');
  const assets = readFileSync(new URL('../src/daylight.js',import.meta.url),'utf8');
  assert.ok(shader.includes('${MATERIAL_GLSL}') && MATERIAL_GLSL.includes('tidalMaterial'));
  assert.ok(renderer.includes('tideMaterial(threshold,channel,tide,sample,recent)'));
  assert.ok(renderer.includes('illuminateInto(') && renderer.includes('createTerrainAtlas('));
  assert.ok(assets.includes('three-state-v2/'));
  assert.ok(assets.includes('sea-daylight.png') && assets.includes('sea-slant.png') && assets.includes('sea-night.png'));
  assert.ok(shader.includes('u_terrain') && !shader.includes('heightAt(channel, uv)'));
});
for (const [w,h] of [[320,568],[360,640],[390,844],[430,932],[768,1024]]) for (const variant of ['A','B','C']) {
  test(`proportional cover ${w}×${h} ${variant}`, () => {
    const map = coverMapping(w,h,887,1774,variant);
    assert.ok(sourcePoint(map,.5,.5).every(v => Math.abs(v - .5) < 1e-10));
    for (const x of [0,1]) for (const y of [0,1]) for (const uv of sourcePoint(map,x,y)) assert.ok(uv >= -1e-10 && uv <= 1+1e-10);
    const dx = Math.hypot(map.x[0]*887/w,map.y[0]*1774/w);
    const dy = Math.hypot(map.x[1]*887/h,map.y[1]*1774/h);
    assert.ok(Math.abs(dx-dy)<1e-10, 'No stretch on either axis');
    const budget = canvasBudget(w,h,3);
    assert.ok(budget.width*budget.height <= 1002000);
    const photo = photographPlacement(w,h,887,1774,variant);
    assert.equal(photo.width/887,photo.height/1774);
    assert.deepEqual(photo.mapping,map);
    const shownWidth=variant==='B'?photo.height:photo.width;
    const shownHeight=variant==='B'?photo.width:photo.height;
    assert.ok(shownWidth>=w-1e-9 && shownHeight>=h-1e-9,'Rotated photograph covers without exposed corners');
  });
}
test('one persistent surface; intro never creates/replaces a renderer', () => {
  const main = readFileSync(new URL('../src/main.jsx',import.meta.url),'utf8');
  const ocean = readFileSync(new URL('../src/Ocean.jsx',import.meta.url),'utf8');
  const renderer = readFileSync(new URL('../src/RealisticSurface.jsx',import.meta.url),'utf8');
  assert.equal((main.match(/<RealisticSurface\b/g)||[]).length,1);
  assert.ok(!ocean.includes('RealisticSurface'));
  assert.ok(!main.includes('level={0}'));
  assert.ok(renderer.includes('[shoreMaster, forceStatic, tideValue]'));
  assert.ok(renderer.includes('observer?.disconnect()') && renderer.includes('offTide?.()'));
});
test('three active states and legacy noon resolve without an extra switching boundary',()=>{
  assert.deepEqual(PERIOD_KEYS,['morning','afternoon','night']);
  assert.equal(canonicalPeriod('noon'),'morning');
  assert.equal(canonicalPeriod('unknown'),null);
  for(let minute=0;minute<24*60;minute++) {
    const hour=Math.floor(minute/60);
    assert.equal(periodForLocalTime(new Date(2026,9,5,hour,minute%60)),hour>=6&&hour<14?'morning':hour>=14&&hour<18?'afternoon':'night');
  }
});
test('paused homepage ignores old tide modes and elapsed wall-clock time',()=>{
  assert.equal(HOME_TIDE_PAUSED,true);
  assert.equal(FROZEN_HOME_LEVEL,0);
  for(const mode of ['auto','accelerated','low','mid','high','invalid']) for(const now of [0,123456,TIDE_CYCLE_MS,Date.now()]) assert.equal(homeTideTarget(now,mode),0);
  assert.equal(homeTideTarget(0,'high',false),.52,'Archived artistic tide remains available for a later explicit restore');
});
test('active surface uses reviewed unfiltered photos, not the archived tide renderer',()=>{
  const renderer=readFileSync(new URL('../src/RealisticSurface.jsx',import.meta.url),'utf8');
  const frozen=readFileSync(new URL('../src/FrozenSurface.jsx',import.meta.url),'utf8');
  const hook=readFileSync(new URL('../src/useEnvironment.js',import.meta.url),'utf8');
  const main=readFileSync(new URL('../src/main.jsx',import.meta.url),'utf8');
  const selector=readFileSync(new URL('../src/DaylightSelector.jsx',import.meta.url),'utf8');
  assert.ok(renderer.includes('tidePaused ? <FrozenSurface'));
  assert.ok(!frozen.includes('requestAnimationFrame(')&&!frozen.includes('createTerrainAtlas(')&&!frozen.includes('wetHistory('));
  assert.ok(frozen.includes('style={styleFor(shown)}') && frozen.includes('OpeningWater') && frozen.includes('photographPlacement('));
  assert.ok(hook.includes("tidePaused ? 'paused'") && hook.indexOf('if (tidePaused)')<hook.indexOf('setInterval(sync, 1000)'));
  assert.ok(!main.includes('aria-label="模拟潮位"')&&!main.includes('重播退潮'));
  assert.ok(selector.includes('grid-cols-3')&&!selector.includes('grid-cols-4'));
  assert.ok(!selector.includes('lightingMatrix') && !selector.includes('feColorMatrix'),'Paired sea assets and shore receive no second color filter');
});
