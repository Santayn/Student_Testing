import http from './http'

export const rolesApi = {
  getAll() {
    return http.get('/roles')
  },

  createRole(payload) {
    return http.post('/roles', payload)
  },

  getPermissions() {
    return http.get('/roles/permissions')
  },

  createPermission(payload) {
    return http.post('/roles/permissions', payload)
  },

  setPermissions(roleId, permissionIds) {
    return http.put(
      `/roles/${roleId}/permissions`,
      {
        permissionIds,
      }
    )
  },
}
