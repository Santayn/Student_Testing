import {
  describe,
  expect,
  it,
} from 'vitest'

import navigationSource from '@/navigation/navigation.config.js?raw'
import teachingApiSource from '@/api/teaching.api.js?raw'
import viewSource from '@/views/admin/TeachingTemplatesView.vue?raw'

describe('admin teaching workload workspace', () => {
  it('uses the workspace and drawer pattern instead of the legacy template table', () => {
    expect(viewSource).toContain('title="Учебная нагрузка"')
    expect(viewSource).toContain('UiFilterBar')
    expect(viewSource).toContain('UiDrawer')
    expect(viewSource).toContain('useOverlayForm')
    expect(viewSource).toContain('Новое назначение нагрузки')
    expect(viewSource).not.toContain('AdminTable')
    expect(viewSource).not.toContain('Шаблоны нагрузки')
  })

  it('exposes the real teaching-assignment fields supported by backend', () => {
    expect(viewSource).toContain('assignmentForm.hoursPerWeek')
    expect(viewSource).toContain('assignmentForm.loadTypeId')
    expect(viewSource).toContain('assignmentForm.studyCourse')
    expect(viewSource).toContain('assignmentForm.semester')
    expect(viewSource).toContain('assignmentForm.academicYear')
    expect(viewSource).toContain('assignmentForm.status')
    expect(viewSource).toContain('assignmentForm.courseVersionId')
    expect(viewSource).toContain('assignmentForm.notes')
    expect(viewSource).not.toContain('hoursPerWeek: 0')
  })

  it('does not create a default load type as a page-load side effect', () => {
    expect(viewSource).not.toContain('ensureDefaultLoadType')
    expect(viewSource).not.toContain('DEFAULT_LOAD_TYPE_NAME')
    expect(viewSource).not.toContain('Основная нагрузка')
    expect(viewSource).toContain('teachingApi.getLoadTypes()')
    expect(viewSource).toContain('openLoadTypeManager')
  })

  it('supports explicit load-type management and subject-membership binding', () => {
    expect(teachingApiSource).toContain('updateLoadType(loadTypeId, data)')
    expect(viewSource).toContain('teachingApi.createLoadType(')
    expect(viewSource).toContain('teachingApi.updateLoadType(')
    expect(viewSource).toContain('addLoadTypeToSubjectMembership(')
    expect(viewSource).toContain('Типы нагрузки')
  })

  it('keeps teacher selection searchable and validates current membership before mutation', () => {
    expect(viewSource).toContain('filter-placeholder="Поиск по ФИО или email"')
    expect(viewSource).toContain('currentAssignableMembershipIds(')
    expect(viewSource).toContain('revalidateAssignableTeacherMembershipIds')
    expect(viewSource).toContain('TeacherMembershipEligibilityError')
  })

  it('uses the new workload label in admin navigation', () => {
    expect(navigationSource).toContain("label: 'Учебная нагрузка'")
    expect(navigationSource).not.toContain("label: 'Шаблоны нагрузки'")
  })
})
