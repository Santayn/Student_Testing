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

  it('does not send a newly registered incomplete account to the protected home page', () => {
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
        'hasWorkspaceAccess(authStore)'
      )
  })

  it('refreshes token claims when an administrator completes account setup', () => {
    const pendingView =
      source(
        '../views/auth/AccountPendingView.vue'
      )

    expect(pendingView)
      .toContain(
        'authStore.refreshIdentity()'
      )

    expect(pendingView)
      .toContain(
        'hasWorkspaceAccess(authStore)'
      )
  })

  it('routes incomplete logins to the pending account page', () => {
    const loginView =
      source(
        '../views/auth/LoginView.vue'
      )

    expect(loginView)
      .toContain(
        "name: 'account-pending'"
      )

    expect(loginView)
      .toContain(
        'hasWorkspaceAccess(authStore)'
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
