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
  const checks={svgXmlParity:true,htmlLocalLinks:true,controlledSourceEdit:true,smallSizePlanes:{},dimensions:{}};
  const fgXml=fs.readFileSync(path.join(root,'android-candidate/res/drawable/ic_memory_fragment_foreground.xml'),'utf8');
  const monoXml=fs.readFileSync(path.join(root,'android-candidate/res/drawable/ic_memory_fragment_monochrome.xml'),'utf8');
  for(const p of paths){
    assert(fgXml.includes(`android:pathData="${p.d}"`));
    assert(fgXml.includes(`android:fillColor="${p.fill}"`));
    assert(monoXml.includes(`android:pathData="${p.d}"`));
  }
  assert.equal((monoXml.match(/android:fillColor="#000000"/g)||[]).length,3);
  const html=fs.readFileSync(path.join(root,'index.html'),'utf8');
  const localLinks=[];
  for(const m of html.matchAll(/(?:href|src)="([^"]+)"/g)){
    if(/^https?:|^#/.test(m[1]))continue;
    localLinks.push(m[1]);
    if(m[1]==='audit-results.json')continue; // This report is written after all source checks.
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
  const baseline=fs.readFileSync(path.join(root,spec.baselineSource),'utf8');
  const oldPaths=[...baseline.matchAll(/<path id="([^"]+)" fill="([^"]+)" d="([^"]+)"\s*\/>/g)].map(m=>({id:m[1],fill:m[2],d:m[3]}));
  assert.equal(oldPaths.length,3);
  for(let plane=0;plane<3;plane++){
    assert.equal(oldPaths[plane].id,paths[plane].id);
    assert.equal(oldPaths[plane].fill,paths[plane].fill);
    const tokens=d=>d.match(/[A-Za-z]|-?\d+(?:\.\d+)?/g);
    const oldTokens=tokens(oldPaths[plane].d),newTokens=tokens(paths[plane].d);
    assert.equal(oldTokens.length,newTokens.length);
    let i=0;const horizontal=[];
    while(i<oldTokens.length){
      const cmd=oldTokens[i];assert.equal(newTokens[i],cmd);i++;
      const count={M:2,H:1,V:1,C:6,Z:0}[cmd];
      for(let j=0;j<count;j++,i++){
        const n=Number(oldTokens[i]),isY=cmd==='V'||((cmd==='M'||cmd==='C')&&j%2===1);
        if(!isY)horizontal.push(Number(newTokens[i]));
      }
    }
    assert.equal(Math.min(...horizontal),304,`${paths[plane].id}: left alignment changed`);
    assert.equal(Math.max(...horizontal),950,`${paths[plane].id}: right alignment changed`);
  }
  const qa=JSON.parse(fs.readFileSync(path.join(root,'asset-qa.json'),'utf8'));
  assert(Math.abs(qa.aspectRatio-spec.targetAspectRatio)<1e-6);
  assert(Math.abs(qa.widthChangeFromV2)<1e-6);
  assert(Math.abs(qa.heightChangeFromV2-1/15)<1e-6);
  checks.geometry={aspectRatio:qa.aspectRatio,widthChangeFromV2:qa.widthChangeFromV2,heightChangeFromV2:qa.heightChangeFromV2,centerGapsDp:qa.centerGapsDp,safeCircleMarginDp:qa.safeCircleMarginDp};
  checks.controlledSourceEdit='Same path count, order, colors, command structure and left/right alignment; height and curve controls deliberately revised';
  const result={status:'PASS: automated audit, not visual/user/device acceptance',...checks};
  fs.writeFileSync(path.join(root,'audit-results.json'),JSON.stringify(result,null,2)+'\n');
  for(const link of localLinks)assert(fs.existsSync(path.join(root,link)),`Missing HTML asset ${link}`);
  console.log(JSON.stringify(result,null,2));
}
audit().catch(e=>{console.error(e);process.exitCode=1;});
