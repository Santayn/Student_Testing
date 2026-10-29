import {
  sanitizeStudentResultData,
} from '@/utils/resultContracts'

import http from './http'

export const resultsApi = {
  getStudentSubjects(config = {}) {
    return http.get(
      '/results/student/subjects',
      config
    )
  },

  getTeacherSubjects(config = {}) {
    return http.get(
      '/results/teacher/subjects',
      config
    )
  },

  getTeacherLectures(subjectId, config = {}) {
    return http.get(
      '/results/teacher/lectures',
      {
        ...config,
        params: {
          subjectId,
        },
      }
    )
  },

  getTeacherTests(lectureId, config = {}) {
    return http.get(
      '/results/teacher/tests',
      {
        ...config,
        params: {
          lectureId,
        },
      }
    )
  },

  getTeacherGroups(testId, config = {}) {
    return http.get(
      '/results/teacher/groups',
      {
        ...config,
        params: {
          testId,
        },
      }
    )
  },

  getTeacherStudents(groupId, config = {}) {
    return http.get(
      '/results/teacher/students',
      {
        ...config,
        params: {
          groupId,
        },
      }
    )
  },

  getTeacherData(params = {}, config = {}) {
    return http.get(
      '/results/teacher/data',
      {
        ...config,
        params,
      }
    )
  },

  getReviewQueue(params = {}, config = {}) {
    return http.get(
      '/results/teacher/review',
      {
        ...config,
        params,
      }
    )
  },

  gradeResponse(responseId, data) {
    return http.put(
      `/results/teacher/responses/${responseId}/grade`,
      data
    )
  },

  acceptResponse(responseId) {
    return http.post(
      `/results/teacher/responses/${responseId}/accept`
    )
  },

  regradeResponse(responseId) {
    return http.post(
      `/results/teacher/responses/${responseId}/ai-regrade`
    )
  },

  async getStudentData(params = {}, config = {}) {
    const response = await http.get(
      '/results/student/data',
      {
        ...config,
        params,
      }
    )

    return {
      ...response,
      data: sanitizeStudentResultData(
        response?.data
      ),
    }
  },
}
