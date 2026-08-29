import http from './http'

export const resultsApi = {
  getStudentSubjects() {
    return http.get(
      '/results/student/subjects'
    )
  },

  getTeacherSubjects() {
    return http.get(
      '/results/teacher/subjects'
    )
  },

  getTeacherLectures(subjectId) {
    return http.get(
      '/results/teacher/lectures',
      {
        params: {
          subjectId,
        },
      }
    )
  },

  getTeacherTests(lectureId) {
    return http.get(
      '/results/teacher/tests',
      {
        params: {
          lectureId,
        },
      }
    )
  },

  getTeacherGroups(testId) {
    return http.get(
      '/results/teacher/groups',
      {
        params: {
          testId,
        },
      }
    )
  },

  getTeacherStudents(groupId) {
    return http.get(
      '/results/teacher/students',
      {
        params: {
          groupId,
        },
      }
    )
  },

  getTeacherData(params = {}) {
    return http.get(
      '/results/teacher/data',
      {
        params,
      }
    )
  },

  getStudentData(params = {}) {
    return http.get(
      '/results/student/data',
      {
        params,
      }
    )
  },
}
