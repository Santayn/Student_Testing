import http from './http'
import { API_TIMEOUTS } from './timeouts'

export const aiApi = {
  getStatus(config = {}) {
    return http.get('/ai/status', config)
  },

  getJob(jobId, config = {}) {
    return http.get(`/ai/jobs/${jobId}`, config)
  },

  getGeneratedImage(jobId, config = {}) {
    return http.get(`/ai/jobs/${jobId}/image`, {
      ...config,
      responseType: 'blob',
      timeout: API_TIMEOUTS.fileTransfer,
    })
  },

  suggestCriteria(data) {
    return http.post('/ai/questions/criteria-suggestion', data, {
      timeout: API_TIMEOUTS.submitAttempt,
    })
  },

  scanLecture(files, { lectureId = null, attachOriginals = false } = {}) {
    const formData = new FormData()

    Array.from(files ?? []).forEach((file) => {
      formData.append('files', file)
    })

    if (lectureId) {
      formData.append('lectureId', String(lectureId))
    }

    formData.append('attachOriginals', attachOriginals ? 'true' : 'false')

    return http.post('/ai/lecture-scans', formData, {
      timeout: API_TIMEOUTS.fileTransfer,
    })
  },

  createPresentationOutline(lectureId, data) {
    return http.post(`/ai/lectures/${lectureId}/presentation-outline`, data)
  },

  buildPresentation(lectureId, data) {
    return http.post(`/ai/lectures/${lectureId}/presentation`, data)
  },

  searchImages(query, config = {}) {
    return http.get('/ai/images/search', {
      ...config,
      params: { q: query, limit: 8 },
    })
  },

  previewImage(url, config = {}) {
    return http.get('/ai/images/preview', {
      ...config,
      params: { url },
      responseType: 'blob',
      timeout: API_TIMEOUTS.fileTransfer,
    })
  },

  generateImage(data) {
    return http.post('/ai/images/generate', data)
  },

  /** Расход ИИ — только администраторам (право ai.usage.read). */
  getUsageReport(params = {}, config = {}) {
    return http.get('/admin/ai-usage', { ...config, params })
  },
}
