import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import http from '@/api/http'
import { aiApi } from '@/api/ai.api'
import { resultsApi } from '@/api/results.api'
import { API_TIMEOUTS } from '@/api/timeouts'
import {
  sanitizeStudentAttempt,
  sanitizeStudentSubmitResult,
} from '@/utils/resultContracts'

vi.mock('@/api/http', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('AI API contracts', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('uploads scans with the backend multipart fields', async () => {
    http.post.mockResolvedValue({ data: {} })
    const page = new File(['img'], 'page.jpg', { type: 'image/jpeg' })

    await aiApi.scanLecture([page], { lectureId: 5, attachOriginals: true })

    const [url, body, config] = http.post.mock.calls[0]
    expect(url).toBe('/ai/lecture-scans')
    expect(body.getAll('files')).toEqual([page])
    expect(body.get('lectureId')).toBe('5')
    expect(body.get('attachOriginals')).toBe('true')
    expect(config).toEqual({ timeout: API_TIMEOUTS.fileTransfer })
  })

  it('reads image previews and generated images as blobs', async () => {
    http.get.mockResolvedValue({ data: new Blob() })

    await aiApi.previewImage('https://upload.wikimedia.org/a.jpg')
    await aiApi.getGeneratedImage(12)

    expect(http.get).toHaveBeenNthCalledWith(1, '/ai/images/preview', {
      params: { url: 'https://upload.wikimedia.org/a.jpg' },
      responseType: 'blob',
      timeout: API_TIMEOUTS.fileTransfer,
    })
    expect(http.get).toHaveBeenNthCalledWith(2, '/ai/jobs/12/image', {
      responseType: 'blob',
      timeout: API_TIMEOUTS.fileTransfer,
    })
  })

  it('uses the review endpoints of the teacher results api', async () => {
    http.get.mockResolvedValue({ data: {} })
    http.put.mockResolvedValue({ data: {} })
    http.post.mockResolvedValue({ data: {} })

    await resultsApi.getReviewQueue({ status: 'needs_teacher' })
    await resultsApi.gradeResponse(7, { awardedPoints: 1.5, comment: null })
    await resultsApi.acceptResponse(7)
    await resultsApi.regradeResponse(7)

    expect(http.get).toHaveBeenCalledWith('/results/teacher/review', { params: { status: 'needs_teacher' } })
    expect(http.put).toHaveBeenCalledWith('/results/teacher/responses/7/grade', { awardedPoints: 1.5, comment: null })
    expect(http.post).toHaveBeenCalledWith('/results/teacher/responses/7/accept')
    expect(http.post).toHaveBeenCalledWith('/results/teacher/responses/7/ai-regrade')
  })
})

describe('student review visibility', () => {
  it('keeps review state and feedback on the student answer without correctness', () => {
    const attempt = sanitizeStudentAttempt({
      results: [
        {
          questionText: 'Что такое ОЗУ?',
          givenAnswer: 'память',
          correctAnswer: 'оперативная память',
          correct: false,
          awardedPoints: 1,
          questionPoints: 2,
          gradingNote: 'Нужна проверка преподавателя.',
          reviewStatus: 'needs_teacher',
          aiFeedback: 'Не хватает слова «оперативная».',
          teacherComment: '  ',
          aiConfidence: 0.4,
        },
      ],
    })

    expect(attempt.results).toEqual([
      {
        questionText: 'Что такое ОЗУ?',
        givenAnswer: 'память',
        reviewPending: true,
        aiFeedback: 'Не хватает слова «оперативная».',
      },
    ])
  })

  it('reports pending answers in the submit summary', () => {
    const result = sanitizeStudentSubmitResult({
      pendingReviewCount: 1,
      details: [{ questionText: 'Q', givenAnswer: 'A', correct: true, pendingReview: true }],
    })

    expect(result.pendingReviewCount).toBe(1)
    expect(result.details).toEqual([{ questionText: 'Q', givenAnswer: 'A', reviewPending: true }])
  })
})
