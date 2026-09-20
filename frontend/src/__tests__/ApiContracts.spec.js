import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import http from '@/api/http'
import { coursesApi } from '@/api/courses.api'
import { learningApi } from '@/api/learning.api'
import { lecturesApi } from '@/api/lectures.api'
import { membershipsApi } from '@/api/memberships.api'
import { resultsApi } from '@/api/results.api'
import { teachingApi } from '@/api/teaching.api'
import { testsApi } from '@/api/tests.api'

vi.mock('@/api/http', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('frontend API contracts', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads subject load types through the supported endpoint', async () => {
    http.get.mockResolvedValue({ data: [] })

    const params = {
      subjectMembershipId: 17,
      teachingLoadTypeId: 3,
    }

    await teachingApi.getSubjectLoadTypes(params)

    expect(http.get).toHaveBeenCalledWith(
      '/teaching/subject-load-types',
      { params }
    )
  })

  it('adds a load type to a concrete subject membership', async () => {
    http.post.mockResolvedValue({ data: {} })

    const payload = {
      teachingLoadTypeId: 3,
      hours: 36,
    }

    await teachingApi.addLoadTypeToSubjectMembership(
      17,
      payload
    )

    expect(http.post).toHaveBeenCalledWith(
      '/teaching/subject-memberships/17/load-types',
      payload
    )
  })

  it('uploads lecture materials with the backend multipart field name', async () => {
    http.post.mockResolvedValue({ data: [] })

    const first = new File(['a'], 'a.txt', {
      type: 'text/plain',
    })
    const second = new File(['b'], 'b.txt', {
      type: 'text/plain',
    })

    await lecturesApi.uploadMaterials(9, [
      first,
      second,
    ])

    expect(http.post).toHaveBeenCalledTimes(1)

    const [url, body] = http.post.mock.calls[0]

    expect(url).toBe('/lectures/9/materials')
    expect(body).toBeInstanceOf(FormData)
    expect(body.getAll('files')).toEqual([
      first,
      second,
    ])
    expect(body.has('file')).toBe(false)
  })

  it('updates a full subject membership through the supported endpoint', async () => {
    http.put.mockResolvedValue({ data: {} })

    const payload = {
      status: 1,
      notes: 'Возобновлено',
    }

    await membershipsApi.updateSubjectMembership(44, payload)

    expect(http.put).toHaveBeenCalledWith(
      '/memberships/subjects/memberships/44',
      payload
    )
  })


  it('sanitizes student result data at the API boundary', async () => {
    http.get.mockResolvedValue({
      data: {
        attemptCount: 1,
        attempts: [
          {
            attemptId: 8,
            testId: 9,
            stats: {
              total: 1,
              right: 1,
              percent: 100,
            },
            results: [
              {
                questionText: 'Вопрос',
                givenAnswer: 'Ответ',
                correctAnswer: 'Эталон',
                correct: true,
                questionPoints: 2,
                awardedPoints: 2,
                gradingStatus: 'correct',
                gradingNote: 'Скрыть',
              },
            ],
          },
        ],
      },
    })

    const response =
      await resultsApi.getStudentData({
        testId: 9,
      })

    expect(http.get).toHaveBeenCalledWith(
      '/results/student/data',
      {
        params: {
          testId: 9,
        },
      }
    )

    expect(response.data.attempts[0]).toMatchObject({
      score: 2,
      maxScore: 2,
      scorePercent: 100,
    })

    expect(response.data.attempts[0].results).toEqual([
      {
        questionText: 'Вопрос',
        givenAnswer: 'Ответ',
      },
    ])
  })

  it('sanitizes the student submit response at the API boundary', async () => {
    http.post.mockResolvedValue({
      data: {
        attemptId: 10,
        score: 1,
        correctCount: 1,
        totalCount: 1,
        details: [
          {
            questionText: '2 + 2?',
            givenAnswer: '4',
            correctAnswer: '4',
            correct: true,
          },
        ],
      },
    })

    const response =
      await learningApi.submitAttempt(
        10,
        {
          questionIds: [1],
        }
      )

    expect(http.post).toHaveBeenCalledWith(
      '/public/learning/attempts/10/submit',
      {
        questionIds: [1],
      }
    )

    expect(response.data.details).toEqual([
      {
        questionText: '2 + 2?',
        givenAnswer: '4',
      },
    ])
  })

  it('publishes a course version with the required empty request body', async () => {
    http.put.mockResolvedValue({ data: {} })

    await coursesApi.publishVersion(73)

    expect(http.put).toHaveBeenCalledWith(
      '/courses/versions/73/publish',
      {}
    )
  })

  it('deletes a test through the supported endpoint', async () => {
    http.delete.mockResolvedValue({})

    await testsApi.delete(101)

    expect(http.delete).toHaveBeenCalledWith(
      '/tests/101'
    )
  })
})
