import {
  readFileSync,
} from 'node:fs'
import { resolve } from 'node:path'


import {
  describe,
  expect,
  it,
} from 'vitest'

function source(relativePath) {
  return readFileSync(
    resolve(process.cwd(), 'src', relativePath),
    'utf8'
  )
}

describe('teacher workload membership context', () => {
  it('keeps duplicate subject memberships separate in the read-only workload view', () => {
    const workload = source('views/teacher/TeacherWorkloadView.vue')
    const loader = source('composables/teacher/useTeacherWorkloadData.js')
    const presentation = source('composables/teacher/useTeacherWorkloadPresentation.js')

    expect(loader).toContain('const membershipSnapshot =')
    expect(loader).toMatch(/subjectMembershipId:\s*membership\.id/)
    expect(presentation).toContain('const groupedAssignments = computed(')
    expect(presentation).toContain('assignment.subjectMembershipId')

    expect(workload)
      .toContain(':key="group.subjectMembershipId"')

    expect(presentation).not.toContain('membershipBySubjectId')
  })

  it('does not expose workload mutation controls to teachers', () => {
    const workload = source('views/teacher/TeacherWorkloadView.vue')
    const loader = source('composables/teacher/useTeacherWorkloadData.js')

    expect(loader).toContain('teachingApi.getAssignments')

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
    expect(loader).not.toMatch(/teachingApi\.(?:create|update|delete|remove)/)
  })
})
