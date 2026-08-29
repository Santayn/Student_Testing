import http from './http'

export const rolesApi = {
  getAll() {
    return http.get('/roles')
  },
}
