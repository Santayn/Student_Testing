import { ref } from 'vue'

import {
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import {
  useQuestionMutations,
} from '@/composables/questions/useQuestionMutations'

function setup({
  formOverrides = {},
  apiOverrides = {},
} = {}) {
  const form = {
    id: null,
    question: '  Новый вопрос  ',
    type: 4,
    points: 2,
    ordinal: 3,
    correctAnswer: '  Ответ  ',
    matchingPairs: [],
    active: true,
    ...formOverrides,
  }
  const formError = ref('')
  const formFieldErrors = ref({})
  const selectedTopicId = ref('11')
  const questions = ref([])
  const notice = ref({ type: 'info', message: '' })
  const saving = ref(false)

  const api = {
    create: vi.fn(),
    update: vi.fn(),
    updateActive: vi.fn(),
    ...apiOverrides,
  }

  const deps = {
    form,
    formError,
    formFieldErrors,
    selectedTopicId,
    questions,
    notice,
    questionValidationMessage: vi.fn(() => ''),
    beginSaving: vi.fn(),
    saving,
    finishSaving: vi.fn(),
    failSaving: vi.fn(),
    ensureSelectedMembershipActive: vi.fn(async () => {}),
    loadQuestions: vi.fn(async () => {}),
    openEdit: vi.fn(),
    loadOptions: vi.fn(async () => {}),
    clearOptions: vi.fn(),
    api,
  }

  return {
    ...deps,
    ...useQuestionMutations(deps),
  }
}

describe('question mutations', () => {
  it('preserves the existing create payload and reopens the saved question', async () => {
    const saved = {
      id: 41,
      topicId: 11,
      type: 4,
      question: 'Новый вопрос',
      points: 2,
      ordinal: 3,
      correctAnswer: 'Ответ',
      matchingPairs: [],
      active: true,
    }

    const state = setup({
      apiOverrides: {
        create: vi.fn(async () => ({ data: saved })),
      },
    })

    state.loadQuestions.mockImplementation(async () => {
      state.questions.value = [saved]
    })

    await state.saveQuestion()

    expect(state.ensureSelectedMembershipActive).toHaveBeenCalledTimes(1)
    expect(state.api.create).toHaveBeenCalledWith({
      testId: null,
      courseLectureId: null,
      topicId: 11,
      type: 4,
      question: 'Новый вопрос',
      points: 2,
      ordinal: 3,
      correctAnswer: 'Ответ',
      matchingPairs: [],
    })
    expect(state.openEdit).toHaveBeenCalledWith(saved)
    expect(state.clearOptions).toHaveBeenCalled()
    expect(state.notice.value).toEqual({
      type: 'success',
      message:
        'Вопрос создан. Теперь можно добавить варианты ответа, если они нужны.',
    })
  })

  it('preserves update semantics and reloads options for selectable questions', async () => {
    const saved = {
      id: 9,
      topicId: 11,
      type: 2,
      question: 'Обновлённый вопрос',
      points: 4,
      ordinal: 5,
      correctAnswer: null,
      matchingPairs: [],
      active: false,
    }

    const state = setup({
      formOverrides: {
        id: 9,
        question: ' Обновлённый вопрос ',
        type: 2,
        points: 4,
        ordinal: 5,
        correctAnswer: 'не отправляется',
        active: false,
      },
      apiOverrides: {
        update: vi.fn(async () => ({ data: saved })),
      },
    })

    state.loadQuestions.mockImplementation(async () => {
      state.questions.value = [saved]
    })

    await state.saveQuestion()

    expect(state.api.update).toHaveBeenCalledWith(9, {
      courseLectureId: null,
      topicId: 11,
      type: 2,
      question: 'Обновлённый вопрос',
      points: 4,
      ordinal: 5,
      correctAnswer: null,
      matchingPairs: [],
      active: false,
    })
    expect(state.finishSaving).toHaveBeenCalledWith({
      close: false,
      values: expect.objectContaining({
        id: 9,
        type: 2,
        active: false,
      }),
    })
    expect(state.loadOptions).toHaveBeenCalledWith(9)
    expect(state.notice.value).toEqual({
      type: 'success',
      message: 'Вопрос обновлён.',
    })
  })


  it('ignores a repeated save while the previous mutation is still pending', async () => {
    const state = setup()
    state.saving.value = true

    const result = await state.saveQuestion()

    expect(result).toBe(false)
    expect(state.beginSaving).not.toHaveBeenCalled()
    expect(state.ensureSelectedMembershipActive).not.toHaveBeenCalled()
    expect(state.api.create).not.toHaveBeenCalled()
    expect(state.api.update).not.toHaveBeenCalled()
  })

  it('serializes active-status changes and refreshes the list', async () => {
    let release
    const pending = new Promise((resolve) => {
      release = resolve
    })

    const updateActive = vi.fn(() => pending)
    const state = setup({
      apiOverrides: { updateActive },
    })
    const question = { id: 15, active: true }

    const first = state.toggleQuestionActive(question)
    const second = state.toggleQuestionActive(question)

    expect(state.togglingQuestionId.value).toBe(15)

    await Promise.resolve()

    expect(updateActive).toHaveBeenCalledTimes(1)

    release({ data: null })
    await first
    await second

    expect(updateActive).toHaveBeenCalledWith(15, {
      active: false,
    })
    expect(state.loadQuestions).toHaveBeenCalledTimes(1)
    expect(state.togglingQuestionId.value).toBeNull()
    expect(state.notice.value).toEqual({
      type: 'success',
      message: 'Вопрос скрыт.',
    })
  })

  it('does not call the API when editor validation fails', async () => {
    const state = setup()
    state.questionValidationMessage.mockReturnValue('Введите текст вопроса.')

    await state.saveQuestion()

    expect(state.formError.value).toBe('Введите текст вопроса.')
    expect(state.beginSaving).not.toHaveBeenCalled()
    expect(state.api.create).not.toHaveBeenCalled()
    expect(state.api.update).not.toHaveBeenCalled()
  })
})
