// A deliberately small UI demo, not V3 cue scoring or semantic/AI analysis.
// Only copy literal scene words from the original. No inferred story or emotion.
const SCENE_WORDS = /(?:老|旧|小)?(?:车站|站台|码头|街角|路口|书店|咖啡馆|餐馆|小店|桥边|海边)|(?:铁皮|木)?屋檐|窗台|窗边|杯印|纸箱|路灯|树叶|新叶|雨声|滴水声|钟声|脚步声|蝉鸣|夏天|秋天|冬天|春天|清晨|傍晚/gu;

export function localCueCandidates(text) {
  const words = [...new Set([...text.matchAll(SCENE_WORDS)].map(match => match[0]))].slice(0, 3);
  return words.map(word => ({ id: 'local:' + word, original: word, text: word, adopted: false, source: 'local' }));
}

export function cueOptions(draft) {
  const stored = draft.cueChoices || [];
  const current = localCueCandidates(draft.text).map(candidate => stored.find(item => item.id === candidate.id) || candidate);
  // Editing the original never silently erases an explicitly chosen/edited cue.
  for (const item of stored) {
    if (!current.some(candidate => candidate.id === item.id) && (item.adopted || item.text !== item.original)) current.push(item);
  }
  return current;
}

export function changeCue(draft, id, patch) {
  const option = cueOptions(draft).find(item => item.id === id);
  if (!option) return draft.cueChoices || [];
  const updated = { ...option, ...patch };
  if (!updated.text.trim()) updated.adopted = false;
  return [...(draft.cueChoices || []).filter(item => item.id !== id), updated];
}

export function selectedCueTexts(draft) {
  return [...new Set([draft.selfCue, ...(draft.cueChoices || []).filter(item => item.adopted).map(item => item.text)]
    .map(text => text.trim()).filter(Boolean))];
}
