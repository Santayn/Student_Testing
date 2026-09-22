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
  '../views/admin/FacultySubjectsView.vue'
)

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

  it('keeps stale faculty context guards around loading and mutations', () => {
    expect(view).toContain('assignedSubjectsRequest.begin()')
    expect(view).toContain('assignedSubjectsRequest.isCurrent(')
    expect(view.match(/const targetFacultyId/g)).toHaveLength(2)
    expect(view).toContain(':disabled="loading || saving"')
  })

  it('provides search, sorting and mobile stacking', () => {
    expect(view).toContain('searchQuery')
    expect(view).toContain('sortMode')
    expect(view).toContain('filterResultText')
    expect(view).toContain('@media (max-width: 960px)')
    expect(view).toContain('@media (max-width: 640px)')
  })
})
