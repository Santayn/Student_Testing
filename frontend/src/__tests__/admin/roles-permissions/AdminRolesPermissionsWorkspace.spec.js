import {
  readFileSync,
} from 'node:fs'
import { resolve } from 'node:path'


import {
  describe,
  expect,
  it,
} from 'vitest'

function source(relativePath) {
  return readFileSync(
    resolve(process.cwd(), 'src', relativePath),
    'utf8'
  )
}

const view = source('views/admin/RolesPermissionsView.vue')
const rolesApi = source('api/roles.api.js')
const dataSource = source('composables/admin/roles-permissions/useAdminRolesPermissionsData.js')
const editorSource = source('composables/admin/roles-permissions/useAdminRolePermissionsEditor.js')
const mutationsSource = source('composables/admin/roles-permissions/useAdminRolesPermissionsMutations.js')
const drawerSource = source('components/admin/AdminRolePermissionsDrawer.vue')
const adminRoutes = source('router/routes/admin.js')
const navigation = source('navigation/navigation.config.js')

describe('admin roles and permissions workspace', () => {
  it('adds a dedicated admin route and sidebar destination', () => {
    expect(adminRoutes).toContain("path: '/admin/roles'")
    expect(adminRoutes).toContain("name: 'admin-roles'")
    expect(adminRoutes).toContain('NAV_KEYS.ADMIN_ROLES')

    expect(navigation).toContain("ADMIN_ROLES: 'admin-roles'")
    expect(navigation).toContain("label: 'Роли и права'")
    expect(navigation).toContain("routeName: 'admin-roles'")
  })

  it('uses explicit role-permission editing instead of user permission guessing', () => {
    expect(view).toContain('title="Роли и права"')
    expect(view).toContain('AdminRolePermissionsDrawer')
    expect(view).toContain('@save="saveRolePermissions"')
    expect(view).toContain('useOverlayForm')
    expect(view).toContain('useAdminRolesPermissionsData')
    expect(view).toContain('useAdminRolePermissionsEditor')
    expect(view).toContain('useAdminRolesPermissionsMutations')
    expect(editorSource).toContain('useOverlayForm')
    expect(editorSource).toContain('selectedRoleId')
    expect(editorSource).toContain('permissionDrawerSearch')
    expect(drawerSource).toContain('UiDrawer')
    expect(drawerSource).toContain('UiCheckbox')
    expect(drawerSource).toContain('Сохранить права')

    expect(mutationsSource).toContain('rolesApi.createRole')
    expect(mutationsSource).toContain('rolesApi.createPermission')
    expect(mutationsSource).toContain('rolesApi.setPermissions')
    expect(view).not.toContain('usersApi.updatePermissions')
    expect(view).not.toContain('AdminTable')
  })

  it('exposes only backend-supported role and permission operations', () => {
    expect(rolesApi).toContain("return http.post('/roles', payload)")
    expect(rolesApi).toContain("return http.get('/roles/permissions')")
    expect(rolesApi).toContain("return http.post('/roles/permissions', payload)")
    expect(rolesApi).toContain('`/roles/${roleId}/permissions`')
    expect(rolesApi).toContain('permissionIds')

    expect(rolesApi).not.toContain('delete(')
    expect(view).toContain('Изменение имени или описания существующей роли текущим API не предусмотрено.')
    expect(view).toContain('Изменение или удаление существующего права текущим API не предусмотрено.')
  })

  it('provides scalable search and filters for roles and permissions', () => {
    expect(view).toContain('search-placeholder="Название, описание или permission"')
    expect(view).toContain('rolePermissionFilter')
    expect(view).toContain('roleSortMode')
    expect(view).toContain('search-placeholder="Название или описание permission"')
    expect(view).toContain('permissionUsageFilter')
    expect(view).toContain('permissionSortMode')
    expect(drawerSource).toContain('search-placeholder="Найти permission"')
    expect(dataSource).toContain('createLatestRequestGuard')
    expect(dataSource).toContain('rolesApi.getAll()')
    expect(dataSource).toContain('rolesApi.getPermissions()')
    expect(dataSource).toContain('filteredRoles')
    expect(dataSource).toContain('filteredPermissions')
  })
})
