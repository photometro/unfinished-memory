// PROTOTYPE ONLY. A drainage/incision design proxy, not a tidal hydrodynamic solver.
// No prescribed trunk, branch angles, branch count, or recursive botanical growth.
// Source boundaries and limitations: ../TIDAL-FORMATION-RESEARCH.md
const WIDTH = 720;
const HEIGHT = 1440;
const COLS = 72;
const ROWS = 62;
const cache = new Map();
const clamp = (value, low, high) => Math.max(low, Math.min(high, value));

function hash(x, y, seed) {
  let h = Math.imul(x + seed, 374761393) ^ Math.imul(y + seed * 17, 668265263);
  h = Math.imul(h ^ (h >>> 13), 1274126177);
  return ((h ^ (h >>> 16)) >>> 0) / 4294967295;
}
function noise(x, y, seed) {
  const ix = Math.floor(x), iy = Math.floor(y);
  let tx = x - ix, ty = y - iy;
  tx = tx * tx * (3 - 2 * tx); ty = ty * ty * (3 - 2 * ty);
  const a = hash(ix, iy, seed), b = hash(ix + 1, iy, seed);
  const c = hash(ix, iy + 1, seed), d = hash(ix + 1, iy + 1, seed);
  return (a + (b - a) * tx) * (1 - ty) + (c + (d - c) * tx) * ty;
}
// Only the basin envelope / sea orientation are composed. Routes emerge from the field.
const COMPOSITIONS = {
  A: { seed: 431, left: -55, top: 740, width: 855, height: 755, mouth: .77 },
  B: { seed: 621, left: 180, top: 145, width: 620, height: 1350, mouth: .91 },
  C: { seed: 782, left: -65, top: -65, width: 865, height: 680, mouth: .22 },
};
function coastU(v, seed, variant) {
  const t = v * v * (3 - 2 * v);
  return (variant === 'C' ? .82 + .17 * t : .98 - .17 * t) + .014 * Math.sin(v * 9.2 + seed);
}
function landLeft(v, variant) {
  // The left-hand reading zone in B is a closed basin edge, not a clipped tree.
  return variant === 'B' ? .54 * (1 - v) ** 1.6 : -.02;
}
class MinHeap {
  data = [];
  push(id, value) {
    const item = { id, value };
    let index = this.data.length;
    this.data.push(item);
    while (index > 0) {
      const parent = (index - 1) >> 1;
      if (this.data[parent].value <= value) break;
      this.data[index] = this.data[parent]; index = parent;
    }
    this.data[index] = item;
  }
  pop() {
    const head = this.data[0], tail = this.data.pop();
    if (!this.data.length) return head;
    let index = 0;
    while (true) {
      let child = index * 2 + 1;
      if (child >= this.data.length) break;
      if (child + 1 < this.data.length && this.data[child + 1].value < this.data[child].value) child++;
      if (tail.value <= this.data[child].value) break;
      this.data[index] = this.data[child]; index = child;
    }
    this.data[index] = tail;
    return head;
  }
}

function irregularMesh(cells) {
  // Bowyer-Watson triangulation: planar adjacency without an eight-direction grid bias.
  const points = cells.map(cell => ({ x: cell.x, y: cell.y }));
  const count = points.length;
  points.push({ x: -8000, y: -6000 }, { x: 9000, y: -6000 }, { x: 500, y: 12000 });
  function triangle(a, b, c) {
    const p = points[a], q = points[b], r = points[c];
    const d = 2 * (p.x * (q.y - r.y) + q.x * (r.y - p.y) + r.x * (p.y - q.y));
    if (Math.abs(d) < 1e-8) return null;
    const pp = p.x * p.x + p.y * p.y, qq = q.x * q.x + q.y * q.y, rr = r.x * r.x + r.y * r.y;
    const x = (pp * (q.y - r.y) + qq * (r.y - p.y) + rr * (p.y - q.y)) / d;
    const y = (pp * (r.x - q.x) + qq * (p.x - r.x) + rr * (q.x - p.x)) / d;
    return { a, b, c, x, y, radius2: (x - p.x) ** 2 + (y - p.y) ** 2 };
  }
  let triangles = [triangle(count, count + 1, count + 2)];
  for (let id = 0; id < count; id++) {
    const p = points[id];
    const boundary = new Map();
    const keep = [];
    for (const tri of triangles) {
      if ((p.x - tri.x) ** 2 + (p.y - tri.y) ** 2 >= tri.radius2 - 1e-7) { keep.push(tri); continue; }
      for (const [a, b] of [[tri.a, tri.b], [tri.b, tri.c], [tri.c, tri.a]]) {
        const key = a < b ? `${a}-${b}` : `${b}-${a}`;
        if (boundary.has(key)) boundary.delete(key);
        else boundary.set(key, [a, b]);
      }
    }
    for (const [a, b] of boundary.values()) {
      const tri = triangle(a, b, id);
      if (tri) keep.push(tri);
    }
    triangles = keep;
  }
  const adjacency = Array.from(cells, () => new Set());
  for (const tri of triangles) {
    if (tri.a >= count || tri.b >= count || tri.c >= count) continue;
    for (const [a, b] of [[tri.a, tri.b], [tri.b, tri.c], [tri.c, tri.a]]) {
      adjacency[a].add(b); adjacency[b].add(a);
    }
  }
  return adjacency.map(neighbors => [...neighbors]);
}

function drain(field, previous) {
  const { cells, root, adjacency } = field;
  const potential = new Float64Array(cells.length).fill(Infinity);
  const downstream = new Int32Array(cells.length).fill(-1);
  const settled = [];
  const heap = new MinHeap();
  potential[root] = 0;
  heap.push(root, 0);
  while (heap.data.length) {
    const { id, value } = heap.pop();
    if (value !== potential[id]) continue;
    settled.push(id);
    const cell = cells[id];
    for (const next of adjacency[id]) {
      if (!cells[next].land) continue;
      const physicalDistance = Math.hypot(cell.x - cells[next].x, cell.y - cells[next].y);
      // Reused incipient channels have lower resistance: heuristic conductivity feedback.
      const conductance = previous ? 1 + .22 * Math.log1p(Math.min(previous[next], previous[id])) : 1;
      const cost = physicalDistance * (cell.resistance + cells[next].resistance) / (2 * conductance);
      const candidate = value + cost;
      if (candidate < potential[next]) {
        potential[next] = candidate; downstream[next] = id;
        heap.push(next, candidate);
      }
    }
  }
  const flux = new Float64Array(cells.length);
  for (const id of settled) flux[id] = cells[id].storage;
  for (let index = settled.length - 1; index >= 0; index--) {
    const id = settled[index], next = downstream[id];
    if (next >= 0) flux[next] += flux[id];
  }
  return { potential, downstream, flux, settled };
}
function smooth(points) {
  // Keep shared confluences fixed; smooth inside a reach only.
  let result = points;
  for (let pass = 0; pass < 2 && result.length > 2; pass++) {
    const next = [result[0]];
    for (let index = 0; index < result.length - 1; index++) {
      const a = result[index], b = result[index + 1];
      next.push({ x: a.x * .75 + b.x * .25, y: a.y * .75 + b.y * .25, width: a.width * .75 + b.width * .25 });
      next.push({ x: a.x * .25 + b.x * .75, y: a.y * .25 + b.y * .75, width: a.width * .25 + b.width * .75 });
    }
    next.push(result.at(-1)); result = next;
  }
  return result;
}
function ribbon(points, scale, seed) {
  const left = [], right = [];
  for (let index = 0; index < points.length; index++) {
    const p = points[index], a = points[Math.max(0, index - 1)], b = points[Math.min(points.length - 1, index + 1)];
    const length = Math.hypot(b.x - a.x, b.y - a.y) || 1;
    const nx = -(b.y - a.y) / length, ny = (b.x - a.x) / length;
    const irregular = (noise(p.x / 38, p.y / 38, seed) - .5) * .16;
    const radius = p.width * scale / 2;
    left.push(`${(p.x + nx * radius * (1 + irregular)).toFixed(1)},${(p.y + ny * radius * (1 + irregular)).toFixed(1)}`);
    right.push(`${(p.x - nx * radius * (1 - irregular)).toFixed(1)},${(p.y - ny * radius * (1 - irregular)).toFixed(1)}`);
  }
  return `M${left.join('L')}L${right.reverse().join('L')}Z`;
}
function circle(x, y, radius) {
  const r = radius.toFixed(1);
  return `M${(x - radius).toFixed(1)},${y.toFixed(1)}a${r},${r} 0 1,0 ${(radius * 2).toFixed(1)},0a${r},${r} 0 1,0 ${(-radius * 2).toFixed(1)},0`;
}
export function buildTerrain(variant = 'A') {
  if (cache.has(variant)) return cache.get(variant);
  const composition = COMPOSITIONS[variant] || COMPOSITIONS.A;
  const { seed } = composition;
  const cells = Array.from({ length: COLS * ROWS }, (_, id) => {
    const gx = id % COLS, gy = Math.floor(id / COLS);
    const u = gx / (COLS - 1), v = gy / (ROWS - 1);
    const broad = noise(u * 5.2, v * 4.3, seed);
    const medium = noise(u * 13.2, v * 12.1, seed + 9);
    const fine = noise(u * 29, v * 27, seed + 29);
    const boundaryFade = Math.sin(u * Math.PI) * Math.sin(v * Math.PI);
    return {
      gx, gy, u, v, land: u <= coastU(v, seed, variant) && u >= landLeft(v, variant),
      // Spatial resistance, NOT measured bed elevation or shear stress.
      resistance: .28 + broad ** 2 * 2.3 + medium * .85 + fine * .20,
      storage: .70 + noise(u * 7, v * 6, seed + 41) * .60,
      x: composition.left + u * composition.width + (noise(u * 5, v * 5, seed + 81) - .5) * 60 * boundaryFade + (hash(gx, gy, seed + 5) - .5) * composition.width / COLS * .90,
      y: composition.top + v * composition.height + (noise(u * 5, v * 5, seed + 91) - .5) * 60 * boundaryFade + (hash(gx, gy, seed + 6) - .5) * composition.height / ROWS * .90,
    };
  });
  const mouthRow = Math.round(composition.mouth * (ROWS - 1));
  const root = Math.floor(coastU(mouthRow / (ROWS - 1), seed, variant) * (COLS - 1)) + mouthRow * COLS;
  const field = { cells, root, composition, adjacency: irregularMesh(cells) };
  let network = drain(field);
  for (let pass = 0; pass < 2; pass++) network = drain(field, network.flux);
  const { flux, downstream, potential, settled } = network;
  const threshold = 7.5; // Dimensionless visual threshold, no physical units.
  const channel = Uint8Array.from(cells, (cell, id) => cell.land && flux[id] >= threshold ? 1 : 0);
  const upstream = Array.from(cells, () => []);
  for (const id of settled) {
    const next = downstream[id];
    if (channel[id] && next >= 0) upstream[next].push(id);
  }
  const widthOf = id => .8 + 1.42 * (flux[id] / threshold) ** .62;
  const node = id => ({ x: cells[id].x, y: cells[id].y, width: widthOf(id) });
  const segments = [];
  let heads = 0, junctions = 0;
  for (const id of settled) {
    if (!channel[id]) continue;
    if (!upstream[id].length) heads++;
    if (upstream[id].length > 1) junctions++;
    if (id === root || upstream[id].length === 1) continue;
    const ids = [id];
    let next = downstream[id];
    while (next >= 0 && channel[next]) {
      ids.push(next);
      if (next === root || upstream[next].length !== 1) break;
      next = downstream[next];
    }
    if (ids.length > 1) {
      const points = ids.map(node);
      if (!upstream[id].length) points[0].width = .35;
      segments.push({ id: segments.length, points: smooth(points), ids, maxFlux: flux[ids.at(-1)] });
    }
  }
  const outlet = node(root);
  const outletV = cells[root].v;
  segments.push({ id: segments.length, ids: [root], maxFlux: flux[root], points: [outlet, {
    x: composition.left + (coastU(outletV, seed, variant) + .065) * composition.width,
    y: outlet.y, width: outlet.width * 1.18,
  }] });
  const layers = { wet: '', bank: '', rim: '', water: '', fine: '' };
  for (const segment of segments) {
    const { points } = segment;
    const width = points.at(-1).width;
    if (width > 6) layers.wet += ribbon(points, 2.35, seed) + ' ';
    layers.bank += ribbon(points, 1.65, seed) + ' ';
    layers.rim += ribbon(points, 1.17, seed) + ' ';
    layers[width < 2.8 ? 'fine' : 'water'] += ribbon(points, 1, seed) + ' ';
  }
  for (const id of settled) {
    if (!channel[id] || upstream[id].length < 2) continue;
    const p = node(id);
    layers.bank += circle(p.x, p.y, p.width * .825) + ' ';
    layers.rim += circle(p.x, p.y, p.width * .585) + ' ';
    layers.water += circle(p.x, p.y, p.width * .5) + ' ';
  }
  const coast = [];
  for (let index = 0; index <= 90; index++) {
    const v = index / 90;
    coast.push(`${(composition.left + coastU(v, seed, variant) * composition.width).toFixed(1)},${(composition.top + v * composition.height).toFixed(1)}`);
  }
  const sea = `M${coast.join('L')}L${composition.left + composition.width + 100},${composition.top + composition.height}L${composition.left + composition.width + 100},${composition.top}Z`;
  const maxPotential = Math.max(...settled.map(id => potential[id]));
  const heightAt = (x, y) => {
    const u = (x - composition.left) / composition.width, v = (y - composition.top) / composition.height;
    const seaDistance = variant === 'B' ? 1 - x / WIDTH : variant === 'C' ? y / HEIGHT : 1 - y / HEIGHT;
    const base = .11 + .79 * clamp(seaDistance, 0, 1) + (noise(x / 115, y / 135, seed + 8) - .5) * .08;
    if (u < 0 || u > 1 || v < 0 || v > 1 || u < landLeft(v, variant)) return base;
    if (u > coastU(v, seed, variant)) return -.06;
    const gx = u * (COLS - 1), gy = v * (ROWS - 1);
    const ix = Math.min(COLS - 2, Math.floor(gx)), iy = Math.min(ROWS - 2, Math.floor(gy));
    const tx = gx - ix, ty = gy - iy;
    const ids = [ix + iy * COLS, ix + 1 + iy * COLS, ix + (iy + 1) * COLS, ix + 1 + (iy + 1) * COLS];
    const weights = [(1 - tx) * (1 - ty), tx * (1 - ty), (1 - tx) * ty, tx * ty];
    let relief = 0;
    ids.forEach((id, index) => {
      const height = Number.isFinite(potential[id]) ? .14 + .62 * (potential[id] / maxPotential) ** .55 : base;
      const incision = channel[id] ? .06 + .12 * clamp(Math.log1p(flux[id] / threshold) / 6, 0, 1) : 0;
      relief += (height - incision) * weights[index];
    });
    const edgeBlend = clamp(Math.min(u, 1 - u, v, 1 - v) * 9, 0, 1);
    return base * (1 - edgeBlend) + relief * edgeBlend;
  };
  const stats = {
    model: 'weighted-drainage-incision', landCells: settled.length, channelCells: channel.reduce((sum, value) => sum + value, 0),
    reaches: segments.length, heads, junctions, outlets: 1, feedbackPasses: 2,
    outletFlux: flux[root], widest: widthOf(root),
    disconnected: cells.filter((cell, id) => cell.land && !Number.isFinite(potential[id])).length,
    reverseFlux: settled.filter(id => downstream[id] >= 0 && flux[downstream[id]] < flux[id]).length,
    uphillPotential: settled.filter(id => downstream[id] >= 0 && potential[downstream[id]] >= potential[id]).length,
  };
  const result = { layers, sea, stats, heightAt, segments, outlet };
  cache.set(variant, result);
  return result;
}
export const terrainSize = { width: WIDTH, height: HEIGHT };
