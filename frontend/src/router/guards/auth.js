import {
  useAuthStore,
} from '@/stores/auth'

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

function requiredPermissionGroups(to) {
  return matchedMeta(to)
    .map(
      (meta) =>
        Array.isArray(meta.permissions)
          ? meta.permissions
          : []
    )
    .filter(
      (permissions) =>
        permissions.length > 0
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
    requiredPermissionGroups(to).length > 0
  )
}

function routeIsGuestOnly(to) {
  return matchedMeta(to).some(
    (meta) =>
      meta.guestOnly === true
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

function hasRequiredPermissions(
  authStore,
  permissionGroups
) {
  return permissionGroups.every(
    (permissions) =>
      authStore.hasAnyPermission(
        ...permissions
      )
  )
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

  const roleGroups =
    requiredRoleGroups(to)

  const permissionGroups =
    requiredPermissionGroups(to)

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

  if (
    permissionGroups.length > 0 &&
    !hasRequiredPermissions(
      authStore,
      permissionGroups
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
