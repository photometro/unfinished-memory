const sharp = require('sharp');
const path = require('path');

const source = '<local-generated-images>/exec-b2c3c692-80a9-4cd5-b715-44331e55e714.png';
const out = __dirname;
const font = path.resolve(__dirname, '../../../android/app/src/main/res/font/noto_sans_sc_variable.ttf');
const size = 1024;
const master = sharp(source).resize(size, size);

async function label(text, x, y, fontSize = 16) {
  const input = await sharp({text:{text, font:`Noto Sans SC ${fontSize}`, fontfile:font, rgba:true}}).png().toBuffer();
  return {input, left:x, top:y};
}

async function masked(dimension, shape) {
  const resized = await sharp(source).resize(dimension, dimension).png().toBuffer();
  const radius = dimension * 0.225;
  const mask = shape === 'circle'
    ? `<circle cx="${dimension/2}" cy="${dimension/2}" r="${dimension/2}" fill="white"/>`
    : `<rect width="${dimension}" height="${dimension}" rx="${radius}" fill="white"/>`;
  return sharp(resized).composite([{input:Buffer.from(`<svg width="${dimension}" height="${dimension}">${mask}</svg>`),blend:'dest-in'}]).png().toBuffer();
}

(async () => {
  await master.png().toFile(path.join(out, 'icon-1024.png'));
  for (const dimension of [48, 64, 96]) {
    await sharp(source).resize(dimension, dimension).png().toFile(path.join(out, `icon-${dimension}.png`));
  }
  const composites = [
    {input:await masked(360, 'rounded'),left:44,top:44},
    await label('未完成 · 翻开的记忆',44,422,22),
    await label('实际像素尺寸',468,44,20),
    {input:await masked(48, 'rounded'),left:468,top:104},
    {input:await masked(64, 'rounded'),left:558,top:96},
    {input:await masked(96, 'rounded'),left:672,top:80},
    await label('48 px',468,188),await label('64 px',558,188),await label('96 px',672,188),
    await label('圆形裁切检查',468,264,20),
    {input:await masked(96,'circle'),left:468,top:310},
    {input:await masked(64,'circle'),left:614,top:326},
    await label('保存生活片段，日后重新遇见。',468,430,16)
  ];
  await sharp({create:{width:850,height:500,channels:4,background:'#F2EEE7'}}).composite(composites).png().toFile(path.join(out,'preview.png'));
  console.log(JSON.stringify({source, output:out, sizes:[48,64,96,1024], artworkEdited:false}));
})();
