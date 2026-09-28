import {
  describe,
  expect,
  it,
} from 'vitest'

import navigationSource from '@/navigation/navigation.config.js?raw'
import teachingApiSource from '@/api/teaching.api.js?raw'
import viewSource from '@/views/admin/TeachingAssignmentsView.vue?raw'
import dataSource from '@/composables/useAdminTeachingAssignmentsData.js?raw'
import editorSource from '@/composables/useAdminTeachingAssignmentEditor.js?raw'
import drawerSource from '@/components/admin/AdminTeachingAssignmentDrawer.vue?raw'
import loadTypeManagerSource from '@/components/admin/AdminLoadTypeManager.vue?raw'
import loadTypeStateSource from '@/composables/useAdminLoadTypeManager.js?raw'

describe('admin teaching workload workspace', () => {
  it('uses the workspace and dedicated assignment drawer pattern instead of the legacy template table', () => {
    expect(viewSource).toContain('title="Учебная нагрузка"')
    expect(viewSource).toContain('UiFilterBar')
    expect(viewSource).toContain('AdminTeachingAssignmentDrawer')
    expect(viewSource).toContain('useAdminTeachingAssignmentEditor')
    expect(editorSource).toContain('useOverlayForm')
    expect(drawerSource).toContain('UiDrawer')
    expect(editorSource).toContain('Новое назначение нагрузки')
    expect(viewSource).not.toContain('AdminTable')
    expect(viewSource).not.toContain('Шаблоны нагрузки')
  })

  it('exposes the real teaching-assignment fields supported by backend', () => {
    expect(drawerSource).toContain('form.hoursPerWeek')
    expect(drawerSource).toContain('form.loadTypeId')
    expect(drawerSource).toContain('form.studyCourse')
    expect(drawerSource).toContain('form.semester')
    expect(drawerSource).toContain('form.academicYear')
    expect(drawerSource).toContain('form.status')
    expect(drawerSource).toContain('form.courseVersionId')
    expect(drawerSource).toContain('form.notes')
    expect(editorSource).not.toContain('hoursPerWeek: 0')
  })

  it('does not create a default load type as a page-load side effect', () => {
    expect(viewSource).not.toContain('ensureDefaultLoadType')
    expect(viewSource).not.toContain('DEFAULT_LOAD_TYPE_NAME')
    expect(viewSource).not.toContain('Основная нагрузка')
    expect(viewSource).toContain('useAdminTeachingAssignmentsData')
    expect(dataSource).toContain('teachingApi.getLoadTypes()')
    expect(viewSource).toContain('openLoadTypeManager')
    expect(viewSource).toContain('useAdminLoadTypeManager')
  })

  it('supports explicit load-type management and subject-membership binding', () => {
    expect(teachingApiSource).toContain('updateLoadType(loadTypeId, data)')
    expect(loadTypeStateSource).toContain('teachingApi.createLoadType(')
    expect(loadTypeStateSource).toContain('teachingApi.updateLoadType(')
    expect(loadTypeStateSource).toContain('useOverlayForm')
    expect(loadTypeManagerSource).toContain('UiDialog')
    expect(loadTypeManagerSource).toContain('Тип нагрузки нельзя удалить через текущий backend API')
    expect(viewSource).toContain('addLoadTypeToSubjectMembership(')
    expect(viewSource).toContain('AdminLoadTypeManager')
  })

  it('keeps teacher selection searchable and validates current membership before mutation', () => {
    expect(drawerSource).toContain('filter-placeholder="Поиск по ФИО или email"')
    expect(viewSource).toContain('currentAssignableMembershipIds(')
    expect(viewSource).toContain('revalidateAssignableTeacherMembershipIds')
    expect(viewSource).toContain('TeacherMembershipEligibilityError')
  })

  it('uses the new workload label in admin navigation', () => {
    expect(navigationSource).toContain("label: 'Учебная нагрузка'")
    expect(navigationSource).not.toContain("label: 'Шаблоны нагрузки'")
  })
})
