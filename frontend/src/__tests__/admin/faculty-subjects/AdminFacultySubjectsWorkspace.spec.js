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

const view = source('views/admin/FacultySubjectsView.vue')

const dataSource = source('composables/admin/faculty-subjects/useAdminFacultySubjectsData.js')

describe('admin faculty subjects workspace', () => {
  it('uses the shared workspace controls instead of checkbox batches', () => {
    expect(view).toContain('UiFilterBar')
    expect(view).toContain('UiSelect')
    expect(view).toContain('Назначенные предметы')
    expect(view).toContain('Доступные предметы')

    expect(view).not.toContain('UiCheckbox')
    expect(view).not.toContain('runBatchOperation')
    expect(view).not.toContain('addSelection')
    expect(view).not.toContain('removeSelection')
  })

  it('keeps faculty subject mutations explicit and atomic', () => {
    expect(view).toContain('facultiesApi.addSubject(')
    expect(view).toContain('facultiesApi.removeSubject(')
    expect(view).toContain('@click="addSubject(subject)"')
    expect(view).toContain('@click="requestRemoveSubject(subject)"')
    expect(view).toContain('UiDialog')
    expect(view).toContain('Убрать предмет из факультета?')
  })

  it('keeps stale faculty context guards in the read layer and mutation context guards in the view', () => {
    expect(view).toContain('useAdminFacultySubjectsData')
    expect(dataSource).toContain('assignedSubjectsRequest.begin()')
    expect(dataSource).toContain('assignedSubjectsRequest.isCurrent(')
    expect(dataSource).toContain('baseRequest.begin()')
    expect(view.match(/const targetFacultyId/g)).toHaveLength(2)
    expect(view).toContain(':disabled="loading || saving"')
  })

  it('provides search and sorting through the data layer and keeps mobile stacking in the view', () => {
    expect(dataSource).toContain('searchQuery')
    expect(dataSource).toContain('sortMode')
    expect(dataSource).toContain('filterResultText')
    expect(view).toContain('@media (max-width: 960px)')
    expect(view).toContain('@media (max-width: 640px)')
  })
})
