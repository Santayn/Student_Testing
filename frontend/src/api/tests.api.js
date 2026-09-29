import http from './http'

export const testsApi = {
  getAll(params = {}, config = {}) {
    return http.get('/tests', { ...config, params })
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
