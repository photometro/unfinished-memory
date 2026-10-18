import test from 'node:test';
import assert from 'node:assert/strict';
import { WRITING_STEPS, blankDraft, mergeDraft, writingError, backgroundCount, hasDraftContent, photoSelectionError } from '../src/writing-state.js';

test('two primary pages without required optional steps; fresh drafts do not share state', () => {
  assert.deepEqual(WRITING_STEPS, ['写下','封存前']);
  const a = blankDraft(), b = blankDraft(); a.text = 'A';
  assert.equal(b.text, '');
  a.photos.push({id:'one'});
  assert.equal(b.photos.length, 0);
});
test('blank and whitespace writing cannot advance or seal', () => {
  for (const step of ['leave', 'policy', 'review']) assert.ok(writingError(step, blankDraft()));
  assert.ok(writingError('leave', mergeDraft(blankDraft(), {text:' \n '})));
});
test('optional clues and all context can remain empty', () => {
  const draft = mergeDraft(blankDraft(), {text:'一个片刻'});
  for (const step of ['leave', 'clues', 'policy', 'review']) assert.equal(writingError(step, draft), '');
  assert.equal(backgroundCount(draft), 0);
});
test('auxiliary updates preserve all main-step inputs without mutating the original', () => {
  const draft = mergeDraft(blankDraft(), {text:'正文', selfCue:'窗台', policy:'fixed', fixedDate:'2027-02-01'});
  const background = mergeDraft(draft, {city:'青岛', people:'朋友', provenance:'backfill'});
  const preference = mergeDraft(background, {preference:'deep'});
  assert.equal(draft.city, '');
  for (const key of ['text','selfCue','policy','fixedDate']) assert.equal(preference[key], draft[key]);
  assert.equal(preference.city, '青岛');
});
test('fixed date blocks the policy step and defensively blocks final submission', () => {
  const draft = mergeDraft(blankDraft(), {text:'正文', policy:'fixed'});
  assert.ok(writingError('policy', draft));
  assert.ok(writingError('review', draft));
  assert.equal(writingError('review', mergeDraft(draft, {fixedDate:'2027-02-01'})), '');
});
test('switching policy does not destroy a selected date or a stored preference', () => {
  const draft = mergeDraft(blankDraft(), {text:'正文', policy:'fixed', fixedDate:'2027-02-01', preference:'deep'});
  const quiet = mergeDraft(draft, {policy:'quiet'});
  assert.equal(writingError('policy', quiet), '');
  assert.equal(quiet.fixedDate, draft.fixedDate);
  assert.equal(quiet.preference, draft.preference);
  assert.equal(mergeDraft(quiet, {policy:'fixed'}).fixedDate, draft.fixedDate);
});
test('adopt, edit, remove and skip clues do not change content or policy', () => {
  const draft = mergeDraft(blankDraft(), {text:'正文', policy:'quiet'});
  const adopted = mergeDraft(draft, {selfCue:'本地演示建议'});
  const edited = mergeDraft(adopted, {selfCue:'自己写的'});
  const removed = mergeDraft(edited, {selfCue:''});
  assert.equal(removed.text, draft.text);
  assert.equal(removed.policy, draft.policy);
  assert.equal(writingError('review', removed), '');
});
test('context count includes only the three optional context values', () => {
  assert.equal(backgroundCount(mergeDraft(blankDraft(), {city:' 青岛 ', people:' ', provenance:'imported'})), 1);
});

test('photo-only drafts can be reviewed and sealed but still validate a fixed date', () => {
  const draft = mergeDraft(blankDraft(), {photos:[{id:'one',url:'data:image/png;base64,...'}]});
  assert.equal(hasDraftContent(draft), true);
  assert.equal(writingError('leave',draft), '');
  assert.equal(writingError('review',draft), '');
  assert.ok(writingError('review',mergeDraft(draft,{policy:'fixed'})));
  assert.equal(hasDraftContent(mergeDraft(draft,{photos:[]})), false);
});

test('photo selection checks count, supported types and size before reading', () => {
  const png = {type:'image/png',size:1024};
  assert.equal(photoSelectionError([png],3), '');
  assert.ok(photoSelectionError([png,png],3));
  assert.ok(photoSelectionError([{type:'application/pdf',size:10}],0));
  assert.ok(photoSelectionError([{type:'image/jpeg',size:11*1024*1024}],0));
});
