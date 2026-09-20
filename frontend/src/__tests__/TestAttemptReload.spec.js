import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import {
  flushPromises,
  shallowMount,
} from '@vue/test-utils'

const routerState = vi.hoisted(() => ({
  route: {
    params: {
      testId: '12',
    },
    query: {
      assignmentId: '34',
    },
  },
  back: vi.fn(),
  leaveHandler: null,
}))

vi.mock('vue-router', () => ({
  useRoute: () => routerState.route,
  useRouter: () => ({
    back: routerState.back,
  }),
  onBeforeRouteLeave: (handler) => {
    routerState.leaveHandler = handler
  },
}))

vi.mock('@/api', () => ({
  getApiErrorMessage: (
    _error,
    fallback
  ) => fallback,
  learningApi: {
    startAttempt: vi.fn(),
    submitAttempt: vi.fn(),
  },
}))

import { learningApi } from '@/api'
import {
  readCompletedTestSession,
  saveCompletedTestSession,
} from '@/utils/completedTestSession'
import TestView from '@/views/tests/TestView.vue'

function completedSession() {
  return {
    testId: 12,
    assignmentId: 34,
    attemptId: 56,
    test: {
      id: 12,
      title: 'Тест',
      attemptsAllowed: 3,
    },
    resultData: {
      correctCount: 1,
      totalCount: 1,
      score: 1,
      details: [],
    },
  }
}

describe('completed test reload flow', () => {
  beforeEach(() => {
    sessionStorage.clear()
    vi.clearAllMocks()
    routerState.leaveHandler = null

    learningApi.startAttempt.mockResolvedValue({
      data: {
        attemptId: 77,
        assignmentId: 34,
        test: {
          id: 12,
          assignmentId: 34,
          title: 'Тест',
          attemptsAllowed: 3,
        },
        questions: [],
      },
    })
  })

  it('does not start a new attempt after reload of a completed test', async () => {
    saveCompletedTestSession(
      completedSession()
    )

    shallowMount(TestView)
    await flushPromises()

    expect(
      learningApi.startAttempt
    ).not.toHaveBeenCalled()
  })

  it('starts normally when there is no completed-session marker', async () => {
    shallowMount(TestView)
    await flushPromises()

    expect(
      learningApi.startAttempt
    ).toHaveBeenCalledTimes(1)

    expect(
      learningApi.startAttempt
    ).toHaveBeenCalledWith(34)
  })


  it('does not persist per-question correctness in the completed-session marker', () => {
    saveCompletedTestSession({
      ...completedSession(),
      resultData: {
        correctCount: 1,
        totalCount: 1,
        score: 1,
        details: [
          {
            questionText: '2 + 2?',
            givenAnswer: '4',
            correctAnswer: '4',
            correct: true,
            gradingStatus: 'correct',
            gradingNote: 'Скрытая заметка',
            questionPoints: 1,
            awardedPoints: 1,
          },
        ],
      },
    })

    const saved =
      readCompletedTestSession(
        12,
        34
      )

    expect(saved.resultData.details).toEqual([
      {
        questionText: '2 + 2?',
        givenAnswer: '4',
      },
    ])
  })

  it('clears the completed marker when the user leaves the test route', async () => {
    saveCompletedTestSession(
      completedSession()
    )

    shallowMount(TestView)
    await flushPromises()

    expect(
      routerState.leaveHandler
    ).toBeTypeOf('function')

    routerState.leaveHandler()

    expect(
      readCompletedTestSession(
        12,
        34
      )
    ).toBeNull()
  })
})
