import React, { memo, useId, useLayoutEffect, useRef } from 'react';
import { motion, useTransform } from 'framer-motion';
import { PAPER_FACES, affineTriangle, paperColor, paperPose, polygon, smooth, sourceTriangle, triangle } from './paper-geometry.js';

// Local DOM-only visual snapshot: never serialized, uploaded or saved.
// Capture after successful submission, before removing the confirmation page.
export function captureSealPage(page) {
  if (!page) return null;
  const node = page.cloneNode(true), style = getComputedStyle(page);
  const originals = [...page.querySelectorAll('input,textarea,select')];
  node.querySelectorAll('input,textarea,select').forEach((field, index) => {
    field.value = originals[index].value;
    if ('checked' in field) field.checked = originals[index].checked;
  });
  node.querySelectorAll('[id],[name],[data-page-title]').forEach(element => {
    element.removeAttribute('id'); element.removeAttribute('name'); element.removeAttribute('data-page-title');
  });
  node.inert = true; node.setAttribute('aria-hidden', 'true');
  for (const token of ['--paper-rgb', '--ink', '--muted', '--line', '--accent', '--glass-rgb']) node.style.setProperty(token, style.getPropertyValue(token));
  const sourceScroll = page.querySelector('.page-scroll'), frozenScroll = node.querySelector('.page-scroll');
  if (sourceScroll && frozenScroll) {
    const scrollStyle = getComputedStyle(sourceScroll);
    const gutter = sourceScroll.offsetWidth - sourceScroll.clientWidth - parseFloat(scrollStyle.borderLeftWidth) - parseFloat(scrollStyle.borderRightWidth);
    // Removing the scrollbar must not widen the text measure or clamp scrollTop.
    frozenScroll.style.paddingRight = `${parseFloat(scrollStyle.paddingRight) + gutter}px`;
  }
  return { node, width: page.clientWidth, height: page.clientHeight, scrollTop: sourceScroll?.scrollTop || 0, color: style.backgroundColor };
}

const FrozenPage = memo(function FrozenPage({ snapshot }) {
  const host = useRef(null);
  useLayoutEffect(() => {
    const page = snapshot.node.cloneNode(true);
    host.current.replaceChildren(page);
    const scroller = page.querySelector('.page-scroll');
    if (scroller) scroller.scrollTop = snapshot.scrollTop;
    return () => page.remove();
  }, [snapshot]);
  return <div xmlns="http://www.w3.org/1999/xhtml" ref={host} className="frozen-page" style={{ width:snapshot.width, height:snapshot.height }} />;
});

function PaperFacet({ face, pose, fold, snapshot, sourceWidth, sourceHeight, id }) {
  const printRef=useRef(null);
  const source=sourceTriangle(sourceWidth,sourceHeight,face.index);
  const destination=useTransform(()=>triangle(pose.get().edge,pose.get().hub,face.index));
  const d=useTransform(()=>polygon(destination.get()));
  const transform=useTransform(()=>affineTriangle(source,destination.get()));
  useLayoutEffect(()=>{
    if(!printRef.current) return;
    // An SVG matrix uses user-space coordinates, not CSS fill-box centering.
    const apply=value=>printRef.current?.setAttribute('transform',value);
    apply(transform.get());
    return transform.on('change',apply);
  },[transform,snapshot]);
  const visibility=useTransform(()=>((face.index<=4||face.index===9)?pose.get().printTop:pose.get().printBottom)?'visible':'hidden');
  const light=useTransform(()=>paperColor(paperColor(snapshot?.color||'#f3f1e7','#f3eee1',smooth((fold.get()-.08)/.55)),face.light,smooth((fold.get()-.65)/.35)));
  const shade=useTransform(()=>paperColor(paperColor(snapshot?.color||'#f3f1e7','#e5e2d5',smooth((fold.get()-.08)/.55)),face.shade,smooth((fold.get()-.65)/.35)));
  return <g data-paper-face={face.name}>
    <defs>
      <clipPath id={`${id}-clip`} clipPathUnits="userSpaceOnUse"><path d={polygon(source)} /></clipPath>
      <linearGradient id={`${id}-material`} x1="0" y1="0" x2=".3" y2="1"><motion.stop stopColor={light}/><motion.stop offset="1" stopColor={shade}/></linearGradient>
    </defs>
    <motion.path d={d} fill={`url(#${id}-material)`} />
    {snapshot&&<motion.g ref={printRef} style={{visibility}} data-print-face={face.index}>
      <g clipPath={`url(#${id}-clip)`}><foreignObject x="0" y="0" width={sourceWidth} height={sourceHeight}><FrozenPage snapshot={snapshot}/></foreignObject></g>
    </motion.g>}
  </g>;
}

// One mounted mesh: full page -> folded paper -> boat -> drift. Printed fronts
// leave sight only as their own faces turn away; no replacement boat/crossfade.
export function PageFold({ snapshot, fold, driftScale, opacity, width, height, x, y, rotate, memoryId }) {
  const sourceWidth=snapshot?.width||160, sourceHeight=snapshot?.height||100;
  const id=useId().replaceAll(':','');
  const pose=useTransform(()=>paperPose(fold.get(),sourceWidth,sourceHeight));
  const centerX=useTransform(()=>width.get()/2+(x.get()-width.get()/2)*smooth(fold.get()));
  const centerY=useTransform(()=>height.get()/2+(y.get()-height.get()/2)*smooth(fold.get()));
  const scale=useTransform(()=>{
    const p=fold.get(),target=Math.max(96,Math.min(132,width.get()*.29))/sourceWidth;
    const stops=[0,.44,.67,.84,1],sizes=[width.get()/sourceWidth,.8,.67,.49,target];
    const index=Math.min(3,Math.max(0,stops.findIndex((stop,i)=>i>0&&p<=stop)-1));
    return (sizes[index]+(sizes[index+1]-sizes[index])*smooth((p-stops[index])/(stops[index+1]-stops[index])))*driftScale.get();
  });
  const turn=useTransform(()=>rotate.get()*smooth((fold.get()-.67)/.33));
  const creaseOpacity=useTransform(()=>smooth((fold.get()-.44)/.56)*.65);
  const spine=useTransform(()=>{const state=pose.get();return `M${state.edge[2].join(' ')} L${state.hub.join(' ')}`;});
  const hinge=useTransform(()=>{const state=pose.get();return `M${state.edge[9].join(' ')} L${state.hub.join(' ')} L${state.edge[5].join(' ')}`;});
  const hingeOpacity=useTransform(()=>smooth((fold.get()-.05)/.12)*(1-smooth((fold.get()-.44)/.23))*.3);
  const outline=useTransform(()=>polygon(pose.get().edge));
  const baseColor=useTransform(()=>paperColor(snapshot?.color||'#f3f1e7','#e9e4d6',smooth((fold.get()-.08)/.75)));
  const order=[5,6,7,8,9,0,1,2,3,4];
  return <motion.div className="page-fold-position" aria-hidden="true" inert data-memory-id={memoryId} data-carrier="paper-boat" style={{x:centerX,y:centerY,scale,rotate:turn,opacity}}>
    <div className="page-fold-anchor" style={{width:sourceWidth,height:sourceHeight}}>
      <svg className="page-fold-mesh" viewBox={`${-sourceWidth/2} ${-sourceHeight/2} ${sourceWidth} ${sourceHeight}`} width={sourceWidth} height={sourceHeight}>
        <motion.path d={outline} fill={baseColor} data-paper-surface="backing"/>
        {order.map(index=><PaperFacet key={index} face={PAPER_FACES[index]} pose={pose} fold={fold} snapshot={snapshot} sourceWidth={sourceWidth} sourceHeight={sourceHeight} id={`${id}-${index}`}/>)}
        <motion.path d={hinge} stroke="#8b9c93" strokeWidth={sourceWidth/160*.5} fill="none" style={{opacity:hingeOpacity}}/>
        <motion.path d={spine} stroke="#fffbed" strokeWidth={sourceWidth/160*.65} fill="none" style={{opacity:creaseOpacity}}/>
      </svg>
    </div>
  </motion.div>;
}
