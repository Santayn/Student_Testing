import {
  afterEach,
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import {
  mount,
} from '@vue/test-utils'

import {
  defineComponent,
  reactive,
  ref,
} from 'vue'

import {
  useAttemptDraft,
} from '@/composables/tests/useAttemptDraft'

import {
  readTestAttemptDraft,
  saveTestAttemptDraft,
} from '@/utils/testAttemptDraft'

const questionsFixture = [
  {
    id: 701,
    type: 4,
    text: 'Введите ответ',
  },
]

const mountedWrappers = []

function currentDraft() {
  return readTestAttemptDraft({
    testId: 12,
    assignmentId: 34,
    attemptId: 77,
    questions: questionsFixture,
  })
}

function mountHarness() {
  const Harness = defineComponent({
    name: 'AttemptDraftHarness',
    setup() {
      const context = reactive({
        testId: 12,
        assignmentId: 34,
      })

      const attemptId = ref(77)
      const questions = ref(
        questionsFixture
      )
      const submitted = ref(false)

      const singleAnswers =
        reactive({})
      const multipleAnswers =
        reactive({})
      const textAnswers =
        reactive({})
      const matchingAnswers =
        reactive({})

      function initializeAnswers() {
        Object.keys(textAnswers)
          .forEach((key) => {
            delete textAnswers[key]
          })

        textAnswers['701'] = ''
      }

      const draft = useAttemptDraft({
        getRouteContext: () => context,
        getAttemptId: () => attemptId.value,
        getQuestions: () => questions.value,
        isSubmitted: () => submitted.value,
        singleAnswers,
        multipleAnswers,
        textAnswers,
        matchingAnswers,
        initializeAnswers,
      })

      return {
        context,
        attemptId,
        questions,
        submitted,
        textAnswers,
        ...draft,
      }
    },
    template: '<div />',
  })

  const wrapper = mount(Harness)
  mountedWrappers.push(wrapper)
  return wrapper
}

describe('useAttemptDraft', () => {
  beforeEach(() => {
    sessionStorage.clear()
    vi.useFakeTimers()
  })

  afterEach(() => {
    mountedWrappers
      .splice(0)
      .forEach((wrapper) => {
        wrapper.unmount()
      })

    vi.useRealTimers()
  })

  it('restores a compatible draft while initializing answers', () => {
    saveTestAttemptDraft({
      testId: 12,
      assignmentId: 34,
      attemptId: 77,
      questions: questionsFixture,
      textAnswers: {
        701: 'Сохранённый ответ',
      },
    })

    const wrapper = mountHarness()

    wrapper.vm.initializeAttemptAnswers({
      testId: 12,
      assignmentId: 34,
    })

    expect(
      wrapper.vm.textAnswers['701']
    ).toBe('Сохранённый ответ')
  })

  it('keeps the 250 ms debounce contract', async () => {
    const wrapper = mountHarness()

    wrapper.vm.initializeAttemptAnswers({
      testId: 12,
      assignmentId: 34,
    })

    wrapper.vm.textAnswers['701'] =
      'Новый ответ'

    expect(currentDraft()).toBeNull()

    await vi.advanceTimersByTimeAsync(249)
    expect(currentDraft()).toBeNull()

    await vi.advanceTimersByTimeAsync(1)

    expect(
      currentDraft()?.textAnswers?.['701']
    ).toBe('Новый ответ')
  })

  it('flushes a pending draft on pagehide', () => {
    const wrapper = mountHarness()

    wrapper.vm.initializeAttemptAnswers({
      testId: 12,
      assignmentId: 34,
    })

    wrapper.vm.textAnswers['701'] =
      'Ответ перед pagehide'

    expect(currentDraft()).toBeNull()

    window.dispatchEvent(
      new Event('pagehide')
    )

    expect(
      currentDraft()?.textAnswers?.['701']
    ).toBe('Ответ перед pagehide')
  })

  it('can cancel a scheduled write after the draft was cleared', async () => {
    const wrapper = mountHarness()

    wrapper.vm.initializeAttemptAnswers({
      testId: 12,
      assignmentId: 34,
    })

    wrapper.vm.textAnswers['701'] =
      'Не должен воскреснуть'

    wrapper.vm.clearDraftForContext({
      testId: 12,
      assignmentId: 34,
    })
    wrapper.vm.cancelScheduledDraftPersistence()

    await vi.advanceTimersByTimeAsync(300)

    expect(currentDraft()).toBeNull()
  })
})
