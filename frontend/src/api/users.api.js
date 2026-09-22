import http from './http'

export const usersApi = {
  getAll() {
    return http.get('/users')
  },

  getMe() {
    return http.get('/users/me')
  },

  getPeople(params = {}) {
    return http.get('/users/people', { params })
  },

  getPerson(personId) {
    return http.get(`/users/people/${personId}`)
  },

  createPerson(payload) {
    return http.post('/users/people', payload)
  },

  updatePerson(personId, payload) {
    return http.put(
      `/users/people/${personId}`,
      payload
    )
  },

  updateRoles(userId, roles) {
    return http.put(`/users/${userId}/roles`, roles)
  },

  updatePersonBinding(userId, personId) {
    return http.put(
      `/users/${userId}/person`,
      {
        personId,
      }
    )
  },

  setActive(userId, active) {
    return http.put(
      `/users/${userId}/active`,
      {
        active: Boolean(active),
      }
    )
  },
}
