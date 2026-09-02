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
}
