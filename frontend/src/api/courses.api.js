import http from './http'

export const coursesApi = {
  getTemplates(params = {}) {
    return http.get('/courses/templates', {
      params,
    })
  },

  createTemplate(data) {
    return http.post('/courses/templates', data)
  },

  updateTemplate(templateId, data) {
    return http.put(`/courses/templates/${templateId}`, data)
  },

  removeTemplate(templateId) {
    return http.delete(`/courses/templates/${templateId}`)
  },

  getVersions(templateId) {
    return http.get(`/courses/templates/${templateId}/versions`)
  },

  createVersion(templateId, data) {
    return http.post(
      `/courses/templates/${templateId}/versions`,
      data,
    )
  },

  updateVersion(versionId, data) {
    return http.put(`/courses/versions/${versionId}`, data)
  },

  publishVersion(versionId, data = undefined) {
    return http.put(`/courses/versions/${versionId}/publish`, data)
  },

  unpublishVersion(versionId, data = undefined) {
    return http.put(`/courses/versions/${versionId}/unpublish`, data)
  },
}
