import {
  describe,
  expect,
  it,
} from 'vitest'

import {
  hasWorkspaceAccess,
  hasWorkspaceRole,
} from '@/utils/accountAccess'

function authState({
  personId = null,
  roles = [],
} = {}) {
  return {
    personId,
    hasAnyRole(...requiredRoles) {
      return requiredRoles.some(
        (role) => roles.includes(role)
      )
    },
  }
}

describe('account workspace access', () => {
  it('does not grant workspace access when a role exists but personId is missing', () => {
    const authStore = authState({
      roles: ['STUDENT'],
    })

    expect(
      hasWorkspaceRole(authStore)
    ).toBe(true)

    expect(
      hasWorkspaceAccess(authStore)
    ).toBe(false)
  })

  it('does not grant workspace access to USER even when a person is linked', () => {
    const authStore = authState({
      personId: 15,
      roles: ['USER'],
    })

    expect(
      hasWorkspaceRole(authStore)
    ).toBe(false)

    expect(
      hasWorkspaceAccess(authStore)
    ).toBe(false)
  })

  it.each([
    'STUDENT',
    'TEACHER',
    'ADMIN',
  ])('grants workspace access to %s only with a linked person', (role) => {
    const authStore = authState({
      personId: 15,
      roles: [role],
    })

    expect(
      hasWorkspaceAccess(authStore)
    ).toBe(true)
  })
})
