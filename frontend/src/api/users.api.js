import http from './http'

export const usersApi = {
  getAll(params = {}) {
    return http.get(
      '/users',
      { params }
    )
  },

  getMe() {
    return http.get('/users/me')
  },

  getPeople(params = {}) {
    return http.get(
      '/users/people',
      { params }
    )
  },

  getPerson(personId) {
    return http.get(
      `/users/people/${personId}`
    )
  },

  updateRoles(userId, data) {
    return http.put(
      `/users/${userId}/roles`,
      data
    )
  },
}
