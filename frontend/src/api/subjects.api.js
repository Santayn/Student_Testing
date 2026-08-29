import http from './http'

export const subjectsApi = {
  getAll() {
    return http.get('/subjects')
  },

  getById(subjectId) {
    return http.get(`/subjects/${subjectId}`)
  },

  create(data) {
    return http.post('/subjects', data)
  },

  update(subjectId, data) {
    return http.put(`/subjects/${subjectId}`, data)
  },

  remove(subjectId) {
    return http.delete(`/subjects/${subjectId}`)
  },
}
