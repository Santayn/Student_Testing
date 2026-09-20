import http from './http'

export const lecturesApi = {
  getAll(params = {}) {
    return http.get('/lectures', { params })
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


  uploadMaterials(lectureId, files) {
    const formData = new FormData()

    Array.from(files ?? []).forEach((file) => {
      formData.append('files', file)
    })

    return http.post(
      `/lectures/${lectureId}/materials`,
      formData
    )
  },

  downloadMaterial(lectureId, materialId) {
    return http.get(
      `/lectures/${lectureId}/materials/${materialId}/download`,
      {
        responseType: 'blob',
      }
    )
  },

  removeMaterial(lectureId, materialId) {
    return http.delete(
      `/lectures/${lectureId}/materials/${materialId}`
    )
  },
}
