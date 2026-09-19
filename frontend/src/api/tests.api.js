import http from './http'

export const testsApi = {
  getAll(params = {}) {
    return http.get('/tests', { params })
  },

  getById(id) {
    return http.get(`/tests/${id}`)
  },

  create(data) {
    return http.post('/tests', data)
  },

  update(id, data) {
    return http.put(`/tests/${id}`, data)
  },

  delete(id) {
    return http.delete(`/tests/${id}`)
  },

  getSelectionRules(testId) {
    return http.get(`/tests/${testId}/selection-rules`)
  },

  updateSelectionRules(testId, data) {
    return http.put(`/tests/${testId}/selection-rules`, data)
  },

  getAssignments(params = {}) {
    return http.get('/tests/assignments', { params })
  },

  createAssignments(testId, data) {
    return http.post(`/tests/${testId}/assignments`, data)
  },

  updateAssignment(assignmentId, data) {
    return http.put(`/tests/assignments/${assignmentId}`, data)
  },

  updateAssignmentStatus(assignmentId, data) {
    return http.put(
      `/tests/assignments/${assignmentId}/status`,
      data
    )
  },
}
