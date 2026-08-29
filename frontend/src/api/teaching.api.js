import http from './http'

export const teachingApi = {
  getAssignments(params = {}) {
    return http.get('/teaching/assignments', {
      params,
    })
  },

  getAssignment(assignmentId) {
    return http.get(`/teaching/assignments/${assignmentId}`)
  },

  createAssignment(data) {
    return http.post('/teaching/assignments', data)
  },

  updateAssignment(assignmentId, data) {
    return http.put(
      `/teaching/assignments/${assignmentId}`,
      data,
    )
  },

  getEnrollments(params = {}) {
    return http.get('/teaching/enrollments', {
      params,
    })
  },

  getLoadTypes() {
    return http.get('/teaching/load-types')
  },

  createLoadType(data) {
    return http.post('/teaching/load-types', data)
  },

  addLoadTypeToSubjectMembership(subjectMembershipId, data) {
    return http.post(
      `/teaching/subject-memberships/${subjectMembershipId}/load-types`,
      data,
    )
  },

  createSubjectLoadType(subjectMembershipId, teachingLoadTypeId, data = undefined) {
    return http.post('/teaching/subject-load-types', data, {
      params: {
        subjectMembershipId,
        teachingLoadTypeId,
      },
    })
  },

  getLectureAssignments(params = {}) {
    return http.get('/teaching/lecture-assignments', {
      params,
    })
  },

  createLectureAssignment(teachingAssignmentId, data) {
    return http.post(
      `/teaching/assignments/${teachingAssignmentId}/lecture-assignments`,
      data,
    )
  },

  updateLectureAssignmentStatus(lectureAssignmentId, data) {
    return http.put(
      `/teaching/lecture-assignments/${lectureAssignmentId}/status`,
      data,
    )
  },
}
