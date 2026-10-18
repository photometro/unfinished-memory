// Hand calibrated to shore-master-v4, in unchanged source-image coordinates.
// This is an artistic scene map, not surveyed elevation or a physical tide model.
// Trunks provide spatial constraints; raw-photo color only refines banks inside
// those constraints. Lighting, cover crop and viewport never classify terrain.
export const SHORE_MAP_VERSION = 'v4-connected-2';
export const SEA_OUTLINE = [
  [1,.535],[.972,.582],[.91,.613],[.84,.641],[.73,.649],[.63,.671],
  [.665,.715],[.73,.754],[.64,.789],[.556,.843],[.598,.891],
  [.57,.95],[.31,1],[1,1],
];
export const CREEKS = [
  { radius:.018, points:[[.83,.648],[.65,.630],[.44,.620],[.404,.609],[.421,.579],[.389,.537],[.423,.486],[.389,.452],[.409,.425],[.456,.398],[.478,.365]] },
  { radius:.015, points:[[.399,.536],[.272,.489],[.185,.470],[.118,.456],[.069,.431],[.039,.384],[.026,.334],[.015,.277]] },
  { radius:.010, points:[[.40,.618],[.31,.613],[.248,.589],[.216,.564],[.101,.546],[.038,.526],[0,.510]] },
  { radius:.010, points:[[.456,.622],[.466,.545],[.514,.521],[.548,.465],[.575,.430],[.630,.418],[.692,.397]] },
  { radius:.009, points:[[.80,.636],[.783,.586],[.742,.566],[.804,.543],[.834,.519],[.874,.503]] },
  { radius:.010, points:[[.831,.630],[.907,.594],[.946,.554],[.989,.548],[1,.525]] },
  { radius:.008, points:[[.642,.672],[.554,.701],[.492,.735],[.524,.773],[.563,.813],[.496,.854],[.485,.899],[.449,.949],[.314,1]] },
  { radius:.008, points:[[.077,.434],[.105,.376],[.150,.350],[.216,.324],[.257,.306],[.293,.289]] },
  { radius:.006, points:[[.393,.447],[.359,.407],[.380,.381],[.341,.348],[.339,.321]] },
  { radius:.005, points:[[.515,.522],[.591,.494],[.651,.485],[.698,.466]] },
];
// Broad raised sand tongues visible in the master; not rectangular UI guards.
export const HIGH_SAND = [
  [.23,.655,.13,.066,.28], [.35,.741,.13,.068,.27],
  [.425,.874,.075,.049,.29], [.611,.58,.092,.046,.23],
  [.885,.469,.105,.061,.21], [.675,.453,.105,.047,.18],
];
const clamp = x => Math.max(0,Math.min(1,x));
const smooth = (a,b,x) => { const t=clamp((x-a)/(b-a)); return t*t*(3-2*t); };
export function pointInPolygon(u,v,polygon=SEA_OUTLINE) {
  let inside=false;
  for(let i=0,j=polygon.length-1;i<polygon.length;j=i++) {
    const [x,y]=polygon[i], [px,py]=polygon[j];
    if((y>v)!==(py>v) && u < (px-x)*(v-y)/(py-y)+x) inside=!inside;
  }
  return inside;
}
function segmentDistance(u,v,a,b) {
  const dx=b[0]-a[0],dy=(b[1]-a[1])*2;
  const t=clamp(((u-a[0])*dx+(v-a[1])*2*dy)/(dx*dx+dy*dy));
  return Math.hypot(u-a[0]-dx*t,(v-a[1])*2-dy*t);
}
export function calibratedTerrain(rgb,u,v) {
  const sea=pointInPolygon(u,v);
  let distance=2;
  for(const creek of CREEKS) for(let i=1;i<creek.points.length;i++) {
    const d=segmentDistance(u,v,creek.points[i-1],creek.points[i]);
    distance=Math.min(distance,Math.max(0,d-creek.radius*.4));
  }
  const detail=smooth(.015,.055,rgb[2]-rgb[0]);
  const envelope=1-smooth(.065,.135,distance);
  // Original blue water belongs to the creek envelope or open-estuary polygon.
  const water=detail*Math.max(envelope,sea?1:0);
  let height=.58+.13*(1-Math.exp(-distance/.085));
  for(const [x,y,rx,ry,raise] of HIGH_SAND) {
    height+=raise*Math.exp(-2*((u-x)**2/rx**2+(v-y)**2/ry**2));
  }
  // Channels do not get filled with seawater texture: these are just elevations.
  height=height*(1-water)+(.025+.06*(1-v))*water;
  return { height:Math.min(.94,height), channel:water, seed:sea&&water>.8, distance };
}

// Distance to actual photographed water, not to an invented centerline. This
// preserves the irregular source banks, branch tips and islands as tide rises.
export function bankDistances(channels,width,height) {
  const d=Float32Array.from(channels,n=>n>.8?0:1000), diagonal=Math.SQRT2;
  for(let y=0;y<height;y++) for(let x=0;x<width;x++) {
    const q=y*width+x;
    if(x) d[q]=Math.min(d[q],d[q-1]+1);
    if(y) d[q]=Math.min(d[q],d[q-width]+1);
    if(x&&y) d[q]=Math.min(d[q],d[q-width-1]+diagonal);
    if(y&&x+1<width) d[q]=Math.min(d[q],d[q-width+1]+diagonal);
  }
  for(let y=height-1;y>=0;y--) for(let x=width-1;x>=0;x--) {
    const q=y*width+x;
    if(x+1<width) d[q]=Math.min(d[q],d[q+1]+1);
    if(y+1<height) d[q]=Math.min(d[q],d[q+width]+1);
    if(x+1<width&&y+1<height) d[q]=Math.min(d[q],d[q+width+1]+diagonal);
    if(x&&y+1<height) d[q]=Math.min(d[q],d[q+width-1]+diagonal);
  }
  return Float32Array.from(d,n=>n/width);
}

// Priority flood: each pixel stores the smallest possible maximum terrain
// threshold on a path to the sea. Isolated basins cannot fill through a ridge.
export function connectedLevels(heights,width,height,seeds) {
  const count=width*height, levels=new Float32Array(count); levels.fill(Infinity);
  const heap=[];
  function push(q,level) {
    let i=heap.length; heap.push([q,level]);
    while(i>0) { const p=(i-1)>>1; if(heap[p][1]<=level) break; heap[i]=heap[p]; i=p; }
    heap[i]=[q,level];
  }
  function pop() {
    const result=heap[0],last=heap.pop();
    if(heap.length) { let i=0; while(i*2+1<heap.length) {
      let child=i*2+1; if(child+1<heap.length && heap[child+1][1]<heap[child][1]) child++;
      if(heap[child][1]>=last[1]) break; heap[i]=heap[child]; i=child;
    } heap[i]=last; }
    return result;
  }
  for(let q=0;q<count;q++) if(seeds[q]) { levels[q]=heights[q]; push(q,levels[q]); }
  if(!heap.length) throw new Error('Calibrated terrain requires sea-connected seeds');
  while(heap.length) {
    const [q,level]=pop(); if(level>levels[q]) continue;
    const x=q%width,y=Math.floor(q/width);
    const neighbors=[];
    if(x>0) neighbors.push(q-1); if(x+1<width) neighbors.push(q+1);
    if(y>0) neighbors.push(q-width); if(y+1<height) neighbors.push(q+width);
    for(const next of neighbors) {
      const candidate=Math.max(level,heights[next]);
      if(candidate<levels[next]) { levels[next]=candidate; push(next,candidate); }
    }
  }
  return levels;
}
export function createTerrainAtlas(pixels,width,height) {
  const count=width*height, raw=new Float32Array(count), channels=new Float32Array(count), seeds=new Uint8Array(count);
  for(let y=0;y<height;y++) for(let x=0;x<width;x++) {
    const q=y*width+x,i=q*4;
    const f=calibratedTerrain([pixels[i]/255,pixels[i+1]/255,pixels[i+2]/255],(x+.5)/width,(y+.5)/height);
    channels[q]=f.channel; seeds[q]=f.seed?1:0;
  }
  const distance=bankDistances(channels,width,height);
  for(let y=0;y<height;y++) for(let x=0;x<width;x++) {
    const q=y*width+x,u=(x+.5)/width,v=(y+.5)/height;
    let bank=.11+.65*(1-Math.exp(-distance[q]/.027));
    for(const [sx,sy,rx,ry,raise] of HIGH_SAND) {
      bank+=raise*Math.exp(-2*((u-sx)**2/rx**2+(v-sy)**2/ry**2));
    }
    raw[q]=Math.min(.94,bank)*(1-channels[q])+(.02+.025*(1-v))*channels[q];
    if(seeds[q]) raw[q]=.009;
  }
  // A grey synthetic scene or an unavailable classification still needs a
  // deterministic sea boundary; this does not turn its sand into photo water.
  if(!seeds.some(Boolean)) for(let y=height-1;y>=Math.floor(height*.98);y--) for(let x=Math.floor(width*.9);x<width;x++) seeds[y*width+x]=1;
  const levels=connectedLevels(raw,width,height,seeds), data=new Uint8ClampedArray(count*4);
  for(let y=0;y<height;y++) for(let x=0;x<width;x++) {
    const q=y*width+x,i=q*4;
    const nx=Math.max(-.8,Math.min(.8,(raw[y*width+Math.max(0,x-2)]-raw[y*width+Math.min(width-1,x+2)])*7));
    const ny=Math.max(-.8,Math.min(.8,(raw[Math.max(0,y-2)*width+x]-raw[Math.min(height-1,y+2)*width+x])*7));
    data[i]=Math.round(clamp(levels[q])*255); data[i+1]=Math.round(channels[q]*255);
    data[i+2]=Math.round((nx*.5+.5)*255); data[i+3]=Math.round((ny*.5+.5)*255);
  }
  // CPU intentionally uses the same quantized atlas values sampled by WebGL.
  return { data, levels:Float32Array.from(data.filter((_,i)=>i%4===0),n=>n/255), width,height, version:SHORE_MAP_VERSION };
}
