import http from './http'

export const topicsApi = {
  getAll(params = {}) {
    return http.get('/topics', { params })
  },

  getOne(topicId) {
    return http.get(`/topics/${topicId}`)
  },

  create(data) {
    return http.post('/topics', data)
  },

  update(topicId, data) {
    return http.put(`/topics/${topicId}`, data)
  },

  remove(topicId) {
    return http.delete(`/topics/${topicId}`)
  },
}
