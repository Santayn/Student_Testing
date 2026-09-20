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

import {
  nextTick,
} from 'vue'

const routerState = vi.hoisted(() => ({
  routeSeed: {
    params: {
      testId: '12',
    },
    query: {
      assignmentId: '34',
    },
  },
  route: null,
  back: vi.fn(),
  leaveHandler: null,
}))

vi.mock('vue-router', async () => {
  const {
    reactive,
  } = await vi.importActual('vue')

  routerState.route = reactive(
    routerState.routeSeed
  )

  return {
    useRoute: () => routerState.route,
    useRouter: () => ({
      back: routerState.back,
    }),
    onBeforeRouteLeave: (handler) => {
      routerState.leaveHandler = handler
    },
  }
})

vi.mock('@/api', () => ({
  getApiErrorMessage: (
    error,
    fallback
  ) => error?.message || fallback,
  learningApi: {
    startAttempt: vi.fn(),
    submitAttempt: vi.fn(),
  },
}))

import { learningApi } from '@/api'
import TestsPageShell from '@/components/tests/TestsPageShell.vue'
import {
  UiAlert,
  UiButton,
} from '@/components/ui'
import TestView from '@/views/tests/TestView.vue'

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
  testId,
  assignmentId,
  attemptId,
  title,
}) {
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
          id: attemptId * 10,
          type: 4,
          text: `${title}: вопрос`,
          points: 1,
        },
      ],
    },
  }
}

function setRoute(
  testId,
  assignmentId
) {
  routerState.route.params.testId =
    String(testId)
  routerState.route.query.assignmentId =
    String(assignmentId)
}

describe('TestView request context integrity', () => {
  beforeEach(() => {
    sessionStorage.clear()
    vi.clearAllMocks()
    routerState.leaveHandler = null

    setRoute(12, 34)

    learningApi.submitAttempt.mockResolvedValue({
      data: {
        correctCount: 0,
        totalCount: 1,
        score: 0,
        details: [],
      },
    })
  })

  it('does not let an older startAttempt response overwrite a newer route', async () => {
    const first = deferred()
    const second = deferred()

    learningApi.startAttempt
      .mockImplementationOnce(
        () => first.promise
      )
      .mockImplementationOnce(
        () => second.promise
      )

    const wrapper =
      shallowMount(TestView)

    await nextTick()

    expect(
      learningApi.startAttempt
    ).toHaveBeenCalledWith(34)

    setRoute(13, 35)
    await nextTick()

    expect(
      learningApi.startAttempt
    ).toHaveBeenCalledWith(35)

    second.resolve(
      startResponse({
        testId: 13,
        assignmentId: 35,
        attemptId: 200,
        title: 'Новый тест',
      })
    )

    await flushPromises()

    first.resolve(
      startResponse({
        testId: 12,
        assignmentId: 34,
        attemptId: 100,
        title: 'Старый тест',
      })
    )

    await flushPromises()

    expect(
      wrapper
        .findComponent(TestsPageShell)
        .props('title')
    ).toBe('Новый тест')

    const buttons =
      wrapper.findAllComponents(UiButton)

    await buttons[1].trigger('click')
    await flushPromises()

    expect(
      learningApi.submitAttempt
    ).toHaveBeenCalledTimes(1)

    expect(
      learningApi.submitAttempt
    ).toHaveBeenCalledWith(
      200,
      expect.any(Object)
    )
  })

  it('ignores a submit response after the route changes to another test', async () => {
    const submit = deferred()

    learningApi.startAttempt
      .mockResolvedValueOnce(
        startResponse({
          testId: 12,
          assignmentId: 34,
          attemptId: 400,
          title: 'Первый тест',
        })
      )
      .mockResolvedValueOnce(
        startResponse({
          testId: 13,
          assignmentId: 35,
          attemptId: 500,
          title: 'Второй тест',
        })
      )

    learningApi.submitAttempt
      .mockImplementationOnce(
        () => submit.promise
      )

    const wrapper =
      shallowMount(TestView)

    await flushPromises()

    const buttons =
      wrapper.findAllComponents(UiButton)

    await buttons[1].trigger('click')
    await nextTick()

    expect(
      learningApi.submitAttempt
    ).toHaveBeenCalledWith(
      400,
      expect.any(Object)
    )

    setRoute(13, 35)
    await flushPromises()

    submit.resolve({
      data: {
        correctCount: 1,
        totalCount: 1,
        score: 1,
        details: [],
      },
    })

    await flushPromises()

    expect(
      wrapper
        .findComponent(TestsPageShell)
        .props('title')
    ).toBe('Второй тест')

    expect(
      wrapper.findComponent(UiAlert).exists()
    ).toBe(false)
  })

  it('blocks an attempt when backend returns another assignment context', async () => {
    learningApi.startAttempt.mockResolvedValue(
      startResponse({
        testId: 12,
        assignmentId: 99,
        attemptId: 300,
        title: 'Чужой контекст',
      })
    )

    const wrapper =
      shallowMount(TestView)

    await flushPromises()

    const alert =
      wrapper.findComponent(UiAlert)

    expect(alert.exists()).toBe(true)
    expect(alert.props('message')).toContain(
      'не соответствует открытому тесту'
    )

    const buttons =
      wrapper.findAllComponents(UiButton)

    await buttons[1].trigger('click')
    await flushPromises()

    expect(
      learningApi.submitAttempt
    ).not.toHaveBeenCalled()
  })

  it('blocks an attempt when backend returns another test for the route', async () => {
    learningApi.startAttempt.mockResolvedValue(
      startResponse({
        testId: 99,
        assignmentId: 34,
        attemptId: 301,
        title: 'Другой тест',
      })
    )

    const wrapper =
      shallowMount(TestView)

    await flushPromises()

    const alert =
      wrapper.findComponent(UiAlert)

    expect(alert.exists()).toBe(true)
    expect(alert.props('message')).toContain(
      'не соответствует открытому тесту'
    )

    expect(
      learningApi.submitAttempt
    ).not.toHaveBeenCalled()
  })
})
