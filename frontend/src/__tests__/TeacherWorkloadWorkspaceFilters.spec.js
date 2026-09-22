// @vitest-environment node

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import {
  describe,
  expect,
  it,
} from 'vitest'

const workload = readFileSync(
  resolve(
    process.cwd(),
    'src',
    'views',
    'teacher',
    'TeacherWorkloadView.vue'
  ),
  'utf8'
)

describe('teacher workload read-only workspace', () => {
  it('adds client-side workspace filters without changing assignment loading', () => {
    expect(workload).toContain('UiFilterBar')
    expect(workload).toContain("const searchQuery = ref('')")
    expect(workload).toContain("const subjectFilter = ref('all')")
    expect(workload).toContain("const groupFilter = ref('all')")
    expect(workload).toContain("const loadTypeFilter = ref('all')")
    expect(workload).toContain("const statusFilter = ref('all')")
    expect(workload).toContain('const filteredAssignments = computed(')
    expect(workload).toContain('search-placeholder="Предмет, группа, тип нагрузки или примечание"')
    expect(workload).toContain('teachingApi.getAssignments')
  })

  it('groups only visible assignments while keeping membership context', () => {
    expect(workload).toContain('filteredAssignments.value.forEach(')
    expect(workload).toContain('assignment.subjectMembershipId')
    expect(workload).toContain(':key="group.subjectMembershipId"')
    expect(workload).toContain('subjectMembershipId:\n        group.subjectMembershipId')
  })

  it('keeps workload mutation unavailable in the teacher UI', () => {
    expect(workload).toContain('Страница работает только в режиме просмотра')
    expect(workload).not.toContain('createAssignment')
    expect(workload).not.toContain('updateAssignment')
    expect(workload).not.toContain('createLectureAssignment')
    expect(workload).not.toContain('updateLectureAssignmentStatus')
    expect(workload).not.toContain('UiDrawer')
    expect(workload).not.toContain('useOverlayForm')
  })

  it('provides a recoverable empty state for over-filtered data', () => {
    expect(workload).toContain('!filteredAssignments.length')
    expect(workload).toContain('По текущему поиску и фильтрам записи нагрузки не найдены.')
    expect(workload).toContain('@click="resetWorkspaceFilters"')
  })
})
