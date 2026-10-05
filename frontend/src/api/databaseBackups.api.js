import http from './http'

export const databaseBackupsApi = {
  create(config = {}) {
    return http.post(
      '/admin/database-backups',
      null,
      {
        ...config,
        responseType: 'blob',
        timeout:
          config.timeout ?? 0,
      }
    )
  },

  restore(file, config = {}) {
    const formData = new FormData()
    formData.append('file', file)

    return http.post(
      '/admin/database-backups/restore',
      formData,
      {
        ...config,
        timeout:
          config.timeout ?? 0,
      }
    )
  },
}
