import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import http, {
  configureHttpAuth,
} from '@/api/http'

function deferred() {
  let resolve

  const promise = new Promise(
    (resolvePromise) => {
      resolve = resolvePromise
    }
  )

  return {
    promise,
    resolve,
  }
}

describe('HTTP auth session epoch', () => {
  let token
  let epoch
  let ensure
  let invalid

  beforeEach(() => {
    token = 'alice-access'
    epoch = 7
    ensure = vi.fn()
    invalid = vi.fn()

    configureHttpAuth({
      getAccessToken: () => token,
      getSessionEpoch: () => epoch,
      ensureAccessToken: ensure,
      refreshSession: vi.fn(),
      onSessionInvalid: invalid,
    })
  })

  it('does not send a request from an old session using a newer session token', async () => {
    const preflight = deferred()
    const adapter = vi.fn(
      async (config) => ({
        data: {
          authorization:
            config.headers.Authorization,
        },
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
      })
    )

    ensure.mockReturnValueOnce(
      preflight.promise
    )

    const request = http.get(
      '/epoch-test',
      {
        adapter,
      }
    )

    epoch = 8
    token = 'bob-access'
    preflight.resolve('bob-access')

    await expect(request)
      .rejects.toMatchObject({
        code: 'AUTH_SESSION_STALE',
      })

    expect(adapter)
      .not.toHaveBeenCalled()
    expect(invalid)
      .not.toHaveBeenCalled()
  })

  it('preserves the original epoch when a request is retried', async () => {
    epoch = 8
    token = 'bob-access'

    const adapter = vi.fn(
      async (config) => ({
        data: config.url,
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
      })
    )

    await expect(
      http.get('/old-retry', {
        _authSessionEpoch: 7,
        _authRetry: true,
        adapter,
      })
    ).rejects.toMatchObject({
      code: 'AUTH_SESSION_STALE',
    })

    expect(ensure)
      .not.toHaveBeenCalled()
    expect(adapter)
      .not.toHaveBeenCalled()
    expect(invalid)
      .not.toHaveBeenCalled()
  })

})
