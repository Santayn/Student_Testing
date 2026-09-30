import http from './http'

export const facultiesApi = {
  getAll(config = {}) {
    return http.get('/faculties', config)
  },

  getById(facultyId, config = {}) {
    return http.get(`/faculties/${facultyId}`, config)
  },

  create(data) {
    return http.post('/faculties', data)
  },

  update(facultyId, data) {
    return http.put(`/faculties/${facultyId}`, data)
  },

  remove(facultyId) {
    return http.delete(`/faculties/${facultyId}`)
  },

  getSubjects(facultyId, config = {}) {
    return http.get(
      `/faculties/${facultyId}/subjects`,
      config
    )
  },

  addSubject(facultyId, subjectId) {
    return http.post(
      `/faculties/${facultyId}/subjects/${subjectId}`
    )
  },

  removeSubject(facultyId, subjectId) {
    return http.delete(
      `/faculties/${facultyId}/subjects/${subjectId}`
    )
  },
}
