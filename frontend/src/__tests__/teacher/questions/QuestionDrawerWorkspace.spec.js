import {
  reactive,
  ref,
} from 'vue'

import {
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import {
  useQuestionDrawerWorkspace,
} from '@/composables/questions/useQuestionDrawerWorkspace'

function createWorkspace(overrides = {}) {
  const dependencies = {
    canCreateQuestion: ref(true),
    questions: ref([
      { id: 1, ordinal: 2 },
      { id: 2, ordinal: 5 },
    ]),
    form: reactive({
      id: null,
      matchingPairs: [],
    }),
    formError: ref('old error'),
    savingQuestion: ref(false),
    savingOption: ref(false),
    questionDirty: ref(false),
    optionDraftDirty: ref(false),
    confirmCloseVisible: ref(false),
    openCreate: vi.fn(),
    openEdit: vi.fn(),
    closeImmediately: vi.fn(),
    discardAndClose: vi.fn(),
    resetOptionForm: vi.fn(),
    clearOptions: vi.fn(),
    loadOptions: vi.fn().mockResolvedValue(undefined),
    notice: ref({
      type: 'info',
      message: '',
    }),
    ...overrides,
  }

  return {
    dependencies,
    workspace: useQuestionDrawerWorkspace(dependencies),
  }
}

describe('useQuestionDrawerWorkspace', () => {
  it('opens a new question with the next ordinal and clears stale drawer state', () => {
    const { dependencies, workspace } = createWorkspace()

    workspace.openCreateQuestion()

    expect(dependencies.formError.value).toBe('')
    expect(dependencies.clearOptions).toHaveBeenCalledTimes(1)
    expect(dependencies.openCreate).toHaveBeenCalledWith({
      ordinal: 6,
    })
  })

  it('blocks create when the current subject/topic context is incomplete', () => {
    const { dependencies, workspace } = createWorkspace({
      canCreateQuestion: ref(false),
    })

    workspace.openCreateQuestion()

    expect(dependencies.openCreate).not.toHaveBeenCalled()
    expect(dependencies.notice.value).toEqual({
      type: 'danger',
      message: 'Выберите предмет и тему.',
    })
  })

  it('loads answer options only for selectable question types when editing', async () => {
    const { dependencies, workspace } = createWorkspace()

    await workspace.editQuestion({
      id: 42,
      type: 2,
    })

    expect(dependencies.openEdit).toHaveBeenCalledWith({
      id: 42,
      type: 2,
    })
    expect(dependencies.loadOptions).toHaveBeenCalledWith(42)

    dependencies.loadOptions.mockClear()

    await workspace.editQuestion({
      id: 43,
      type: 4,
    })

    expect(dependencies.loadOptions).not.toHaveBeenCalled()
  })

  it('asks for confirmation instead of closing when either draft is dirty', () => {
    const questionDirty = ref(true)
    const { dependencies, workspace } = createWorkspace({
      questionDirty,
    })

    expect(workspace.requestQuestionDrawerClose()).toBe(false)
    expect(dependencies.confirmCloseVisible.value).toBe(true)
    expect(dependencies.closeImmediately).not.toHaveBeenCalled()
  })

  it('does not close while the question or an option is being saved', () => {
    const { dependencies, workspace } = createWorkspace({
      savingOption: ref(true),
    })

    expect(workspace.requestQuestionDrawerClose()).toBe(false)
    expect(dependencies.confirmCloseVisible.value).toBe(false)
    expect(dependencies.closeImmediately).not.toHaveBeenCalled()
  })

  it('closes immediately and clears transient drawer state when clean', () => {
    const { dependencies, workspace } = createWorkspace()

    expect(workspace.requestQuestionDrawerClose()).toBe(true)
    expect(dependencies.closeImmediately).toHaveBeenCalledTimes(1)
    expect(dependencies.formError.value).toBe('')
    expect(dependencies.clearOptions).toHaveBeenCalledTimes(1)
  })

  it('prepares matching rows and switches option loading according to type', () => {
    const { dependencies, workspace } = createWorkspace()

    workspace.handleQuestionTypeChange(3)

    expect(dependencies.form.matchingPairs.length).toBeGreaterThanOrEqual(2)
    expect(dependencies.clearOptions).toHaveBeenCalled()

    dependencies.form.id = 99
    dependencies.clearOptions.mockClear()

    workspace.handleQuestionTypeChange(1)
    expect(dependencies.loadOptions).toHaveBeenCalledWith(99)

    workspace.handleQuestionTypeChange(4)
    expect(dependencies.clearOptions).toHaveBeenCalledTimes(1)
  })
})
