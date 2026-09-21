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

describe('teacher workload membership context', () => {
  it('keeps duplicate subject memberships separate in the read-only workload view', () => {
    const workload = source(
      '../views/teacher/TeacherWorkloadView.vue'
    )

    expect(workload)
      .toContain('const membershipSnapshot =')

    expect(workload)
      .toContain('subjectMembershipId:\n              membership.id')

    expect(workload)
      .toContain('const groupedAssignments = computed(')

    expect(workload)
      .toContain('assignment.subjectMembershipId')

    expect(workload)
      .toContain(':key="group.subjectMembershipId"')

    expect(workload)
      .not.toContain('membershipBySubjectId')
  })

  it('does not expose workload mutation controls to teachers', () => {
    const workload = source(
      '../views/teacher/TeacherWorkloadView.vue'
    )

    expect(workload)
      .toContain('teachingApi.getAssignments')

    expect(workload)
      .toContain('Изменения нагрузки выполняет администратор системы')

    expect(workload)
      .not.toContain('createLectureAssignment')

    expect(workload)
      .not.toContain('updateLectureAssignmentStatus')

    expect(workload)
      .not.toContain('assignLectures')

    expect(workload)
      .not.toContain('UiCheckbox')
  })
})
