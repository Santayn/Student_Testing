import {
  computed,
  ref,
} from 'vue'

import {
  databaseBackupsApi,
} from '@/api'

import {
  databaseBackupFileName,
  downloadDatabaseBackupBlob,
  normalizeDatabaseBackupError,
} from '@/utils/databaseBackup'

export function useDatabaseBackupDownload({
  api = databaseBackupsApi,
  download =
    downloadDatabaseBackupBlob,
} = {}) {
  const creating = ref(false)
  const failure = ref(null)
  const lastFileName = ref('')

  const errorMessage = computed(
    () => failure.value?.message ?? ''
  )

  async function createBackup() {
    if (creating.value) {
      return false
    }

    creating.value = true
    failure.value = null

    try {
      const response =
        await api.create()

      const fileName =
        databaseBackupFileName(
          response
        )

      download(
        response.data,
        fileName
      )

      lastFileName.value =
        fileName

      return {
        fileName,
        blob: response.data,
      }
    } catch (error) {
      failure.value =
        await normalizeDatabaseBackupError(
          error,
          'Не удалось создать резервную копию'
        )

      return false
    } finally {
      creating.value = false
    }
  }

  function clearFailure() {
    failure.value = null
  }

  return {
    creating,
    failure,
    errorMessage,
    lastFileName,
    createBackup,
    clearFailure,
  }
}
