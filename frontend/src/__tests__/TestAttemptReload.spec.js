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
        test: {
          id: 12,
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
