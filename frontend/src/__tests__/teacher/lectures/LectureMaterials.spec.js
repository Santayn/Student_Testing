import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'
import {
  computed,
  ref,
} from 'vue'

import {
  useLectureMaterials,
} from '@/composables/lectures/useLectureMaterials'

function deferred() {
  let resolve
  let reject
  const promise = new Promise((res, rej) => {
    resolve = res
    reject = rej
  })
  return { promise, resolve, reject }
}

function createHarness() {
  const lectureId = ref(10)
  const formError = ref('')
  const ensureSelectedMembershipActive = vi.fn(async () => undefined)
  const api = {
    getMaterials: vi.fn(async () => ({ data: [] })),
    removeMaterial: vi.fn(async () => ({ data: null })),
    downloadMaterial: vi.fn(async () => ({ data: new Blob(['file']) })),
  }

  const materials = useLectureMaterials({
    lectureId: computed(() => lectureId.value),
    lecturesApi: api,
    ensureSelectedMembershipActive,
    formError,
    getApiErrorMessage: (_error, fallback) => fallback,
  })

  return {
    lectureId,
    formError,
    ensureSelectedMembershipActive,
    api,
    ...materials,
  }
}

describe('lecture materials flow', () => {
  beforeEach(() => vi.restoreAllMocks())

  it('tracks pending files independently and resets the material editor state', () => {
    const h = createHarness()
    const first = { name: 'first.pdf' }
    const second = { name: 'second.pdf' }

    h.setPendingFiles([first, second])
    expect(h.pendingFilesDirty.value).toBe(true)

    h.removePendingFile(0)
    expect(h.pendingFiles.value).toEqual([second])

    const previousKey = h.fileInputKey.value
    h.resetMaterials()

    expect(h.pendingFiles.value).toEqual([])
    expect(h.materials.value).toEqual([])
    expect(h.pendingFilesDirty.value).toBe(false)
    expect(h.fileInputKey.value).toBe(previousKey + 1)
  })

  it('ignores a late material response after a newer lecture load starts', async () => {
    const h = createHarness()
    const first = deferred()

    h.api.getMaterials.mockImplementation((id) => {
      if (id === 1) return first.promise
      return Promise.resolve({ data: [{ id: 22, fileName: 'new.pdf' }] })
    })

    const staleLoad = h.loadMaterials(1)
    await h.loadMaterials(2)
    first.resolve({ data: [{ id: 11, fileName: 'old.pdf' }] })
    await staleLoad

    expect(h.materials.value).toEqual([
      { id: 22, fileName: 'new.pdf' },
    ])
  })

  it('revalidates teacher membership before deleting a material', async () => {
    const h = createHarness()
    h.materials.value = [
      { id: 7, fileName: 'notes.pdf' },
      { id: 8, fileName: 'slides.pdf' },
    ]
    h.requestDeleteMaterial(h.materials.value[0])

    await h.deleteMaterial()

    expect(h.ensureSelectedMembershipActive).toHaveBeenCalledTimes(1)
    expect(h.api.removeMaterial).toHaveBeenCalledWith(10, 7)
    expect(h.materials.value).toEqual([
      { id: 8, fileName: 'slides.pdf' },
    ])
    expect(h.materialDeleteConfirmVisible.value).toBe(false)
  })

  it('keeps the delete dialog open and exposes a user-facing error on failure', async () => {
    const h = createHarness()
    const material = { id: 7, fileName: 'notes.pdf' }
    h.materials.value = [material]
    h.api.removeMaterial.mockRejectedValue(new Error('offline'))
    h.requestDeleteMaterial(material)

    await h.deleteMaterial()

    expect(h.materials.value).toEqual([material])
    expect(h.materialDeleteConfirmVisible.value).toBe(true)
    expect(h.materialDeleteError.value).toBe('Не удалось удалить материал')
    expect(h.deletingMaterialId.value).toBeNull()
  })

  it('does not request materials or mutations without a lecture id', async () => {
    const h = createHarness()
    h.lectureId.value = null
    h.requestDeleteMaterial({ id: 1 })

    await h.loadMaterials()
    await h.deleteMaterial()
    await h.downloadMaterial({ id: 1 })

    expect(h.api.getMaterials).not.toHaveBeenCalled()
    expect(h.api.removeMaterial).not.toHaveBeenCalled()
    expect(h.api.downloadMaterial).not.toHaveBeenCalled()
  })
})
