import http from './http'

export const learningApi = {
  getSnapshot() {
    return http.get(
      '/public/learning/snapshot'
    )
  },

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

  getCurrentAttempt(assignmentId) {
    return http.get(
      `/public/learning/test-assignments/${assignmentId}/attempts/current`
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

  getAttemptStatus(attemptId) {
    return http.get(
      `/public/learning/attempts/${attemptId}/status`
    )
  },

  getAttemptResult(attemptId) {
    return http.get(
      `/public/learning/attempts/${attemptId}/result`
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
