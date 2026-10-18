import test from 'node:test';
import assert from 'node:assert/strict';
import { blankDraft, mergeDraft, writingError } from '../src/writing-state.js';
import { localCueCandidates, cueOptions, changeCue, selectedCueTexts } from '../src/cue-state.js';

test('local candidates are literal original words, unique, bounded, never adopted by default', () => {
  const text = '夏天，老车站旁边。铁皮屋檐下，窗台上的杯印。老车站。';
  const items = localCueCandidates(text);
  assert.equal(items.length,3);
  assert.equal(new Set(items.map(item => item.text)).size,3);
  for (const item of items) {assert.ok(text.includes(item.text));assert.equal(item.adopted,false);assert.equal(item.source,'local');}
  assert.deepEqual(localCueCandidates(text),items);
});
test('no meaningful local match yields no invented candidate; a photo is not analyzed', () => {
  assert.deepEqual(localCueCandidates('今天有一点累，不知道怎样描述。'),[]);
  const draft = mergeDraft(blankDraft(),{photos:[{id:'photo'}]});
  assert.deepEqual(cueOptions(draft),[]);
  assert.equal(writingError('review',draft),'');
});
test('adoption, editing and undo are explicit; original text is never changed', () => {
  let draft = mergeDraft(blankDraft(),{text:'老车站旁边。'});
  const id = cueOptions(draft)[0].id;
  draft = mergeDraft(draft,{cueChoices:changeCue(draft,id,{text:'屋檐下的声音'})});
  assert.deepEqual(selectedCueTexts(draft),[]); // Editing alone is not adoption.
  draft = mergeDraft(draft,{cueChoices:changeCue(draft,id,{adopted:true})});
  assert.deepEqual(selectedCueTexts(draft),['屋檐下的声音']);
  draft = mergeDraft(draft,{cueChoices:changeCue(draft,id,{adopted:false})});
  assert.deepEqual(selectedCueTexts(draft),[]);
  assert.equal(draft.text,'老车站旁边。');
});
test('chosen and edited cues survive original revision and auxiliary updates', () => {
  let draft = mergeDraft(blankDraft(),{text:'老车站旁边。'});
  const id = cueOptions(draft)[0].id;
  draft = mergeDraft(draft,{cueChoices:changeCue(draft,id,{adopted:true})});
  draft = mergeDraft(draft,{text:'后来又补了一点。',city:'青岛',policy:'quiet'});
  assert.equal(cueOptions(draft)[0].text,'老车站');
  assert.deepEqual(selectedCueTexts(draft),['老车站']);
});
test('clearing edited text unselects it; no candidate or custom cue is required to seal', () => {
  let draft = mergeDraft(blankDraft(),{text:'窗台上。'});
  const id = cueOptions(draft)[0].id;
  draft = mergeDraft(draft,{cueChoices:changeCue(draft,id,{adopted:true})});
  draft = mergeDraft(draft,{cueChoices:changeCue(draft,id,{text:'  '})});
  assert.deepEqual(selectedCueTexts(draft),[]);
  assert.equal(cueOptions(draft)[0].adopted,false);
  assert.equal(writingError('review',draft),'');
});
test('custom cue plus adopted candidates are deduplicated; drafts share no cue array', () => {
  const a=blankDraft(),b=blankDraft();
  a.cueChoices.push({adopted:true,text:'窗台'});
  assert.equal(b.cueChoices.length,0);
  assert.deepEqual(selectedCueTexts({...a,selfCue:' 窗台 '}),['窗台']);
});
