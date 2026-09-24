import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'
import { reactive, ref } from 'vue'
import { useLectureSaveFlow } from '@/composables/useLectureSaveFlow'

function createHarness({ existingLecture = null, files = [] } = {}) {
  const form = reactive({
    id: existingLecture?.id ?? null,
    title: existingLecture?.title ?? 'Первая лекция',
    description: existingLecture?.description ?? '',
    publicVisible: true,
    testIds: [3, 3, '4', 0],
  })
  const formError = ref('')
  const lectureFormMode = ref(existingLecture ? 'edit' : 'create')
  const lectures = ref(existingLecture ? [existingLecture] : [])
  const lectureTestsById = ref(new Map())
  const pendingFiles = ref(files)
  const fileInputKey = ref(0)
  const notice = ref({ type: 'info', message: '' })

  const api = {
    create: vi.fn(async (payload) => ({ data: { id: 100, ...payload } })),
    update: vi.fn(async (id, payload) => ({ data: { id, ...payload } })),
    setTests: vi.fn(async () => ({ data: [{ id: 3 }, { id: 4 }] })),
    uploadMaterials: vi.fn(async () => ({ data: [] })),
  }
  const ensureSelectedMembershipActive = vi.fn(async () => undefined)
  const beginSaving = vi.fn()
  const finishSaving = vi.fn()
  const failSaving = vi.fn()
  const loadLectures = vi.fn(async () => undefined)
  const clearLectureDrawerState = vi.fn()

  const flow = useLectureSaveFlow({
    form,
    formError,
    lectureFormMode,
    selectedSubject: ref({ id: 20 }),
    selectedMembership: ref({ id: 10 }),
    ensureSelectedMembershipActive,
    lectures,
    lectureTestsById,
    pendingFiles,
    fileInputKey,
    notice,
    beginSaving,
    finishSaving,
    failSaving,
    loadLectures,
    clearLectureDrawerState,
    lecturesApi: api,
    getApiErrorMessage: (_error, fallback) => fallback,
  })

  return {
    ...flow,
    form,
    formError,
    lectureFormMode,
    lectures,
    lectureTestsById,
    pendingFiles,
    fileInputKey,
    notice,
    api,
    ensureSelectedMembershipActive,
    beginSaving,
    finishSaving,
    failSaving,
    loadLectures,
    clearLectureDrawerState,
  }
}

describe('lecture save flow', () => {
  beforeEach(() => vi.clearAllMocks())

  it('creates the lecture once, records its id, and saves dependent data', async () => {
    const file = { name: 'notes.pdf' }
    const h = createHarness({ files: [file] })

    await h.saveLecture()

    expect(h.ensureSelectedMembershipActive).toHaveBeenCalledTimes(1)
    expect(h.api.create).toHaveBeenCalledTimes(1)
    expect(h.api.update).not.toHaveBeenCalled()
    expect(h.form.id).toBe(100)
    expect(h.lectureFormMode.value).toBe('edit')
    expect(h.api.setTests).toHaveBeenCalledWith(100, { testIds: [3, 4] })
    expect(h.api.uploadMaterials).toHaveBeenCalledWith(100, [file])
    expect(h.pendingFiles.value).toEqual([])
    expect(h.fileInputKey.value).toBe(1)
    expect(h.partialCreatePending.value).toBe(false)
    expect(h.notice.value.message).toBe('Лекция создана.')
    expect(h.finishSaving).toHaveBeenCalledWith({ close: true })
    expect(h.loadLectures).toHaveBeenCalledTimes(1)
  })

  it('does not issue a second POST or an unnecessary PUT after test-link failure', async () => {
    const h = createHarness()
    h.api.setTests
      .mockRejectedValueOnce(new Error('links unavailable'))
      .mockResolvedValue({ data: [] })

    await h.saveLecture()

    expect(h.form.id).toBe(100)
    expect(h.lectureFormMode.value).toBe('edit')
    expect(h.partialCreatePending.value).toBe(true)
    expect(h.formError.value).toContain('Лекция уже создана')
    expect(h.failSaving).toHaveBeenCalledTimes(1)
    expect(h.api.uploadMaterials).not.toHaveBeenCalled()

    await h.saveLecture()

    expect(h.api.create).toHaveBeenCalledTimes(1)
    expect(h.api.update).not.toHaveBeenCalled()
    expect(h.api.setTests).toHaveBeenCalledTimes(2)
    expect(h.partialCreatePending.value).toBe(false)
    expect(h.notice.value.message).toBe('Лекция создана.')
  })

  it('keeps pending files after upload failure and retries on the same lecture', async () => {
    const file = { name: 'notes.pdf' }
    const h = createHarness({ files: [file] })
    h.api.uploadMaterials
      .mockRejectedValueOnce(new Error('upload unavailable'))
      .mockResolvedValue({ data: [] })

    await h.saveLecture()

    expect(h.form.id).toBe(100)
    expect(h.partialCreatePending.value).toBe(true)
    expect(h.pendingFiles.value).toEqual([file])
    expect(h.fileInputKey.value).toBe(0)
    expect(h.lectureTestsById.value.get(100)).toEqual([{ id: 3 }, { id: 4 }])

    await h.saveLecture()

    expect(h.api.create).toHaveBeenCalledTimes(1)
    expect(h.api.update).not.toHaveBeenCalled()
    expect(h.api.uploadMaterials).toHaveBeenCalledTimes(2)
    expect(h.api.uploadMaterials).toHaveBeenLastCalledWith(100, [file])
    expect(h.pendingFiles.value).toEqual([])
    expect(h.fileInputKey.value).toBe(1)
    expect(h.partialCreatePending.value).toBe(false)
  })

  it('updates the existing id when core fields change during partial-create retry', async () => {
    const h = createHarness()
    h.api.setTests
      .mockRejectedValueOnce(new Error('links unavailable'))
      .mockResolvedValue({ data: [] })

    await h.saveLecture()
    h.form.title = 'Исправленное название'
    await h.saveLecture()

    expect(h.api.create).toHaveBeenCalledTimes(1)
    expect(h.api.update).toHaveBeenCalledTimes(1)
    expect(h.api.update).toHaveBeenCalledWith(
      100,
      expect.objectContaining({ title: 'Исправленное название' })
    )
    expect(h.notice.value.message).toBe('Лекция создана.')
  })

  it('updates an existing lecture without switching into partial-create mode', async () => {
    const h = createHarness({ existingLecture: {
      id: 27,
      ordinal: 5,
      title: 'Было',
      contentFolderKey: 'existing-folder',
    } })
    h.form.title = 'Стало'

    await h.saveLecture()

    expect(h.api.create).not.toHaveBeenCalled()
    expect(h.api.update).toHaveBeenCalledWith(
      27,
      expect.objectContaining({
        ordinal: 5,
        title: 'Стало',
        contentFolderKey: 'existing-folder',
      })
    )
    expect(h.notice.value.message).toBe('Лекция обновлена.')
    expect(h.partialCreatePending.value).toBe(false)
  })

  it('revalidates the teacher membership before performing any mutation', async () => {
    const h = createHarness()
    h.ensureSelectedMembershipActive.mockRejectedValue(new Error('revoked'))

    await h.saveLecture()

    expect(h.api.create).not.toHaveBeenCalled()
    expect(h.api.update).not.toHaveBeenCalled()
    expect(h.api.setTests).not.toHaveBeenCalled()
    expect(h.form.id).toBeNull()
    expect(h.formError.value).toBe('Не удалось создать лекцию')
    expect(h.failSaving).toHaveBeenCalledTimes(1)
  })
})
