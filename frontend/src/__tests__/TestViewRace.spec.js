import {
  afterEach,
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
  defineComponent,
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
import TestView from '@/views/tests/TestView.vue'

const TestsPageShellStub = defineComponent({
  name: 'TestsPageShell',
  props: {
    title: {
      type: String,
      required: true,
    },
    subtitle: {
      type: String,
      default: '',
    },
  },
  template: `
    <main data-testid="tests-page-shell">
      <h1 data-testid="page-title">{{ title }}</h1>
      <div data-testid="page-actions">
        <slot name="actions" />
      </div>
      <div data-testid="page-content">
        <slot />
      </div>
    </main>
  `,
})

const UiButtonStub = defineComponent({
  name: 'UiButton',
  props: {
    disabled: {
      type: Boolean,
      default: false,
    },
    loading: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['click'],
  template: `
    <button
      type="button"
      :disabled="disabled || loading"
      @click="$emit('click', $event)"
    >
      <slot />
    </button>
  `,
})

const UiAlertStub = defineComponent({
  name: 'UiAlert',
  props: {
    message: {
      type: String,
      default: '',
    },
  },
  template: `
    <div data-testid="alert">{{ message }}</div>
  `,
})

const mountedWrappers = []

function mountTestView() {
  const wrapper = shallowMount(TestView, {
    global: {
      stubs: {
        TestsPageShell: TestsPageShellStub,
        UiButton: UiButtonStub,
        UiAlert: UiAlertStub,
      },
    },
  })

  mountedWrappers.push(wrapper)
  return wrapper
}

afterEach(() => {
  mountedWrappers.splice(0).forEach((wrapper) => {
    wrapper.unmount()
  })
})

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

function submitButton(wrapper) {
  const button = wrapper
    .findAll('button')
    .find((candidate) => (
      candidate.text().includes(
        'Завершить тест'
      )
    ))

  if (!button) {
    throw new Error(
      'Submit button was not rendered by TestView.'
    )
  }

  return button
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
      mountTestView()

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
        .get('[data-testid="page-title"]')
        .text()
    ).toBe('Новый тест')

    await submitButton(wrapper)
      .trigger('click')

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
    const secondLoad = deferred()

    learningApi.startAttempt
      .mockResolvedValueOnce(
        startResponse({
          testId: 12,
          assignmentId: 34,
          attemptId: 400,
          title: 'Первый тест',
        })
      )
      .mockImplementationOnce(
        () => secondLoad.promise
      )

    learningApi.submitAttempt
      .mockImplementationOnce(
        () => submit.promise
      )

    const wrapper =
      mountTestView()

    await flushPromises()

    await submitButton(wrapper)
      .trigger('click')

    await nextTick()

    expect(
      learningApi.submitAttempt
    ).toHaveBeenCalledWith(
      400,
      expect.any(Object)
    )

    setRoute(13, 35)
    await nextTick()

    expect(
      learningApi.startAttempt
    ).toHaveBeenCalledWith(35)

    expect(
      learningApi.startAttempt
    ).toHaveBeenCalledTimes(2)

    secondLoad.resolve(
      startResponse({
        testId: 13,
        assignmentId: 35,
        attemptId: 500,
        title: 'Второй тест',
      })
    )

    await flushPromises()

    expect(
      wrapper
        .get('[data-testid="page-title"]')
        .text()
    ).toBe('Второй тест')

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
        .get('[data-testid="page-title"]')
        .text()
    ).toBe('Второй тест')

    expect(
      wrapper.find('[data-testid="alert"]').exists()
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
      mountTestView()

    await flushPromises()

    const alert =
      wrapper.get('[data-testid="alert"]')

    expect(alert.text()).toContain(
      'не соответствует открытому тесту'
    )

    expect(
      submitButton(wrapper).attributes('disabled')
    ).toBeDefined()

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
      mountTestView()

    await flushPromises()

    const alert =
      wrapper.get('[data-testid="alert"]')

    expect(alert.text()).toContain(
      'не соответствует открытому тесту'
    )

    expect(
      submitButton(wrapper).attributes('disabled')
    ).toBeDefined()

    expect(
      learningApi.submitAttempt
    ).not.toHaveBeenCalled()
  })

  it('blocks a blind second submit when the first request outcome is unknown', async () => {
    learningApi.startAttempt.mockResolvedValue(
      startResponse({
        testId: 12,
        assignmentId: 34,
        attemptId: 601,
        title: 'Тест с долгой отправкой',
      })
    )

    learningApi.submitAttempt.mockRejectedValue({
      code: 'ECONNABORTED',
      isAxiosError: true,
      config: {
        url: '/submit',
      },
    })

    const wrapper =
      mountTestView()

    await flushPromises()

    await submitButton(wrapper)
      .trigger('click')

    await flushPromises()

    expect(
      learningApi.submitAttempt
    ).toHaveBeenCalledTimes(1)

    const alert =
      wrapper.get('[data-testid="alert"]')

    expect(alert.text()).toContain(
      'повторная отправка заблокирована'
    )

    expect(
      submitButton(wrapper).attributes('disabled')
    ).toBeDefined()

    await submitButton(wrapper)
      .trigger('click')

    await flushPromises()

    expect(
      learningApi.submitAttempt
    ).toHaveBeenCalledTimes(1)
  })
})
