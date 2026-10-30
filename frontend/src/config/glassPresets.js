/**
 * Glass presentation is independent from motion. Restrict dynamic class names
 * to this allowlist; an ordinary UI remains unchanged unless opted in.
 */
export const SURFACE_PRESETS = Object.freeze(['default', 'glass', 'frosted'])
export const SHINE_PRESETS = Object.freeze(['none', 'sweep'])
export const SHINE_INTENSITIES = Object.freeze(['subtle', 'normal', 'strong'])

export function isGlassSurface(value) {
  return SURFACE_PRESETS.includes(value)
}

export function isShinePreset(value) {
  return SHINE_PRESETS.includes(value)
}

export function isShineIntensity(value) {
  return SHINE_INTENSITIES.includes(value)
}

export function glassClasses(surface, shine, intensity) {
  if (!isGlassSurface(surface) || surface === 'default') return []

  const classes = ['st-glass-surface', `st-glass-surface--${surface}`]
  if (isShinePreset(shine) && shine === 'sweep') {
    classes.push('st-glass-shine--sweep')
    if (isShineIntensity(intensity)) {
      classes.push(`st-glass-shine--${intensity}`)
    }
  }
  return classes
}
