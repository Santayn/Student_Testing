// @vitest-environment node

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import {
  describe,
  expect,
  it,
} from 'vitest'

const subjectsView = readFileSync(
  resolve(
    process.cwd(),
    'src',
    'views',
    'admin',
    'SubjectsAdminView.vue'
  ),
  'utf8'
)

describe('admin subjects overlay workspace', () => {
  it('uses a searchable workspace instead of an inline editor and legacy table', () => {
    expect(subjectsView).toContain('UiFilterBar')
    expect(subjectsView).toContain('filteredSubjects')
    expect(subjectsView).toContain('descriptionFilter')
    expect(subjectsView).toContain('sortMode')
    expect(subjectsView).toContain('resetFilters')
    expect(subjectsView).not.toContain('AdminTable')
  })

  it('creates and edits subjects through the shared dialog lifecycle', () => {
    expect(subjectsView).toContain('useOverlayForm')
    expect(subjectsView).toContain('subjectDialogModel')
    expect(subjectsView).toContain('openCreateSubject')
    expect(subjectsView).toContain('openEditSubject')
    expect(subjectsView).toContain('UiUnsavedChangesConfirm')
    expect(subjectsView).toContain('requestClose')
  })

  it('uses an application confirmation dialog for deletion', () => {
    expect(subjectsView).toContain('deleteConfirmVisible')
    expect(subjectsView).toContain('title="Удалить предмет?"')
    expect(subjectsView).toContain('requestDeleteSubject')
    expect(subjectsView).not.toContain('window.confirm')
  })

  it('preserves the existing subject API contract', () => {
    expect(subjectsView).toContain('subjectsApi.getAll()')
    expect(subjectsView).toContain('subjectsApi.create(payload)')
    expect(subjectsView).toContain('subjectsApi.update(')
    expect(subjectsView).toContain('subjectsApi.remove(subject.id)')
  })

  it('matches backend field limits and protects the unique subject name before submit', () => {
    expect(subjectsView).toContain('name.length > 200')
    expect(subjectsView).toContain('description.length > 1000')
    expect(subjectsView).toContain('Предмет «${name}» уже существует.')
  })

  it('keeps workspace filters outside the CRUD lifecycle', () => {
    const saveStart = subjectsView.indexOf('async function saveSubject()')
    const saveEnd = subjectsView.indexOf('async function deleteSubject()')
    const saveSource = subjectsView.slice(saveStart, saveEnd)

    expect(saveSource).not.toContain('resetFilters()')
    expect(saveSource).toContain('await loadSubjects()')
    expect(saveSource).toContain('finishSaving({ close: true })')
  })
})
