import {
  readFileSync,
} from 'node:fs'

import {
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import {
  assertAssignableTeacherMembership,
  revalidateAssignableTeacherMembership,
  revalidateAssignableTeacherMembershipIds,
  TeacherMembershipEligibilityError,
} from '@/utils/teacherMembershipEligibility'

function membership(overrides = {}) {
  return {
    id: 10,
    personId: 20,
    subjectId: 30,
    role: 1,
    status: 1,
    removedAtUtc: null,
    ...overrides,
  }
}

function source(relativePath) {
  return readFileSync(
    new URL(relativePath, import.meta.url),
    'utf8'
  )
}

describe('teacher membership mutation guard', () => {
  it('accepts only the expected active teacher membership context', () => {
    const active = membership()

    expect(
      assertAssignableTeacherMembership(
        active,
        {
          expectedMembershipId: 10,
          expectedPersonId: 20,
          expectedSubjectId: 30,
        }
      )
    ).toBe(active)

    for (const stale of [
      membership({ status: 2 }),
      membership({ removedAtUtc: '2026-09-20T10:00:00Z' }),
      membership({ role: 2 }),
      membership({ subjectId: 31 }),
      membership({ personId: 21 }),
    ]) {
      expect(() =>
        assertAssignableTeacherMembership(
          stale,
          {
            expectedMembershipId: 10,
            expectedPersonId: 20,
            expectedSubjectId: 30,
          }
        )
      ).toThrow(TeacherMembershipEligibilityError)
    }
  })

  it('revalidates a membership from the API immediately before mutation', async () => {
    const api = {
      getSubjectMembership: vi.fn()
        .mockResolvedValue({
          data: membership(),
        }),
    }

    const result =
      await revalidateAssignableTeacherMembership({
        api,
        membershipId: 10,
        expectedSubjectId: 30,
        expectedPersonId: 20,
      })

    expect(api.getSubjectMembership)
      .toHaveBeenCalledWith(10)
    expect(result.id).toBe(10)
  })

  it('deduplicates memberships when revalidating a batch', async () => {
    const api = {
      getSubjectMembership: vi.fn()
        .mockImplementation(
          async (membershipId) => ({
            data: membership({
              id: membershipId,
            }),
          })
        ),
    }

    const ids =
      await revalidateAssignableTeacherMembershipIds({
        api,
        membershipIds: [10, 10, 11],
      })

    expect([...ids]).toEqual([10, 11])
    expect(api.getSubjectMembership)
      .toHaveBeenCalledTimes(2)
  })

  it('guards teacher mutation views instead of trusting load-time state', () => {
    const guardedViews = [
      '../views/teacher/LectureManagementView.vue',
      '../views/teacher/TopicLibraryView.vue',
      '../views/teacher/QuestionsView.vue',
      '../views/teacher/TestEditorView.vue',
      '../views/teacher/CourseTemplatesView.vue',
    ]

    for (const view of guardedViews) {
      expect(source(view))
        .toContain('ensureSelectedMembershipActive()')
    }

    const workload = source(
      '../views/teacher/TeacherWorkloadView.vue'
    )

    expect(workload)
      .not.toContain('createAssignment(')
    expect(workload)
      .not.toContain('updateAssignment(')
    expect(workload)
      .not.toContain('createLectureAssignment(')
    expect(workload)
      .not.toContain('updateLectureAssignmentStatus(')

    expect(
      source('../views/admin/TeachingTemplatesView.vue')
    ).toContain(
      'revalidateAssignableTeacherMembershipIds'
    )
  })
})
