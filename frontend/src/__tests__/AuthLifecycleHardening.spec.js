import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'
import {
  createPinia,
  setActivePinia,
} from 'pinia'

const api = vi.hoisted(() => ({
  login: vi.fn(),
  register: vi.fn(),
  refresh: vi.fn(),
  revoke: vi.fn(),
  changePassword: vi.fn(),
  me: vi.fn(),
  getApiErrorMessage: vi.fn(
    (error, fallback) =>
      error?.message || fallback
  ),
}))

vi.mock('@/api', () => ({
  authApi: {
    login: api.login,
    register: api.register,
    refresh: api.refresh,
    revoke: api.revoke,
    changePassword:
      api.changePassword,
    me: api.me,
  },
  getApiErrorMessage:
    api.getApiErrorMessage,
}))

import {
  useAuthStore,
} from '@/stores/auth'

function deferred() {
  let resolve
  let reject

  const promise = new Promise(
    (resolvePromise, rejectPromise) => {
      resolve = resolvePromise
      reject = rejectPromise
    }
  )

  return {
    promise,
    resolve,
    reject,
  }
}

function tokenPair(prefix) {
  return {
    tokenType: 'Bearer',
    accessToken: `${prefix}-access`,
    accessTokenExpiresAtUtc:
      '2099-01-01T00:00:00Z',
    refreshToken: `${prefix}-refresh`,
    refreshTokenExpiresAtUtc:
      '2099-02-01T00:00:00Z',
    lifetimeKind: 1,
  }
}

function user(prefix, id = 1) {
  return {
    id,
    login: prefix,
    personId: id,
    roles: ['STUDENT'],
    permissions: [],
  }
}

function authenticatedStore(prefix = 'a') {
  const store = useAuthStore()

  store.setSessionTokens(
    tokenPair(prefix)
  )
  store.setUser(user(prefix))

  return store
}

describe('auth session lifecycle hardening', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('does not restore a session when an old refresh succeeds after clearSession', async () => {
    const store =
      authenticatedStore('old')
    const refresh = deferred()

    api.refresh.mockReturnValueOnce(
      refresh.promise
    )

    const pending =
      store.refreshSession()

    store.clearSession()

    refresh.resolve({
      data: tokenPair('rotated'),
    })

    await expect(pending)
      .rejects.toMatchObject({
        code: 'AUTH_SESSION_STALE',
      })

    expect(store.accessToken).toBeNull()
    expect(store.refreshToken).toBeNull()
    expect(store.user).toBeNull()
  })

  it('does not let an old refresh overwrite a newer login', async () => {
    const store =
      authenticatedStore('alice')
    const oldRefresh = deferred()

    api.refresh.mockReturnValueOnce(
      oldRefresh.promise
    )

    const staleRefresh =
      store.refreshSession()

    api.login.mockResolvedValueOnce({
      data: tokenPair('bob'),
    })
    api.me.mockResolvedValueOnce({
      data: user('bob', 2),
    })

    await store.login(
      'bob',
      'password'
    )

    oldRefresh.resolve({
      data: tokenPair('alice-rotated'),
    })

    await expect(staleRefresh)
      .rejects.toMatchObject({
        code: 'AUTH_SESSION_STALE',
      })

    expect(store.accessToken)
      .toBe('bob-access')
    expect(store.refreshToken)
      .toBe('bob-refresh')
    expect(store.user?.login)
      .toBe('bob')
  })

  it('does not let an old refresh failure clear a newer login', async () => {
    const store =
      authenticatedStore('alice')
    const oldRefresh = deferred()

    api.refresh.mockReturnValueOnce(
      oldRefresh.promise
    )

    const staleRefresh =
      store.refreshSession()

    api.login.mockResolvedValueOnce({
      data: tokenPair('bob'),
    })
    api.me.mockResolvedValueOnce({
      data: user('bob', 2),
    })

    await store.login(
      'bob',
      'password'
    )

    oldRefresh.reject(
      new Error('old refresh failed')
    )

    await expect(staleRefresh)
      .rejects.toThrow(
        'old refresh failed'
      )

    expect(store.accessToken)
      .toBe('bob-access')
    expect(store.refreshToken)
      .toBe('bob-refresh')
    expect(store.user?.login)
      .toBe('bob')
  })

  it('keeps refresh single-flight inside one session epoch', async () => {
    const store =
      authenticatedStore('alice')
    const refresh = deferred()

    api.refresh.mockReturnValueOnce(
      refresh.promise
    )

    const first =
      store.refreshSession()
    const second =
      store.refreshSession()

    expect(api.refresh)
      .toHaveBeenCalledTimes(1)

    refresh.resolve({
      data: tokenPair('rotated'),
    })

    await expect(
      Promise.all([first, second])
    ).resolves.toEqual([
      'rotated-access',
      'rotated-access',
    ])

    expect(store.accessToken)
      .toBe('rotated-access')
    expect(store.refreshing)
      .toBe(false)
  })

  it('keeps the current session when change-password rejects the current password', async () => {
    const store =
      authenticatedStore('alice')
    const error = Object.assign(
      new Error(
        'The current password is incorrect.'
      ),
      {
        response: {
          status: 401,
        },
      }
    )

    api.changePassword.mockRejectedValueOnce(
      error
    )

    await expect(
      store.changePassword(
        'wrong-password',
        'new-password'
      )
    ).rejects.toBe(error)

    expect(store.accessToken)
      .toBe('alice-access')
    expect(store.refreshToken)
      .toBe('alice-refresh')
    expect(store.passwordError)
      .toBe(
        'Текущий пароль указан неверно.'
      )
  })

  it('clears locally revoked credentials after a successful password change', async () => {
    const store =
      authenticatedStore('alice')
    const beforeEpoch =
      store.sessionEpoch

    api.changePassword.mockResolvedValueOnce({
      status: 204,
    })

    await expect(
      store.changePassword(
        'old-password',
        'new-password'
      )
    ).resolves.toEqual({
      requiresReauthentication: true,
    })

    expect(store.sessionEpoch)
      .toBe(beforeEpoch + 1)
    expect(store.accessToken).toBeNull()
    expect(store.refreshToken).toBeNull()
    expect(store.user).toBeNull()
  })

  it('logs out locally before a pending refresh finishes and revokes the rotated pair without restoring it', async () => {
    const store =
      authenticatedStore('alice')
    const refresh = deferred()

    api.refresh.mockReturnValueOnce(
      refresh.promise
    )
    api.revoke.mockResolvedValueOnce({
      status: 204,
    })

    const pendingRefresh =
      store.refreshSession().catch(
        (error) => error
      )

    const logout = store.logout()

    expect(store.accessToken).toBeNull()
    expect(store.refreshToken).toBeNull()
    expect(store.user).toBeNull()

    refresh.resolve({
      data: tokenPair('rotated'),
    })

    const refreshError =
      await pendingRefresh

    expect(refreshError)
      .toMatchObject({
        code: 'AUTH_SESSION_STALE',
      })

    await logout

    expect(api.revoke)
      .toHaveBeenCalledWith(
        'rotated-refresh',
        'rotated-access',
        'Bearer'
      )

    expect(store.accessToken).toBeNull()
    expect(store.refreshToken).toBeNull()
    expect(store.user).toBeNull()
    expect(store.loggingOut).toBe(false)
  })

  it('keeps auth errors scoped to their operation', async () => {
    const store = useAuthStore()

    api.login.mockRejectedValueOnce(
      new Error('login failed')
    )

    await expect(
      store.login('alice', 'password')
    ).rejects.toThrow('login failed')

    expect(store.loginError)
      .toBe('Не удалось войти в систему')
    expect(store.registerError)
      .toBeNull()
    expect(store.passwordError)
      .toBeNull()

    store.clearError('login')

    expect(store.loginError).toBeNull()
  })
})
