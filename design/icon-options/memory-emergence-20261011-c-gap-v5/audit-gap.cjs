const fs=require('node:fs');
const path=require('node:path');
const sharp=require('sharp');
async function run(){
 const results=[];
 for(const [id,file] of [['v3','a-concept.png'],['v4-rejected','b-concept.png'],['v5','c-concept.png']]){
  const {data,info}=await sharp(path.join(__dirname,file)).removeAlpha().raw().toBuffer({resolveWithObject:true});
  const samples=[];
  for(const fraction of [.32,.4,.5,.6,.68]){
   const x=Math.round(info.width*fraction),runs=[];let start=-1;
   for(let y=0;y<=info.height;y++){
    const n=(y*info.width+x)*info.channels;
    const mark=y<info.height&&data[n+2]-data[n]>15&&data[n+1]-data[n]>5&&data[n]<220;
    if(mark&&start<0)start=y;
    if(!mark&&start>=0){if(y-start>10)runs.push([start,y-1]);start=-1;}
   }
   samples.push({x,runs,gaps:runs.slice(1).map((r,i)=>r[0]-runs[i][1]-1)});
  }
  results.push({id,width:info.width,height:info.height,samples});
 }
 fs.writeFileSync(path.join(__dirname,'gap-audit.json'),JSON.stringify(results,null,2));
 console.log(JSON.stringify(results.map(r=>({id:r.id,center:r.samples[2]}))));
}
run().catch(e=>{console.error(e);process.exitCode=1});

