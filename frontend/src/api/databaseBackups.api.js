import http from './http'

export const databaseBackupsApi = {
  create() {
    return http.post('/admin/database-backups', null, {
      responseType: 'blob',
      timeout: 0,
    })
  },

  restore(file) {
    const formData = new FormData()
    formData.append('file', file)

    return http.post('/admin/database-backups/restore', formData, {
      timeout: 0,
    })
  },
}
