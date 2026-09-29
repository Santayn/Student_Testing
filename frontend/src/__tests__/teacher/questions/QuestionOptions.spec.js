import { computed, reactive, ref } from 'vue'

import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

vi.mock('@/api', () => ({
  questionsApi: {},
  getApiErrorMessage: vi.fn(
    (error, fallback) => error?.message || fallback
  ),
}))

import { useQuestionOptions } from '@/composables/questions/useQuestionOptions'

function deferred() {
  let resolve
  const promise = new Promise((res) => {
    resolve = res
  })
  return { promise, resolve }
}

function setup(overrides = {}) {
  const form = reactive({
    id: 11,
    type: 1,
  })
  const notice = ref({ type: 'info', message: '' })
  const formError = ref('')
  const ensureSelectedMembershipActive = vi.fn()
    .mockResolvedValue({ id: 7 })
  const api = {
    getOptions: vi.fn().mockResolvedValue({ data: [] }),
    createOption: vi.fn().mockResolvedValue({ data: {} }),
    updateOption: vi.fn().mockResolvedValue({ data: {} }),
  }
  const isSelectableType = computed(
    () => Number(form.type) === 1 || Number(form.type) === 2
  )

  return {
    form,
    notice,
    formError,
    ensureSelectedMembershipActive,
    api,
    ...useQuestionOptions({
      form,
      isSelectableType,
      ensureSelectedMembershipActive,
      notice,
      formError,
      api,
      ...overrides,
    }),
  }
}

describe('question option editor', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads and sorts options while resetting the option draft ordinal', async () => {
    const state = setup()
    state.api.getOptions.mockResolvedValue({
      data: [
        { id: 2, ordinal: 2, text: 'B', correct: false },
        { id: 1, ordinal: 1, text: 'A', correct: true },
      ],
    })

    await state.loadOptions(11)

    expect(state.options.value.map((item) => item.id)).toEqual([1, 2])
    expect(state.optionForm.value.ordinal).toBe(3)
    expect(state.optionDraftDirty.value).toBe(false)
  })

  it('ignores a stale option response after a newer load starts', async () => {
    const first = deferred()
    const second = deferred()
    const state = setup()

    state.api.getOptions
      .mockReturnValueOnce(first.promise)
      .mockReturnValueOnce(second.promise)

    const firstLoad = state.loadOptions(11)
    const secondLoad = state.loadOptions(12)

    second.resolve({ data: [{ id: 12, ordinal: 1 }] })
    await secondLoad
    first.resolve({ data: [{ id: 11, ordinal: 1 }] })
    await firstLoad

    expect(state.options.value).toEqual([{ id: 12, ordinal: 1 }])
  })

  it('revalidates membership before creating an option and reloads the list', async () => {
    const state = setup()
    state.optionForm.value = {
      id: null,
      text: '  Ответ  ',
      ordinal: 2,
      correct: true,
    }

    await state.saveOption()

    expect(state.ensureSelectedMembershipActive).toHaveBeenCalledTimes(1)
    expect(state.api.createOption).toHaveBeenCalledWith(11, {
      text: 'Ответ',
      ordinal: 2,
      correct: true,
    })
    expect(state.api.getOptions).toHaveBeenCalledWith(11)
    expect(state.notice.value.message).toBe('Вариант ответа создан.')
  })

  it('updates an existing option and preserves validation errors', async () => {
    const state = setup()
    state.optionForm.value = {
      id: 4,
      text: 'Исправленный ответ',
      ordinal: 1,
      correct: false,
    }

    await state.saveOption()

    expect(state.api.updateOption).toHaveBeenCalledWith(4, {
      text: 'Исправленный ответ',
      ordinal: 1,
      correct: false,
    })

    state.optionForm.value = {
      id: null,
      text: '',
      ordinal: 1,
      correct: false,
    }
    await state.saveOption()

    expect(state.formError.value).toContain('заполните текст варианта')
  })

  it('clears option state and invalidates an in-flight load', async () => {
    const pending = deferred()
    const state = setup()
    state.api.getOptions.mockReturnValueOnce(pending.promise)

    const load = state.loadOptions(11)
    state.clearOptions()

    pending.resolve({ data: [{ id: 1, ordinal: 1 }] })
    await load

    expect(state.options.value).toEqual([])
    expect(state.loadingOptions.value).toBe(false)
  })
})
