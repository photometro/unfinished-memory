// Same settled connected paper net as the continuous full-page ritual.
import React, { memo, useId } from 'react';
import { BOAT_EDGE, BOAT_HUB, PAPER_FACES, polygon, triangle } from './paper-geometry.js';

export const PaperBoat = memo(function PaperBoat() {
  const id=useId().replaceAll(':','');
  const order=[5,6,7,8,9,0,1,2,3,4];
  return <svg viewBox="0 0 160 100" fill="none" className="memory-boat">
    <defs>{PAPER_FACES.map(face=><linearGradient key={face.name} id={`${id}-${face.name}`} x1="0" y1="0" x2=".3" y2="1"><stop stopColor={face.light}/><stop offset="1" stopColor={face.shade}/></linearGradient>)}</defs>
    {order.map(index=><path key={index} data-paper-face={PAPER_FACES[index].name} d={polygon(triangle(BOAT_EDGE,BOAT_HUB,index))} fill={`url(#${id}-${PAPER_FACES[index].name})`}/>)}
    <path d="M84 10 L83 63" stroke="#fffbed" strokeOpacity=".65" strokeWidth=".65"/>
  </svg>;
});
