// PROTOTYPE: connected paper net and authored fold poses, not origami physics.
// Shared vertices remain shared throughout; no sheet-to-boat object replacement.
export const PAPER_EDGE = [[0,0],[.25,0],[.5,0],[.75,0],[1,0],[1,.5],[1,1],[.5,1],[0,1],[0,.5]];
export const BOAT_EDGE = [[12,47],[58,33],[84,10],[107,36],[132,42],[148,40],[113,79],[78,81],[46,82],[34,66]];
export const BOAT_HUB = [83,63];
export const PAPER_FACES = [
  ['inner-left', '#c3ccc1', '#829a92'], ['central-light', '#fcf8ed', '#e9e4d6'],
  ['central-shade', '#e3e0d2', '#b5c0b8'], ['inner-right', '#b9c7bd', '#718b83'],
  ['rim-right', '#e4e7d8', '#c5cfc2'], ['bow-right', '#e8e6d8', '#c5cec4'],
  ['hull-right', '#e8e6d8', '#c5cec4'], ['hull-bottom', '#f5f0e2', '#deddd0'],
  ['hull-left', '#f5f0e2', '#deddd0'], ['bow-left', '#f5f0e2', '#deddd0'],
].map(([name, light, shade], index) => ({ name, light, shade, index }));

export const clamp = value => Math.max(0, Math.min(1, value));
export const smooth = value => { const t = clamp(value); return t * t * (3 - 2 * t); };
export const polygon = points => `M${points.map(point => point.join(' ')).join(' L')} Z`;
export const triangle = (edge, hub, index) => [edge[index], edge[(index + 1) % edge.length], hub];
export const signedArea = ([a,b,c]) => (b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0]);
const mixPoint = (a,b,t) => a.map((value,index) => value + (b[index]-value)*t);
const project = ([x,y,z], distance) => { const ratio = distance / (distance - z); return [x*ratio,y*ratio]; };

export function sourceTriangle(width, height, index) {
  return triangle(PAPER_EDGE.map(([x,y]) => [x*width,y*height]), [width/2,height/2], index);
}

export function paperPose(value, width, height) {
  const p = clamp(value), perspective = Math.max(width,height)*3;
  const initial = [...PAPER_EDGE.map(([x,y]) => [(x-.5)*width,(y-.5)*height,0]), [0,0,0]];
  const closeAngle = Math.PI*smooth((p-.035)/.405);
  const closed = initial.map(([x,y]) => [x, y < 0 ? -y : y, 0]);
  let points;
  if (p <= .44) {
    points = initial.map(([x,y,z]) => y < 0 ? [x,y*Math.cos(closeAngle),-y*Math.sin(closeAngle)] : [x,y,z]);
  } else {
    const roof = [[-.5,.20],[-.25,.20],[0,-.36],[.25,.20],[.5,.20],[.5,.24],[.5,.26],[0,.26],[-.5,.26],[-.5,.24],[0,.20]].map(([x,y])=>[x*width,y*width,0]);
    const hat = [[-.48,.20],[-.24,-.08],[0,-.36],[.24,-.08],[.48,.20],[.48,.20],[.18,.24],[0,.24],[-.18,.24],[-.48,.20],[0,.16]].map(([x,y]) => [x*width,y*width,0]);
    const boat = [...BOAT_EDGE, BOAT_HUB].map(([x,y]) => [(x-80)*width/160,(y-50)*width/160,0]);
    if (p < .67) points = closed.map((point,index) => mixPoint(point,roof[index],smooth((p-.44)/.23)));
    else if (p < .84) points = roof.map((point,index) => mixPoint(point,hat[index],smooth((p-.67)/.17)));
    else points = hat.map((point,index) => mixPoint(point,boat[index],smooth((p-.84)/.16)));
  }
  points = points.map(point => project(point,perspective));
  const centerY = (Math.min(...points.map(point=>point[1]))+Math.max(...points.map(point=>point[1])))/2;
  const compensation = 1-smooth((p-.84)/.16);
  points = points.map(([x,y]) => [x,y-centerY*compensation]);
  return { edge: points.slice(0,10), hub: points[10],
    printTop: p <= .44 && Math.cos(closeAngle) > .015,
    // The turned upper sheet covers the lower printed side after the first fold.
    printBottom: p < .44,
  };
}

// Original-page triangle to moving face. Edge-on transforms stay finite.
export function affineTriangle(source, destination) {
  const [[x0,y0],[x1,y1],[x2,y2]] = source;
  const [[u0,v0],[u1,v1],[u2,v2]] = destination;
  const determinant = (x1-x0)*(y2-y0)-(x2-x0)*(y1-y0);
  const a = ((u1-u0)*(y2-y0)-(u2-u0)*(y1-y0))/determinant;
  const c = ((u2-u0)*(x1-x0)-(u1-u0)*(x2-x0))/determinant;
  const b = ((v1-v0)*(y2-y0)-(v2-v0)*(y1-y0))/determinant;
  const d = ((v2-v0)*(x1-x0)-(v1-v0)*(x2-x0))/determinant;
  return `matrix(${a} ${b} ${c} ${d} ${u0-a*x0-c*y0} ${v0-b*x0-d*y0})`;
}

const rgb = color => color.startsWith('#') ? color.slice(1).match(/../g).map(part => parseInt(part,16)) : (color.match(/[\d.]+/g) || [242,241,231]).slice(0,3).map(Number);
export function paperColor(source, target, amount) {
  const a=rgb(source), b=rgb(target), t=clamp(amount);
  return `rgb(${a.map((value,index)=>Math.round(value+(b[index]-value)*t)).join(',')})`;
}
