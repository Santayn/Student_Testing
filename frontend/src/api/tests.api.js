import http from './http'

export const testsApi = {
  getAll(params = {}) {
    return http.get('/tests', { params })
  },

  create(data) {
    return http.post('/tests', data)
  },

  delete(testId) {
    return http.delete(`/tests/${testId}`)
  },

  createAssignments(testId, data) {
    return http.post(`/tests/${testId}/assignments`, data)
  },
}
