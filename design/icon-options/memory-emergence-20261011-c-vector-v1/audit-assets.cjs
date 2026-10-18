const fs=require('node:fs');
const path=require('node:path');
const assert=require('node:assert/strict');
const sharp=require(process.env.ICON_SHARP_PATH||'sharp');
const root=__dirname;
const spec=JSON.parse(fs.readFileSync(path.join(root,'spec.json'),'utf8'));
const src=fs.readFileSync(path.join(root,'mark-source.svg'),'utf8');
const paths=[...src.matchAll(/<path id="([^"]+)" fill="([^"]+)" d="([^"]+)"\s*\/>/g)].map(m=>({id:m[1],fill:m[2],d:m[3]}));

function connectedComponents(alpha,w,h){
  const seen=new Uint8Array(w*h),sizes=[];
  for(let y=0;y<h;y++)for(let x=0;x<w;x++){
    const index=y*w+x;
    if(seen[index]||alpha[index]<128)continue;
    const todo=[index];seen[index]=1;let count=0;
    while(todo.length){
      const n=todo.pop(),cx=n%w,cy=Math.floor(n/w);count++;
      for(const [nx,ny] of [[cx-1,cy],[cx+1,cy],[cx,cy-1],[cx,cy+1]]){
        if(nx<0||ny<0||nx>=w||ny>=h)continue;
        const ni=ny*w+nx;
        if(!seen[ni]&&alpha[ni]>=128){seen[ni]=1;todo.push(ni);}
      }
    }
    sizes.push(count);
  }
  return sizes.sort((a,b)=>b-a);
}

async function audit(){
  const checks={svgXmlParity:true,htmlLocalLinks:true,sourceMaskOverlap:null,smallSizePlanes:{},dimensions:{}};
  const fgXml=fs.readFileSync(path.join(root,'android-candidate/res/drawable/ic_memory_fragment_foreground.xml'),'utf8');
  const monoXml=fs.readFileSync(path.join(root,'android-candidate/res/drawable/ic_memory_fragment_monochrome.xml'),'utf8');
  for(const p of paths){
    assert(fgXml.includes(`android:pathData="${p.d}"`));
    assert(fgXml.includes(`android:fillColor="${p.fill}"`));
    assert(monoXml.includes(`android:pathData="${p.d}"`));
  }
  assert.equal((monoXml.match(/android:fillColor="#000000"/g)||[]).length,3);
  const html=fs.readFileSync(path.join(root,'index.html'),'utf8');
  for(const m of html.matchAll(/(?:href|src)="([^"]+)"/g)){
    if(/^https?:|^#/.test(m[1]))continue;
    assert(fs.existsSync(path.join(root,m[1])),`Missing HTML asset ${m[1]}`);
  }
  for(const [name,w,h] of [['comparison.png',1240,910],['desktop-simulation.png',1170,850],['icon-1024.png',1024,1024],['store-512.png',512,512]]){
    const meta=await sharp(path.join(root,name)).metadata();
    assert.equal(meta.width,w);assert.equal(meta.height,h);
    checks.dimensions[name]=[w,h];
  }
  const mono=fs.readFileSync(path.join(root,'monochrome.svg'),'utf8').replace('viewBox="0 0 108 108"','viewBox="18 18 72 72"').replace('width="108" height="108"','width="1024" height="1024"');
  for(const size of [48,64]){
    const raster=await sharp(Buffer.from(mono)).resize(size,size).ensureAlpha().raw().toBuffer({resolveWithObject:true});
    const alpha=new Uint8Array(size*size);
    for(let i=0;i<alpha.length;i++)alpha[i]=raster.data[i*4+3];
    const islands=connectedComponents(alpha,size,size);
    assert.equal(islands.length,3,`At ${size}px the planes merge or disappear`);
    assert(islands.every(n=>n>=100));
    checks.smallSizePlanes[size]={connectedPlanes:islands.length,planeAreaPixels:islands};
  }
  if(fs.existsSync(spec.reference)){
    const ref=await sharp(spec.reference).removeAlpha().raw().toBuffer({resolveWithObject:true});
    const vec=await sharp(Buffer.from(src)).ensureAlpha().raw().toBuffer({resolveWithObject:true});
    assert.equal(ref.info.width,vec.info.width);assert.equal(ref.info.height,vec.info.height);
    let intersection=0,union=0;
    for(let i=0;i<ref.info.width*ref.info.height;i++){
      const r=ref.data[i*3],g=ref.data[i*3+1],b=ref.data[i*3+2];
      const a=b-r>15&&g-r>5&&r<220, v=vec.data[i*4+3]>=128;
      if(a&&v)intersection++;if(a||v)union++;
    }
    checks.sourceMaskOverlap=Number((intersection/union).toFixed(6));
    assert(checks.sourceMaskOverlap>0.97,'Redraw deviates from the accepted silhouette');
  }
  const result={status:'PASS: automated audit, not visual/user/device acceptance',...checks};
  fs.writeFileSync(path.join(root,'audit-results.json'),JSON.stringify(result,null,2)+'\n');
  console.log(JSON.stringify(result,null,2));
}
audit().catch(e=>{console.error(e);process.exitCode=1;});
