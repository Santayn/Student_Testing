import { reactive, ref } from 'vue'
import { describe, expect, it, vi } from 'vitest'

import { useLectureDelete } from '@/composables/lectures/useLectureDelete'

function createState(overrides = {}) {
  const remove = vi.fn().mockResolvedValue({})
  const ensureSelectedMembershipActive = vi.fn().mockResolvedValue(undefined)
  const closeLectureDrawerImmediately = vi.fn()
  const loadLectures = vi.fn().mockResolvedValue(undefined)
  const getApiErrorMessage = vi.fn((error, fallback) => error?.message || fallback)

  const deps = {
    form: reactive({ id: null }),
    lecturesApi: { remove },
    ensureSelectedMembershipActive,
    closeLectureDrawerImmediately,
    loadLectures,
    notice: ref({ type: 'info', message: '' }),
    getApiErrorMessage,
    ...overrides,
  }

  return {
    deps,
    state: useLectureDelete(deps),
    remove,
    ensureSelectedMembershipActive,
    closeLectureDrawerImmediately,
    loadLectures,
  }
}

describe('lecture delete flow', () => {
  it('revalidates membership before delete and refreshes the list', async () => {
    const { deps, state, remove, ensureSelectedMembershipActive, loadLectures } = createState()
    const lecture = { id: 12, title: 'Удалить' }

    state.requestDeleteLecture(lecture)
    expect(state.deleteConfirmVisible.value).toBe(true)

    await expect(state.deleteLecture()).resolves.toBe(true)

    expect(ensureSelectedMembershipActive).toHaveBeenCalledTimes(1)
    expect(remove).toHaveBeenCalledWith(12)
    expect(loadLectures).toHaveBeenCalledTimes(1)
    expect(deps.notice.value).toEqual({
      type: 'success',
      message: 'Лекция удалена.',
    })
    expect(state.deleteConfirmVisible.value).toBe(false)
    expect(state.deleteTarget.value).toBeNull()
  })

  it('closes the editor when the currently edited lecture is deleted', async () => {
    const form = reactive({ id: 7 })
    const { state, closeLectureDrawerImmediately } = createState({ form })

    state.requestDeleteLecture({ id: 7, title: 'Текущая' })
    await state.deleteLecture()

    expect(closeLectureDrawerImmediately).toHaveBeenCalledTimes(1)
  })

  it('keeps the dialog open and exposes an API error when delete fails', async () => {
    const remove = vi.fn().mockRejectedValue(new Error('delete failed'))
    const { state, loadLectures } = createState({ lecturesApi: { remove } })

    state.requestDeleteLecture({ id: 3, title: 'Ошибка' })
    await expect(state.deleteLecture()).resolves.toBe(false)

    expect(state.deleteConfirmVisible.value).toBe(true)
    expect(state.deleteTarget.value?.id).toBe(3)
    expect(state.deleteError.value).toBe('delete failed')
    expect(loadLectures).not.toHaveBeenCalled()
    expect(state.deletingId.value).toBeNull()
  })
})
