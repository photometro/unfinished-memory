import { MATERIAL, MATERIAL_GLSL } from './environment-material.js';
export const SURFACE_VERTEX = `
attribute vec2 a_position;
varying vec2 v_uv;
void main() { v_uv = a_position * .5 + .5; gl_Position = vec4(a_position, 0., 1.); }
`;
export const SURFACE_FRAGMENT = `
precision mediump float;
varying vec2 v_uv;
uniform sampler2D u_shore, u_sea, u_terrain;
uniform vec3 u_mapX, u_mapY;
uniform mat3 u_light;
uniform vec3 u_tone, u_sun, u_sunTint;
uniform float u_glint;
uniform float u_time, u_level, u_recentTide;
${MATERIAL_GLSL}
void main() {
  vec3 screen = vec3(v_uv.x, 1. - v_uv.y, 1.);
  vec2 uv = vec2(dot(u_mapX, screen), dot(u_mapY, screen));
  vec4 terrain = texture2D(u_terrain, uv);
  vec3 ground = texture2D(u_shore, uv).rgb;
  vec4 material = tidalMaterial(terrain, u_level);
  float water = material.x;
  vec2 ripple = ${MATERIAL.ripple} * water * vec2(sin(uv.y * 97. + u_time * .34), cos(uv.x * 83. - u_time * .27));
  vec3 photoWater = texture2D(u_shore, clamp(uv + ripple, .001, .999)).rgb;
  ground = mix(ground,photoWater,terrain.g*.35);
  vec3 sea = texture2D(u_sea, clamp(uv + ripple, .001, .999)).rgb;
  ground *= 1. - material.z * ${MATERIAL.wetShade};
  float depth = smoothstep(terrain.r,terrain.r+.22,u_level);
  float fine = sin(uv.y*370.+u_time*.24)*cos(uv.x*290.-u_time*.17)*.003*water;
  vec3 shallow = mix(vec3(.44,.55,.57),vec3(.39,.52,.545),depth);
  shallow += fine + (dot(ground,vec3(.2126,.7152,.0722))-.58)*.16;
  shallow = mix(ground,shallow,.82);
  vec3 base = mix(ground,shallow,material.y);
  base = mix(base,sea,material.w);
  vec2 normal = (terrain.ba-.5)*2.*(1.-material.w);
  gl_FragColor = vec4(illuminate(base, uv, water, normal), 1.);
}
`;
