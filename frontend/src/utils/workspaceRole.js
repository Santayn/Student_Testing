export const WORKSPACE_ROLE_PRIORITY = Object.freeze([
  'ADMIN',
  'TEACHER',
  'STUDENT',
])

export const WORKSPACE_ROLE_LABELS = Object.freeze({
  ADMIN: 'Администратор',
  TEACHER: 'Преподаватель',
  STUDENT: 'Студент',
})

function roleCode(role) {
  if (typeof role === 'string') {
    return role
  }

  return (
    role?.name ??
    role?.code ??
    role?.authority ??
    ''
  )
}

export function workspaceRolesFromUserRoles(roles = []) {
  const roleSet = new Set(
    (Array.isArray(roles) ? roles : [])
      .map(roleCode)
      .filter(Boolean)
  )

  return WORKSPACE_ROLE_PRIORITY.filter(
    (role) => roleSet.has(role)
  )
}

export function resolveWorkspaceRole(
  roles,
  preferredRole = null
) {
  const available =
    workspaceRolesFromUserRoles(roles)

  if (
    preferredRole &&
    available.includes(preferredRole)
  ) {
    return preferredRole
  }

  return available[0] ?? null
}
