import {
  describe,
  expect,
  it,
} from 'vitest'

import componentSource from '@/views/admin/TeachingTemplatesView.vue?raw'

import {
  assignableTeacherMembershipIds,
  isAssignableTeacherMembership,
} from '@/utils/teacherMembershipEligibility'

describe('admin workload membership eligibility', () => {
  it('allows only active non-removed teacher memberships for new workload', () => {
    expect(
      isAssignableTeacherMembership({
        id: 10,
        role: 1,
        status: 1,
        removedAtUtc: null,
      })
    ).toBe(true)

    expect(
      isAssignableTeacherMembership({
        id: 11,
        role: 1,
        status: 2,
        removedAtUtc: null,
      })
    ).toBe(false)

    expect(
      isAssignableTeacherMembership({
        id: 12,
        role: 1,
        status: 1,
        removedAtUtc: '2026-09-20T10:00:00Z',
      })
    ).toBe(false)

    expect(
      isAssignableTeacherMembership({
        id: 13,
        role: 2,
        status: 1,
        removedAtUtc: null,
      })
    ).toBe(false)
  })

  it('builds the assignable id set only from active teacher memberships', () => {
    const ids = assignableTeacherMembershipIds([
      { id: 1, role: 1, status: 1, removedAtUtc: null },
      { id: 2, role: 1, status: 2, removedAtUtc: null },
      { id: 3, role: 1, status: 1, removedAtUtc: '2026-09-20T10:00:00Z' },
      { id: 4, role: 2, status: 1, removedAtUtc: null },
    ])

    expect([...ids]).toEqual([1])
  })

  it('rechecks the selected membership immediately before creating active workload', () => {
    expect(componentSource).toContain('currentAssignableMembershipIds(')
    expect(componentSource).toContain('revalidateAssignableTeacherMembershipIds')
    expect(componentSource).toContain('assignmentForm.subjectMembershipId')
    expect(componentSource).toContain('Выбранное назначение преподавателя больше не активно')
  })
})
