import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import {
  createLearningContextCache,
  getSharedLearningContextCache,
  invalidateAllLearningContextCaches,
  invalidateLearningContextCache,
} from '@/utils/learningContextCache'

function deferred() {
  let resolve
  let reject
  const promise = new Promise((res, rej) => {
    resolve = res
    reject = rej
  })
  return { promise, resolve, reject }
}

function auth({ userId = 1, personId = 7, activeWorkspaceRole = 'STUDENT' } = {}) {
  return {
    sessionEpoch: 8,
    userId,
    personId,
    activeWorkspaceRole,
  }
}

describe('shared learning context cache', () => {
  beforeEach(() => {
    vi.useRealTimers()
    invalidateAllLearningContextCaches()
  })

  it('deduplicates in-flight loads and reuses only unexpired snapshots', async () => {
    const cache = createLearningContextCache()
    const waiting = deferred()
    const loader = vi.fn(() => waiting.promise)

    const first = cache.load('student:7', loader, { ttlMs: 1000 })
    const second = cache.load('student:7', loader, { ttlMs: 1000 })
    expect(first).toBe(second)

    waiting.resolve({ subjects: [1] })
    expect(await second).toEqual({ subjects: [1] })
    expect(loader).toHaveBeenCalledTimes(1)
    expect(await cache.load('student:7', loader)).toEqual({ subjects: [1] })
    expect(loader).toHaveBeenCalledTimes(1)

    vi.useFakeTimers()
    await vi.advanceTimersByTimeAsync(1001)
    expect(await cache.load('student:7', () => ({ subjects: [2] })))
      .toEqual({ subjects: [2] })
    cache.invalidate()
  })

  it('does not retain failed loads and rejects a result after invalidation', async () => {
    const cache = createLearningContextCache()
    const failing = vi.fn().mockRejectedValueOnce(new Error('offline'))
      .mockResolvedValueOnce('recovered')

    await expect(cache.load('f', failing)).rejects.toThrow('offline')
    expect(await cache.load('f', failing)).toBe('recovered')
    expect(failing).toHaveBeenCalledTimes(2)

    const pending = deferred()
    const stale = cache.load('stale', () => pending.promise)
    await Promise.resolve()
    cache.invalidate()
    pending.resolve('old account')
    await expect(stale).rejects.toMatchObject({
      code: 'LEARNING_CONTEXT_STALE',
    })
  })

  it('isolates user, person, workspace and session changes', async () => {
    const identity = auth()
    const oldCache = getSharedLearningContextCache(identity)
    const pending = deferred()
    const oldRequest = oldCache.load('student:7', () => pending.promise)
    await Promise.resolve()

    identity.activeWorkspaceRole = 'TEACHER'
    const teacherCache = getSharedLearningContextCache(identity)
    expect(teacherCache).not.toBe(oldCache)
    pending.resolve('private student data')
    await expect(oldRequest).rejects.toMatchObject({
      code: 'LEARNING_CONTEXT_STALE',
    })
    expect(await teacherCache.load('teacher:7', () => 'teacher data'))
      .toBe('teacher data')

    identity.personId = 8
    const newPersonCache = getSharedLearningContextCache(identity)
    expect(newPersonCache).not.toBe(teacherCache)

    identity.userId = 2
    const otherAccountCache = getSharedLearningContextCache(identity)
    expect(otherAccountCache).not.toBe(newPersonCache)

    identity.sessionEpoch += 1
    expect(getSharedLearningContextCache(identity))
      .not.toBe(otherAccountCache)

    invalidateLearningContextCache(identity)
    expect(getSharedLearningContextCache(identity))
      .not.toBe(otherAccountCache)
  })

  it('removes cached data after a successful domain mutation', async () => {
    const identity = auth()
    const previous = getSharedLearningContextCache(identity)
    await previous.load('subjects:catalog', () => ['before'])

    invalidateAllLearningContextCaches()
    const current = getSharedLearningContextCache(identity)
    expect(current).not.toBe(previous)
    await expect(previous.load('subjects:catalog', () => ['stale']))
      .rejects.toMatchObject({ code: 'LEARNING_CONTEXT_STALE' })
    expect(await current.load('subjects:catalog', () => ['after']))
      .toEqual(['after'])
  })
})
