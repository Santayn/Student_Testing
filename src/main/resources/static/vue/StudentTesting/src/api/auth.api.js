import http from './http'

export const authApi = {
  login(login, password) {
    return http.post('/auth/login', {
      login,
      password,
    })
  },

  register(data) {
    return http.post('/auth/register', data)
  },

  me() {
    return http.get('/auth/me')
  },
}
