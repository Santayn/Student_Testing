import http from './http'

export const learningApi = {
  getSubject(subjectId) {
    return http.get(
      `/public/learning/subjects/${subjectId}`
    )
  },

  getSubjectLectures(subjectId) {
    return http.get(
      `/public/learning/subjects/${subjectId}/lectures`
    )
  },

  getLecture(lectureId) {
    return http.get(
      `/public/learning/lectures/${lectureId}`
    )
  },

  getLectureMaterials(lectureId) {
    return http.get(
      `/public/learning/lectures/${lectureId}/materials`
    )
  },

  getLectureTests(lectureId) {
    return http.get(
      `/public/learning/lectures/${lectureId}/tests`
    )
  },

  getTest(testId) {
    return http.get(
      `/public/learning/tests/${testId}`
    )
  },

  startAttempt(
    assignmentId,
    data = undefined
  ) {
    return http.post(
      `/public/learning/test-assignments/${assignmentId}/attempts/start`,
      data
    )
  },

  submitTest(
    testId,
    data
  ) {
    return http.post(
      `/public/learning/tests/${testId}/submit`,
      data
    )
  },

  submitAttempt(
    attemptId,
    data
  ) {
    return http.post(
      `/public/learning/attempts/${attemptId}/submit`,
      data
    )
  },

  downloadMaterial(
    lectureId,
    materialId
  ) {
    return http.get(
      `/public/learning/lectures/${lectureId}/materials/${materialId}/download`,
      {
        responseType: 'blob',
      }
    )
  },
}
