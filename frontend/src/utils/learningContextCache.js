/*
 * Short-lived, memory-only read cache for the current security context.
 * It never persists authorization-dependent lists in browser storage.
 */
const cachesByAuthStore = new WeakMap()
const activeCaches = new Set()

export const LEARNING_CONTEXT_STALE_CODE =
  'LEARNING_CONTEXT_STALE'

export const LEARNING_CONTEXT_TTL_MS = 20_000
export const REFERENCE_TTL_MS = 60_000

function staleContextError() {
  const error = new Error(
    'Учебный контекст изменился во время загрузки'
  )
  error.code = LEARNING_CONTEXT_STALE_CODE
  return error
}

export function createLearningContextCache() {
  const entries = new Map()
  let active = true

  const cache = {
    isActive() {
      return active
    },

    load(key, loader, { ttlMs = LEARNING_CONTEXT_TTL_MS } = {}) {
      if (!active) {
        return Promise.reject(staleContextError())
      }

      const existing = entries.get(key)
      if (
        existing &&
        (existing.pending || existing.expiresAt > Date.now())
      ) {
        return existing.promise
      }

      const entry = { pending: true, expiresAt: 0, promise: null }
      entries.set(key, entry)

      entry.promise = Promise.resolve()
        .then(() => {
          if (!active) throw staleContextError()
          return loader()
        })
        .then((value) => {
          if (!active || entries.get(key) !== entry) {
            throw staleContextError()
          }

          entry.pending = false
          entry.expiresAt = Date.now() + Math.max(0, ttlMs)
          return value
        })
        .catch((error) => {
          if (entries.get(key) === entry) entries.delete(key)
          throw error
        })

      return entry.promise
    },

    invalidate() {
      active = false
      entries.clear()
      activeCaches.delete(cache)
    },
  }

  activeCaches.add(cache)
  return cache
}

function authScope(authStore) {
  if (
    !Number.isInteger(authStore?.sessionEpoch) ||
    authStore.userId == null ||
    !authStore.activeWorkspaceRole
  ) {
    // Existing isolated component tests may provide only a partial auth mock.
    // Such consumers still load normally, just without shared caching.
    return null
  }

  return JSON.stringify([
    authStore.sessionEpoch,
    authStore.userId,
    authStore.personId ?? null,
    authStore.activeWorkspaceRole,
    authStore.roles ?? [],
    authStore.permissions ?? [],
  ])
}

export function getSharedLearningContextCache(authStore) {
  const scope = authScope(authStore)
  if (!scope) return null

  const previous = cachesByAuthStore.get(authStore)
  if (previous?.scope === scope && previous.cache.isActive()) {
    return previous.cache
  }

  previous?.cache.invalidate()
  const cache = createLearningContextCache()
  cachesByAuthStore.set(authStore, { scope, cache })
  return cache
}

export function invalidateLearningContextCache(authStore) {
  const previous = cachesByAuthStore.get(authStore)
  previous?.cache.invalidate()
  cachesByAuthStore.delete(authStore)
}

export function invalidateAllLearningContextCaches() {
  for (const cache of [...activeCaches]) cache.invalidate()
  // The corresponding WeakMap entries will be replaced at their next access.
}
