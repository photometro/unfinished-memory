const fs = require('node:fs');
const path = require('node:path');
const sharp = require('sharp');
const root = __dirname;
const rgba = (data, width, height) => ({raw: {width, height, channels: 4}});
const image = data => `data:image/png;base64,${data.toString('base64')}`;
const text = (x, y, value, size = 20) => `<text x="${x}" y="${y}" font-family="Microsoft YaHei,sans-serif" font-size="${size}" fill="#253D4C">${value}</text>`;

async function main() {
  const input = path.join(root, 'foreground-cutout.png');
  const {data, info} = await sharp(input).ensureAlpha().raw().toBuffer({resolveWithObject: true});
  const {width, height} = info;
  if (width !== height) throw Error('Cutout must have square original canvas');
  if ([0, width - 1, width * (height - 1), width * height - 1].some(p => data[p * 4 + 3] !== 0)) throw Error('Cutout corners are not transparent');
  const visited = new Uint8Array(width * height), components = [];
  for (let p = 0; p < visited.length; p++) {
    if (visited[p] || data[p * 4 + 3] < 128) continue;
    const queue = [p]; visited[p] = 1;
    let l = width, t = height, r = 0, b = 0, area = 0, red = 0, green = 0, blue = 0;
    for (let q = 0; q < queue.length; q++) {
      const n = queue[q], x = n % width, y = Math.floor(n / width);
      area++; l = Math.min(l, x); r = Math.max(r, x); t = Math.min(t, y); b = Math.max(b, y);
      red += data[n * 4]; green += data[n * 4 + 1]; blue += data[n * 4 + 2];
      for (const [nx, ny] of [[x-1,y],[x+1,y],[x,y-1],[x,y+1]]) {
        if (nx < 0 || nx >= width || ny < 0 || ny >= height) continue;
        const next = ny * width + nx;
        if (!visited[next] && data[next * 4 + 3] >= 128) {visited[next] = 1; queue.push(next);}
      }
    }
    if (area > 500) components.push({area, bounds: [l,t,r,b], meanRGB: [red/area,green/area,blue/area].map(v => Math.round(v))});
  }
  components.sort((a,b) => b.area-a.area);
  if (components.length !== 3) throw Error(`Expected merged blue mark and two separate yellow elements, got ${components.length}`);
  if (components.slice(1).some(c => c.meanRGB[0] < 200 || c.meanRGB[2] > 100)) throw Error('Both yellow elements must remain');
  const left = Math.min(...components.map(c => c.bounds[0])), right = Math.max(...components.map(c => c.bounds[2]));
  const top = Math.min(...components.map(c => c.bounds[1])), bottom = Math.max(...components.map(c => c.bounds[3]));
  const center = [(left+right)/2,(top+bottom)/2];
  let radius = 0;
  for (let p = 0; p < visited.length; p++) {
    if (data[p * 4 + 3] <= 16) continue;
    radius = Math.max(radius, Math.hypot(p % width-center[0],Math.floor(p/width)-center[1]));
  }
  // Only a uniform platform transform is applied to the complete generated cutout.
  // No per-shape geometry, palette, or semantic image edit is performed here.
  const scale = 31.7/radius, sourceSize = Math.round(width*scale*10);
  const tx = Math.round(540-center[0]*sourceSize/width), ty = Math.round(540-center[1]*sourceSize/height);
  if (tx < 0 || ty < 0 || tx+sourceSize > 1080 || ty+sourceSize > 1080) throw Error('Export would clip full source canvas');
  const resized = await sharp(input).resize(sourceSize,sourceSize).png().toBuffer();
  const foreground = await sharp({create:{width:1080,height:1080,channels:4,background:'#00000000'}}).composite([{input:resized,left:tx,top:ty}]).png().toBuffer();
  fs.writeFileSync(path.join(root,'foreground-1080.png'),foreground);
  const {data: fdata, info: finfo} = await sharp(foreground).raw().toBuffer({resolveWithObject:true});
  let beyondSafe = 0, radiusDp = 0;
  const mono = Buffer.alloc(fdata.length);
  for (let p = 0; p < finfo.width*finfo.height; p++) {
    const a = fdata[p*4+3]; mono[p*4+3] = a;
    if (a <= 16) continue;
    const rd = Math.hypot(p%1080-540,Math.floor(p/1080)-540)/10;
    radiusDp = Math.max(radiusDp,rd); if(rd>33) beyondSafe++;
  }
  if (beyondSafe) throw Error('Foreground exceeds central 66dp circle');
  const monochrome = await sharp(mono,rgba(mono,1080,1080)).png().toBuffer();
  fs.writeFileSync(path.join(root,'monochrome-1080.png'),monochrome);
  const visible = await sharp(foreground).extract({left:180,top:180,width:720,height:720}).png().toBuffer();
  const monoVisible = await sharp(monochrome).extract({left:180,top:180,width:720,height:720}).png().toBuffer();
  const square = await sharp({create:{width:720,height:720,channels:4,background:'#FFFFFF'}}).composite([{input:visible}]).png().toBuffer();
  const monoSquare = await sharp({create:{width:720,height:720,channels:4,background:'#E1EAF2'}}).composite([{input:monoVisible}]).png().toBuffer();
  for (const [name,buffer,size] of [['store-512.png',square,512],['icon-1024.png',square,1024]]) await sharp(buffer).resize(size,size).png().toFile(path.join(root,name));
  const mask = shape => Buffer.from(`<svg xmlns="http://www.w3.org/2000/svg" width="720" height="720">${shape==='circle'?'<circle cx="360" cy="360" r="360" fill="white"/>':`<rect width="720" height="720" rx="${shape==='rounded'?145:210}" fill="white"/>`}</svg>`);
  for (const shape of ['circle','rounded','squircle']) {
    const masked = await sharp(square).composite([{input:mask(shape),blend:'dest-in'}]).png().toBuffer();
    for (const size of [48,64,256]) await sharp(masked).resize(size,size).png().toFile(path.join(root,`${shape}-${size}.png`));
  }
  const monoMasked = await sharp(monoSquare).composite([{input:mask('squircle'),blend:'dest-in'}]).png().toBuffer();
  for (const size of [48,64,256]) await sharp(monoMasked).resize(size,size).png().toFile(path.join(root,`mono-${size}.png`));
  const candidate = path.join(root,'android-candidate','res');
  fs.mkdirSync(path.join(candidate,'drawable-nodpi'),{recursive:true});
  fs.writeFileSync(path.join(candidate,'drawable-nodpi','ic_memory_overlap_art.png'),foreground);
  fs.writeFileSync(path.join(candidate,'drawable-nodpi','ic_memory_overlap_mono_art.png'),monochrome);
  for (const [density,size] of [['mdpi',48],['hdpi',72],['xhdpi',96],['xxhdpi',144],['xxxhdpi',192]]) {
    const folder = path.join(candidate,`mipmap-${density}`); fs.mkdirSync(folder,{recursive:true});
    await sharp(square).resize(size,size).png().toFile(path.join(folder,'ic_launcher_overlap.png'));
  }
  const qa = {approvedSource:'approved-reference.png',builtInCutout:'foreground-cutout.png',canvasPixels:[width,height],components,wholeMarkScaleDpPerPixel:scale,sourceSizeOn1080Canvas:sourceSize,translationPixels:[tx,ty],actualMaxRadiusDp:radiusDp,safeRadiusDp:33,pixelsOutsideSafeRadius:beyondSafe,majorMarkBoundsDp:[left,top,right,bottom].map((v,i) => (i%2?ty:tx)/10+v*sourceSize/width/10),cornerAlpha:[0,0,0,0],preservation:'Uniform whole-mark scale and translation only after imagegen background extraction; generated cutout is not guaranteed pixel-identical to original.',themeIconLimit:'Monochrome alpha keeps silhouette and two yellow-point silhouettes but cannot encode the two blue colors.'};
  fs.writeFileSync(path.join(root,'asset-qa.json'),JSON.stringify(qa,null,2)+'\n');
  let board=`<svg xmlns="http://www.w3.org/2000/svg" width="1080" height="680"><rect width="1080" height="680" fill="#F0F3F5"/>${text(35,44,'未完成 · 无眼睛蓝黄叠层 / 桌面资源适配')}${text(35,77,'同一整体图形等比缩放；白底和遮罩由系统提供，两个黄色元素均保留。',16)}`;
  for(const [i,name] of ['approved-reference.png','squircle-256.png','circle-256.png','mono-256.png'].entries()) {
    const data=fs.readFileSync(path.join(root,name)),x=35+i*260;
    board+=`<image x="${x}" y="110" width="230" height="230" href="${image(data)}"/>`+text(x,371,['用户选定原图','圆角遮罩','圆形遮罩','主题单色轮廓'][i],18);
  }
  for(const [i,shape] of ['squircle','circle'].entries()) {
    const x=45+i*470; board+=text(x,437,i?'圆形 · 原生小尺寸':'圆角 · 原生小尺寸',18);
    for(const [j,size] of [48,64].entries()) board+=`<image x="${x+j*130}" y="462" width="${size}" height="${size}" href="${image(fs.readFileSync(path.join(root,`${shape}-${size}.png`)))}"/>`+text(x+j*130,555,`${size}px`,16);
  }
  board+=text(35,610,`前景最大半径 ${radiusDp.toFixed(2)}dp / 中央安全半径 33dp；超出像素 ${beyondSafe}。`,16)+text(35,640,'这是资源预览，桌面显示以实际手机截图为准；单色版不保留叠层色差。',16)+'</svg>';
  fs.writeFileSync(path.join(root,'comparison.svg'),board);
  await sharp(Buffer.from(board)).png().toFile(path.join(root,'comparison.png'));
  console.log(JSON.stringify(qa,null,2));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
