import http from './http'

export const facultiesApi = {
  getAll() {
    return http.get('/faculties')
  },

  getById(facultyId) {
    return http.get(`/faculties/${facultyId}`)
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

  getSubjects(facultyId) {
    return http.get(`/faculties/${facultyId}/subjects`)
  },

  replaceSubjects(facultyId, subjectIds) {
    return http.put(
      `/faculties/${facultyId}/subjects`,
      { subjectIds }
    )
  },

  addSubject(facultyId, subjectId, data = undefined) {
    return http.post(
      `/faculties/${facultyId}/subjects/${subjectId}`,
      data
    )
  },

  removeSubject(facultyId, subjectId) {
    return http.delete(
      `/faculties/${facultyId}/subjects/${subjectId}`
    )
  },
}
