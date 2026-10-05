import http from './http'

export const teachingApi = {
  getAssignments(params = {}, config = {}) {
    return http.get('/teaching/assignments', { ...config, params })
  },

  getAssignment(assignmentId, config = {}) {
    return http.get(
      `/teaching/assignments/${assignmentId}`,
      config
    )
  },

  createAssignment(data) {
    return http.post('/teaching/assignments', data)
  },

  updateAssignment(assignmentId, data) {
    return http.put(`/teaching/assignments/${assignmentId}`, data)
  },

  getEnrollments(params = {}, config = {}) {
    return http.get(
      '/teaching/enrollments',
      { ...config, params }
    )
  },

  getLoadTypes(config = {}) {
    return http.get('/teaching/load-types', config)
  },

  createLoadType(data) {
    return http.post('/teaching/load-types', data)
  },

  updateLoadType(loadTypeId, data) {
    return http.put(`/teaching/load-types/${loadTypeId}`, data)
  },

  addLoadTypeToSubjectMembership(subjectMembershipId, data) {
    return http.post(
      `/teaching/subject-memberships/${subjectMembershipId}/load-types`,
      data
    )
  },

  getSubjectLoadTypes(params = {}, config = {}) {
    return http.get(
      '/teaching/subject-load-types',
      { ...config, params }
    )
  },


  getLectureAssignments(params = {}, config = {}) {
    return http.get(
      '/teaching/lecture-assignments',
      { ...config, params }
    )
  },

  createLectureAssignment(teachingAssignmentId, data) {
    return http.post(
      `/teaching/assignments/${teachingAssignmentId}/lecture-assignments`,
      data
    )
  },

  updateLectureAssignmentStatus(lectureAssignmentId, data) {
    return http.put(
      `/teaching/lecture-assignments/${lectureAssignmentId}/status`,
      data
    )
  },
}
