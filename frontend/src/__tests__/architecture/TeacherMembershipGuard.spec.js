import {
  readFileSync,
} from 'node:fs'
import { resolve } from 'node:path'


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
    resolve(process.cwd(), 'src', relativePath),
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
    const topicLibraryView = source('views/teacher/TopicLibraryView.vue')
    const topicMutations = source('composables/teacher/useTeacherTopicMutations.js')

    expect(topicLibraryView).toContain('useTeacherTopicMutations')
    expect(topicLibraryView).toContain('ensureSelectedMembershipActive,')
    expect(topicMutations).toContain(
      'await ensureSelectedMembershipActive()'
    )

    const lectureManagementView = source('views/teacher/LectureManagementView.vue')
    const lectureSaveFlow = source('composables/lectures/useLectureSaveFlow.js')
    const lectureMaterials = source('composables/lectures/useLectureMaterials.js')
    const lectureDelete = source('composables/lectures/useLectureDelete.js')

    expect(lectureManagementView).toContain(
      'ensureSelectedMembershipActive,'
    )
    expect(lectureManagementView).toContain(
      'useLectureSaveFlow'
    )
    expect(lectureManagementView).toContain(
      'useLectureMaterials'
    )
    expect(lectureManagementView).toContain(
      'useLectureDelete'
    )
    expect(lectureSaveFlow).toContain(
      'await ensureSelectedMembershipActive()'
    )
    expect(lectureMaterials).toContain(
      'await ensureSelectedMembershipActive()'
    )
    expect(lectureDelete).toContain(
      'await ensureSelectedMembershipActive()'
    )

    const courseTemplatesView = source('views/teacher/CourseTemplatesView.vue')
    const courseTemplateMutations = source('composables/course-templates/useCourseTemplateMutations.js')

    expect(courseTemplatesView).toContain(
      'ensureSelectedMembershipActive,'
    )
    expect(courseTemplatesView).toContain(
      'useCourseTemplateMutations'
    )
    expect(courseTemplateMutations).toContain(
      'await ensureSelectedMembershipActive()'
    )

    const questionsView = source('views/teacher/QuestionsView.vue')
    const questionMutations = source('composables/questions/useQuestionMutations.js')

    expect(questionsView).toContain(
      'ensureSelectedMembershipActive,'
    )
    expect(questionsView).toContain(
      'useQuestionMutations'
    )
    expect(questionMutations).toContain(
      'await ensureSelectedMembershipActive()'
    )

    const testEditor = source('views/teacher/TestEditorView.vue')
    const testEditorSaveFlow = source('composables/tests/useTestEditorSaveFlow.js')

    expect(testEditor).toContain(
      'ensureSelectedMembershipActive,'
    )
    expect(testEditor).toContain(
      'useTestEditorSaveFlow'
    )
    expect(testEditorSaveFlow).toContain(
      'await ensureSelectedMembershipActive()'
    )

    const workload = source('views/teacher/TeacherWorkloadView.vue')

    expect(workload)
      .not.toContain('createAssignment(')
    expect(workload)
      .not.toContain('updateAssignment(')
    expect(workload)
      .not.toContain('createLectureAssignment(')
    expect(workload)
      .not.toContain('updateLectureAssignmentStatus(')

    const adminTeachingAssignments = source('views/admin/TeachingAssignmentsView.vue')
    const adminTeachingAssignmentMutations = source('composables/admin/teaching-assignments/useAdminTeachingAssignmentMutations.js')

    expect(adminTeachingAssignments).toContain(
      'useAdminTeachingAssignmentMutations'
    )
    expect(adminTeachingAssignmentMutations).toContain(
      'revalidateAssignableTeacherMembershipIds'
    )
  })
})
