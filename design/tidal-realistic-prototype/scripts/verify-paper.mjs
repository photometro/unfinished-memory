// Pure geometry checks. This does not automate a browser or accept visual taste.
import assert from 'node:assert/strict';
import { BOAT_EDGE, BOAT_HUB, PAPER_FACES, PAPER_EDGE, affineTriangle, paperColor, paperPose, sourceTriangle, triangle } from '../src/paper-geometry.js';

let samples=0;
const near=(a,b,tolerance=1e-6)=>assert.ok(Math.abs(a-b)<tolerance,`${a} differs from ${b}`);
for(const [width,height] of [[294,549],[373,828],[320,510],[480,900]]) {
  const first=paperPose(0,width,height), last=paperPose(1,width,height);
  PAPER_EDGE.forEach(([x,y],index)=>{
    near(first.edge[index][0],(x-.5)*width);
    near(first.edge[index][1],(y-.5)*height);
  });
  [...BOAT_EDGE,BOAT_HUB].forEach(([x,y],index)=>{
    const point=index<10?last.edge[index]:last.hub;
    near(point[0],(x-80)*width/160); near(point[1],(y-50)*width/160);
  });
  assert.equal(last.printTop,false); assert.equal(last.printBottom,false);
  for(let step=0;step<=1000;step++) {
    const state=paperPose(step/1000,width,height);
    for(const face of PAPER_FACES) {
      const points=triangle(state.edge,state.hub,face.index);
      assert.ok(points.flat().every(Number.isFinite));
      assert.equal(points[1],state.edge[(face.index+1)%10]);
      assert.equal(points[2],state.hub);
      const matrix=affineTriangle(sourceTriangle(width,height,face.index),points);
      assert.ok(!/NaN|Infinity/.test(matrix)); samples++;
    }
  }
  for(const key of [.44,.67,.84]) {
    const a=paperPose(key-1e-7,width,height),b=paperPose(key+1e-7,width,height);
    [...a.edge,a.hub].flat().forEach((value,index)=>near(value,[...b.edge,b.hub].flat()[index],.005));
  }
}
assert.equal(paperColor('rgb(237,242,238)','#fcf8ed',0),'rgb(237,242,238)');
assert.equal(paperColor('rgb(237,242,238)','#fcf8ed',1),'rgb(252,248,237)');
console.log(`PASS: ${samples} finite face/matrix samples; shared edges, exact endpoints, key-pose continuity and colour endpoints.`);
