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

/**
 * Product-wide defaults for foundation controls. Keep the map intentionally
 * small and limited to effects that do not change layout or input behaviour.
 * Explicit `motion="default"` retains the original, pre-motion visual rules;
 * `motion="none"` opts out of decorative transitions.
 */
export const STANDARD_MOTION_PRESETS = Object.freeze({
  button: 'press',
  card: 'focused',
  alert: 'fade',
  dialog: 'scale-fade',
  select: 'focus',
  field: 'focused',
})

export function standardMotionFor(surface) {
  const preset = STANDARD_MOTION_PRESETS[surface]
  return preset && isMotionPresetFor(surface, preset) ? preset : 'default'
}

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

/**
 * Shared navigation motion policy. These are visual choices only: routing,
 * focus handling, disclosure state and viewport measurement remain unchanged.
 */
export const NAVIGATION_MOTION_PRESETS = Object.freeze({
  surface: 'slide',
  header: 'expand',
  disclosure: 'expand',
  flyout: 'fade-slide',
  sheet: 'slide-up',
  bottom: 'slide',
  item: 'highlight',
})

const NAVIGATION_MOTION_ALLOWED = Object.freeze({
  surface: ['slide', 'none'],
  header: ['expand', 'none'],
  disclosure: ['expand', 'none'],
  flyout: ['fade-slide', 'none'],
  sheet: ['slide-up', 'none'],
  bottom: ['slide', 'none'],
  item: ['highlight', 'none'],
})

export function navigationMotionClass(part) {
  const preset = NAVIGATION_MOTION_PRESETS[part]
  return NAVIGATION_MOTION_ALLOWED[part]?.includes(preset)
    ? `st-nav-motion--${part}-${preset}`
    : ''
}

export function navigationTransitionName(part) {
  if (!['surface', 'disclosure', 'flyout'].includes(part)) return ''
  const preset = NAVIGATION_MOTION_PRESETS[part]
  return preset !== 'none' && NAVIGATION_MOTION_ALLOWED[part]?.includes(preset)
    ? `st-nav-${part}`
    : ''
}
