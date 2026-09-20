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

describe('public registration frontend contract', () => {
  it('uses a frontend registration flag and synchronizes Docker build with backend setting', () => {
    const features =
      source('../config/features.js')

    const compose =
      source('../../../docker-compose.yml')

    expect(features)
      .toContain(
        'VITE_PUBLIC_REGISTRATION_ENABLED'
      )

    expect(compose)
      .toContain(
        'VITE_PUBLIC_REGISTRATION_ENABLED: ${APP_PUBLIC_REGISTRATION_ENABLED:-false}'
      )
  })

  it('does not send a newly registered role-less account to the protected home page', () => {
    const registerView =
      source(
        '../views/auth/RegisterView.vue'
      )

    expect(registerView)
      .toContain(
        "name: 'account-pending'"
      )

    expect(registerView)
      .toContain(
        'authStore.hasAnyRole('
      )
  })

  it('refreshes token claims when an administrator assigns a role', () => {
    const pendingView =
      source(
        '../views/auth/AccountPendingView.vue'
      )

    expect(pendingView)
      .toContain(
        'authStore.refreshSession()'
      )

    expect(pendingView)
      .toContain(
        'authStore.loadCurrentUser()'
      )
  })

  it('routes role-less logins to the pending account page', () => {
    const loginView =
      source(
        '../views/auth/LoginView.vue'
      )

    expect(loginView)
      .toContain(
        "name: 'account-pending'"
      )
  })

  it('blocks direct registration navigation when public registration is disabled', () => {
    const guard =
      source(
        '../router/guards/auth.js'
      )

    const routes =
      source(
        '../router/routes/public.js'
      )

    expect(routes)
      .toContain(
        'registrationOnly: true'
      )

    expect(guard)
      .toContain(
        '!publicRegistrationEnabled'
      )
  })
})
