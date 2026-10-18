const fs = require('node:fs');
const path = require('node:path');
const sharp = require('sharp');
async function audit(file) {
  const {data,info} = await sharp(file).removeAlpha().raw().toBuffer({resolveWithObject:true});
  const {width:w,height:h,channels} = info;
  const mask = new Uint8Array(w*h);
  for(let p=0;p<w*h;p++){
    const n=p*channels,r=data[n],g=data[n+1],b=data[n+2];
    if(b-r>15 && g-r>5 && r<220) mask[p]=1;
  }
  const seen = new Uint8Array(w*h),components=[];
  for(let p=0;p<mask.length;p++){
    if(!mask[p]||seen[p])continue;
    const queue=[p];seen[p]=1;
    let count=0,minX=w,minY=h,maxX=0,maxY=0,rs=0,gs=0,bs=0;
    for(let head=0;head<queue.length;head++){
      const q=queue[head],x=q%w,y=Math.floor(q/w),n=q*channels;
      count++;minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
      rs+=data[n];gs+=data[n+1];bs+=data[n+2];
      for(const next of [x>0?q-1:-1,x+1<w?q+1:-1,y>0?q-w:-1,y+1<h?q+w:-1]){
        if(next>=0 && mask[next] && !seen[next]){seen[next]=1;queue.push(next);}
      }
    }
    if(count>w*h*.01){
      const rgb=[rs,gs,bs].map(v=>Math.round(v/count));
      const luma=.2126*rgb[0]+.7152*rgb[1]+.0722*rgb[2];
      components.push({minX,maxX,minY,maxY,width:maxX-minX+1,height:maxY-minY+1,meanRGB:rgb,luma:Number(luma.toFixed(1)),area:count});
    }
  }
  components.sort((a,b)=>a.minY-b.minY);
  return {width:w,height:h,components,leftSpread:components.length?Math.max(...components.map(c=>c.minX))-Math.min(...components.map(c=>c.minX)):null,rightSpread:components.length?Math.max(...components.map(c=>c.maxX))-Math.min(...components.map(c=>c.maxX)):null};
}
async function main(){
  const composites=[];
  for(const [key,x] of [['before',36],['c-width-v3',384]]){
    const input=path.join(__dirname,key+'.png');
    for(const size of [48,64,256]){
      const buf=await sharp(input).resize(size,size,{kernel:'lanczos3'}).png().toBuffer();
      fs.writeFileSync(path.join(__dirname,key+'-'+size+'.png'),buf);
      if(size===256)composites.push({input:buf,left:x,top:86});
      if(size===48)composites.push({input:buf,left:x,top:405});
      if(size===64){
        const circle=Buffer.from('<svg width="64" height="64"><circle cx="32" cy="32" r="32" fill="white"/></svg>');
        const round=Buffer.from('<svg width="64" height="64"><rect width="64" height="64" rx="15" fill="white"/></svg>');
        composites.push({input:await sharp(buf).composite([{input:circle,blend:'dest-in'}]).png().toBuffer(),left:x+82,top:397});
        composites.push({input:await sharp(buf).composite([{input:round,blend:'dest-in'}]).png().toBuffer(),left:x+184,top:397});
      }
    }
  }
  const board=Buffer.from('<svg xmlns="http://www.w3.org/2000/svg" width="712" height="520"><style>text{font-family:Microsoft YaHei,sans-serif;fill:#283e48}.label{font-size:12px;fill:#65747c}</style><rect width="712" height="520" fill="#eef0ee"/><text x="36" y="30" font-size="15">C · 存片：比例对照</text><text x="36" y="64" font-size="17">v2 · 对齐与深浅配色</text><text x="384" y="64" font-size="17">v3 · 横向舒展</text><g class="label"><text x="36" y="385">48px</text><text x="118" y="385">64px · 圆形</text><text x="220" y="385">64px · 圆角</text><text x="384" y="385">48px</text><text x="466" y="385">64px · 圆形</text><text x="568" y="385">64px · 圆角</text><text x="36" y="499">仅调整图形比例；概念裁切预览，未替换原生图标。</text></g></svg>');
  await sharp(board).composite(composites).png().toFile(path.join(__dirname,'comparison.png'));
  const result={before:await audit(path.join(__dirname,'before.png')),after:await audit(path.join(__dirname,'c-width-v3.png'))};
  fs.writeFileSync(path.join(__dirname,'alignment-audit.json'),JSON.stringify(result,null,2));
  console.log(JSON.stringify(result));
}
main().catch(e=>{console.error(e);process.exitCode=1});

