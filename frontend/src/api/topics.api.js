import http from './http'

export const topicsApi = {
  getAll(params = {}, config = {}) {
    return http.get('/topics', { ...config, params })
  },

  getOne(topicId, config = {}) {
    return http.get(`/topics/${topicId}`, config)
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
