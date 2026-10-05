import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import http, { configureHttpAuth } from '@/api/http'
import {
  getSharedLearningContextCache,
  invalidateAllLearningContextCaches,
} from '@/utils/learningContextCache'

const identity = {
  sessionEpoch: 3,
  userId: 1,
  personId: 7,
  activeWorkspaceRole: 'TEACHER',
}

function successfulAdapter(config) {
  return Promise.resolve({
    data: {},
    status: 200,
    statusText: 'OK',
    headers: {},
    config,
  })
}

describe('learning context invalidation after domain writes', () => {
  beforeEach(() => {
    invalidateAllLearningContextCaches()
    identity.sessionEpoch = 3
    configureHttpAuth({
      getAccessToken: () => 'access',
      getSessionEpoch: () => identity.sessionEpoch,
      ensureAccessToken: vi.fn(),
      refreshSession: vi.fn(),
      onSessionInvalid: vi.fn(),
    })
  })

  it('invalidates after successful subjects/memberships/teaching writes', async () => {
    for (const url of [
      '/subjects/41',
      '/memberships/subjects/memberships/8/status',
      '/teaching/assignments/91',
    ]) {
      const cached = getSharedLearningContextCache(identity)
      await cached.load('teacher:context:7', () => 'before')
      await http.put(url, { active: true }, { adapter: successfulAdapter })
      expect(cached.isActive()).toBe(false)
      expect(getSharedLearningContextCache(identity)).not.toBe(cached)
    }
  })

  it('does not invalidate on failed writes or unrelated writes', async () => {
    const cached = getSharedLearningContextCache(identity)
    await cached.load('teacher:context:7', () => 'before')

    await expect(http.post('/subjects', {}, {
      adapter: async () => { throw new Error('offline') },
    })).rejects.toThrow('offline')
    expect(cached.isActive()).toBe(true)

    await http.post('/tests', {}, { adapter: successfulAdapter })
    expect(cached.isActive()).toBe(true)
  })
})
