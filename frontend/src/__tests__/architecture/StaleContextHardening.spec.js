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

describe('stale context hardening', () => {
  it.each([
    [
      'subject lectures',
      'views/lectures/SubjectLecturesView.vue',
      'lecturesRequest',
      'requestedSubjectId',
    ],
    [
      'lecture details',
      'views/lectures/LectureDetailsView.vue',
      'lectureRequest',
      'requestedLectureId',
    ],
    [
      'subject details',
      'views/subjects/SubjectDetailsView.vue',
      'subjectRequest',
      'requestedSubjectId',
    ],
  ])(
    'guards %s loader commits with the latest request token',
    (
      _name,
      relativePath,
      guardName,
      capturedContext
    ) => {
      const view = source(relativePath)

      expect(view)
        .toContain('createAbortableRequestGuard')

      expect(view)
        .toContain(`${guardName}.begin()`)

      expect(view)
        .toContain(`${guardName}.isCurrent(`)

      expect(view)
        .toContain('{ signal }')

      expect(view)
        .toContain(`${guardName}.invalidate()`)

      expect(view)
        .toContain(capturedContext)
    }
  )

  it('reloads subject details when the active workspace mode changes', () => {
    const view = source('views/subjects/SubjectDetailsView.vue')

    expect(view)
      .toContain('requestedStudentMode')

    expect(view)
      .toContain('() => authStore.isStudentMode')
  })

  it('guards faculty subject loading and captures mutation faculty context', () => {
    const view = source('views/admin/FacultySubjectsView.vue')
    const dataSource = source('composables/admin/faculty-subjects/useAdminFacultySubjectsData.js')

    expect(dataSource)
      .toContain('assignedSubjectsRequest.begin()')

    expect(dataSource)
      .toContain('assignedSubjectsRequest.isCurrent(')

    expect(dataSource)
      .toContain('requestedFacultyId')

    expect(view.match(/const targetFacultyId/g))
      .toHaveLength(2)

    expect(view)
      .toContain('facultiesApi.addSubject(\n            targetFacultyId,')

    expect(view)
      .toContain('facultiesApi.removeSubject(\n            targetFacultyId,')

    expect(view)
      .toContain(':disabled="loading || saving"')
  })

  it('builds read-only teacher workload off a captured period and commits it atomically', () => {
    const loader = source('composables/teacher/useTeacherWorkloadData.js')

    expect(loader)
      .toContain('createAbortableRequestGuard')

    expect(loader)
      .toContain('assignmentsRequest.begin()')

    expect(loader)
      .toContain('assignmentsRequest.isCurrent(')

    expect(loader)
      .toContain('const periodContext = {')

    expect(loader)
      .toContain('const membershipSnapshot =')

    expect(loader)
      .toContain('const rawAssignments = responses')

    expect(loader)
      .toContain('getSharedLearningContextCache')

    expect(loader)
      .toContain('`group:${groupId}`')

    expect(loader)
      .toContain('groupsApi.getById(\n                    groupId\n                  )')

    expect(loader)
      .not.toContain('groupsApi.getById(groupId, { signal })')
  })

  it('keeps teacher workload free from mutation requests', () => {
    const view = source('views/teacher/TeacherWorkloadView.vue')

    expect(view)
      .not.toContain('createAssignment(')

    expect(view)
      .not.toContain('updateAssignment(')

    expect(view)
      .not.toContain('createLectureAssignment(')

    expect(view)
      .not.toContain('updateLectureAssignmentStatus(')
  })
})
