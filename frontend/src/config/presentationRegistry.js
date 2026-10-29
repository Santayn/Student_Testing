/**
 * Vite turns this static glob into a map of lazy import() functions.
 * Naming convention:
 *   src/views/teacher/presentations/questions.compact.vue
 *   src/views/teacher/presentations/questions.medium.vue
 *   src/views/teacher/presentations/questions.wide.vue
 *   src/views/teacher/presentations/questions.shared.vue
 * View key for all four files: "teacher/questions".
 *
 * Never put component constructors / import functions into Pinia state.
 */
const viewModules = import.meta.glob(
  '/src/views/**/presentations/*.{compact,medium,wide,shared}.vue'
)

const VIEW_PATH_PATTERN =
  /^\/src\/views\/(.+)\/presentations\/([a-z0-9-]+)\.(compact|medium|wide|shared)\.vue$/i

const FALLBACK_ORDER = Object.freeze({
  compact: ['compact', 'shared', 'medium', 'wide'],
  medium: ['medium', 'shared', 'compact', 'wide'],
  wide: ['wide', 'shared', 'medium', 'compact'],
})

export function createPresentationRegistry(modules) {
  const map = new Map()

  for (const [path, load] of Object.entries(modules)) {
    const match = VIEW_PATH_PATTERN.exec(path)
    if (!match || typeof load !== 'function') continue

    const key = `${match[1]}/${match[2]}`
    const variant = match[3]
    const variants = map.get(key) ?? {}
    variants[variant] = load
    map.set(key, variants)
  }

  return map
}

export function resolvePresentationLoader(registry, key, mode) {
  const variants = registry.get(key)
  const candidates = FALLBACK_ORDER[mode]
  if (!variants || !candidates) return null

  for (const candidate of candidates) {
    if (variants[candidate]) return variants[candidate]
  }
  return null
}

export const presentationRegistry =
  createPresentationRegistry(viewModules)

export function getPresentationLoader(key, mode) {
  return resolvePresentationLoader(
    presentationRegistry,
    key,
    mode
  )
}

export function getPresentationKeys() {
  return [...presentationRegistry.keys()].sort()
}
