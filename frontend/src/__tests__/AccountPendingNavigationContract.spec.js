import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

const state = vi.hoisted(() => ({
  auth: null,
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => state.auth,
}))

vi.mock('@/config/features', () => ({
  publicRegistrationEnabled: false,
}))

import {
  WORKSPACE_ROLES,
} from '@/router/roles'
import {
  authGuard,
} from '@/router/guards/auth'

function authState({
  personId = null,
  roles = [],
  isAuthenticated = true,
  canRefresh = false,
} = {}) {
  return {
    initialized: true,
    isAuthenticated,
    personId,
    canRefresh,
    init: vi.fn(),
    refreshIdentity: vi.fn(),
    hasAnyRole(...requiredRoles) {
      return requiredRoles.some(
        (role) => roles.includes(role)
      )
    },
  }
}

function target({
  name = 'home',
  fullPath = '/',
  meta = {
    requiresAuth: true,
    roles: WORKSPACE_ROLES,
  },
} = {}) {
  return {
    name,
    fullPath,
    matched: [
      {
        meta,
      },
    ],
  }
}

describe('account pending navigation guard', () => {
  beforeEach(() => {
    state.auth = authState()
  })

  it('keeps a user with a workspace role but without personId on account-pending', async () => {
    state.auth = authState({
      roles: ['STUDENT'],
      personId: null,
    })

    await expect(
      authGuard(target())
    ).resolves.toEqual({
      name: 'account-pending',
    })
  })

  it('keeps USER out of workspace even when personId exists', async () => {
    state.auth = authState({
      roles: ['USER'],
      personId: 17,
    })

    await expect(
      authGuard(target())
    ).resolves.toEqual({
      name: 'account-pending',
    })
  })

  it('allows a linked student into workspace routes', async () => {
    state.auth = authState({
      roles: ['STUDENT'],
      personId: 17,
    })

    await expect(
      authGuard(target())
    ).resolves.toBe(true)
  })

  it('redirects a ready account away from account-pending', async () => {
    state.auth = authState({
      roles: ['TEACHER'],
      personId: 17,
      canRefresh: false,
    })

    await expect(
      authGuard(
        target({
          name: 'account-pending',
          fullPath: '/account-pending',
          meta: {
            requiresAuth: true,
            pendingRoleOnly: true,
          },
        })
      )
    ).resolves.toEqual({
      name: 'home',
    })
  })

  it('sends unauthenticated users to auth-required with the original path', async () => {
    state.auth = authState({
      isAuthenticated: false,
      roles: [],
    })

    await expect(
      authGuard(
        target({
          fullPath: '/results',
        })
      )
    ).resolves.toEqual({
      name: 'auth-required',
      query: {
        redirect: '/results',
      },
    })
  })
})
