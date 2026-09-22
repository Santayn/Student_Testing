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
  '../views/admin/TeacherSubjectsView.vue'
)

describe('admin teacher subjects workspace', () => {
  it('uses relation workspace controls instead of checkbox batches', () => {
    expect(view).toContain('UiFilterBar')
    expect(view).toContain('Назначенные предметы')
    expect(view).toContain('Доступные предметы')
    expect(view).toContain('searchQuery')
    expect(view).toContain('sortMode')

    expect(view).not.toContain('UiCheckbox')
    expect(view).not.toContain('runBatchOperation')
    expect(view).not.toContain('availableSelection')
    expect(view).not.toContain('assignedSelection')
    expect(view).not.toContain('Назначить выбранные')
    expect(view).not.toContain('Снять выбранные')
  })

  it('keeps assignments atomic and preserves paused membership reactivation', () => {
    expect(view).toContain('assignSubjectToTeacher({')
    expect(view).toContain('@click="addSubject(subject)"')
    expect(view).toContain('subject.pausedMembership')
    expect(view).toContain("'Восстановить'")
    expect(view).toContain("result.action === 'reactivated'")
    expect(view).toContain('updateSubjectMembershipStatus(')
  })

  it('requires confirmation before removing a teacher subject relation', () => {
    expect(view).toContain('UiDialog')
    expect(view).toContain('Снять предмет с преподавателя?')
    expect(view).toContain('@click="requestRemoveSubject(subject)"')
    expect(view).toContain('status: REMOVED_STATUS')
    expect(view).toContain('история сохранится')
  })

  it('provides searchable teacher selection for large teacher lists', () => {
    expect(view).toContain(':filter="true"')
    expect(view).toContain('filter-placeholder="Поиск по ФИО или email"')
    expect(view).toContain('teacher?.email')
    expect(view).toContain('`${personName(teacher)} · ${email}`')
  })

  it('protects teacher context around mutations and remains responsive', () => {
    expect(view.match(/const targetTeacherId/g)).toHaveLength(2)
    expect(view).toContain('Number(teacherId.value) !==')
    expect(view).toContain(':disabled="loading || saving"')
    expect(view).toContain('@media (max-width: 960px)')
    expect(view).toContain('@media (max-width: 640px)')
  })
})
