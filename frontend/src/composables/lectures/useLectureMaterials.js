import {
  computed,
  ref,
  unref,
} from 'vue'

import {
  listFromResponse,
} from '@/utils/apiData'

import {
  createLatestRequestGuard,
} from '@/utils/latestRequest'

/**
 * Owns lecture material state and material-only API operations.
 * Pending files stay shared with useLectureSaveFlow so a partial create retry
 * can continue uploading the same files without creating the lecture again.
 */
export function useLectureMaterials({
  lectureId,
  lecturesApi,
  ensureSelectedMembershipActive,
  formError,
  getApiErrorMessage,
}) {
  const materials = ref([])
  const pendingFiles = ref([])
  const fileInputKey = ref(0)
  const loadingMaterials = ref(false)

  const deleteTarget = ref(null)
  const deleteConfirmVisible = ref(false)
  const deletingMaterialId = ref(null)
  const deleteError = ref('')

  const materialsRequest = createLatestRequestGuard()

  const pendingFilesDirty = computed(() => pendingFiles.value.length > 0)

  function currentLectureId() {
    return Number(unref(lectureId) ?? 0)
  }

  function setPendingFiles(files) {
    pendingFiles.value = Array.isArray(files)
      ? files
      : Array.from(files ?? [])
  }

  function removePendingFile(index) {
    pendingFiles.value = pendingFiles.value.filter(
      (_, itemIndex) => itemIndex !== index
    )
  }

  function requestDeleteMaterial(material) {
    deleteTarget.value = material
    deleteError.value = ''
    deleteConfirmVisible.value = true
  }

  function closeDeleteDialog({ force = false } = {}) {
    if (!force && deletingMaterialId.value !== null) {
      return
    }

    deleteConfirmVisible.value = false
    deleteTarget.value = null
    deleteError.value = ''
  }

  function resetMaterials() {
    materialsRequest.invalidate()
    materials.value = []
    pendingFiles.value = []
    loadingMaterials.value = false
    fileInputKey.value += 1
    closeDeleteDialog()
  }

  async function loadMaterials(lectureIdOverride = currentLectureId()) {
    const requestId = materialsRequest.begin()
    const id = Number(lectureIdOverride ?? 0)

    materials.value = []

    if (!id) {
      loadingMaterials.value = false
      return
    }

    loadingMaterials.value = true

    try {
      const response = await lecturesApi.getMaterials(id)

      if (!materialsRequest.isCurrent(requestId)) {
        return
      }

      materials.value = listFromResponse(response)
    } catch (error) {
      if (!materialsRequest.isCurrent(requestId)) {
        return
      }

      formError.value = getApiErrorMessage(
        error,
        'Не удалось загрузить материалы лекции'
      )
    } finally {
      if (materialsRequest.isCurrent(requestId)) {
        loadingMaterials.value = false
      }
    }
  }

  async function deleteMaterial() {
    const id = currentLectureId()
    const material = deleteTarget.value

    if (!id || !material || deletingMaterialId.value !== null) {
      return
    }

    deletingMaterialId.value = material.id
    deleteError.value = ''

    try {
      await ensureSelectedMembershipActive()
      await lecturesApi.removeMaterial(id, material.id)

      materials.value = materials.value.filter(
        (item) => Number(item.id) !== Number(material.id)
      )

      deleteConfirmVisible.value = false
      deleteTarget.value = null
    } catch (error) {
      deleteError.value = getApiErrorMessage(
        error,
        'Не удалось удалить материал'
      )
    } finally {
      deletingMaterialId.value = null
    }
  }

  async function downloadMaterial(material) {
    const id = currentLectureId()

    if (!id || !material) {
      return
    }

    try {
      const response = await lecturesApi.downloadMaterial(
        id,
        material.id
      )

      const url = URL.createObjectURL(response.data)
      const anchor = document.createElement('a')

      anchor.href = url
      anchor.download = material.fileName || `material-${material.id}`
      anchor.click()
      URL.revokeObjectURL(url)
    } catch (error) {
      formError.value = getApiErrorMessage(
        error,
        'Не удалось скачать материал'
      )
    }
  }

  return {
    materials,
    pendingFiles,
    fileInputKey,
    loadingMaterials,
    pendingFilesDirty,
    materialDeleteTarget: deleteTarget,
    materialDeleteConfirmVisible: deleteConfirmVisible,
    deletingMaterialId,
    materialDeleteError: deleteError,
    setPendingFiles,
    removePendingFile,
    requestDeleteMaterial,
    closeMaterialDeleteDialog: closeDeleteDialog,
    resetMaterials,
    loadMaterials,
    deleteMaterial,
    downloadMaterial,
  }
}
