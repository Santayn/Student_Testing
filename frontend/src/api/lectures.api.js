import http from './http'
import { API_TIMEOUTS } from './timeouts'

export const lecturesApi = {
  getAll(params = {}, config = {}) {
    return http.get('/lectures', { ...config, params })
  },

  getOne(lectureId, config = {}) {
    return http.get(`/lectures/${lectureId}`, config)
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

  getTests(lectureId, config = {}) {
    return http.get(`/lectures/${lectureId}/tests`, config)
  },

  setTests(lectureId, data) {
    return http.put(`/lectures/${lectureId}/tests`, data)
  },

  getMaterials(lectureId, config = {}) {
    return http.get(
      `/lectures/${lectureId}/materials`,
      config
    )
  },


  uploadMaterials(lectureId, files) {
    const formData = new FormData()

    Array.from(files ?? []).forEach((file) => {
      formData.append('files', file)
    })

    return http.post(
      `/lectures/${lectureId}/materials`,
      formData,
      {
        timeout: API_TIMEOUTS.fileTransfer,
      }
    )
  },

  downloadMaterial(lectureId, materialId) {
    return http.get(
      `/lectures/${lectureId}/materials/${materialId}/download`,
      {
        responseType: 'blob',
        timeout: API_TIMEOUTS.fileTransfer,
      }
    )
  },

  /** Текст лекции файлом: format — docx | pdf. */
  exportText(lectureId, format) {
    return http.get(
      `/lectures/${lectureId}/text/export`,
      {
        params: { format },
        responseType: 'blob',
        timeout: API_TIMEOUTS.fileTransfer,
      }
    )
  },

  saveTextAsMaterial(lectureId, format) {
    return http.post(
      `/lectures/${lectureId}/text/materials`,
      null,
      { params: { format } }
    )
  },

  removeMaterial(lectureId, materialId) {
    return http.delete(
      `/lectures/${lectureId}/materials/${materialId}`
    )
  },
}
