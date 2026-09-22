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

const view = source(
  '../views/admin/RolesPermissionsView.vue'
)
const rolesApi = source(
  '../api/roles.api.js'
)
const adminRoutes = source(
  '../router/routes/admin.js'
)
const navigation = source(
  '../navigation/navigation.config.js'
)

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
    expect(view).toContain('UiDrawer')
    expect(view).toContain('v-model="rolePermissionsOverlay.form.permissionIds"')
    expect(view).toContain('@click="saveRolePermissions"')
    expect(view).toContain('Сохранить права')
    expect(view).toContain('useOverlayForm')

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
    expect(view).toContain('Изменение или удаление существующего permission текущим API не предусмотрено.')
  })

  it('provides scalable search and filters for roles and permissions', () => {
    expect(view).toContain('search-placeholder="Название, описание или permission"')
    expect(view).toContain('rolePermissionFilter')
    expect(view).toContain('roleSortMode')
    expect(view).toContain('search-placeholder="Название или описание permission"')
    expect(view).toContain('permissionUsageFilter')
    expect(view).toContain('permissionSortMode')
    expect(view).toContain('search-placeholder="Найти permission"')
  })
})
