import { ref } from 'vue'
import { describe, expect, it } from 'vitest'
import { useTeacherWorkloadPresentation } from '@/composables/useTeacherWorkloadPresentation'

function setup() {
  const assignments = ref([
    { id: 1, subjectMembershipId: 11, groupId: 20, groupName: 'КБ-23', loadTypeId: 3, status: 1, hoursPerWeek: 2.5, notes: 'Основной поток' },
    { id: 2, subjectMembershipId: 12, groupId: 21, groupName: 'КБ-24', loadTypeId: 4, status: 2, hoursPerWeek: 1, notes: 'Дополнительный поток' },
  ])
  const state = useTeacherWorkloadPresentation({
    subjectMemberships: ref([
      { id: 11, subjectId: 7 },
      { id: 12, subjectId: 7 },
    ]),
    subjects: ref([{ id: 7, name: 'Алгоритмы' }]),
    assignments,
    loadTypes: ref([{ id: 3, name: 'Лекции' }, { id: 4, name: 'Практики' }]),
    studyCourse: ref(1),
    semester: ref(1),
    academicYear: ref(2026),
  })
  return { ...state, assignments }
}

describe('useTeacherWorkloadPresentation', () => {
  it('keeps different membership ids separate even for the same subject', () => {
    const workload = setup()
    expect(workload.groupedAssignments.value).toHaveLength(2)
    expect(workload.groupedAssignments.value.map((group) => group.subjectMembershipId)).toEqual([11, 12])
    expect(workload.subjectFilterOptions.value).toEqual([
      { value: 'all', label: 'Все предметы' },
      { value: '7', label: 'Алгоритмы' },
    ])
    expect(workload.subjectCount.value).toBe(1)
    expect(workload.groupCount.value).toBe(2)
    expect(workload.totalHoursPerWeek.value).toBe(3.5)
  })

  it('filters by subject, group, load type, status and text without changing the source', () => {
    const workload = setup()
    workload.subjectFilter.value = '7'
    workload.groupFilter.value = '21'
    workload.loadTypeFilter.value = '4'
    workload.statusFilter.value = '2'
    workload.searchQuery.value = 'ДОПОЛНИТЕЛЬНЫЙ'

    expect(workload.filteredAssignments.value.map((item) => item.id)).toEqual([2])
    expect(workload.groupedAssignments.value.map((group) => group.subjectMembershipId)).toEqual([12])
    expect(workload.workloadResultText.value).toBe('Показано: 1 из 2')
    expect(workload.hasActiveWorkspaceFilters.value).toBe(true)
    expect(workload.assignments.value).toHaveLength(2)

    workload.resetWorkspaceFilters()
    expect(workload.filteredAssignments.value).toHaveLength(2)
    expect(workload.hasActiveWorkspaceFilters.value).toBe(false)
    expect(workload.workloadResultText.value).toBe('Записей нагрузки: 2')
  })

  it('formats display labels and gives non-numeric hours a safe fallback', () => {
    const workload = setup()
    expect(workload.periodLabel.value).toContain('2026')
    expect(workload.groupFilterOptions.value.map((item) => item.label)).toContain('КБ-23')
    expect(workload.statusLabel(1)).toBe('Активно')
    expect(workload.statusClass(4)).toContain('teacher-status--danger')
    expect(workload.formatHours(Number.NaN)).toBe('0')
  })
})
