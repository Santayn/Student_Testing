import http, { rootHttp } from './http'

export const lecturesApi = {
  getAll(params = {}) {
    return http.get('/lectures', {
      params,
    })
  },

  create(data) {
    return http.post('/lectures', data)
  },

  update(lectureId, data) {
    return http.put(`/lectures/${lectureId}`, data)
  },

  remove(lectureId) {
    return http.delete(`/lectures/${lectureId}`)
  },

  getTests(lectureId) {
    return http.get(`/lectures/${lectureId}/tests`)
  },

  setTests(lectureId, data) {
    return http.put(`/lectures/${lectureId}/tests`, data)
  },

  getMaterials(lectureId) {
    return http.get(`/lectures/${lectureId}/materials`)
  },

  /**
   * В текущем backend загрузка материала находится под /api,
   * а не /api/v1.
   */
  uploadMaterial(lectureId, file, extraFields = {}) {
    const formData = new FormData()
    formData.append('file', file)

    Object.entries(extraFields).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        formData.append(key, value)
      }
    })

    return rootHttp.post(
      `/lectures/${lectureId}/materials`,
      formData,
    )
  },

  removeMaterial(lectureId, materialId) {
    return http.delete(
      `/lectures/${lectureId}/materials/${materialId}`,
    )
  },
}
