// @vitest-environment node

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import {
  describe,
  expect,
  it,
} from 'vitest'

const source = (relativePath) => readFileSync(resolve(process.cwd(), 'src', relativePath), 'utf8')
const workload = source('views/teacher/TeacherWorkloadView.vue')
const presentation = source('composables/useTeacherWorkloadPresentation.js')
const loader = source('composables/useTeacherWorkloadData.js')

describe('teacher workload read-only workspace', () => {
  it('connects the read-only loader and presentation state without moving UI into composables', () => {
    expect(workload).toContain('useTeacherWorkloadData({')
    expect(workload).toContain('useTeacherWorkloadPresentation({')
    expect(workload).toContain('onBeforeUnmount(workload.dispose)')
    expect(workload).not.toContain('teachingApi.getAssignments')
    expect(loader).not.toMatch(/teachingApi\.(?:create|update|delete|remove)/)
  })

  it('adds client-side workspace filters without changing assignment loading', () => {
    expect(workload).toContain('UiFilterBar')
    expect(presentation).toContain("const searchQuery = ref('')")
    expect(presentation).toContain("const subjectFilter = ref('all')")
    expect(presentation).toContain("const groupFilter = ref('all')")
    expect(presentation).toContain("const loadTypeFilter = ref('all')")
    expect(presentation).toContain("const statusFilter = ref('all')")
    expect(presentation).toContain('const filteredAssignments = computed(')
    expect(workload).toContain('search-placeholder="Предмет, группа, тип нагрузки или примечание"')
    expect(loader).toContain('teachingApi.getAssignments')
  })

  it('groups only visible assignments while keeping membership context', () => {
    expect(presentation).toContain('filteredAssignments.value.forEach(')
    expect(presentation).toContain('assignment.subjectMembershipId')
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
