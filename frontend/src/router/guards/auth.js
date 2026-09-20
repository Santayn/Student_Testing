import {
  publicRegistrationEnabled,
} from '@/config/features'

import {
  useAuthStore,
} from '@/stores/auth'

import {
  APP_ROLES,
} from '../roles'

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

function routeRequiresAuth(to) {
  const meta =
    matchedMeta(to)

  return (
    meta.some(
      (item) =>
        item.requiresAuth === true
    ) ||
    requiredRoleGroups(to).length > 0
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

function hasApplicationRole(authStore) {
  return authStore.hasAnyRole(
    ...APP_ROLES
  )
}

function authenticatedLanding(authStore) {
  return {
    name: hasApplicationRole(authStore)
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

  if (
    pendingRoleOnly &&
    hasApplicationRole(authStore)
  ) {
    /*
     * Роль могла быть назначена администратором уже
     * после выпуска текущего access token. Обновляем
     * пару токенов, чтобы backend authorities и /me
     * снова описывали одно и то же состояние.
     */
    if (authStore.canRefresh) {
      try {
        await authStore.refreshSession()
        await authStore.loadCurrentUser()
      } catch {
        return {
          name: 'login',
        }
      }
    }

    return {
      name: 'home',
    }
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

  return true
}
