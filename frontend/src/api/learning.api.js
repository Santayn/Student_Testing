import {
  sanitizeStudentSubmitResult,
} from '@/utils/resultContracts'

import http from './http'
import { API_TIMEOUTS } from './timeouts'

export const learningApi = {
  getSubject(subjectId) {
    return http.get(
      `/public/learning/subjects/${subjectId}`
    )
  },

  getSubjectLectures(subjectId) {
    return http.get(
      `/public/learning/subjects/${subjectId}/lectures`
    )
  },

  getLecture(lectureId) {
    return http.get(
      `/public/learning/lectures/${lectureId}`
    )
  },

  getLectureMaterials(lectureId) {
    return http.get(
      `/public/learning/lectures/${lectureId}/materials`
    )
  },

  getLectureTests(lectureId) {
    return http.get(
      `/public/learning/lectures/${lectureId}/tests`
    )
  },

  startAttempt(
    assignmentId,
    data = undefined
  ) {
    return http.post(
      `/public/learning/test-assignments/${assignmentId}/attempts/start`,
      data
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
