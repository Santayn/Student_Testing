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
