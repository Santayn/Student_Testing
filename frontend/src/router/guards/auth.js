import {
  publicRegistrationEnabled,
} from '@/config/features'

import {
  useAuthStore,
} from '@/stores/auth'

import {
  hasWorkspaceAccess,
} from '@/utils/accountAccess'

function matchedMeta(to) {
  return to.matched.map(
    (record) =>
      record.meta ?? {}
  )
}

function requiredRoleGroups(to) {
  return matchedMeta(to)
    .map(
      (meta) =>
        Array.isArray(meta.roles)
          ? meta.roles
          : []
    )
    .filter(
      (roles) =>
        roles.length > 0
    )
}

function requiredWorkspaceRoleGroups(to) {
  return matchedMeta(to)
    .map(
      (meta) =>
        Array.isArray(meta.workspaceRoles)
          ? meta.workspaceRoles
          : []
    )
    .filter(
      (roles) =>
        roles.length > 0
    )
}

function routeRequiresAuth(to) {
  const meta =
    matchedMeta(to)

  return (
    meta.some(
      (item) =>
        item.requiresAuth === true
    ) ||
    requiredRoleGroups(to).length > 0 ||
    requiredWorkspaceRoleGroups(to).length > 0
  )
}

function routeIsGuestOnly(to) {
  return matchedMeta(to).some(
    (meta) =>
      meta.guestOnly === true
  )
}

function routeIsRegistrationOnly(to) {
  return matchedMeta(to).some(
    (meta) =>
      meta.registrationOnly === true
  )
}

function routeIsPendingRoleOnly(to) {
  return matchedMeta(to).some(
    (meta) =>
      meta.pendingRoleOnly === true
  )
}

function hasRequiredRoles(
  authStore,
  roleGroups
) {
  /*
   * Каждая группа ролей относится к одному matched route.
   *
   * Например:
   * parent.roles = ['ADMIN']
   * child.roles = ['ADMIN', 'TEACHER']
   *
   * Пользователь должен удовлетворять КАЖДОЙ группе.
   * Так дочерний route не сможет случайно ослабить
   * ограничение родителя.
   */
  return roleGroups.every(
    (roles) =>
      authStore.hasAnyRole(
        ...roles
      )
  )
}

function hasRequiredWorkspaceRoles(
  authStore,
  roleGroups
) {
  return roleGroups.every(
    (roles) =>
      roles.includes(
        authStore.workspaceRole
      )
  )
}

function authenticatedLanding(authStore) {
  return {
    name: hasWorkspaceAccess(authStore)
      ? 'home'
      : 'account-pending',
  }
}

export async function authGuard(to) {
  const authStore =
    useAuthStore()

  if (!authStore.initialized) {
    await authStore.init()
  }

  const requiresAuth =
    routeRequiresAuth(to)

  const guestOnly =
    routeIsGuestOnly(to)

  const registrationOnly =
    routeIsRegistrationOnly(to)

  const pendingRoleOnly =
    routeIsPendingRoleOnly(to)

  const roleGroups =
    requiredRoleGroups(to)

  const workspaceRoleGroups =
    requiredWorkspaceRoleGroups(to)

  if (
    requiresAuth &&
    !authStore.isAuthenticated
  ) {
    return {
      name: 'auth-required',

      query: {
        redirect:
          to.fullPath,
      },
    }
  }

  if (
    guestOnly &&
    authStore.isAuthenticated
  ) {
    return authenticatedLanding(
      authStore
    )
  }

  if (
    registrationOnly &&
    !publicRegistrationEnabled
  ) {
    return {
      name: 'login',

      query: {
        registration:
          'disabled',
      },
    }
  }

  /*
   * Рабочие маршруты требуют не только роль, но и
   * привязанный Person. Это закрывает промежуточное
   * состояние, когда администратор уже назначил роль,
   * но профиль пользователя ещё не привязан.
   */
  if (
    requiresAuth &&
    authStore.isAuthenticated &&
    !pendingRoleOnly &&
    !hasWorkspaceAccess(authStore)
  ) {
    return {
      name: 'account-pending',
    }
  }

  if (
    pendingRoleOnly &&
    hasWorkspaceAccess(authStore)
  ) {
    /*
     * Роль/Person могли быть изменены администратором уже
     * после выпуска текущего access token. Обновляем
     * пару токенов, чтобы backend authorities и /me
     * снова описывали одно и то же состояние.
     */
    if (authStore.canRefresh) {
      try {
        await authStore.refreshIdentity()
      } catch {
        return {
          name: 'login',
        }
      }
    }

    if (
      hasWorkspaceAccess(authStore)
    ) {
      return {
        name: 'home',
      }
    }

    return true
  }

  if (
    roleGroups.length > 0 &&
    !hasRequiredRoles(
      authStore,
      roleGroups
    )
  ) {
    return {
      name: 'forbidden',

      query: {
        from:
          to.fullPath,
      },
    }
  }

  if (
    workspaceRoleGroups.length > 0 &&
    !hasRequiredWorkspaceRoles(
      authStore,
      workspaceRoleGroups
    )
  ) {
    return {
      name: 'forbidden',

      query: {
        from:
          to.fullPath,
      },
    }
  }

  return true
}
