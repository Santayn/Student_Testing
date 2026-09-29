import { ref } from 'vue'

import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

vi.mock('@/api', () => ({
  questionsApi: {
    getAll: vi.fn(),
  },
  getApiErrorMessage: vi.fn(
    (error, fallback) => error?.message || fallback
  ),
}))

import {
  getApiErrorMessage,
  questionsApi,
} from '@/api'

import { useQuestionsList } from '@/composables/questions/useQuestionsList'

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

function setup(topicId = '10') {
  const selectedTopicId = ref(topicId)
  const notice = ref({
    type: 'info',
    message: '',
  })

  return {
    selectedTopicId,
    notice,
    ...useQuestionsList({
      selectedTopicId,
      notice,
    }),
  }
}

describe('question list loader', () => {
  beforeEach(() => {
    questionsApi.getAll.mockReset()
    getApiErrorMessage.mockClear()
  })

  it('loads the selected topic and normalizes ordering by ordinal', async () => {
    questionsApi.getAll.mockResolvedValue({
      data: [
        { id: 2, ordinal: 3 },
        { id: 1, ordinal: 1 },
        { id: 3, ordinal: 2 },
      ],
    })

    const state = setup('42')
    await state.loadQuestions()

    expect(questionsApi.getAll).toHaveBeenCalledWith({
      topicId: 42,
    })
    expect(
      state.questions.value.map((item) => item.id)
    ).toEqual([1, 3, 2])
    expect(state.loading.value).toBe(false)
  })

  it('does not request questions without a selected topic', async () => {
    const state = setup('')
    state.questions.value = [{ id: 1 }]

    await state.loadQuestions()

    expect(questionsApi.getAll).not.toHaveBeenCalled()
    expect(state.questions.value).toEqual([])
    expect(state.loading.value).toBe(false)
  })

  it('ignores a stale response after the selected topic is loaded again', async () => {
    const first = deferred()
    const second = deferred()

    questionsApi.getAll
      .mockReturnValueOnce(first.promise)
      .mockReturnValueOnce(second.promise)

    const state = setup('10')
    const firstLoad = state.loadQuestions()

    state.selectedTopicId.value = '20'
    const secondLoad = state.loadQuestions()

    second.resolve({
      data: [{ id: 20, ordinal: 1 }],
    })
    await secondLoad

    first.resolve({
      data: [{ id: 10, ordinal: 1 }],
    })
    await firstLoad

    expect(state.questions.value).toEqual([
      { id: 20, ordinal: 1 },
    ])
  })

  it('invalidates an in-flight read when the question list is reset', async () => {
    const pending = deferred()
    questionsApi.getAll.mockReturnValueOnce(pending.promise)

    const state = setup('10')
    const load = state.loadQuestions()

    state.resetQuestions()

    pending.resolve({
      data: [{ id: 10, ordinal: 1 }],
    })
    await load

    expect(state.questions.value).toEqual([])
    expect(state.loading.value).toBe(false)
  })

  it('reports the current request failure through the existing notice surface', async () => {
    questionsApi.getAll.mockRejectedValue(
      new Error('Сеть недоступна')
    )

    const state = setup('10')
    await state.loadQuestions()

    expect(state.notice.value).toEqual({
      type: 'danger',
      message: 'Сеть недоступна',
    })
    expect(getApiErrorMessage).toHaveBeenCalled()
    expect(state.loading.value).toBe(false)
  })
})
