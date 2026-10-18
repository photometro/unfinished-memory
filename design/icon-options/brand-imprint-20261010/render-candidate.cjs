const sharp=require('sharp');
const path=require('path');
const fs=require('fs');
const font=path.resolve(__dirname,'../../../android/app/src/main/res/font/noto_sans_sc_variable.ttf');
const root=__dirname;
const master=fs.readFileSync(path.join(root,'icon-master.svg'),'utf8');
const fg=fs.readFileSync(path.join(root,'foreground.svg'),'utf8');
const generated='<local-generated-images>/exec-acf80040-319e-4d85-bfa3-2c2b52812a84.png';
async function label(text,left,top,size=16,color='#244450'){return{input:await sharp({text:{text:`<span foreground="${color}">${text}</span>`,font:`Noto Sans SC ${size}`,fontfile:font,rgba:true}}).png().toBuffer(),left,top};}
async function viewport(size,shape='square',theme){
  let svg=master.replace('viewBox="0 0 108 108"','viewBox="18 18 72 72"');
  if(theme)svg=svg.replaceAll('#315966',theme.bg).replaceAll('#F2EADD',theme.fg);
  let render=sharp(Buffer.from(svg)).resize(size,size);
  if(shape!=='square'){
    const mask=shape==='circle'?`<circle cx="${size/2}" cy="${size/2}" r="${size/2}" fill="white"/>`:`<rect width="${size}" height="${size}" rx="${size*.225}" fill="white"/>`;
    render=render.composite([{input:Buffer.from(`<svg width="${size}" height="${size}">${mask}</svg>`),blend:'dest-in'}]);
  }
  return render.png().toBuffer();
}
async function boundaryCheck(){
  const dim=1080;
  const {data,info}=await sharp(Buffer.from(fg)).resize(dim,dim).ensureAlpha().raw().toBuffer({resolveWithObject:true});
  let minX=dim,maxX=0,minY=dim,maxY=0,maxRadius=0;
  for(let y=0;y<dim;y++)for(let x=0;x<dim;x++)if(data[(y*dim+x)*info.channels+3]>127){minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);maxRadius=Math.max(maxRadius,Math.hypot((x+.5)/10-54,(y+.5)/10-54));}
  return {viewport:108,safeZone:{min:21,max:87},foregroundBounds:{left:minX/10,right:(maxX+1)/10,top:minY/10,bottom:(maxY+1)/10},maxRadiusDp:Math.round(maxRadius*100)/100,within66Square:minX>=210&&maxX<870&&minY>=210&&maxY<870,within66Circle:maxRadius<=33};
}
(async()=>{
  await sharp(generated).png().toFile(path.join(root,'imagegen-refinement.png'));
  await sharp(Buffer.from(master)).resize(1080,1080).png().toFile(path.join(root,'asset-1080.png'));
  await sharp(await viewport(1024)).png().toFile(path.join(root,'icon-1024.png'));
  await sharp(await viewport(512)).png().toFile(path.join(root,'store-512.png'));
  for(const size of [48,64,96])await sharp(await viewport(size,'rounded')).png().toFile(path.join(root,`icon-${size}.png`));
  for(const [density,size] of [['mdpi',48],['hdpi',72],['xhdpi',96],['xxhdpi',144],['xxxhdpi',192]]){
    const dir=path.join(root,'android-candidate','res',`mipmap-${density}`);fs.mkdirSync(dir,{recursive:true});
    await sharp(await viewport(size,'rounded')).png().toFile(path.join(dir,'ic_launcher_memory.png'));
  }
  const layers=[await label('未完成 · 未字印记',36,28,26),await label('名称识别优先；色彩承接气质，主体不复制界面。',36,70,16),
    {input:await viewport(330,'rounded'),left:36,top:116},
    await label('实际桌面尺寸',420,116,20),
    {input:await viewport(48,'rounded'),left:420,top:174},{input:await viewport(64,'rounded'),left:518,top:166},{input:await viewport(96,'rounded'),left:636,top:150},
    await label('48 px',420,262,14),await label('64 px',518,262,14),await label('96 px',636,262,14),
    await label('裁切与主题着色预览',420,312,20),
    {input:await viewport(80,'circle'),left:420,top:360},{input:await viewport(80,'rounded',{bg:'#DDD6F0',fg:'#504367'}),left:534,top:360},{input:await viewport(80,'rounded',{bg:'#D9E9DC',fg:'#335842'}),left:648,top:360},
    await label('圆形',434,454,14),await label('主题色 1',539,454,14),await label('主题色 2',653,454,14),
    await label('独立候选 · 未安装 · 尚待用户识别与真机验收',36,510,16)];
  await sharp({create:{width:830,height:556,channels:4,background:'#F0ECE5'}}).composite(layers).png().toFile(path.join(root,'preview.png'));
  const safe=master.replace('</svg>','<rect x="21" y="21" width="66" height="66" fill="none" stroke="#E8AD62" stroke-width=".4" stroke-dasharray="2 1"/><circle cx="54" cy="54" r="33" fill="none" stroke="#E8AD62" stroke-width=".4" stroke-dasharray="2 1"/></svg>');
  await sharp(Buffer.from(safe)).resize(540,540).png().toFile(path.join(root,'safe-zone.png'));
  const desktops=[];
  for(let i=0;i<2;i++){
    const bg=i===0?'#E3DCCB':'#1E2F35';
    const textColor=i===0?'#244450':'#F2EADD';
    const arrangement=[{input:await viewport(64,'rounded'),left:166,top:70},await label('未完成',166,146,13,textColor)];
    const generic=[['记事','#D6A354'],['相机','#587C88'],['天气','#82A9B8'],['工具','#C2A890']];
    for(let j=0;j<generic.length;j++){
      const x=38+(j%2)*128,y=j<2?240:366;
      const fixture=[
        '<rect x="20" y="14" width="26" height="36" rx="3" fill="none" stroke="#F8F4ED" stroke-width="4"/><path d="M26 24 H39 M26 33 H39 M26 41 H36" stroke="#F8F4ED" stroke-width="3"/>',
        '<rect x="12" y="22" width="40" height="29" rx="5" fill="#F8F4ED"/><rect x="20" y="17" width="13" height="7" rx="2" fill="#F8F4ED"/><circle cx="32" cy="36" r="10" fill="#587C88"/>',
        '<circle cx="25" cy="24" r="10" fill="#F8F4ED"/><path d="M17 43 C7 40 14 28 22 30 C28 18 44 23 43 33 C56 30 58 47 44 47 H20 Z" fill="#F8F4ED" stroke="#82A9B8" stroke-width="2"/>',
        '<path d="M12 22 Q12 17 17 17 H30 L35 23 H49 Q53 23 53 28 V47 H12 Z" fill="#F8F4ED"/>'
      ][j];
      arrangement.push({input:await sharp({create:{width:64,height:64,channels:4,background:generic[j][1]}}).composite([{input:Buffer.from(`<svg width="64" height="64">${fixture}</svg>`)}]).png().toBuffer(),left:x,top:y});
      arrangement.push(await label(generic[j][0],x+8,y+74,13,textColor));
    }
    const wallpaper=await sharp({create:{width:300,height:504,channels:4,background:bg}}).composite(arrangement).png().toBuffer();
    desktops.push({input:wallpaper,left:30+i*340,top:70});
  }
  await sharp({create:{width:680,height:600,channels:4,background:'#F0ECE5'}}).composite([await label('桌面模拟 · 邻近图标为占位符，不是真机截图',30,24,17),...desktops]).png().toFile(path.join(root,'desktop-simulation.png'));
  const checks=await boundaryCheck();
  if(!checks.within66Square||!checks.within66Circle)throw new Error('Foreground is outside the safe zone');
  const vectorPaths=[...fs.readFileSync(path.join(root,'android-candidate/res/drawable/ic_launcher_memory_foreground.xml'),'utf8').matchAll(/android:pathData="([^"]+)"/g)].map(m=>m[1]);
  const svgPaths=[...master.matchAll(/ d="([^"]+)"/g)].map(m=>m[1]);
  if(JSON.stringify(vectorPaths)!==JSON.stringify(svgPaths))throw new Error('SVG and Android paths diverged');
  console.log(JSON.stringify({checks,sourceAndAndroidPathsMatch:true,assetFiles:['foreground.svg','background.svg','monochrome.svg','icon-master.svg'],deviceTested:false,recognitionTested:false}));
})();
