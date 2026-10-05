import {
  describe,
  expect,
  it,
} from 'vitest'

import {
  createAbortableRequestGuard,
} from '@/utils/latestRequest'

function deferred() {
  let resolve
  const promise = new Promise((done) => {
    resolve = done
  })
  return { promise, resolve }
}

describe('abortable view-owned read guard', () => {
  it('aborts the previous transport and keeps only the latest request current', () => {
    const guard = createAbortableRequestGuard()
    const first = guard.begin()
    expect(first.signal.aborted).toBe(false)
    expect(guard.isCurrent(first.requestId)).toBe(true)

    const second = guard.begin()
    expect(first.signal.aborted).toBe(true)
    expect(second.signal.aborted).toBe(false)
    expect(guard.isCurrent(first.requestId)).toBe(false)
    expect(guard.isCurrent(second.requestId)).toBe(true)
  })

  it('aborts on unmount/context invalidation and starts the next request with a fresh signal', () => {
    const guard = createAbortableRequestGuard()
    const first = guard.begin()
    guard.invalidate()

    expect(first.signal.aborted).toBe(true)
    expect(guard.isCurrent(first.requestId)).toBe(false)

    const next = guard.begin()
    expect(next.signal).not.toBe(first.signal)
    expect(next.signal.aborted).toBe(false)
    expect(guard.isCurrent(next.requestId)).toBe(true)
  })

  it('rejects late commits even if an adapter ignores abort()', async () => {
    const guard = createAbortableRequestGuard()
    const oldResult = deferred()
    const commits = []

    const first = guard.begin()
    const pending = oldResult.promise.then((value) => {
      if (guard.isCurrent(first.requestId)) commits.push(value)
    })

    const second = guard.begin()
    expect(first.signal.aborted).toBe(true)
    if (guard.isCurrent(second.requestId)) commits.push('new')

    oldResult.resolve('old')
    await pending
    expect(commits).toEqual(['new'])
  })
})
