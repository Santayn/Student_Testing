import {
  readFileSync,
} from 'node:fs'

import {
  describe,
  expect,
  it,
} from 'vitest'

function source(relativePath) {
  return readFileSync(
    new URL(
      relativePath,
      import.meta.url
    ),
    'utf8'
  )
}

describe('browser auth cookie contract', () => {
  it('does not keep refresh or access secrets in persisted Pinia state', () => {
    const authStore = source(
      '../stores/auth.js'
    )

    expect(authStore)
      .not.toContain('refreshToken: null')
    expect(authStore)
      .not.toContain("'refreshToken',")
    expect(authStore)
      .not.toContain("'accessToken',")
    expect(authStore)
      .toContain("'refreshTokenExpiresAtUtc',")
    expect(authStore)
      .toContain('authApi.csrf()')
    expect(authStore)
      .toContain('authApi.refresh(')

    expect(authStore)
      .toContain('return this.refreshSession()')

    expect(authStore)
      .not.toContain('if (!this.canRefresh)')
  })

  it('sends refresh and revoke without a refresh token request body', () => {
    const authApi = source(
      '../api/auth.api.js'
    )

    expect(authApi)
      .toContain("'/auth/csrf'")
    expect(authApi)
      .toContain("'X-CSRF-Token': csrfToken")
    expect(authApi)
      .not.toContain('refreshToken')
  })

  it('enables credentialed requests for cookie transport', () => {
    const http = source(
      '../api/http.js'
    )

    expect(http)
      .toContain('withCredentials: true')
  })
  it('always attempts server-side revoke even when JavaScript session metadata is empty', () => {
    const authStore = source(
      '../stores/auth.js'
    )

    const logoutStart =
      authStore.indexOf('async logout()')
    const logoutEnd =
      authStore.indexOf('clearError()', logoutStart)
    const logoutSource =
      authStore.slice(logoutStart, logoutEnd)

    expect(logoutSource)
      .toContain('await this.refreshSession()')
    expect(logoutSource)
      .toContain('await authApi.revoke(')
    expect(logoutSource)
      .not.toContain('canTryServerLogout')
  })

})
