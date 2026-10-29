/**
 * UI modes describe available space, not the physical device / user-agent.
 * Viewport thresholds follow the existing global 640px / 960px CSS rules.
 * A local workspace can override them with its own container thresholds.
 */
export const VIEWPORT_BREAKPOINTS = Object.freeze({
  compactMax: 640,
  mediumMax: 960,
})

export const CONTAINER_BREAKPOINTS = Object.freeze({
  compactMax: 560,
  mediumMax: 880,
})

export function resolvePresentationMode(
  width,
  breakpoints = VIEWPORT_BREAKPOINTS
) {
  if (
    typeof width !== 'number' ||
    !Number.isFinite(width) ||
    width <= 0
  ) {
    return null
  }

  if (width <= breakpoints.compactMax) {
    return 'compact'
  }

  if (width <= breakpoints.mediumMax) {
    return 'medium'
  }

  return 'wide'
}
