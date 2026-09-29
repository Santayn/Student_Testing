// @vitest-environment node

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import {
  describe,
  expect,
  it,
} from 'vitest'

function source(...segments) {
  return readFileSync(
    resolve(process.cwd(), 'src', ...segments),
    'utf8'
  )
}

describe('UI Stage 4 admin experience contracts', () => {
  it('provides task-oriented admin actions on Home', () => {
    const home = source('views', 'HomeView.vue')

    expect(home).toContain('adminActions')
    expect(home).toContain("name: 'admin-users'")
    expect(home).toContain("name: 'admin-groups'")
    expect(home).toContain("name: 'admin-subjects'")
    expect(home).toContain("name: 'admin-teaching'")
  })

  it('keeps destructive directory actions secondary through action menus', () => {
    const faculties = source('views', 'admin', 'FacultiesView.vue')
    const subjects = source('views', 'admin', 'SubjectsAdminView.vue')
    const groups = source('views', 'admin', 'GroupsView.vue')

    for (const view of [faculties, subjects, groups]) {
      expect(view).toContain('UiActionMenu')
    }

    expect(groups).toContain('label="Состав группы"')
  })

  it('gives user administration explicit account/profile/access sections', () => {
    const drawer = source('components', 'admin', 'AdminUserDrawer.vue')

    expect(drawer).toContain('1. Учётная запись')
    expect(drawer).toContain('2. Профиль пользователя')
    expect(drawer).toContain('3. Доступ · Роли')
    expect(drawer).toContain('Итоговые права · Только для просмотра')
  })

  it('keeps the permissions directory secondary to role management', () => {
    const roles = source('views', 'admin', 'RolesPermissionsView.vue')

    expect(roles).toContain('permissionDirectoryOpen')
    expect(roles).toContain('Показать справочник прав')
    expect(roles).toContain('v-if="permissionDirectoryOpen"')
  })

  it('reduces teaching filter density without changing filter models', () => {
    const teaching = source('views', 'admin', 'TeachingAssignmentsView.vue')

    expect(teaching).toContain('advancedFiltersOpen')
    expect(teaching).toContain('Дополнительные фильтры')
    expect(teaching).toContain('v-model="subjectFilter"')
    expect(teaching).toContain('v-model="teacherFilter"')
    expect(teaching).toContain('v-model="groupFilter"')
    expect(teaching).toContain('v-model="statusFilter"')
  })

  it('presents teaching assignment editing as semantic sections', () => {
    const drawer = source(
      'components',
      'admin',
      'AdminTeachingAssignmentDrawer.vue'
    )

    expect(drawer).toContain('1. Что назначаем')
    expect(drawer).toContain('2. Учебный период')
    expect(drawer).toContain('3. Кому назначаем')
    expect(drawer).toContain('4. Дополнительно')
  })
})
