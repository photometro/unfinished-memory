// A boat's lifecycle and the surrounding current are separate.
export function isCurrentActive(phase) {
  return ['opening', 'ready', 'drifting', 'complete'].includes(phase);
}
