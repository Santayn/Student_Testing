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
  '../views/admin/UsersView.vue'
)
const usersApi = source(
  '../api/users.api.js'
)
const navigation = source(
  '../navigation/navigation.config.js'
)

describe('admin users workspace', () => {
  it('replaces the inline mutation table with cards and a user drawer', () => {
    expect(view).toContain('title="Пользователи"')
    expect(view).toContain('UiFilterBar')
    expect(view).toContain('UiDrawer')
    expect(view).toContain('useOverlayForm')
    expect(view).toContain('admin-user-grid')

    expect(view).not.toContain('AdminTable')
    expect(view).not.toContain('applyRoles')
    expect(view).not.toContain('applyPersonBinding')
  })

  it('keeps role and profile changes local until explicit save', () => {
    expect(view).toContain('v-model="userForm.roleIds"')
    expect(view).toContain('v-model="userForm.personId"')
    expect(view).toContain('v-model="userForm.active"')
    expect(view).toContain('@click="saveUser"')
    expect(view).toContain('Сохранить изменения')

    expect(view).not.toContain('@change="applyRoles')
    expect(view).not.toContain('@change="applyPersonBinding')
  })

  it('saves only backend-supported unambiguous user fields', () => {
    expect(view).toContain('usersApi.updatePersonBinding(')
    expect(view).toContain('usersApi.updateRoles(')
    expect(view).toContain('usersApi.setActive(')
    expect(usersApi).toContain('setActive(userId, active)')
    expect(usersApi).toContain('`/users/${userId}/active`')

    expect(view).not.toContain('updatePermissions(')
    expect(view).toContain('индивидуальные права пользователя этим экраном не изменяются')
  })

  it('supports scalable filtering and searchable person binding', () => {
    expect(view).toContain('search-placeholder="Логин, ФИО, email, телефон или роль"')
    expect(view).toContain('activeFilter')
    expect(view).toContain('profileFilter')
    expect(view).toContain('roleFilter')
    expect(view).toContain('sortMode')
    expect(view).toContain('filter-placeholder="Поиск по ФИО или email"')
  })

  it('keeps users and role permissions as separate admin destinations', () => {
    expect(navigation).toContain("label: 'Пользователи'")
    expect(navigation).toContain("label: 'Роли и права'")
    expect(navigation).not.toContain("label: 'Роли пользователей'")
  })
})
