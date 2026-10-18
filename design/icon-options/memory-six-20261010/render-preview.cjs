const sharp = require('sharp');
const path = require('path');
const manifest = require('./manifest.json');
const out = __dirname;
const font = path.resolve(__dirname, '../../../android/app/src/main/res/font/noto_sans_sc_variable.ttf');

async function label(text, left, top, size = 16) {
  const input = await sharp({text:{text,font:`Noto Sans SC ${size}`,fontfile:font,rgba:true}}).png().toBuffer();
  return {input,left,top};
}

async function masked(source, size, circle = false) {
  const input = await sharp(source).resize(size,size).png().toBuffer();
  const mask = circle
    ? `<circle cx="${size/2}" cy="${size/2}" r="${size/2}" fill="white"/>`
    : `<rect width="${size}" height="${size}" rx="${size*.225}" fill="white"/>`;
  return sharp(input).composite([{input:Buffer.from(`<svg width="${size}" height="${size}">${mask}</svg>`),blend:'dest-in'}]).png().toBuffer();
}

async function group(items, title, subtitle, name) {
  const layers = [await label(title,40,26,26),await label(subtitle,40,66,16)];
  for(let i=0;i<items.length;i++) {
    const m=items[i];
    const x=40+i*360;
    layers.push({input:await masked(m.source,280),left:x,top:112});
    layers.push(await label(`${m.id.toUpperCase()} · ${m.name}`,x,414,23));
    layers.push(await label(m.line,x,452,13));
    layers.push(await label('48 px',x,502,13),await label('64 px',x+84,502,13),await label('圆形裁切',x+186,502,13));
    layers.push({input:await masked(m.source,48),left:x,top:536});
    layers.push({input:await masked(m.source,64),left:x+84,top:528});
    layers.push({input:await masked(m.source,64,true),left:x+186,top:528});
  }
  return sharp({create:{width:1080,height:628,channels:4,background:'#F0ECE5'}}).composite(layers).png().toFile(path.join(out,name));
}

(async()=>{
  for(const m of manifest) {
    await sharp(m.source).resize(1024,1024).png().toFile(path.join(out,`${m.id}-icon-1024.png`));
    for(const size of [48,64]) {
      await sharp(m.source).resize(size,size).png().toFile(path.join(out,`${m.id}-icon-${size}.png`));
    }
    await sharp(await masked(m.source,96,true)).png().toFile(path.join(out,`${m.id}-circle-96.png`));
  }
  await group(manifest.slice(0,3),'A—C · 物品识别','保留照片方案，再加入记忆册与漂流瓶。','regular-comparison.png');
  await group(manifest.slice(3),'D—F · 物品解构','流程不变：产品行为 → 代指物品 → 解构风格 → 图标。','deconstructed-comparison.png');
  await sharp({create:{width:1080,height:1256,channels:4,background:'#F0ECE5'}}).composite([
    {input:path.join(out,'regular-comparison.png'),left:0,top:0},
    {input:path.join(out,'deconstructed-comparison.png'),left:0,top:628}
  ]).png().toFile(path.join(out,'comparison.png'));
  console.log(JSON.stringify({directory:out,count:manifest.length,aPreserved:true,checks:['48px','64px','circular crop'],androidResourcesChanged:false}));
})();
