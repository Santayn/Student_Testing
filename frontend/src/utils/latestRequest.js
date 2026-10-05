export function createLatestRequestGuard() {
  let version = 0

  return {
    begin() {
      version += 1
      return version
    },

    invalidate() {
      version += 1
    },

    isCurrent(requestId) {
      return requestId === version
    },
  }
}

/**
 * An independent view-owned read request: cancel its transport when another
 * read supersedes it, while keeping the latest-request check as a second
 * defence against adapters that settle after abort(). Do not use this for
 * shared single-flight reads or domain mutations.
 */
export function createAbortableRequestGuard() {
  const latest = createLatestRequestGuard()
  let controller = null

  return {
    begin() {
      const requestId = latest.begin()
      controller?.abort()
      controller = new AbortController()

      return {
        requestId,
        signal: controller.signal,
      }
    },

    invalidate() {
      latest.invalidate()
      controller?.abort()
      controller = null
    },

    isCurrent(requestId) {
      return latest.isCurrent(requestId)
    },
  }
}
