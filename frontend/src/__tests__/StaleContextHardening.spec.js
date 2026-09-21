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

describe('stale context hardening', () => {
  it.each([
    [
      'subject lectures',
      '../views/lectures/SubjectLecturesView.vue',
      'lecturesRequest',
      'requestedSubjectId',
    ],
    [
      'lecture details',
      '../views/lectures/LectureDetailsView.vue',
      'lectureRequest',
      'requestedLectureId',
    ],
    [
      'subject details',
      '../views/subjects/SubjectDetailsView.vue',
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
        .toContain('createLatestRequestGuard')

      expect(view)
        .toContain(`${guardName}.begin()`)

      expect(view)
        .toContain(`${guardName}.isCurrent(`)

      expect(view)
        .toContain(capturedContext)
    }
  )

  it('reloads subject details when the active workspace mode changes', () => {
    const view = source(
      '../views/subjects/SubjectDetailsView.vue'
    )

    expect(view)
      .toContain('requestedStudentMode')

    expect(view)
      .toContain('() => authStore.isStudentMode')
  })

  it('guards faculty subject loading and captures mutation faculty context', () => {
    const view = source(
      '../views/admin/FacultySubjectsView.vue'
    )

    expect(view)
      .toContain('assignedSubjectsRequest.begin()')

    expect(view)
      .toContain('assignedSubjectsRequest.isCurrent(')

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
    const view = source(
      '../views/teacher/TeacherWorkloadView.vue'
    )

    expect(view)
      .toContain('assignmentsRequest.begin()')

    expect(view)
      .toContain('assignmentsRequest.isCurrent(')

    expect(view)
      .toContain('const periodContext = {')

    expect(view)
      .toContain('const membershipSnapshot =')

    expect(view)
      .toContain('const rawAssignments = responses')

    expect(view)
      .toContain('groupsApi.getById(groupId)')
  })

  it('keeps teacher workload free from mutation requests', () => {
    const view = source(
      '../views/teacher/TeacherWorkloadView.vue'
    )

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
