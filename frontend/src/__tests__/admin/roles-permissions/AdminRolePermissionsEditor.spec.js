import { ref } from 'vue'

import {
  describe,
  expect,
  it,
} from 'vitest'

import {
  useAdminRolePermissionsEditor,
} from '@/composables/admin/roles-permissions/useAdminRolePermissionsEditor'

function rolePermissionIds(role) {
  return Array.isArray(role?.permissions)
    ? role.permissions
        .map((permission) => Number(permission?.id))
        .filter(Number.isFinite)
    : []
}

function setup() {
  const roles = ref([
    {
      id: 1,
      name: 'Администратор',
      permissions: [
        { id: 10, name: 'users.read' },
        { id: 20, name: 'users.write' },
      ],
    },
  ])
  const permissions = ref([
    { id: 20, name: 'users.write', description: 'Изменение пользователей' },
    { id: 10, name: 'users.read', description: 'Чтение пользователей' },
    { id: 30, name: 'audit.read', description: 'Чтение аудита' },
  ])

  return {
    roles,
    permissions,
    ...useAdminRolePermissionsEditor({
      roles,
      permissions,
      rolePermissionIds,
    }),
  }
}

describe('admin role permissions editor state', () => {
  it('owns selected role, draft permissions and permission search', () => {
    const state = setup()
    const role = state.roles.value[0]

    state.openRolePermissions(role)

    expect(state.selectedRoleId.value).toBe(1)
    expect(state.drawerRole.value).toBe(role)
    expect(state.rolePermissionsOverlay.isOpen.value).toBe(true)
    expect(state.rolePermissionsOverlay.form.permissionIds).toEqual([10, 20])
    expect(state.rolePermissionsOverlay.dirty.value).toBe(false)
    expect(state.drawerPermissions.value.map((permission) => permission.id)).toEqual([30, 10, 20])

    state.permissionDrawerSearch.value = 'изменение'
    expect(state.drawerPermissions.value.map((permission) => permission.id)).toEqual([20])
  })

  it('keeps dirty close confirmation inside editor state', () => {
    const state = setup()
    state.openRolePermissions(state.roles.value[0])

    state.rolePermissionsOverlay.form.permissionIds = [10]

    expect(state.rolePermissionsOverlay.dirty.value).toBe(true)
    expect(state.requestRolePermissionsClose()).toBe(false)
    expect(state.rolePermissionsOverlay.confirmCloseVisible.value).toBe(true)
    expect(state.selectedRoleId.value).toBe(1)

    state.discardRolePermissionsAndClose()

    expect(state.rolePermissionsOverlay.isOpen.value).toBe(false)
    expect(state.selectedRoleId.value).toBeNull()
    expect(state.permissionDrawerSearch.value).toBe('')
  })

  it('refreshes an open draft and closes cleanly when the role disappears', () => {
    const state = setup()
    state.openRolePermissions(state.roles.value[0])
    state.rolePermissionsOverlay.form.permissionIds = [30]

    state.onSelectedRoleRefreshed({
      id: 1,
      permissions: [{ id: 30, name: 'audit.read' }],
    })

    expect(state.rolePermissionsOverlay.form.permissionIds).toEqual([30])
    expect(state.rolePermissionsOverlay.dirty.value).toBe(false)

    state.rolePermissionsError.value = 'Ошибка'
    state.permissionDrawerSearch.value = 'audit'
    state.onSelectedRoleMissing()

    expect(state.selectedRoleId.value).toBeNull()
    expect(state.rolePermissionsOverlay.isOpen.value).toBe(false)
    expect(state.rolePermissionsError.value).toBe('')
    expect(state.permissionDrawerSearch.value).toBe('')
  })
})
