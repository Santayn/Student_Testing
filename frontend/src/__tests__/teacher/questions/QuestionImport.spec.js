import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'
import { ref } from 'vue'

const { importFile, getApiErrorMessage } = vi.hoisted(() => ({
  importFile: vi.fn(),
  getApiErrorMessage: vi.fn(),
}))

vi.mock('@/api', () => ({
  questionsApi: {
    importFile,
  },
  getApiErrorMessage,
}))

import { useQuestionImport } from '@/composables/questions/useQuestionImport'

function createHarness(overrides = {}) {
  const selectedMembership = ref({ id: 3 })
  const topicOptions = ref([
    { value: '7', label: 'Topic 7' },
    { value: '8', label: 'Topic 8' },
  ])
  const selectedTopicId = ref('7')
  const selectedImportTopicId = ref('')
  const notice = ref({ type: 'info', message: '' })
  const ensureSelectedMembershipActive = vi.fn()
  const loadQuestions = vi.fn()

  const state = useQuestionImport({
    selectedMembership,
    topicOptions,
    selectedTopicId,
    selectedImportTopicId,
    ensureSelectedMembershipActive,
    loadQuestions,
    notice,
    ...overrides,
  })

  return {
    state,
    selectedMembership,
    topicOptions,
    selectedTopicId,
    selectedImportTopicId,
    notice,
    ensureSelectedMembershipActive,
    loadQuestions,
  }
}

describe('useQuestionImport', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    importFile.mockResolvedValue({
      data: {
        importedQuestions: 2,
        importedOptions: 5,
      },
    })
    getApiErrorMessage.mockReturnValue('Import failed')
  })

  it('opens the dialog on the active topic and resets previous files', () => {
    const h = createHarness()
    h.state.wordFiles.value = [new File(['old'], 'old.docx')]
    const previousKey = h.state.fileInputKey.value

    h.state.openImportDialog()

    expect(h.selectedImportTopicId.value).toBe('7')
    expect(h.state.wordFiles.value).toEqual([])
    expect(h.state.fileInputKey.value).toBe(previousKey + 1)
    expect(h.state.importDialogVisible.value).toBe(true)
  })

  it('refuses to open without a teacher subject or available topics', () => {
    const h = createHarness()
    h.selectedMembership.value = null

    h.state.openImportDialog()

    expect(h.state.importDialogVisible.value).toBe(false)
    expect(h.notice.value).toEqual({
      type: 'danger',
      message: 'Выберите предмет с доступными темами.',
    })
  })

  it('validates topic and file before starting import', async () => {
    const h = createHarness()

    await h.state.importWord()

    expect(importFile).not.toHaveBeenCalled()
    expect(h.state.importError.value).toBe('Выберите тему и файл .docx.')
  })

  it('imports into the selected topic and reloads the visible list when contexts match', async () => {
    const h = createHarness()
    const file = new File(['questions'], 'questions.docx')
    h.selectedImportTopicId.value = '7'
    h.state.onWordFiles([file])

    await h.state.importWord()

    expect(h.ensureSelectedMembershipActive).toHaveBeenCalledTimes(1)
    expect(importFile).toHaveBeenCalledWith(file, { topicId: 7 })
    expect(h.loadQuestions).toHaveBeenCalledTimes(1)
    expect(h.notice.value).toEqual({
      type: 'success',
      message: 'Импортировано вопросов: 2. Вариантов ответа: 5.',
    })
    expect(h.state.importDialogVisible.value).toBe(false)
    expect(h.state.wordFiles.value).toEqual([])
    expect(h.state.importing.value).toBe(false)
  })

  it('does not reload the visible list when importing into another topic', async () => {
    const h = createHarness()
    h.selectedImportTopicId.value = '8'
    h.state.onWordFiles([new File(['questions'], 'questions.docx')])

    await h.state.importWord()

    expect(h.loadQuestions).not.toHaveBeenCalled()
  })

  it('keeps the dialog open and exposes the mapped API error on failure', async () => {
    const h = createHarness()
    h.selectedImportTopicId.value = '7'
    h.state.openImportDialog()
    h.state.onWordFiles([new File(['questions'], 'questions.docx')])
    importFile.mockRejectedValue(new Error('boom'))

    await h.state.importWord()

    expect(getApiErrorMessage).toHaveBeenCalledWith(
      expect.any(Error),
      'Не удалось импортировать вопросы'
    )
    expect(h.state.importError.value).toBe('Import failed')
    expect(h.state.importDialogVisible.value).toBe(true)
    expect(h.state.importing.value).toBe(false)
  })

  it('does not close the dialog while import is in progress', () => {
    const h = createHarness()
    h.state.importDialogVisible.value = true
    h.state.importing.value = true

    h.state.closeImportDialog()

    expect(h.state.importDialogVisible.value).toBe(true)
  })
})
