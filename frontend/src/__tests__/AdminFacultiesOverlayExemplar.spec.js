// @vitest-environment node

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import {
  describe,
  expect,
  it,
} from 'vitest'

const facultiesView = readFileSync(
  resolve(
    process.cwd(),
    'src',
    'views',
    'admin',
    'FacultiesView.vue'
  ),
  'utf8'
)

describe('admin faculties overlay exemplar', () => {
  it('uses a searchable workspace instead of an inline editor and legacy table', () => {
    expect(facultiesView).toContain('UiFilterBar')
    expect(facultiesView).toContain('filteredFaculties')
    expect(facultiesView).toContain('descriptionFilter')
    expect(facultiesView).toContain('sortMode')
    expect(facultiesView).toContain('resetFilters')
    expect(facultiesView).not.toContain('AdminTable')
  })

  it('creates and edits faculties through the shared dialog lifecycle', () => {
    expect(facultiesView).toContain('useOverlayForm')
    expect(facultiesView).toContain('facultyDialogModel')
    expect(facultiesView).toContain('openCreateFaculty')
    expect(facultiesView).toContain('openEditFaculty')
    expect(facultiesView).toContain('UiUnsavedChangesConfirm')
    expect(facultiesView).toContain('requestClose')
  })

  it('uses an application confirmation dialog for deletion', () => {
    expect(facultiesView).toContain('deleteConfirmVisible')
    expect(facultiesView).toContain('title="Удалить факультет?"')
    expect(facultiesView).toContain('requestDeleteFaculty')
    expect(facultiesView).not.toContain('window.confirm')
  })

  it('preserves the existing faculty API contract', () => {
    expect(facultiesView).toContain('facultiesApi.getAll()')
    expect(facultiesView).toContain('facultiesApi.create(payload)')
    expect(facultiesView).toContain('facultiesApi.update(')
    expect(facultiesView).toContain('facultiesApi.remove(faculty.id)')
  })

  it('matches backend field limits and protects the unique faculty code before submit', () => {
    expect(facultiesView).toContain('name.length > 200')
    expect(facultiesView).toContain('code.length > 50')
    expect(facultiesView).toContain('description.length > 1000')
    expect(facultiesView).toContain('Факультет с кодом')
  })

  it('keeps workspace filters outside the CRUD lifecycle', () => {
    const saveStart = facultiesView.indexOf('async function saveFaculty()')
    const saveEnd = facultiesView.indexOf('async function deleteFaculty()')
    const saveSource = facultiesView.slice(saveStart, saveEnd)

    expect(saveSource).not.toContain('resetFilters()')
    expect(saveSource).toContain('await loadFaculties()')
    expect(saveSource).toContain('finishSaving({ close: true })')
  })
})
