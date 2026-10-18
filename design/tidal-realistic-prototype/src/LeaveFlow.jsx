// UI study only. Drafts and photos stay in browser memory; no AI or V3 scheduling.
import React, { useEffect, useRef, useState } from 'react';
import { motion } from 'framer-motion';
import { ArrowLeft, ArrowRight, CaretDown, Check, ImageSquare, PencilSimple, Plus, X } from '@phosphor-icons/react';
import { hasDraftContent, writingError } from './writing-state.js';
import { cueOptions, changeCue } from './cue-state.js';

const POLICIES = {
  serendipity: { label: '偶然遇见', hint: '让它在未来某个时刻，偶然回来。' },
  fixed: { label: '留给某一天', hint: '选一个想再次打开它的日子。' },
  quiet: { label: '静静保存', hint: '不主动出现，随时可以在「过去」查看。' },
};
const PREFERENCES = [
  { value: 'inherit', label: '沿用全局偏好', hint: '不为这一段单独设置。' },
  { value: 'light', label: '轻一点', hint: '更突出直接查看。' },
  { value: 'normal', label: '让我先想想', hint: '先给自己一次回忆的机会。' },
  { value: 'deep', label: '我喜欢慢慢想', hint: '主动请求线索的入口更明显。' },
];
const PROVENANCE = { now: '现在留下', backfill: '补记过去', imported: '旧记录' };

function LeaveButton({ children, reduced, className = '', ...props }) {
  return <motion.button type="button" {...props} className={'pressable ' + className}
    whileTap={reduced ? undefined : { scale: .975 }} transition={{ duration: .16 }}>{children}</motion.button>;
}
function LeaveHeader({ title, onBack, backLabel = '返回封存前', action, onAction, reduced, disabled = false }) {
  return <header className="leave-header">
    <LeaveButton className="leave-back" reduced={reduced} disabled={disabled} aria-label={backLabel} onClick={onBack}><ArrowLeft size={21} /></LeaveButton>
    <h2 data-page-title tabIndex={-1}>{title}</h2>
    {action ? <LeaveButton className="leave-header-action" reduced={reduced} disabled={disabled} onClick={onAction}>{action}</LeaveButton> : <span aria-hidden="true" />}
  </header>;
}
function DraftPhotos({ photos = [], onRemove, onView }) {
  if (!photos.length) return null;
  return <div className="leave-photos" aria-label="留下的照片">{photos.map(photo => <figure key={photo.id} className={onRemove ? 'has-remove' : ''}>
    {onView ? <button type="button" className="leave-photo-open" aria-label={'查看照片 ' + photo.name} onClick={() => onView(photo)}><img src={photo.url} alt={photo.name} /></button> : <img src={photo.url} alt={photo.name} />}
    {onRemove && <button type="button" className="leave-photo-remove" aria-label={'移除照片 ' + photo.name} onClick={() => onRemove(photo.id)}><X size={13} /><span>移除</span></button>}
  </figure>)}</div>;
}
function PhotoView({ photo, onBack, reduced }) {
  const pageRef = useRef(null);
  useEffect(() => {pageRef.current?.querySelector('[data-page-title]')?.focus({preventScroll:true});}, []);
  return <section ref={pageRef} className="function-page photo-page leave-ui absolute inset-0 flex flex-col" onKeyDown={event => {if (event.key === 'Escape') {event.stopPropagation();onBack();}}}>
    <LeaveHeader title="照片" {...{onBack,reduced}} backLabel="返回，保留这张照片" />
    <div className="leave-photo-view"><img src={photo.url} alt={photo.name} /></div>
  </section>;
}

function CueChoices({ draft, updateDraft }) {
  const [editingId, setEditingId] = useState(null);
  const [writingOwn, setWritingOwn] = useState(!!draft.selfCue);
  const [focusOwn, setFocusOwn] = useState(false);
  const options = cueOptions(draft);
  const patchCue = (id, patch) => updateDraft({cueChoices:changeCue(draft,id,patch)});
  return <section className="leave-cues" aria-labelledby="leave-cues-title">
    <div className="leave-cue-heading"><h3 id="leave-cues-title">给未来一点线索</h3><span>可选</span></div>
    {options.length > 0 && <>
      <p className="leave-cue-source">本地原文摘词 · 选择后才会保留</p>
      <div className="leave-cue-list">{options.map(option => <div key={option.id} className={'leave-cue-option' + (option.adopted ? ' adopted' : '')}>
        <label className="leave-cue-pick"><input type="checkbox" aria-label={'保留线索 ' + option.text} checked={option.adopted} disabled={!option.text.trim()} onChange={event => patchCue(option.id,{adopted:event.target.checked})} /><span>{option.text || '线索已清空'}{option.text !== option.original && <small>已编辑</small>}</span></label>
        <button type="button" className="leave-cue-edit" aria-label={'编辑线索 ' + option.text} aria-expanded={editingId === option.id} onClick={() => setEditingId(editingId === option.id ? null : option.id)}><PencilSimple size={16} /></button>
        {editingId === option.id && <div className="leave-cue-inline"><input autoFocus aria-label="修改这条线索" maxLength={80} value={option.text} onChange={event => patchCue(option.id,{text:event.target.value})} /><button type="button" className="leave-text-action" onClick={() => setEditingId(null)}>完成</button></div>}
      </div>)}</div>
    </>}
    {!writingOwn ? <button type="button" className="leave-text-action leave-own-cue" onClick={() => {setWritingOwn(true);setFocusOwn(true);}}><Plus size={15} />自己留一点</button> : <div className="leave-self-cue">
      <label className="leave-small-label" htmlFor="self-cue">自己认得的细节</label>
      <textarea autoFocus={focusOwn} id="self-cue" rows={2} maxLength={80} value={draft.selfCue} onChange={event => updateDraft({selfCue:event.target.value})} placeholder="一个声音，一处地方…" />
      <button type="button" className="leave-text-action" onClick={() => {updateDraft({selfCue:''});setWritingOwn(false);}}>不留自写线索</button>
    </div>}
  </section>;
}

export function LeavePage({ draft, updateDraft, onBack, onSave, onNext, onPhotos, onRemovePhoto, reduced }) {
  const [error, setError] = useState('');
  const [readingPhotos, setReadingPhotos] = useState(false);
  const [viewingPhoto, setViewingPhoto] = useState(null);
  const inputRef = useRef(null), photoRef = useRef(null);
  const advance = () => { const message = writingError('leave', draft); setError(message); if (message) inputRef.current?.focus(); else onNext(); };
  const addPhotos = async event => {
    const files = Array.from(event.target.files || []); event.target.value = '';
    if (!files.length) return;
    setReadingPhotos(true);
    try { const message = await onPhotos(files); setError(message || ''); }
    catch { setError('照片未能打开，请重新选择。'); }
    finally { setReadingPhotos(false); }
  };
  if (viewingPhoto) return <PhotoView photo={viewingPhoto} onBack={() => setViewingPhoto(null)} reduced={reduced} />;
  return <section className="function-page writing-page leave-ui absolute inset-0 flex flex-col">
    <LeaveHeader title="留下" {...{onBack,reduced}} backLabel="返回岸边，保留草稿" action="继续" onAction={advance} disabled={readingPhotos} />
    <div className="leave-editor-area">
      <textarea ref={inputRef} id="memory-input" aria-label="想留下的内容" className="leave-editor" value={draft.text}
        onChange={event => { updateDraft({text:event.target.value}); setError(''); }} placeholder="写下想留下的片刻…"
        aria-invalid={!!error} aria-describedby={error ? 'draft-error' : undefined} />
      <DraftPhotos photos={draft.photos} onRemove={onRemovePhoto} onView={setViewingPhoto} />
      {error && <p id="draft-error" role="alert" className="task-error">{error}</p>}
    </div>
    <footer className="leave-editor-tools">
      <input ref={photoRef} type="file" accept="image/jpeg,image/png,image/webp" multiple hidden onChange={addPhotos} />
      <LeaveButton className="leave-tool" reduced={reduced} disabled={readingPhotos || draft.photos.length >= 4} onClick={() => photoRef.current?.click()}><ImageSquare size={20} />{readingPhotos ? '读取中…' : '添加照片'}</LeaveButton>
      <LeaveButton className="leave-tool" reduced={reduced} disabled={!hasDraftContent(draft) || readingPhotos} onClick={onSave}>暂存草稿</LeaveButton>
    </footer>
  </section>;
}

export function ReviewPage({ draft, updateDraft, onBack, onEdit, onPolicy, onBackground, onConfirm, pending, error, reduced }) {
  const [expanded, setExpanded] = useState(false);
  const [overflows, setOverflows] = useState(false);
  const [viewingPhoto, setViewingPhoto] = useState(null);
  const originalRef = useRef(null);
  useEffect(() => {
    const node = originalRef.current;
    if (!node || expanded) return;
    const measure = () => setOverflows(node.scrollHeight > node.clientHeight + 1);
    measure(); const observer = new ResizeObserver(measure); observer.observe(node);
    return () => observer.disconnect();
  }, [draft.text, expanded, viewingPhoto]);
  const background = [draft.occurredAt, draft.city, draft.people].filter(value => value.trim()).join(' · ');
  if (viewingPhoto) return <PhotoView photo={viewingPhoto} onBack={() => setViewingPhoto(null)} reduced={reduced} />;
  return <section className="function-page review-page leave-ui absolute inset-0 flex flex-col" aria-busy={pending}>
    <LeaveHeader title="封存前" {...{onBack,reduced}} backLabel="返回书写" disabled={pending} />
    <div className="leave-scroll page-scroll" inert={pending ? true : undefined}>
      <div className="leave-content leave-review-content">
        <section className="leave-original" aria-label="封存前的内容">
          <div className="leave-section-heading"><h3>这一段</h3><button type="button" className="leave-text-action" onClick={onEdit}>编辑<PencilSimple size={15} /></button></div>
          {draft.text && <p ref={originalRef} id="review-original-text" className={'leave-original-text' + (expanded ? ' expanded' : '')}>{draft.text}</p>}
          {(overflows || expanded) && <button type="button" className="leave-text-action leave-expand" aria-expanded={expanded} aria-controls="review-original-text" onClick={() => setExpanded(value => !value)}>{expanded ? '收起' : '展开全文'}<CaretDown size={14} /></button>}
          <DraftPhotos photos={draft.photos} onView={setViewingPhoto} />
        </section>
        <CueChoices {...{draft,updateDraft}} />
        <section className="leave-return-summary" aria-label="以后怎样出现">
          <p className="leave-small-label">以后</p><button type="button" onClick={onPolicy}><span>{POLICIES[draft.policy].label}{draft.policy === 'fixed' && <small>{draft.fixedDate || '未选日期'}</small>}</span><span className="leave-summary-change">更改<ArrowRight size={15} /></span></button>
        </section>
        <button type="button" className="leave-background-link" onClick={onBackground}><span>{background ? '背景 · ' + background : draft.provenance !== 'now' ? '背景 · ' + PROVENANCE[draft.provenance] : '补充背景'}</span><Plus size={14} /></button>
      </div>
    </div>
    <footer className="leave-footer">
      {error && <p role="alert" className="task-error">{error}</p>}
      <LeaveButton reduced={reduced} className="solid-action leave-seal-action" disabled={pending} onClick={onConfirm}>{pending ? '正在收好…' : draft.policy === 'quiet' ? '静存这一段' : '封存这一段'}{pending ? <span aria-hidden="true">···</span> : <ArrowRight size={18} />}</LeaveButton>
      <p className="leave-seal-note">封存后，仍可在「过去」查看。</p>
    </footer>
  </section>;
}

export function PolicyPage({ draft, updateDraft, onBack, onNext, onPreference, reduced }) {
  const [error, setError] = useState('');
  const dateRef = useRef(null);
  const complete = () => { const message = writingError('policy',draft); setError(message); if (message) dateRef.current?.focus(); else onNext(); };
  return <section className="function-page policy-page leave-ui absolute inset-0 flex flex-col">
    <LeaveHeader title="怎样回来" {...{onBack,reduced}} action="完成" onAction={complete} />
    <div className="leave-scroll page-scroll"><div className="leave-content">
      <fieldset className="leave-choice-list"><legend className="sr-only">以后怎样出现</legend>{Object.entries(POLICIES).map(([key,item]) => <label className="leave-choice" key={key}><input type="radio" name="resurface-policy" checked={draft.policy === key} onChange={() => { updateDraft({policy:key}); setError(''); }} /><span><span className="leave-choice-title">{item.label}</span><span className="leave-choice-hint">{item.hint}</span></span></label>)}</fieldset>
      {draft.policy === 'fixed' && <div className="leave-date-field"><label className="leave-field-label" htmlFor="fixed-date">再见的日期</label><input ref={dateRef} id="fixed-date" type="date" value={draft.fixedDate} aria-invalid={!!error && !draft.fixedDate} aria-describedby={error ? 'policy-error' : undefined} onInput={event => {updateDraft({fixedDate:event.currentTarget.value});setError('');}} onChange={event => {updateDraft({fixedDate:event.target.value});setError('');}} /></div>}
      {error && <p id="policy-error" role="alert" className="task-error">{error}</p>}
      {draft.policy !== 'quiet' && <button type="button" className="leave-preference-link" onClick={onPreference}><span>回忆偏好</span><span>{PREFERENCES.find(item => item.value === draft.preference)?.label}<ArrowRight size={15} /></span></button>}
    </div></div>
  </section>;
}

export function BackgroundPage({ draft, updateDraft, onBack, reduced }) {
  return <section className="function-page background-page leave-ui absolute inset-0 flex flex-col">
    <LeaveHeader title="补充背景" {...{onBack,reduced}} action="完成" onAction={onBack} />
    <div className="leave-scroll page-scroll"><div className="leave-content">
      <div className="leave-background-fields">{[['occurredAt','时间','例如，某年夏天'],['city','地点','一个你记得的地方'],['people','人物','你习惯的称呼']].map(([key,label,placeholder]) => <label className="leave-background-field" key={key}><span className="leave-field-label">{label}</span><input autoComplete="off" value={draft[key]} placeholder={placeholder} onChange={event => updateDraft({[key]:event.target.value})} /></label>)}</div>
      <fieldset className="leave-source-list"><legend className="leave-field-label">记录来源</legend>{Object.entries(PROVENANCE).map(([key,label]) => <label key={key}><input type="radio" name="source" checked={draft.provenance === key} onChange={() => updateDraft({provenance:key})} /><span>{label}</span></label>)}</fieldset>
    </div></div>
  </section>;
}

export function PreferencePage({ draft, updateDraft, onBack, reduced }) {
  return <section className="function-page preference-page leave-ui absolute inset-0 flex flex-col">
    <LeaveHeader title="回忆偏好" {...{onBack,reduced}} backLabel="返回出现方式" action="完成" onAction={onBack} />
    <div className="leave-scroll page-scroll"><div className="leave-content"><fieldset className="leave-choice-list"><legend className="sr-only">这段记忆的回忆偏好</legend>{PREFERENCES.map(item => <label className="leave-choice" key={item.value}><input type="radio" name="memory-preference" checked={draft.preference === item.value} onChange={() => updateDraft({preference:item.value})} /><span><span className="leave-choice-title">{item.label}</span><span className="leave-choice-hint">{item.hint}</span></span></label>)}</fieldset></div></div>
  </section>;
}
