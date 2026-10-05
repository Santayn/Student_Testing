// @vitest-environment node

import {
  readFileSync,
} from 'node:fs'

import {
  resolve,
} from 'node:path'

import {
  describe,
  expect,
  it,
} from 'vitest'

function source(relativePath) {
  return readFileSync(
    resolve(
      process.cwd(),
      'src',
      relativePath
    ),
    'utf8'
  )
}

describe('request lifecycle contract', () => {
  it('keeps high-churn question reads abortable', () => {
    const list = source(
      'composables/questions/useQuestionsList.js'
    )
    const topics = source(
      'composables/questions/useQuestionTopicContext.js'
    )
    const options = source(
      'composables/questions/useQuestionOptions.js'
    )

    for (const file of [
      list,
      topics,
      options,
    ]) {
      expect(file).toContain(
        'createAbortableRequestGuard'
      )
      expect(file).toContain(
        '{ signal }'
      )
      expect(file).toContain(
        '.invalidate()'
      )
    }
  })

  it('keeps TestEditor context transport abortable while shared group references remain single-flight', () => {
    const editor = source(
      'views/teacher/TestEditorView.vue'
    )

    expect(editor).toContain(
      'createAbortableRequestGuard'
    )
    expect(editor).toContain(
      'topicsApi.getAll('
    )
    expect(editor).toContain(
      'teachingApi.getAssignments('
    )
    expect(editor).toContain(
      '{ signal }'
    )
    expect(editor).toContain(
      'getSharedLearningContextCache'
    )
    expect(editor).toContain(
      '`group:${groupId}`'
    )

    /*
     * Shared cache loaders are deliberately not bound to a single view's
     * AbortSignal: aborting one consumer must not cancel another consumer's
     * single-flight reference request.
     */
    expect(editor).not.toContain(
      'groupsApi.getById(groupId, { signal })'
    )
  })

  it('aborts superseded route-owned detail reads', () => {
    const subject = source(
      'views/subjects/SubjectDetailsView.vue'
    )
    const lecture = source(
      'views/lectures/LectureDetailsView.vue'
    )
    const subjectLectures = source(
      'views/lectures/SubjectLecturesView.vue'
    )

    for (const view of [
      subject,
      lecture,
      subjectLectures,
    ]) {
      expect(view).toContain(
        'createAbortableRequestGuard'
      )
      expect(view).toContain(
        '{ signal }'
      )
      expect(view).toContain(
        '.invalidate()'
      )
    }
  })

  it('forwards request config through public learning detail reads', () => {
    const learning = source(
      'api/learning.api.js'
    )

    expect(learning).toContain(
      'getSubject(subjectId, config = {})'
    )
    expect(learning).toContain(
      'getSubjectLectures(subjectId, config = {})'
    )
    expect(learning).toContain(
      'getLecture(lectureId, config = {})'
    )
    expect(learning).toContain(
      'getLectureMaterials(lectureId, config = {})'
    )
    expect(learning).toContain(
      'getLectureTests(lectureId, config = {})'
    )
  })

  it('reuses security-scoped references in workload and student context', () => {
    const workload = source(
      'composables/teacher/useTeacherWorkloadData.js'
    )
    const student = source(
      'utils/studentLearningContext.js'
    )

    expect(workload).toContain(
      'getSharedLearningContextCache'
    )
    expect(workload).toContain(
      '`group:${groupId}`'
    )
    expect(workload).not.toContain(
      'groupsApi.getById(groupId, { signal })'
    )

    expect(student).toContain(
      '`subject-membership:${membershipId}`'
    )
    expect(student).toContain(
      'REFERENCE_TTL_MS'
    )
  })

  it('does not repeat /auth/me when Profile mounts normally', () => {
    const profileContext = source(
      'composables/profile/useProfileContext.js'
    )

    expect(profileContext).toContain(
      'authStore.refreshIdentity()'
    )
    expect(profileContext).not.toContain(
      'authStore.loadCurrentUser()'
    )
  })

  it('keeps read API methods ready to forward request config', () => {
    const subjects = source(
      'api/subjects.api.js'
    )
    const faculties = source(
      'api/faculties.api.js'
    )
    const memberships = source(
      'api/memberships.api.js'
    )
    const teaching = source(
      'api/teaching.api.js'
    )
    const questions = source(
      'api/questions.api.js'
    )

    expect(subjects).toContain(
      'getById(subjectId, config = {})'
    )
    expect(faculties).toContain(
      'getById(facultyId, config = {})'
    )
    expect(memberships).toContain(
      'getSubjectMembership(membershipId, config = {})'
    )
    expect(teaching).toContain(
      'getEnrollments(params = {}, config = {})'
    )
    expect(questions).toContain(
      'getAll(params = {}, config = {})'
    )
  })
})
