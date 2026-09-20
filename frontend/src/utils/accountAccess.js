import {
  WORKSPACE_ROLES,
} from '@/router/roles'

export function hasWorkspaceRole(authStore) {
  return authStore.hasAnyRole(
    ...WORKSPACE_ROLES
  )
}

export function hasWorkspaceAccess(authStore) {
  return Boolean(
    authStore.personId &&
    hasWorkspaceRole(authStore)
  )
}
