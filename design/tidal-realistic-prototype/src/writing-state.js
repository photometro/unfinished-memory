// Prototype draft helpers only: no AI, persistence or resurfacing algorithm.
// The two primary pages; auxiliary pages are not required steps or progress UI.
export const WRITING_STEPS = ['写下', '封存前'];
export const blankDraft = () => ({
  text: '', provenance: 'now', occurredAt: '', city: '', people: '',
  selfCue: '', cueChoices: [], policy: 'serendipity', fixedDate: '', preference: 'inherit', photos: [],
});
export const mergeDraft = (draft, patch) => ({ ...draft, ...patch });
export const hasDraftContent = draft => !!draft.text.trim() || !!draft.photos?.length;
export function writingError(step, draft) {
  if (!hasDraftContent(draft)) return '写下一点内容，或留下一张照片。';
  if ((step === 'policy' || step === 'review') && draft.policy === 'fixed' && !draft.fixedDate)
    return '选一个再见的日期，或改用其他出现方式。';
  return '';
}
export const backgroundCount = draft => ['occurredAt', 'city', 'people'].filter(key => draft[key].trim()).length;
export function photoSelectionError(files, existingCount) {
  if (files.length + existingCount > 4) return '这一段最多留下 4 张照片。';
  if (files.some(file => !['image/jpeg','image/png','image/webp'].includes(file.type))) return '请选择 JPG、PNG 或 WebP 照片。';
  if (files.some(file => file.size > 10 * 1024 * 1024)) return '单张照片请小于 10 MB。';
  return '';
}
