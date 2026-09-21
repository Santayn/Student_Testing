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
} from 'vue'

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
    error,
    fallback
  ) => error?.message || fallback,
  learningApi: {
    startAttempt: vi.fn(),
    submitAttempt: vi.fn(),
  },
}))

import { learningApi } from '@/api'
import {
  readTestAttemptDraft,
  saveTestAttemptDraft,
} from '@/utils/testAttemptDraft'
import TestView from '@/views/tests/TestView.vue'

const testQuestions = [
  {
    id: 701,
    type: 4,
    text: 'Введите ответ',
    points: 1,
  },
]

function startResponse() {
  return {
    data: {
      attemptId: 77,
      assignmentId: 34,
      test: {
        id: 12,
        assignmentId: 34,
        title: 'Тест с черновиком',
        attemptsAllowed: 3,
      },
      questions: testQuestions,
    },
  }
}

const TestsPageShellStub = defineComponent({
  name: 'TestsPageShell',
  template: `
    <main>
      <slot name="actions" />
      <slot />
    </main>
  `,
})

const UiCardStub = defineComponent({
  name: 'UiCard',
  template: `
    <section><slot /></section>
  `,
})

const UiInputStub = defineComponent({
  name: 'UiInput',
  props: {
    modelValue: {
      type: [String, Number],
      default: '',
    },
  },
  emits: ['update:modelValue'],
  template: `
    <input
      data-testid="text-answer"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
    />
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

const mountedWrappers = []

function mountTestView() {
  const wrapper =
    shallowMount(TestView, {
      global: {
        stubs: {
          TestsPageShell:
            TestsPageShellStub,
          UiCard: UiCardStub,
          UiInput: UiInputStub,
          UiButton: UiButtonStub,
        },
      },
    })

  mountedWrappers.push(wrapper)
  return wrapper
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

function currentDraft() {
  return readTestAttemptDraft({
    testId: 12,
    assignmentId: 34,
    attemptId: 77,
    questions: testQuestions,
  })
}

describe('TestView draft recovery', () => {
  beforeEach(() => {
    sessionStorage.clear()
    vi.clearAllMocks()
    routerState.leaveHandler = null

    learningApi.startAttempt.mockResolvedValue(
      startResponse()
    )

    learningApi.submitAttempt.mockResolvedValue({
      data: {
        correctCount: 0,
        totalCount: 1,
        score: 0,
        details: [],
      },
    })

    if (!HTMLElement.prototype.scrollIntoView) {
      Object.defineProperty(
        HTMLElement.prototype,
        'scrollIntoView',
        {
          configurable: true,
          value: vi.fn(),
        }
      )
    }
  })

  afterEach(() => {
    mountedWrappers
      .splice(0)
      .forEach((wrapper) => {
        wrapper.unmount()
      })
  })

  it('restores the draft after backend resumes the same attempt', async () => {
    saveTestAttemptDraft({
      testId: 12,
      assignmentId: 34,
      attemptId: 77,
      questions: testQuestions,
      textAnswers: {
        701: 'Сохранённый ответ',
      },
    })

    const wrapper =
      mountTestView()

    await flushPromises()

    expect(
      wrapper
        .get('[data-testid="text-answer"]')
        .element.value
    ).toBe('Сохранённый ответ')
  })

  it('persists a changed answer immediately in sessionStorage', async () => {
    const wrapper =
      mountTestView()

    await flushPromises()

    await wrapper
      .get('[data-testid="text-answer"]')
      .setValue('Новый черновик')

    expect(
      currentDraft()?.textAnswers?.['701']
    ).toBe('Новый черновик')
  })

  it('clears the draft after a confirmed successful submit', async () => {
    const wrapper =
      mountTestView()

    await flushPromises()

    await wrapper
      .get('[data-testid="text-answer"]')
      .setValue('Ответ для отправки')

    expect(currentDraft()).not.toBeNull()

    await submitButton(wrapper)
      .trigger('click')

    await flushPromises()

    expect(currentDraft()).toBeNull()
  })
})
