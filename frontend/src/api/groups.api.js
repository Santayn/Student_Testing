import http from './http'

export const groupsApi = {
  getAll(params = {}) {
    return http.get('/groups', { params })
  },

  getById(groupId, config = {}) {
    return http.get(`/groups/${groupId}`, config)
  },

  create(data) {
    return http.post('/groups', data)
  },

  update(groupId, data) {
    return http.put(`/groups/${groupId}`, data)
  },

  remove(groupId) {
    return http.delete(`/groups/${groupId}`)
  },
}
