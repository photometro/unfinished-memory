const sharp=require('sharp');
const path=require('path');
const source='<local-generated-images>/exec-ebf3b643-3723-46e2-820d-148830cf8349.png';
const font=path.resolve(__dirname,'../../../android/app/src/main/res/font/noto_sans_sc_variable.ttf');
async function label(text,left,top,size=16){return{input:await sharp({text:{text,font:`Noto Sans SC ${size}`,fontfile:font,rgba:true}}).png().toBuffer(),left,top};}
(async()=>{
  const {data,info}=await sharp(source).removeAlpha().greyscale().raw().toBuffer({resolveWithObject:true});
  const names=['未字标','记忆匣','回返印记'];
  const layers=[await label('轮廓筛选 · 精修前',32,24,26)];
  const bounds=[];
  for(let i=0;i<3;i++){
    const x0=Math.floor(info.width*i/3),x1=Math.floor(info.width*(i+1)/3);
    let left=x1,top=info.height,right=x0,bottom=0;
    for(let y=0;y<info.height;y++)for(let x=x0;x<x1;x++)if(data[y*info.width+x]<80){left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x);bottom=Math.max(bottom,y);}
    const region={left,top,width:right-left+1,height:bottom-top+1};bounds.push({name:names[i],...region});
    const crop=await sharp(source).extract(region).png().toBuffer();
    await sharp(crop).resize(256,256,{fit:'contain',background:'white'}).png().toFile(path.join(__dirname,`sketch-${i+1}.png`));
    layers.push({input:await sharp(crop).resize(170,170,{fit:'contain',background:'white'}).png().toBuffer(),left:48+i*300,top:84});
    layers.push(await label(names[i],48+i*300,278,21));
    for(const s of [48,64]){
      const px=48+i*300+(s===48?0:110);
      const thumb=await sharp(crop).resize(s-10,s-10,{fit:'contain',background:'white'}).extend({top:5,bottom:5,left:5,right:5,background:'white'}).png().toBuffer();
      layers.push({input:thumb,left:px,top:334});layers.push(await label(`${s}px`,px,414,14));
      await sharp(thumb).png().toFile(path.join(__dirname,`sketch-${i+1}-${s}.png`));
    }
  }
  await sharp(source).png().toFile(path.join(__dirname,'concept-sheet.png'));
  await sharp({create:{width:900,height:470,channels:4,background:'#F1EDE6'}}).composite(layers).png().toFile(path.join(__dirname,'concept-check.png'));
  console.log(JSON.stringify({bounds,checks:['48px','64px'],artisticEdit:false}));
})();
