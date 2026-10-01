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

describe('learning aggregate contracts', () => {
  it('loads the student subject context through one public snapshot', () => {
    const subjectsView =
      source(
        '../views/subjects/SubjectsView.vue'
      )

    expect(subjectsView)
      .toContain(
        'learningApi.getSnapshot()'
      )

    expect(subjectsView)
      .not.toContain(
        'teachingApi.getEnrollments'
      )

    expect(subjectsView)
      .not.toContain(
        'groupsApi.getById'
      )

    expect(subjectsView)
      .not.toContain(
        'facultiesApi.getById'
      )
  })

  it('loads teacher workload through one aggregate endpoint', () => {
    const workloadView =
      source(
        '../views/teacher/TeacherWorkloadView.vue'
      )

    expect(workloadView)
      .toContain(
        'teachingApi.getWorkload('
      )

    expect(workloadView)
      .not.toContain(
        'teachingApi.getAssignments('
      )

    expect(workloadView)
      .not.toContain(
        'teachingApi.getLectureAssignments('
      )

    expect(workloadView)
      .not.toContain(
        'groupsApi.getById'
      )

    expect(workloadView)
      .not.toContain(
        'lecturesApi.getAll'
      )
  })

  it('loads profile learning data without request fan-out', () => {
    const profileView =
      source(
        '../views/ProfileView.vue'
      )

    expect(profileView)
      .toContain(
        'learningApi.getSnapshot()'
      )

    expect(profileView)
      .toContain(
        'teachingApi.getProfileContext()'
      )

    expect(profileView)
      .not.toContain(
        'membershipsApi.getSubjectMembership'
      )

    expect(profileView)
      .not.toContain(
        'groupsApi.getById'
      )

    expect(profileView)
      .not.toContain(
        'subjectsApi.getById'
      )
  })
  it('uses the public subject endpoint for student subject details', () => {
    const subjectDetails =
      source(
        '../views/subjects/SubjectDetailsView.vue'
      )

    expect(subjectDetails)
      .toContain(
        'learningApi.getSubject('
      )
  })

  it('loads teacher subject cards from the teacher profile aggregate instead of per-subject requests', () => {
    const subjectsView =
      source(
        '../views/subjects/SubjectsView.vue'
      )

    expect(subjectsView)
      .toContain(
        'teachingApi.getProfileContext()'
      )

    expect(subjectsView)
      .not.toContain(
        'membershipsApi.getSubjectMemberships'
      )

    expect(subjectsView)
      .not.toContain(
        'subjectIds.map('
      )
  })

  it('teacher subject context uses one scoped profile aggregate and test editor does not read generic groups', () => {
    const composable = fs.readFileSync(
      path.resolve('src/composables/useTeacherSubjects.js'),
      'utf8'
    )
    const editor = fs.readFileSync(
      path.resolve('src/views/teacher/TestEditorView.vue'),
      'utf8'
    )

    expect(composable)
      .toContain('teachingApi')
    expect(composable)
      .toContain('.getProfileContext()')
    expect(composable)
      .not.toContain('membershipsApi')
    expect(composable)
      .not.toContain('subjectsApi.getById')
    expect(editor)
      .not.toContain('groupsApi.getById')
  })

})
