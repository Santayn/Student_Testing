// @vitest-environment node

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import {
  describe,
  expect,
  it,
} from 'vitest'

const groupsView = readFileSync(
  resolve(
    process.cwd(),
    'src',
    'views',
    'admin',
    'GroupsView.vue'
  ),
  'utf8'
)

describe('admin groups overlay workspace', () => {
  it('uses a searchable workspace instead of an inline editor and legacy table', () => {
    expect(groupsView).toContain('UiFilterBar')
    expect(groupsView).toContain('filteredGroups')
    expect(groupsView).toContain('facultyFilter')
    expect(groupsView).toContain('sortMode')
    expect(groupsView).toContain('resetFilters')
    expect(groupsView).not.toContain('AdminTable')
    expect(groupsView).not.toContain('window.scrollTo')
  })

  it('creates and edits groups through the shared dialog lifecycle', () => {
    expect(groupsView).toContain('useOverlayForm')
    expect(groupsView).toContain('groupDialogModel')
    expect(groupsView).toContain('openCreateGroup')
    expect(groupsView).toContain('openEditGroup')
    expect(groupsView).toContain('UiUnsavedChangesConfirm')
    expect(groupsView).toContain('requestClose')
  })

  it('uses an application confirmation dialog for deletion', () => {
    expect(groupsView).toContain('deleteConfirmVisible')
    expect(groupsView).toContain('title="Удалить группу?"')
    expect(groupsView).toContain('requestDeleteGroup')
    expect(groupsView).not.toContain('window.confirm')
  })

  it('preserves the existing group and faculty API contracts', () => {
    expect(groupsView).toContain('facultiesApi.getAll()')
    expect(groupsView).toContain('groupsApi.getAll()')
    expect(groupsView).toContain('groupsApi.create(payload)')
    expect(groupsView).toContain('groupsApi.update(')
    expect(groupsView).toContain('groupsApi.remove(group.id)')
  })

  it('matches backend field limits and protects the unique group code before submit', () => {
    expect(groupsView).toContain('name.length > 200')
    expect(groupsView).toContain('code.length > 50')
    expect(groupsView).toContain('Группа с кодом «${code}» уже существует.')
    expect(groupsView).toContain("return 'Выберите факультет.'")
  })

  it('keeps workspace filters outside the CRUD lifecycle', () => {
    const saveStart = groupsView.indexOf('async function saveGroup()')
    const saveEnd = groupsView.indexOf('async function deleteGroup()')
    const saveSource = groupsView.slice(saveStart, saveEnd)

    expect(saveSource).not.toContain('resetFilters()')
    expect(saveSource).toContain('await loadData()')
    expect(saveSource).toContain('finishSaving({ close: true })')
  })
})
