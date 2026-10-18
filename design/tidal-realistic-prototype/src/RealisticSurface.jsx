'use client';
import React, { memo, useEffect, useRef, useState } from 'react';
import { SURFACE_FRAGMENT, SURFACE_VERTEX } from './water-shader.js';
import { SHORE_MASTER_CANDIDATE, SEA_BASE, validPeriod } from './daylight.js';
import { lightingVector, HOME_TIDE_PAUSED } from './environment-state.js';
import { FrozenSurface } from './FrozenSurface.jsx';
import { MATERIAL, smoothstep, tideMaterial, illuminateInto, wetHistory } from './environment-material.js';
import { createTerrainAtlas, SHORE_MAP_VERSION } from './shore-terrain.js';
import { coverMapping, canvasBudget } from './environment-geometry.js';

// One bounded immutable geography cache. Changing the backend does not repeat
// source classification or retain its temporary priority-flood arrays.
let cachedTerrain;

function loadImage(url) {
  return new Promise((resolve, reject) => {
    const image = new Image(); image.onload = () => resolve(image);
    image.onerror = () => reject(new Error(`Environment asset unavailable: ${url}`)); image.src = url;
  });
}
function compile(gl, type, source) {
  const shader = gl.createShader(type); gl.shaderSource(shader, source); gl.compileShader(shader);
  if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) { const log = gl.getShaderInfoLog(shader); gl.deleteShader(shader); throw new Error(log); }
  return shader;
}
function prepare(image) {
  // Display-resolution textures, not eight ultra-resolution image variants.
  const canvas = document.createElement('canvas'); canvas.width = 444; canvas.height = 888;
  const ctx = canvas.getContext('2d', { willReadFrequently: true });
  const scale = Math.max(444 / image.naturalWidth, 888 / image.naturalHeight);
  ctx.drawImage(image, (444 - image.naturalWidth * scale) / 2, (888 - image.naturalHeight * scale) / 2, image.naturalWidth * scale, image.naturalHeight * scale);
  return { canvas, pixels: ctx.getImageData(0, 0, 444, 888).data };
}
// Feature gate preserves the former renderer without running its effects.
export const RealisticSurface = memo(function RealisticSurface({ tidePaused = HOME_TIDE_PAUSED, ...props }) {
  return tidePaused ? <FrozenSurface {...props} /> : <ArchivedTidalSurface {...props} />;
});

const ArchivedTidalSurface = memo(function ArchivedTidalSurface({ variant = 'A', period = 'morning', active = true, reduced = false, opening = false, tideValue, onReady, shoreMaster = SHORE_MASTER_CANDIDATE, forceStatic = false }) {
  const canvasRef = useRef(null), staticRef = useRef(null), rootRef = useRef(null), controllerRef = useRef(null);
  const propsRef = useRef(null); propsRef.current = { variant, period: validPeriod(period), active, reduced, opening, tideValue, onReady };
  const [backend, setBackend] = useState('loading');
  useEffect(() => { controllerRef.current?.(); }, [variant, period, active, reduced, opening]);
  useEffect(() => {
    const canvas = canvasRef.current, fallbackCanvas = staticRef.current, root = rootRef.current;
    let cancelled = false, frame = 0, staticTimer = 0, wetTimer = 0, observer, gl, program, buffer, ready = false, mode = 'loading';
    let width = 1, height = 1, cssWidth = 1, cssHeight = 1, time = 0, lastTime = 0, lastDraw = 0, draws = 0;
    const shaders = [], textures = [];
    let prepared, atlas, composite, compositeCtx, composed, lastStaticKey = '';
    let recentTide = .08, previousOpening = true, wetClock = 0;
    let light = lightingVector(propsRef.current.period), from = light.slice(), target = light.slice(), blendStart = 0, blending = false;
    let draw = () => {};
    const releaseGPU = () => {
      if (!gl) return;
      textures.splice(0).forEach(t => gl.deleteTexture(t));
      shaders.splice(0).forEach(s => gl.deleteShader(s));
      if (buffer) gl.deleteBuffer(buffer); if (program) gl.deleteProgram(program);
      buffer = null; program = null;
    };
    const advance = now => {
      if (!blending) return;
      const t = Math.min(1, Math.max(0, (now - blendStart) / 720)), eased = t * t * (3 - 2 * t);
      light = target.map((v, i) => from[i] + (v - from[i]) * eased); blending = t < 1;
    };
    const wetLevel = tide => {
      const now=performance.now(), elapsed=wetClock?(now-wetClock)/1000:0;
      recentTide=wetHistory(recentTide,tide,elapsed,propsRef.current.opening,previousOpening);
      previousOpening=propsRef.current.opening; wetClock=now; return recentTide;
    };
    const schedule = () => {
      const p = propsRef.current;
      if (!cancelled && mode !== 'loading' && mode !== 'error' && !document.hidden && !frame && (blending || mode === 'webgl' && p.active && !p.reduced)) frame = requestAnimationFrame(tick);
    };
    const tick = now => {
      frame = 0; if (cancelled || document.hidden) return;
      const p = propsRef.current;
      if (mode === 'webgl' && p.active && !p.reduced && lastTime) time += Math.min(now - lastTime, 80) / 1000;
      lastTime = now; advance(now);
      if (now - lastDraw >= (mode === 'static' ? 100 : 32) || mode === 'static' && !blending) { draw(); lastDraw = now; }
      schedule();
    };
    const stamp = (mapping, tide) => {
      root.dataset.tide = tide.toFixed(4); root.dataset.frames = String(++draws);
      root.dataset.mapping = JSON.stringify(mapping); root.dataset.light = light.map(v => v.toFixed(4)).join(',');
      root.dataset.lightSettled = String(!blending);
      root.dataset.surfaceStudy = '14'; root.dataset.terrainMap = SHORE_MAP_VERSION;
      root.dataset.recentTide = recentTide.toFixed(4);
      root.dataset.animationActive = String(mode === 'webgl' && propsRef.current.active && !propsRef.current.reduced && !document.hidden);
      // Damp banks dry on wall-clock time even if water motion is disabled.
      // This bounded 0.5Hz task stops once dry, off-screen, or unmounted.
      if(!wetTimer && !cancelled && propsRef.current.active && !document.hidden && (mode==='static'||propsRef.current.reduced) && recentTide>tide+.001) {
        wetTimer=setTimeout(()=>{ wetTimer=0; if(!cancelled&&!document.hidden&&propsRef.current.active) draw(); },2000);
      }
    };
    const drawStatic = () => {
      if (cancelled || !prepared || document.hidden) return;
      const tide = propsRef.current.tideValue?.get() ?? .08;
      const recent = wetLevel(tide);
      const mapping = coverMapping(cssWidth, cssHeight, 887, 1774, propsRef.current.variant);
      const key = [tide.toFixed(3),recent.toFixed(3), ...light.map(v => v.toFixed(3))].join(',');
      if (lastStaticKey !== key) {
        lastStaticKey = key;
        const shore = prepared[0].pixels, sea = prepared[1].pixels, terrain = atlas.data, out = composed.data;
        const sample = {};
        for (let q = 0; q < 444 * 888; q++) {
          const i = q * 4, threshold=terrain[i]/255, channel=terrain[i+1]/255;
          const { water: w, wet, flood, intro } = tideMaterial(threshold,channel,tide,sample,recent);
          const shade = 1 - wet * MATERIAL.wetShade;
          const gr=shore[i]/255*shade,gg=shore[i+1]/255*shade,gb=shore[i+2]/255*shade;
          const depth=smoothstep(threshold,threshold+.22,tide), variation=(gr*.2126+gg*.7152+gb*.0722-.58)*.16;
          const sr=gr*.18+(.44+(.39-.44)*depth+variation)*.82,sg=gg*.18+(.55+(.52-.55)*depth+variation)*.82,sb=gb*.18+(.57+(.545-.57)*depth+variation)*.82;
          const r=(gr*(1-flood)+sr*flood)*(1-intro)+sea[i]/255*intro;
          const g=(gg*(1-flood)+sg*flood)*(1-intro)+sea[i+1]/255*intro;
          const b=(gb*(1-flood)+sb*flood)*(1-intro)+sea[i+2]/255*intro;
          illuminateInto(r,g,b,light,((q%444)+.5)/444,(Math.floor(q/444)+.5)/888,w,out,i,255,(terrain[i+2]/255-.5)*2*(1-intro),(terrain[i+3]/255-.5)*2*(1-intro));
          out[i + 3] = 255;
        }
        compositeCtx.putImageData(composed, 0, 0);
      }
      const { x: a, y: b } = mapping, det = a[0] * b[1] - a[1] * b[0];
      const inv = [b[1] / det, -a[1] / det, -b[0] / det, a[0] / det];
      const ctx = fallbackCanvas.getContext('2d'); ctx.setTransform(1, 0, 0, 1, 0, 0); ctx.clearRect(0, 0, width, height);
      ctx.setTransform(width * inv[0] / 444, height * inv[2] / 444, width * inv[1] / 888, height * inv[3] / 888, -width * (inv[0] * a[2] + inv[1] * b[2]), -height * (inv[2] * a[2] + inv[3] * b[2]));
      ctx.drawImage(composite, 0, 0); stamp(mapping, tide);
    };
    const notifyReady = () => { if (!cancelled && !ready) { ready = true; propsRef.current.onReady?.(); } };
    const fallback = () => {
      if (cancelled) return; cancelAnimationFrame(frame); frame = 0; releaseGPU();
      mode = prepared ? 'static' : 'error'; draw = prepared ? drawStatic : () => {};
      root.dataset.textureCount = '0'; root.dataset.textureBytesEstimate = '0'; setBackend(mode); draw(); notifyReady(); schedule();
    };
    const update = () => {
      const now = performance.now(); advance(now);
      const next = lightingVector(propsRef.current.period);
      if (next.some((v, i) => Math.abs(v - target[i]) > .00001)) {
        from = light.slice(); target = next; blendStart = now; blending = propsRef.current.active && !propsRef.current.reduced;
        if (!blending) light = target.slice();
      }
      if (propsRef.current.reduced || !propsRef.current.active) { light = target.slice(); blending = false; }
      cancelAnimationFrame(frame); frame = 0; lastTime = 0; draw(); schedule();
    };
    controllerRef.current = update;
    const onVisibility = () => { cancelAnimationFrame(frame); clearTimeout(staticTimer); clearTimeout(wetTimer); frame = staticTimer = wetTimer = 0; lastTime = 0; if (!document.hidden) update(); else root.dataset.animationActive = 'false'; };
    const onContextLost = event => { event.preventDefault(); fallback(); };
    canvas.addEventListener('webglcontextlost', onContextLost); document.addEventListener('visibilitychange', onVisibility);
    const offTide = tideValue?.on('change', () => {
      if (document.hidden || cancelled || !propsRef.current.active) return;
      const now = performance.now();
      if (mode === 'webgl') { if (propsRef.current.reduced) draw(); return; }
      if (now - lastDraw >= 100) { clearTimeout(staticTimer); staticTimer = 0; draw(); lastDraw = now; }
      else if (!staticTimer) staticTimer = setTimeout(() => { staticTimer = 0; if (!cancelled && !document.hidden && propsRef.current.active) { draw(); lastDraw = performance.now(); } }, 100 - (now - lastDraw));
    });
    const resize = bounds => {
      cssWidth = Math.max(1, bounds.width); cssHeight = Math.max(1, bounds.height);
      ({ width, height } = canvasBudget(cssWidth, cssHeight, devicePixelRatio || 1));
      canvas.width = fallbackCanvas.width = width; canvas.height = fallbackCanvas.height = height;
      draw();
    };
    resize(canvas.getBoundingClientRect()); observer = new ResizeObserver(([entry]) => resize(entry.contentRect)); observer.observe(canvas);
    Promise.all([loadImage(shoreMaster), loadImage(SEA_BASE)]).then(images => {
      if (cancelled) return;
      prepared = images.map(prepare);
      const atlasStart=performance.now();
      if(cachedTerrain?.key===shoreMaster) atlas=cachedTerrain.atlas;
      else {
        const {levels,...packed}=createTerrainAtlas(prepared[0].pixels,444,888);
        atlas=packed; cachedTerrain={key:shoreMaster,atlas};
      }
      root.dataset.atlasBuildMs=(performance.now()-atlasStart).toFixed(1);
      prepared.push({pixels:atlas.data, atlas:true});
      composite = document.createElement('canvas'); composite.width = 444; composite.height = 888;
      compositeCtx = composite.getContext('2d'); composed = compositeCtx.createImageData(444, 888);
      root.dataset.assetDimensions = images.map(i => `${i.naturalWidth}x${i.naturalHeight}`).join(',');
      root.dataset.decodedBytesEstimate = String(images.reduce((n, i) => n + i.naturalWidth * i.naturalHeight * 4, 0));
      root.dataset.textureBytesEstimate = String(444 * 888 * 4 * 3);
      root.dataset.atlasBytes = String(atlas.data.byteLength);
      light = target = lightingVector(propsRef.current.period); from = light.slice(); blending = false;
      try {
        gl = !forceStatic && canvas.getContext('webgl', { alpha: false, antialias: false, powerPreference: 'low-power' });
        if (!gl || gl.getParameter(gl.MAX_TEXTURE_IMAGE_UNITS) < 3) { fallback(); return; }
        shaders.push(compile(gl, gl.VERTEX_SHADER, SURFACE_VERTEX)); shaders.push(compile(gl, gl.FRAGMENT_SHADER, SURFACE_FRAGMENT));
        program = gl.createProgram(); shaders.forEach(s => gl.attachShader(program, s)); gl.linkProgram(program);
        if (!gl.getProgramParameter(program, gl.LINK_STATUS)) throw new Error(gl.getProgramInfoLog(program));
        gl.useProgram(program); buffer = gl.createBuffer(); gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
        gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 1, -1, -1, 1, 1, 1]), gl.STATIC_DRAW);
        const position = gl.getAttribLocation(program, 'a_position'); gl.enableVertexAttribArray(position); gl.vertexAttribPointer(position, 2, gl.FLOAT, false, 0, 0);
        prepared.forEach((item, index) => {
          const t = gl.createTexture(); textures.push(t); gl.activeTexture(gl.TEXTURE0 + index); gl.bindTexture(gl.TEXTURE_2D, t);
          gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE); gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
          gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, item.atlas?gl.NEAREST:gl.LINEAR); gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, item.atlas?gl.NEAREST:gl.LINEAR);
          if(item.atlas) gl.texImage2D(gl.TEXTURE_2D,0,gl.RGBA,444,888,0,gl.RGBA,gl.UNSIGNED_BYTE,item.pixels);
          else gl.texImage2D(gl.TEXTURE_2D,0,gl.RGBA,gl.RGBA,gl.UNSIGNED_BYTE,item.canvas);
          gl.uniform1i(gl.getUniformLocation(program,['u_shore','u_sea','u_terrain'][index]),index);
        });
        const uniforms = Object.fromEntries(['u_time', 'u_level','u_recentTide', 'u_light', 'u_tone', 'u_sun', 'u_sunTint', 'u_glint', 'u_mapX', 'u_mapY'].map(n => [n, gl.getUniformLocation(program, n)]));
        draw = () => {
          if (cancelled || document.hidden || gl.isContextLost()) return;
          const mapping = coverMapping(cssWidth, cssHeight, 887, 1774, propsRef.current.variant), tide = propsRef.current.tideValue?.get() ?? .08;
          gl.uniform1f(uniforms.u_recentTide,wetLevel(tide));
          gl.uniform3fv(uniforms.u_mapX, mapping.x); gl.uniform3fv(uniforms.u_mapY, mapping.y);
          gl.uniformMatrix3fv(uniforms.u_light, false, [light[0], light[3], light[6], light[1], light[4], light[7], light[2], light[5], light[8]]);
          gl.uniform3fv(uniforms.u_tone, light.slice(9, 12)); gl.uniform3fv(uniforms.u_sun, light.slice(12, 15));
          gl.uniform3fv(uniforms.u_sunTint, light.slice(15, 18)); gl.uniform1f(uniforms.u_glint, light[18]);
          gl.uniform1f(uniforms.u_time, time); gl.uniform1f(uniforms.u_level, tide); gl.viewport(0, 0, width, height);
          gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4); stamp(mapping, tide);
        };
        mode = 'webgl'; root.dataset.textureCount = '3'; setBackend(mode); draw(); notifyReady(); schedule();
      } catch (error) { console.warn('Environment uses static fallback:', error.message); fallback(); }
    }).catch(error => { if (!cancelled) { root.dataset.error = error.message; fallback(); } });
    return () => {
      cancelled = true; cancelAnimationFrame(frame); clearTimeout(staticTimer); clearTimeout(wetTimer); observer?.disconnect(); offTide?.(); releaseGPU();
      canvas.removeEventListener('webglcontextlost', onContextLost); document.removeEventListener('visibilitychange', onVisibility);
      if (controllerRef.current === update) controllerRef.current = null;
      prepared = composite = composed = atlas = null;
    };
  }, [shoreMaster, forceStatic, tideValue]);
  return <div ref={rootRef} className={`natural-surface natural-${variant}`} data-renderer={backend} data-period={validPeriod(period)} data-shore-master="selected-v4" aria-hidden="true">
    <canvas ref={staticRef} className={`natural-static-canvas ${backend === 'static' ? 'ready' : ''}`} />
    <canvas ref={canvasRef} className={`natural-canvas ${backend === 'webgl' ? 'ready' : ''}`} />
    {backend === 'loading' && <div className="surface-loading" />}
    {backend === 'error' && <div className="surface-error">海岸素材暂不可用</div>}
  </div>;
});
