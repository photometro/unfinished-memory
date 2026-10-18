import { calibratedTerrain } from './shore-terrain.js';
// Source-coordinate calibrated atlas drives geography; these only style surfaces.
export const MATERIAL = Object.freeze({
  edge: .028, wetAdvance: .040, floodWater: .78,
  introStart: .57, introEnd: .94, wetShade: .045,
  ripple: .00045, channelShade: .035,
});
export const clamp = value => Math.max(0, Math.min(1, value));
export function smoothstep(a, b, x) { const t = clamp((x - a) / (b - a)); return t * t * (3 - 2 * t); }
export function wetHistory(recent,tide,elapsed,opening=false,wasOpening=false) {
  return opening||wasOpening ? tide : Math.max(tide,recent-Math.max(0,elapsed)*.0015);
}
export function terrainFeatures(rgb, u, v) {
  return calibratedTerrain(rgb,u,v);
}
export function tideMaterial(height, channel, tide, out = {}, recentTide = tide) {
  const flood = smoothstep(height, height + MATERIAL.edge, tide);
  const intro = smoothstep(MATERIAL.introStart, MATERIAL.introEnd, tide);
  out.water = Math.max(channel,flood);
  out.flood = flood*(1-channel)*MATERIAL.floodWater;
  out.intro = intro;
  out.wet = (1-flood)*(1-channel)*smoothstep(height-MATERIAL.wetAdvance,height,Math.max(tide,recentTide));
  out.depth = 1-channel*MATERIAL.channelShade*(1-intro);
  return out;
}
// Matrix (9), land/water lift + contrast (3), direction + intensity (3), tint (3), glint (1).
export function illuminate(rgb, light, u, v, water, nx=0, ny=0) {
  return illuminateInto(rgb[0], rgb[1], rgb[2], light, u, v, water, [],0,1,nx,ny);
}
export function illuminateInto(r, g, b, light, u, v, water, out, offset = 0, scale = 1, nx=0, ny=0) {
  const normal = nx*light[12]+ny*light[13];
  const side = smoothstep(-.48,.48,(u-.5)*light[12]+(v-.5)*light[13]);
  const directional = side*.18 + Math.max(normal,0)*.82;
  const shadow = 1-Math.max(-normal,0)*light[14]*.6;
  const lift = light[9] + (light[10] - light[9]) * water;
  const sheen = light[18]*water*Math.exp(-(((u*.65+(1-v)*.35-.62)/.14)**2));
  for (let row = 0; row < 3; row++) {
    const value = light[row * 3] * r + light[row * 3 + 1] * g + light[row * 3 + 2] * b;
    const toned = ((value - .5) * light[11] + .5)*shadow;
    out[offset + row] = scale * clamp(toned + (lift + directional * light[14] * light[15 + row]) * (1 - toned) + sheen);
  }
  return out;
}
const n = value => Number.isInteger(value) ? `${value}.0` : String(value);
const m = Object.fromEntries(Object.entries(MATERIAL).map(([key, value]) => [key, n(value)]));
export const MATERIAL_GLSL = `
vec4 tidalMaterial(vec4 terrain, float tide) {
  float flood = smoothstep(terrain.r, terrain.r + ${m.edge}, tide);
  float intro = smoothstep(${m.introStart}, ${m.introEnd}, tide);
  float wet = (1. - flood) * (1. - terrain.g) * smoothstep(terrain.r - ${m.wetAdvance}, terrain.r, max(tide,u_recentTide));
  return vec4(max(flood,terrain.g), flood*(1.-terrain.g)*${m.floodWater}, wet, intro);
}
vec3 illuminate(vec3 base, vec2 uv, float water, vec2 normal) {
  float facing = dot(normal,u_sun.xy);
  float side = smoothstep(-.48,.48,dot(uv-.5,u_sun.xy));
  float directional = side*.18 + max(facing,0.)*.82;
  float shadow = 1.-max(-facing,0.)*u_sun.z*.6;
  float lift = mix(u_tone.x, u_tone.y, water);
  float sheen = u_glint*water*exp(-pow((uv.x*.65+(1.-uv.y)*.35-.62)/.14,2.));
  vec3 color = ((u_light * base - .5) * u_tone.z + .5)*shadow;
  return clamp(color + (lift + directional * u_sun.z * u_sunTint) * (1. - color) + sheen, 0., 1.);
}
`;
