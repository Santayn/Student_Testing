import { ref } from 'vue'
import { describe, expect, it, vi } from 'vitest'

import { useLectureDrawerWorkspace } from '@/composables/lectures/useLectureDrawerWorkspace'

function createState(overrides = {}) {
  const deps = {
    canEdit: ref(true),
    saving: ref(false),
    lectureDirty: ref(false),
    pendingFilesDirty: ref(false),
    partialCreatePending: ref(false),
    confirmCloseVisible: ref(false),
    openCreate: vi.fn(),
    openEdit: vi.fn(),
    closeImmediately: vi.fn(),
    discardAndClose: vi.fn(),
    clearLectureDrawerState: vi.fn(),
    loadMaterials: vi.fn().mockResolvedValue(undefined),
    notice: ref({ type: 'info', message: '' }),
    ...overrides,
  }

  return {
    deps,
    state: useLectureDrawerWorkspace(deps),
  }
}

describe('lecture drawer workspace', () => {
  it('opens create only when a teacher membership is selected', () => {
    const { deps, state } = createState({ canEdit: ref(false) })

    expect(state.openCreateLecture()).toBe(false)
    expect(deps.openCreate).not.toHaveBeenCalled()
    expect(deps.notice.value.message).toContain('Выберите предмет')

    deps.canEdit.value = true
    expect(state.openCreateLecture()).toBe(true)
    expect(deps.clearLectureDrawerState).toHaveBeenCalledTimes(1)
    expect(deps.openCreate).toHaveBeenCalledTimes(1)
  })

  it('loads materials after opening an existing lecture', async () => {
    const { deps, state } = createState()
    const lecture = { id: 17, title: 'Лекция' }

    await state.openEditLecture(lecture)

    expect(deps.clearLectureDrawerState).toHaveBeenCalledTimes(1)
    expect(deps.openEdit).toHaveBeenCalledWith(lecture)
    expect(deps.loadMaterials).toHaveBeenCalledWith(17)
  })

  it('protects dirty, pending-file and partial-create state from accidental close', () => {
    const lectureDirty = ref(true)
    const { deps, state } = createState({ lectureDirty })

    expect(state.requestLectureDrawerClose()).toBe(false)
    expect(deps.confirmCloseVisible.value).toBe(true)
    expect(deps.closeImmediately).not.toHaveBeenCalled()

    lectureDirty.value = false
    deps.confirmCloseVisible.value = false
    deps.pendingFilesDirty.value = true
    expect(state.requestLectureDrawerClose()).toBe(false)
    expect(deps.confirmCloseVisible.value).toBe(true)

    deps.pendingFilesDirty.value = false
    deps.confirmCloseVisible.value = false
    deps.partialCreatePending.value = true
    expect(state.requestLectureDrawerClose()).toBe(false)
    expect(deps.confirmCloseVisible.value).toBe(true)
  })

  it('closes and clears a clean editor', () => {
    const { deps, state } = createState()

    expect(state.requestLectureDrawerClose()).toBe(true)
    expect(deps.closeImmediately).toHaveBeenCalledTimes(1)
    expect(deps.clearLectureDrawerState).toHaveBeenCalledTimes(1)
  })
})
