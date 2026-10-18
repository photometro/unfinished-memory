const fs = require('node:fs');
const path = require('node:path');
const sharp = require('sharp');

// Only mechanical resize and preview assembly; no edits to the generated artwork.
const concepts = [
  {id:'a', name:'浮页', color:'#f9f7f2'},
  {id:'b', name:'回页', color:'#155363'},
  {id:'c', name:'存片', color:'#f9f7f2'}
];
async function build() {
  const composites = [];
  let labels = '';
  const mask = size => Buffer.from(`<svg width="${size}" height="${size}"><circle cx="${size/2}" cy="${size/2}" r="${size/2}" fill="white"/></svg>`);
  const roundMask = size => Buffer.from(`<svg width="${size}" height="${size}"><rect width="${size}" height="${size}" rx="${size*.23}" fill="white"/></svg>`);
  for (let i=0; i<concepts.length; i++) {
    const c = concepts[i];
    const source = path.join(__dirname, `${c.id}-concept.png`);
    const meta = await sharp(source).metadata();
    if (meta.width !== meta.height) throw new Error(`Concept ${c.id} must be square`);
    const x=28+i*320;
    for (const size of [48,64,256]) {
      const resized = await sharp(source).resize(size,size,{kernel:'lanczos3'}).png().toBuffer();
      fs.writeFileSync(path.join(__dirname,`${c.id}-${size}.png`),resized);
      if (size===256) composites.push({input:resized,left:x+18,top:108});
      if (size===48) composites.push({input:resized,left:x+22,top:450});
      if (size===64) {
        const circle = await sharp(resized).composite([{input:mask(size),blend:'dest-in'}]).png().toBuffer();
        const round = await sharp(resized).composite([{input:roundMask(size),blend:'dest-in'}]).png().toBuffer();
        composites.push({input:circle,left:x+110,top:442});
        composites.push({input:round,left:x+210,top:442});
      }
    }
    labels+=`<text x="${x+18}" y="83" class="name">${c.id.toUpperCase()} · ${c.name}</text><text x="${x+22}" y="427" class="label">48 px</text><text x="${x+110}" y="427" class="label">64 px · 圆形</text><text x="${x+208}" y="427" class="label">64 px · 圆角</text>`;
  }
  const board = Buffer.from(`<svg xmlns="http://www.w3.org/2000/svg" width="1016" height="570"><style>text{font-family:'Microsoft YaHei',sans-serif;fill:#243b46}.name{font-size:20px;font-weight:600}.label{font-size:12px;fill:#6b787e}</style><rect width="1016" height="570" fill="#eef0ef"/><text x="46" y="35" font-size="15">未完成 · 形状与小尺寸对照</text><text x="46" y="549" font-size="12" fill="#6b787e">概念稿；裁切为整图预览，并非 Android 自适应图标资源。实际桌面仍需后续验证。</text>${labels}</svg>`);
  await sharp(board).composite(composites).png().toFile(path.join(__dirname,'comparison.png'));
  const html = fs.readFileSync(path.join(__dirname,'index.html'),'utf8');
  const references = [...html.matchAll(/(?:href|src)="([^"]+)"/g)].map(m=>m[1]);
  const missing = [...new Set(references)].filter(file=>!fs.existsSync(path.join(__dirname,file)));
  if (missing.length) throw new Error(`Missing preview files: ${missing.join(', ')}`);
  const dimensions = await Promise.all(concepts.flatMap(c=>[48,64,256].map(async size=>{
    const file = `${c.id}-${size}.png`;
    const m = await sharp(path.join(__dirname,file)).metadata();
    if (m.width !== size || m.height !== size) throw new Error(`Unexpected dimensions: ${file}`);
    return {file,width:m.width,height:m.height};
  })));
  console.log(JSON.stringify({board:path.join(__dirname,'comparison.png'),missing,dimensions}));
}
build().catch(e=>{console.error(e);process.exitCode=1;});
