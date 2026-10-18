const fs = require('node:fs');
const path = require('node:path');
const sharp = require('sharp');
const root = __dirname;
const fontPath = path.resolve(__dirname, '../../../android/app/src/main/res/font/noto_sans_sc_variable.ttf');

async function label(text, width, colour = '#47616A', size = 16) {
  return sharp({ text: { text: `<span foreground="${colour}">${text}</span>`, font: `Noto Sans SC ${size}`, fontfile: fontPath, width, rgba: true } }).png().toBuffer();
}
async function mask(buffer, size, kind) {
  const shape = kind === 'circle' ? `<circle cx="${size / 2}" cy="${size / 2}" r="${size / 2}" fill="white"/>` : `<rect width="${size}" height="${size}" rx="${size * 0.235}" fill="white"/>`;
  return sharp(buffer).composite([{ input: Buffer.from(`<svg width="${size}" height="${size}" xmlns="http://www.w3.org/2000/svg">${shape}</svg>`), blend: 'dest-in' }]).png().toBuffer();
}
async function main() {
  await label('未完成', 800);
  const source = fs.readFileSync(path.join(root, 'icon.svg'), 'utf8');
  const icon = await sharp(Buffer.from(source)).png().toBuffer();
  await sharp(icon).toFile(path.join(root, 'icon-1024.png'));
  for (const size of [32, 48, 64, 96, 192]) await sharp(icon).resize(size, size).png().toFile(path.join(root, `icon-${size}.png`));
  const monoSource = source.replace('#315E6C', '#202020').replaceAll('#F6F0E3', '#FFFFFF').replaceAll('#ACCCD0', '#FFFFFF').replaceAll('#3B6A77', '#202020').replaceAll('#477987', '#202020');
  const mono = await sharp(Buffer.from(monoSource)).png().toBuffer();
  await sharp(mono).toFile(path.join(root, 'icon-monochrome.png'));
  const layers = [];
  layers.push({ input: await label('未完成', 500, '#254E5B', 24), left: 48, top: 34 });
  layers.push({ input: await label('潮汐字标 · 图标草稿', 500, '#6B7F83', 14), left: 48, top: 78 });
  layers.push({ input: await mask(await sharp(icon).resize(320, 320).toBuffer(), 320, 'rounded'), left: 48, top: 142 });
  layers.push({ input: await label('名称作为识别中心', 340, '#526B72', 14), left: 48, top: 482 });
  for (let i = 0; i < 3; i++) {
    const size = [96, 64, 48][i]; const x = [434, 598, 750][i];
    layers.push({ input: await mask(await sharp(icon).resize(size, size).toBuffer(), size, 'rounded'), left: x, top: 183 });
    layers.push({ input: await label(`${size}px`, 100, '#6B7F83', 13), left: x, top: 300 });
  }
  layers.push({ input: await mask(await sharp(icon).resize(112, 112).toBuffer(), 112, 'circle'), left: 432, top: 387 });
  layers.push({ input: await label('圆形裁切', 140, '#6B7F83', 13), left: 432, top: 517 });
  layers.push({ input: await mask(await sharp(mono).resize(112, 112).toBuffer(), 112, 'rounded'), left: 622, top: 387 });
  layers.push({ input: await label('黑白轮廓', 140, '#6B7F83', 13), left: 622, top: 517 });
  await sharp({ create: { width: 960, height: 620, channels: 4, background: '#F5F3EE' } }).composite(layers).png().toFile(path.join(root, 'preview.png'));
  console.log(JSON.stringify({ icon: path.join(root, 'icon-1024.png'), preview: path.join(root, 'preview.png') }));
}
main().catch(error => { console.error(error); process.exitCode = 1; });
