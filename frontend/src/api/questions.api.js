import http from './http'

export const questionsApi = {
  getAll(params = {}) {
    return http.get('/questions', { params })
  },

  create(data) {
    return http.post('/questions', data)
  },

  update(questionId, data) {
    return http.put(`/questions/${questionId}`, data)
  },

  updateActive(questionId, data) {
    return http.put(`/questions/${questionId}/active`, data)
  },

  getOptions(questionId) {
    return http.get(`/questions/${questionId}/options`)
  },

  getOption(optionId) {
    return http.get(`/questions/options/${optionId}`)
  },

  createOption(questionId, data) {
    return http.post(`/questions/${questionId}/options`, data)
  },

  updateOption(optionId, data) {
    return http.put(`/questions/options/${optionId}`, data)
  },

  importFile(file, extraFields = {}) {
    const formData = new FormData()
    formData.append('file', file)

    Object.entries(extraFields).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        formData.append(key, value)
      }
    })

    return http.post('/questions/import', formData)
  },
}
