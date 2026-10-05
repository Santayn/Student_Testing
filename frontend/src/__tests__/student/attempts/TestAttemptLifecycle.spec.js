import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import {
  useTestAttemptLifecycle,
} from '@/composables/tests/useTestAttemptLifecycle'

import {
  readCompletedTestSession,
  saveCompletedTestSession,
} from '@/utils/completedTestSession'

function deferred() {
  let resolve
  let reject

  const promise = new Promise((res, rej) => {
    resolve = res
    reject = rej
  })

  return {
    promise,
    resolve,
    reject,
  }
}

function startResponse({
  testId = 12,
  assignmentId = 34,
  attemptId = 56,
  title = 'Тест',
} = {}) {
  return {
    data: {
      attemptId,
      assignmentId,
      test: {
        id: testId,
        assignmentId,
        title,
        attemptsAllowed: 3,
      },
      questions: [
        {
          id: 701,
          type: 4,
          text: 'Вопрос',
        },
      ],
    },
  }
}

function createHarness({
  api,
  route = {
    testId: 12,
    assignmentId: 34,
  },
} = {}) {
  const context = {
    ...route,
  }

  const resetAnswers = vi.fn()
  const initializeAttemptAnswers = vi.fn()
  const beforeSubmit = vi.fn()
  const clearAttemptDraft = vi.fn()
  const cancelDraftPersistence = vi.fn()
  const scrollToResult = vi.fn()

  const lifecycle = useTestAttemptLifecycle({
    getRouteContext: () => ({ ...context }),
    isSameRouteContext: (candidate) => (
      candidate?.testId === context.testId &&
      candidate?.assignmentId === context.assignmentId
    ),
    resetAnswers,
    initializeAttemptAnswers,
    buildSubmission: () => ({
      questionIds: [701],
      answers: ['Ответ'],
      selectedOptionIds: [[]],
    }),
    beforeSubmit,
    clearAttemptDraft,
    cancelDraftPersistence,
    api,
    resolveApiError: (error, fallback) => (
      error?.message || fallback
    ),
    scrollToResult,
  })

  return {
    context,
    lifecycle,
    resetAnswers,
    initializeAttemptAnswers,
    beforeSubmit,
    clearAttemptDraft,
    cancelDraftPersistence,
    scrollToResult,
  }
}

describe('useTestAttemptLifecycle', () => {
  beforeEach(() => {
    sessionStorage.clear()
    vi.clearAllMocks()
  })

  it('starts the assignment attempt and initializes answers only for the current route', async () => {
    const api = {
      startAttempt: vi.fn().mockResolvedValue(
        startResponse()
      ),
      submitAttempt: vi.fn(),
    }

    const {
      lifecycle,
      initializeAttemptAnswers,
    } = createHarness({ api })

    await lifecycle.loadTest()

    expect(api.startAttempt).toHaveBeenCalledWith(34)
    expect(lifecycle.attemptId.value).toBe(56)
    expect(lifecycle.test.value?.id).toBe(12)
    expect(lifecycle.questions.value).toHaveLength(1)
    expect(initializeAttemptAnswers).toHaveBeenCalledWith({
      testId: 12,
      assignmentId: 34,
    })
  })

  it('does not let an older start response overwrite a newer route context', async () => {
    const first = deferred()
    const second = deferred()

    const api = {
      startAttempt: vi.fn()
        .mockImplementationOnce(() => first.promise)
        .mockImplementationOnce(() => second.promise),
      submitAttempt: vi.fn(),
    }

    const {
      context,
      lifecycle,
    } = createHarness({ api })

    const firstLoad = lifecycle.loadTest()

    context.testId = 13
    context.assignmentId = 35

    const secondLoad = lifecycle.loadTest()

    second.resolve(
      startResponse({
        testId: 13,
        assignmentId: 35,
        attemptId: 200,
        title: 'Новый тест',
      })
    )

    await secondLoad

    first.resolve(
      startResponse({
        testId: 12,
        assignmentId: 34,
        attemptId: 100,
        title: 'Старый тест',
      })
    )

    await firstLoad

    expect(lifecycle.test.value?.title).toBe('Новый тест')
    expect(lifecycle.attemptId.value).toBe(200)
  })

  it('rejects a start response from another assignment context', async () => {
    const api = {
      startAttempt: vi.fn().mockResolvedValue(
        startResponse({
          assignmentId: 99,
        })
      ),
      submitAttempt: vi.fn(),
    }

    const { lifecycle } =
      createHarness({ api })

    await lifecycle.loadTest()

    expect(lifecycle.attemptId.value).toBeNull()
    expect(lifecycle.error.value).toContain(
      'не соответствует открытому тесту'
    )
  })

  it('restores a completed session without starting another attempt', async () => {
    saveCompletedTestSession({
      testId: 12,
      assignmentId: 34,
      attemptId: 56,
      test: {
        id: 12,
        title: 'Завершённый тест',
      },
      resultData: {
        correctCount: 1,
        totalCount: 1,
        score: 1,
        details: [],
      },
    })

    const api = {
      startAttempt: vi.fn(),
      submitAttempt: vi.fn(),
    }

    const {
      lifecycle,
      clearAttemptDraft,
    } = createHarness({ api })

    await lifecycle.loadTest()

    expect(api.startAttempt).not.toHaveBeenCalled()
    expect(lifecycle.submitted.value).toBe(true)
    expect(lifecycle.attemptId.value).toBe(56)
    expect(clearAttemptDraft).toHaveBeenCalledWith({
      testId: 12,
      assignmentId: 34,
    })
  })

  it('blocks a blind retry when submit outcome is unknown', async () => {
    const api = {
      startAttempt: vi.fn().mockResolvedValue(
        startResponse()
      ),
      submitAttempt: vi.fn().mockRejectedValue({
        code: 'ERR_NETWORK',
        isAxiosError: true,
        config: {
          url: '/submit',
        },
      }),
    }

    const {
      lifecycle,
      beforeSubmit,
    } = createHarness({ api })

    await lifecycle.loadTest()
    await lifecycle.submitTest()
    await lifecycle.submitTest()

    expect(beforeSubmit).toHaveBeenCalledTimes(1)
    expect(api.submitAttempt).toHaveBeenCalledTimes(1)
    expect(lifecycle.submitOutcomeUnknown.value).toBe(true)
    expect(lifecycle.error.value).toContain(
      'повторная отправка заблокирована'
    )
  })

  it('clears draft state and saves the completed marker after a confirmed submit', async () => {
    const api = {
      startAttempt: vi.fn().mockResolvedValue(
        startResponse()
      ),
      submitAttempt: vi.fn().mockResolvedValue({
        data: {
          correctCount: 1,
          totalCount: 1,
          score: 1,
          details: [],
        },
      }),
    }

    const {
      lifecycle,
      beforeSubmit,
      clearAttemptDraft,
      cancelDraftPersistence,
      scrollToResult,
    } = createHarness({ api })

    await lifecycle.loadTest()
    await lifecycle.submitTest()

    expect(beforeSubmit).toHaveBeenCalledTimes(1)
    expect(clearAttemptDraft).toHaveBeenLastCalledWith({
      testId: 12,
      assignmentId: 34,
      attemptId: 56,
    })
    expect(cancelDraftPersistence).toHaveBeenCalledTimes(1)
    expect(lifecycle.submitted.value).toBe(true)
    expect(scrollToResult).toHaveBeenCalledTimes(1)

    expect(
      readCompletedTestSession(12, 34)?.attemptId
    ).toBe(56)
  })
})
