// Art-directed flow, not geographic data, an arrival route or an algorithm state.
// All visible trails and the memory piece sample this same continuous field.
const EDDIES = [
  { x: .06, y: .49, radius: .26, spin: -2.9 },
  { x: .94, y: .78, radius: .32, spin: 2.4 },
];

export function currentVelocity([x, y], aspect = 2) {
  let vx = .21 + .025 * Math.sin(y * 8 + x * 2);
  let vy = -.095 + .022 * Math.sin(x * 5 - y * 3);
  for (const eddy of EDDIES) {
    const dx = x - eddy.x, dy = (y - eddy.y) * aspect;
    const weight = Math.exp(-(dx * dx + dy * dy) / (eddy.radius * eddy.radius));
    vx = vx * (1 - .72 * weight) - dy * eddy.spin * weight;
    vy = vy * (1 - .72 * weight) + dx * eddy.spin * weight / aspect;
  }
  return [vx, vy];
}

export function advanceCurrent(point, dt, aspect = 2) {
  const v = currentVelocity(point, aspect);
  const mid = currentVelocity([point[0] + v[0] * dt / 2, point[1] + v[1] * dt / 2], aspect);
  return [point[0] + mid[0] * dt, point[1] + mid[1] * dt];
}

export function carrierPath(aspect = 2, startY = .63) {
  const path = [[.39, startY]];
  for (let i = 0; i < 120; i++) path.push(advanceCurrent(path.at(-1), 3.2 / 120, aspect));
  return path;
}

export function samplePath(path, progress) {
  const index = Math.max(0, Math.min(1, progress)) * (path.length - 1);
  const first = path[Math.floor(index)], next = path[Math.min(path.length - 1, Math.floor(index) + 1)];
  return first.map((value, axis) => value + (next[axis] - value) * (index % 1));
}
