/**
 * Public motion vocabulary. Keep values constrained: CSS class names must never
 * be derived from unvalidated user input.
 * `default` preserves existing component behaviour; `none` explicitly opts out.
 */
export const MOTION_PRESETS = Object.freeze({
  button: Object.freeze(['default', 'none', 'press', 'lift', 'pulse', 'hover', 'focused']),
  card: Object.freeze(['default', 'none', 'lift', 'hover', 'focused']),
  alert: Object.freeze(['default', 'none', 'fade', 'fade-slide', 'pulse']),
  dialog: Object.freeze(['default', 'none', 'fade', 'fade-slide', 'scale-fade']),
  select: Object.freeze(['default', 'none', 'focus', 'hover', 'focused']),
  table: Object.freeze(['default', 'none', 'row-highlight', 'focused']),
  field: Object.freeze(['default', 'none', 'error-reveal', 'focused']),
  loading: Object.freeze(['default', 'none', 'fade', 'soft-pulse']),
  toast: Object.freeze(['default', 'none', 'fade', 'fade-slide']),
  disclosure: Object.freeze(['default', 'none', 'expand']),
  transition: Object.freeze(['none', 'fade', 'fade-slide', 'scale-fade', 'expand']),
})

export function isMotionPresetFor(surface, preset) {
  return MOTION_PRESETS[surface]?.includes(preset) ?? false
}

export function motionClassFor(surface, preset) {
  if (!isMotionPresetFor(surface, preset) || preset === 'default') {
    return ''
  }

  return `st-motion-${surface}--${preset}`
}

export function motionTransitionName(preset) {
  return isMotionPresetFor('transition', preset) && preset !== 'none'
    ? `st-motion-${preset}`
    : ''
}
