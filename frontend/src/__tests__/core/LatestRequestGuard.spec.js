import {
  describe,
  expect,
  it,
} from 'vitest'

import {
  createLatestRequestGuard,
} from '@/utils/latestRequest'

function deferred() {
  let resolve
  let reject

  const promise = new Promise((res, rej) => {
    resolve = res
    reject = rej
  })

  return {
    promise,
    resolve,
    reject,
  }
}

describe('latest request guard', () => {
  it('allows only the newest overlapping request to commit', async () => {
    const guard = createLatestRequestGuard()
    const commits = []

    const first = deferred()
    const second = deferred()

    async function load(source) {
      const requestId = guard.begin()
      const value = await source.promise

      if (guard.isCurrent(requestId)) {
        commits.push(value)
      }
    }

    const firstLoad = load(first)
    const secondLoad = load(second)

    second.resolve('new context')
    await secondLoad

    first.resolve('old context')
    await firstLoad

    expect(commits).toEqual([
      'new context',
    ])
  })

  it('invalidates an in-flight request when context is cleared', async () => {
    const guard = createLatestRequestGuard()
    const requestId = guard.begin()

    guard.invalidate()

    expect(
      guard.isCurrent(requestId)
    ).toBe(false)
  })
})
