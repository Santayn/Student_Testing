import {
  computed,
  ref,
} from 'vue'

import {
  databaseBackupsApi,
} from '@/api'

import {
  normalizeDatabaseBackupError,
  normalizeDatabaseRestoreResult,
  validateDatabaseBackupFile,
} from '@/utils/databaseBackup'

export function useDatabaseRestore({
  api = databaseBackupsApi,
} = {}) {
  const file = ref(null)
  const fileError = ref('')
  const restoring = ref(false)
  const failure = ref(null)
  const result = ref(null)

  const selectedFileName =
    computed(
      () => file.value?.name ?? ''
    )

  const selectedFileSizeBytes =
    computed(() => {
      const size = Number(
        file.value?.size
      )

      return Number.isFinite(size)
        ? size
        : null
    })

  const canRestore = computed(
    () =>
      Boolean(file.value) &&
      !fileError.value &&
      !restoring.value
  )

  const errorMessage = computed(
    () => failure.value?.message ?? ''
  )

  function setFile(candidate) {
    file.value = candidate ?? null
    result.value = null
    failure.value = null

    const validation =
      validateDatabaseBackupFile(
        file.value
      )

    fileError.value =
      validation.valid
        ? ''
        : validation.message

    return validation
  }

  function setFiles(files) {
    return setFile(
      Array.from(files ?? [])[0] ??
        null
    )
  }

  function clearFile() {
    file.value = null
    fileError.value = ''
    failure.value = null
    result.value = null
  }

  function clearFailure() {
    failure.value = null
  }

  async function restoreBackup() {
    if (restoring.value) {
      return false
    }

    const validation =
      validateDatabaseBackupFile(
        file.value
      )

    if (!validation.valid) {
      fileError.value =
        validation.message
      return false
    }

    restoring.value = true
    failure.value = null
    result.value = null

    try {
      const response =
        await api.restore(
          file.value
        )

      result.value =
        normalizeDatabaseRestoreResult(
          response.data
        )

      return result.value
    } catch (error) {
      failure.value =
        await normalizeDatabaseBackupError(
          error,
          'Не удалось восстановить базу из резервной копии'
        )

      return false
    } finally {
      restoring.value = false
    }
  }

  return {
    file,
    fileError,
    restoring,
    failure,
    result,
    selectedFileName,
    selectedFileSizeBytes,
    canRestore,
    errorMessage,
    setFile,
    setFiles,
    clearFile,
    clearFailure,
    restoreBackup,
  }
}
