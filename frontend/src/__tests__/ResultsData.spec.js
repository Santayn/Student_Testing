import { ref } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api', () => ({
  getApiErrorMessage: (_error, fallback) => fallback,
  resultsApi: {
    getStudentData: vi.fn(),
    getTeacherData: vi.fn(),
  },
}))

import { resultsApi } from '@/api'
import { useResultsData } from '@/composables/useResultsData'

function deferred() {
  let resolve
  let reject
  const promise = new Promise((done, fail) => {
    resolve = done
    reject = fail
  })
  return { promise, resolve, reject }
}

function setup({ teacher = false } = {}) {
  const teacherMode = ref(teacher)
  const subjectId = ref('')
  const testId = ref('')
  const tests = ref([])
  const error = ref('')

  const api = useResultsData({
    teacherMode,
    subjectId,
    testId,
    teacherParams: () => ({ testId: testId.value || undefined }),
    studentParams: () => ({ subjectId: subjectId.value || undefined, testId: testId.value || undefined }),
    setStudentTestOptions: (data) => {
      const attempts = Array.isArray(data?.attempts) ? data.attempts : []
      tests.value = attempts.map((attempt) => ({ id: attempt.testId }))
    },
    selectedStudentTestIsValid: () => !testId.value || tests.value.some((item) => String(item.id) === String(testId.value)),
    resetInvalidStudentTest: () => { testId.value = '' },
    error,
  })

  return { ...api, teacherMode, subjectId, testId, tests, error }
}

describe('useResultsData', () => {
  beforeEach(() => vi.clearAllMocks())

  it('loads and sanitizes student context while deriving allowed test ids', async () => {
    resultsApi.getStudentData.mockResolvedValue({
      data: { attempts: [{ attemptId: 1, testId: 31, testName: 'A', isCorrect: true }] },
    })
    const data = setup()
    data.subjectId.value = 17
    await data.loadStudentContextResults()

    expect(resultsApi.getStudentData).toHaveBeenCalledWith({ subjectId: 17 }, expect.objectContaining({ signal: expect.any(AbortSignal) }))
    expect(data.tests.value).toEqual([{ id: 31 }])
    expect(data.resultData.value.attempts[0]).not.toHaveProperty('isCorrect')
    expect(data.loadingResults.value).toBe(false)
  })

  it('loads teacher results with teacher params', async () => {
    resultsApi.getTeacherData.mockResolvedValue({ data: { attempts: [{ attemptId: 2 }] } })
    const data = setup({ teacher: true })
    data.testId.value = 44
    await data.loadResults()
    expect(resultsApi.getTeacherData).toHaveBeenCalledWith({ testId: 44 }, expect.objectContaining({ signal: expect.any(AbortSignal) }))
    expect(data.resultData.value.attempts).toEqual([{ attemptId: 2 }])
  })

  it('cancels an older results read and ignores its late response', async () => {
    const first = deferred()
    resultsApi.getTeacherData
      .mockImplementationOnce((_params, { signal }) => {
        expect(signal).toBeInstanceOf(AbortSignal)
        return first.promise
      })
      .mockResolvedValueOnce({ data: { attempts: [{ attemptId: 2 }] } })

    const data = setup({ teacher: true })
    const oldRequest = data.loadResults()
    await Promise.resolve()
    const newRequest = data.loadResults()
    await newRequest
    first.resolve({ data: { attempts: [{ attemptId: 1 }] } })
    await oldRequest

    expect(data.resultData.value.attempts).toEqual([{ attemptId: 2 }])
  })

  it('rejects a student test outside the current result context without another request', async () => {
    const data = setup()
    data.tests.value = [{ id: 31 }]
    data.testId.value = 999
    await data.onStudentTestChange()

    expect(resultsApi.getStudentData).not.toHaveBeenCalled()
    expect(data.testId.value).toBe('')
    expect(data.resultData.value).toBeNull()
    expect(data.error.value).toContain('не относится')
  })
})
