/* Mechanical exports from the editable SVG. No image generation or app mutation. */
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const sharp = require(process.env.ICON_SHARP_PATH || 'sharp');
const root = __dirname;
const spec = JSON.parse(fs.readFileSync(path.join(root, 'spec.json'), 'utf8'));
const source = fs.readFileSync(path.join(root, 'mark-source.svg'), 'utf8');
const parts = [...source.matchAll(/<path id="([^"]+)" fill="([^"]+)" d="([^"]+)"\s*\/>/g)].map(m => ({id:m[1], color:m[2], d:m[3]}));
assert.equal(parts.length, 3);
assert.deepEqual(parts.map(p=>p.color), spec.colors);

function samplePath(d) {
  const tokens = d.match(/[A-Za-z]|-?\d+(?:\.\d+)?/g);
  const pts=[];
  let i=0, x=0,y=0,sx=0,sy=0;
  while(i<tokens.length) {
    const cmd=tokens[i++];
    const n=()=>Number(tokens[i++]);
    if(cmd==='M') {x=n();y=n();sx=x;sy=y;pts.push([x,y]);}
    else if(cmd==='H'){x=n();pts.push([x,y]);}
    else if(cmd==='V'){y=n();pts.push([x,y]);}
    else if(cmd==='C') {
      const x0=x,y0=y,x1=n(),y1=n(),x2=n(),y2=n(),x3=n(),y3=n();
      for(let j=1;j<=200;j++) {
        const t=j/200,u=1-t;
        pts.push([u*u*u*x0+3*u*u*t*x1+3*u*t*t*x2+t*t*t*x3,u*u*u*y0+3*u*u*t*y1+3*u*t*t*y2+t*t*t*y3]);
      }
      x=x3;y=y3;
    } else if(cmd==='Z'){x=sx;y=sy;pts.push([x,y]);}
    else throw Error(`Unsupported command ${cmd}`);
  }
  return pts;
}

const samples=parts.flatMap(p=>samplePath(p.d));
const maxRadius=Math.max(...samples.map(([x,y])=>Math.hypot(x-spec.sourceCenter[0],y-spec.sourceCenter[1])));
const scale=(spec.safeCircleDiameterDp/2-spec.safeCircleInsetDp)/maxRadius;
const tx=54-spec.sourceCenter[0]*scale,ty=54-spec.sourceCenter[1]*scale;
const f=n=>Number(n.toFixed(9));
const transform=`translate(${f(tx)} ${f(ty)}) scale(${f(scale)})`;
const mark=(color)=>`<g transform="${transform}">${parts.map(p=>`<path fill="${color||p.color}" d="${p.d}"/>`).join('')}</g>`;
const bg=`<rect width="108" height="108" fill="${spec.background}"/>`;
function svg(content,viewBox='0 0 108 108',size=108) {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="${viewBox}" width="${size}" height="${size}">${content}</svg>`;
}
const master=svg(bg+mark());
const foreground=svg(mark());
const mono=svg(mark(spec.monochrome));
const visible=svg(bg+mark(),'18 18 72 72',1024);
const visibleMono=svg(bg+mark(spec.monochrome),'18 18 72 72',1024);
function save(name,content){const p=path.join(root,name);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,content);}
function clipped(content,shape,full=false) {
  const mask=shape==='circle'?'<circle cx="54" cy="54" r="36"/>':shape==='squircle'?'<path d="M54 18 C82 18 90 26 90 54 C90 82 82 90 54 90 C26 90 18 82 18 54 C18 26 26 18 54 18 Z"/>':'<rect x="18" y="18" width="72" height="72" rx="15"/>';
  return svg(`<defs><clipPath id="mask">${mask}</clipPath></defs><g clip-path="url(#mask)">${content}</g>`,full?'0 0 108 108':'18 18 72 72',full?108:1024);
}
async function png(name,body,size,square=true){
  const input=sharp(Buffer.from(body));
  await (square?input.resize(size,size):input.resize({width:size})).png().toFile(path.join(root,name));
}

function vectorXml(color){return `<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">\n  <group android:name="memory_fragments" android:scaleX="${f(scale)}" android:scaleY="${f(scale)}" android:translateX="${f(tx)}" android:translateY="${f(ty)}">\n${parts.map(p=>`    <path android:name="${p.id}" android:fillColor="${color||p.color}" android:pathData="${p.d}"/>`).join('\n')}\n  </group>\n</vector>\n`;}
const candidate='android-candidate/res';
save(`${candidate}/drawable/ic_memory_fragment_foreground.xml`,vectorXml());
save(`${candidate}/drawable/ic_memory_fragment_monochrome.xml`,vectorXml('#000000'));
save(`${candidate}/drawable/ic_memory_fragment_background.xml`,`<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">\n  <path android:fillColor="${spec.background}" android:pathData="M0 0 H108 V108 H0 Z"/>\n</vector>\n`);
for(const v of [26,33])save(`${candidate}/mipmap-anydpi-v${v}/ic_launcher_fragment.xml`,`<?xml version="1.0" encoding="utf-8"?>\n<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n  <background android:drawable="@drawable/ic_memory_fragment_background"/>\n  <foreground android:drawable="@drawable/ic_memory_fragment_foreground"/>${v===33?'\n  <monochrome android:drawable="@drawable/ic_memory_fragment_monochrome"/>':''}\n</adaptive-icon>\n`);
save('icon-master.svg',master);
save('foreground.svg',foreground);
save('background.svg',svg(bg));
save('monochrome.svg',mono);
save('icon-visible.svg',visible);
save('monochrome-visible.svg',visibleMono);

function esc(s){return s.replaceAll('&','&amp;').replaceAll('<','&lt;');}
function label(x,y,title,sub){return `<text x="${x}" y="${y}" fill="#203A49" font-size="24" font-family="Microsoft YaHei, sans-serif">${esc(title)}</text>${sub?`<text x="${x}" y="${y+31}" fill="#607784" font-size="17" font-family="Microsoft YaHei, sans-serif">${esc(sub)}</text>`:''}`;}
const embed=buf=>`data:image/png;base64,${buf.toString('base64')}`;
async function imgTag(file,x,y,w){return `<image x="${x}" y="${y}" width="${w}" height="${w}" href="${embed(fs.readFileSync(path.join(root,file)))}"/>`;}

async function build(){
  const reference=fs.existsSync(spec.reference)?spec.reference:path.join(root,'reference-original.png');
  await png('icon-1024.png',visible,1024);
  await png('store-512.png',visible,512);
  await png('reference-original.png',await sharp(reference).resize(512).png().toBuffer(),512);
  await png('redraw-original-scale.png',svg(`<rect width="1254" height="1254" fill="${spec.background}"/>${parts.map(p=>`<path fill="${p.color}" d="${p.d}"/>`).join('')}`,'0 0 1254 1254',512),512);
  for(const s of [48,64,256]){
    await png(`reference-${s}.png`,await sharp(reference).png().toBuffer(),s);
    await png(`color-${s}.png`,visible,s);
    await png(`mono-${s}.png`,visibleMono,s);
    for(const shape of ['circle','squircle','rounded'])await png(`${shape}-${s}.png`,clipped(bg+mark(),shape),s);
  }
  for(const [density,size] of Object.entries({mdpi:48,hdpi:72,xhdpi:96,xxhdpi:144,xxxhdpi:192})){
    fs.mkdirSync(path.join(root,candidate,`mipmap-${density}`),{recursive:true});
    await png(`${candidate}/mipmap-${density}/ic_launcher_fragment.png`,visible,size);
  }

  const box={minX:Math.min(...samples.map(p=>p[0]))*scale+tx,maxX:Math.max(...samples.map(p=>p[0]))*scale+tx,minY:Math.min(...samples.map(p=>p[1]))*scale+ty,maxY:Math.max(...samples.map(p=>p[1]))*scale+ty};
  const centerCircleRadius=maxRadius*scale;
  assert(centerCircleRadius<=33);
  assert(box.minX>21&&box.maxX<87&&box.minY>21&&box.maxY<87);
  const pngMetadata=await sharp(path.join(root,'icon-1024.png')).metadata();
  assert.equal(pngMetadata.width,1024);
  assert.equal(pngMetadata.height,1024);
  const fullAlpha=(await sharp(Buffer.from(foreground),{density:720}).raw().toBuffer({resolveWithObject:true}));
  let total=0,clippedByMask={circle:0,squircle:0,rounded:0},edgeRasterDiff={};
  for(let i=3;i<fullAlpha.data.length;i+=4)if(fullAlpha.data[i]>=128)total++;
  for(const shape of Object.keys(clippedByMask)){
    const maskBytes=await sharp(Buffer.from(clipped(mark(),shape,true)),{density:720}).raw().toBuffer({resolveWithObject:true});
    let lostSolid=0,aaDiff=0;
    for(let i=3;i<maskBytes.data.length;i+=4){
      if(fullAlpha.data[i]>=250&&maskBytes.data[i]<240)lostSolid++;
      if(Math.abs(fullAlpha.data[i]-maskBytes.data[i])>2)aaDiff++;
    }
    // A clip group can change edge AA by a few alpha units; solid loss is clipping.
    clippedByMask[shape]=lostSolid;
    edgeRasterDiff[shape]=aaDiff;
    assert(clippedByMask[shape]===0,`${shape} cuts the mark`);
  }
  const runs=[];
  const raster=fullAlpha;
  let start=-1;
  for(let y=0;y<=1080;y++){
    const hit=y<1080&&raster.data[(y*1080+540)*4+3]>=128;
    if(hit&&start<0)start=y;
    if(!hit&&start>=0){runs.push([start,y-1]);start=-1;}
  }
  assert.equal(runs.length,3);
  const gaps=[runs[1][0]-runs[0][1]-1,runs[2][0]-runs[1][1]-1].map(n=>n/10);
  assert(gaps.every(n=>n>=1.5&&n<=2.5));
  const qa={status:'PASS: static asset checks only',reference:spec.reference,colors:spec.colors,background:spec.background,sourcePaths:3,sourceGeometry:'manual cubic redraw, not pixel-exact trace',scale:f(scale),translate:[f(tx),f(ty)],boundsDp:Object.fromEntries(Object.entries(box).map(([k,v])=>[k,f(v)])),markSizeDp:[f(box.maxX-box.minX),f(box.maxY-box.minY)],centerGapsDp:gaps,safeCircleRadiusDp:f(centerCircleRadius),maskClippedSolidPixels:clippedByMask,maskEdgeRasterDifferencePixels:edgeRasterDiff,foregroundPixels1080:total,checked:['Uniform scaling preserves source proportions','Entire mark fits within central 66dp circle','All three masks retain solid foreground pixels; minor antialias differences reported separately','Exactly three separated planes at center','SVG and Android XML share path data and fixed colors'],notChecked:['Recognition by users','Launcher/OEM theme rendering on phones','An installed app with this icon','Subjective visual acceptance']};
  save('asset-qa.json',JSON.stringify(qa,null,2)+'\n');

  const safe=svg(`<rect width="108" height="108" fill="#E5EBED"/><rect x="18" y="18" width="72" height="72" fill="${spec.background}"/>${mark()}<circle cx="54" cy="54" r="33" fill="none" stroke="#DB784E" stroke-width="0.35" stroke-dasharray="1 1"/><rect x="21" y="21" width="66" height="66" fill="none" stroke="#426779" stroke-width="0.2" stroke-dasharray="1 1"/><rect x="18" y="18" width="72" height="72" fill="none" stroke="#607784" stroke-width="0.25"/>`,'0 0 108 108',648);
  save('safe-zone.svg',safe);
  await png('safe-zone.png',safe,648);

  let board=`<svg xmlns="http://www.w3.org/2000/svg" width="1240" height="910" viewBox="0 0 1240 910"><rect width="1240" height="910" fill="#F3F5F4"/>${label(55,52,'未完成 · C 存片 / 矢量候选','保持已选形状和间隙；调整平台视口，不横向拉伸')}`;
  const titles=[['reference-original.png','已选 v5','原图比例 / 不修改'],['redraw-original-scale.png','矢量重绘','曲线与色值固定 / 原比例'],['icon-1024.png','桌面视口','等比放大占比 / 未安装']];
  for(let i=0;i<titles.length;i++){const x=55+i*390;board+=await imgTag(titles[i][0],x,116,350);board+=label(x,508,titles[i][1],titles[i][2]);}
  board+=label(55,592,'实际像素大小','48px 和 64px 均按原尺寸排布；请在 100% 缩放下查看');
  for(let i=0;i<3;i++){
    const x=75+i*390;
    const p48=i===0?'reference-48.png':i===1?'color-48.png':'mono-48.png';
    const p64=i===0?'reference-64.png':i===1?'color-64.png':'mono-64.png';
    board+=await imgTag(p48,x,668,48);board+=await imgTag(p64,x+115,660,64);
    board+=label(x,765,['原图','彩色矢量','单色矢量'][i],'48px                    64px');
  }
  board+='</svg>';
  save('comparison.svg',board);await png('comparison.png',board,1240,false);

  let desktop=`<svg xmlns="http://www.w3.org/2000/svg" width="1170" height="850" viewBox="0 0 1170 850"><rect width="1170" height="850" fill="#F3F5F4"/>${label(40,50,'桌面大小 / 多裁切与主题对照','模拟预览：不是安装到手机的截图；邻近图形只是占位符')}`;
  const walls=[{fill:'#E9EDEC',ink:'#2A4551',title:'浅色壁纸',monoBg:'#D7E7E9',monoFg:'#1B4E64'},{fill:'#153342',ink:'#EAF3F4',title:'深色壁纸',monoBg:'#284B5B',monoFg:'#A9D5E4'},{fill:'#E8D9C4',ink:'#51483E',title:'沙色壁纸',monoBg:'#DCCBB1',monoFg:'#67573E'}];
  for(let col=0;col<3;col++){
    const x=40+col*375,w=walls[col];
    desktop+=`<rect x="${x}" y="110" width="340" height="680" rx="26" fill="${w.fill}"/><text x="${x+28}" y="155" font-size="22" fill="${w.ink}" font-family="Microsoft YaHei">${w.title}</text>`;
    for(let row=0;row<4;row++){
      const y=208+row*138;
      const icon=row===3?clipped(`<rect width="108" height="108" fill="${w.monoBg}"/>${mark(w.monoFg)}`,'squircle'):clipped(bg+mark(),['circle','squircle','rounded'][row]);
      const iconBuf=await sharp(Buffer.from(icon)).resize(64).png().toBuffer();
      desktop+=`<image x="${x+32}" y="${y}" width="64" height="64" href="${embed(iconBuf)}"/>`;
      desktop+=`<text x="${x+30}" y="${y+90}" font-size="14" fill="${w.ink}" font-family="Microsoft YaHei">未完成</text>`;
      for(let j=0;j<2;j++){
        const nx=x+139+j*100;
        const fills=['#85A49C','#BE957D','#728AAD','#AAB68F'];
        desktop+=`<rect x="${nx}" y="${y}" width="64" height="64" rx="16" fill="${fills[(row+j)%4]}"/><${j?'circle':'rect'} ${j?`cx="${nx+32}" cy="${y+32}" r="14"`:`x="${nx+19}" y="${y+18}" width="26" height="28" rx="4"`} fill="#FFFFFF" fill-opacity="0.9"/><text x="${nx+7}" y="${y+90}" font-size="14" fill="${w.ink}" font-family="Microsoft YaHei">占位图标</text>`;
      }
      desktop+=`<text x="${x+30}" y="${y+113}" font-size="12" fill="${w.ink}" fill-opacity="0.65" font-family="Microsoft YaHei">${['圆形','超椭圆近似','圆角方形','主题着色模拟'][row]}</text>`;
    }
  }
  desktop+='</svg>';
  save('desktop-simulation.svg',desktop);await png('desktop-simulation.png',desktop,1170,false);
  console.log(JSON.stringify(qa,null,2));
}
build().catch(e=>{console.error(e);process.exitCode=1;});
