import {
  sanitizeStudentSubmitResult,
} from '@/utils/resultContracts'

import http from './http'
import { API_TIMEOUTS } from './timeouts'

export const learningApi = {
  getSubject(subjectId, config = {}) {
    return http.get(
      `/public/learning/subjects/${subjectId}`,
      config
    )
  },

  getSubjectLectures(subjectId, config = {}) {
    return http.get(
      `/public/learning/subjects/${subjectId}/lectures`,
      config
    )
  },

  getLecture(lectureId, config = {}) {
    return http.get(
      `/public/learning/lectures/${lectureId}`,
      config
    )
  },

  getLectureMaterials(lectureId, config = {}) {
    return http.get(
      `/public/learning/lectures/${lectureId}/materials`,
      config
    )
  },

  getLectureTests(lectureId, config = {}) {
    return http.get(
      `/public/learning/lectures/${lectureId}/tests`,
      config
    )
  },

  startAttempt(assignmentId) {
    return http.post(
      `/public/learning/test-assignments/${assignmentId}/attempts/start`
    )
  },

  async submitAttempt(
    attemptId,
    data
  ) {
    const response = await http.post(
      `/public/learning/attempts/${attemptId}/submit`,
      data,
      {
        timeout: API_TIMEOUTS.submitAttempt,
      }
    )

    return {
      ...response,
      data: sanitizeStudentSubmitResult(
        response?.data
      ),
    }
  },

  downloadMaterial(
    lectureId,
    materialId
  ) {
    return http.get(
      `/public/learning/lectures/${lectureId}/materials/${materialId}/download`,
      {
        responseType: 'blob',
        timeout: API_TIMEOUTS.fileTransfer,
      }
    )
  },
}
