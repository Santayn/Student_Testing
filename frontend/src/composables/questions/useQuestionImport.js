import { ref } from 'vue'

import {
  getApiErrorMessage,
  questionsApi,
} from '@/api'

export function useQuestionImport({
  selectedMembership,
  topicOptions,
  selectedTopicId,
  selectedImportTopicId,
  ensureSelectedMembershipActive,
  loadQuestions,
  notice,
}) {
  const importing = ref(false)
  const importDialogVisible = ref(false)
  const importError = ref('')
  const wordFiles = ref([])
  const fileInputKey = ref(0)

  function openImportDialog() {
    if (!selectedMembership.value || !topicOptions.value.length) {
      notice.value = {
        type: 'danger',
        message: 'Выберите предмет с доступными темами.',
      }
      return
    }

    selectedImportTopicId.value =
      selectedTopicId.value ||
      String(topicOptions.value[0]?.value ?? '')
    wordFiles.value = []
    fileInputKey.value += 1
    importError.value = ''
    importDialogVisible.value = true
  }

  function closeImportDialog() {
    if (importing.value) {
      return
    }

    importDialogVisible.value = false
    importError.value = ''
    wordFiles.value = []
  }

  function onWordFiles(files) {
    wordFiles.value = files
    importError.value = ''
  }

  async function importWord() {
    const file = wordFiles.value[0]

    if (!selectedImportTopicId.value || !file) {
      importError.value = 'Выберите тему и файл .docx.'
      return
    }

    importing.value = true
    importError.value = ''

    try {
      await ensureSelectedMembershipActive()

      const response = await questionsApi.importFile(
        file,
        {
          topicId: Number(selectedImportTopicId.value),
        }
      )

      const payload = response.data ?? {}

      notice.value = {
        type: 'success',
        message:
          `Импортировано вопросов: ${payload.importedQuestions ?? 0}. ` +
          `Вариантов ответа: ${payload.importedOptions ?? 0}.`,
      }

      importDialogVisible.value = false
      wordFiles.value = []
      fileInputKey.value += 1

      if (
        String(selectedTopicId.value) ===
        String(selectedImportTopicId.value)
      ) {
        await loadQuestions()
      }
    } catch (error) {
      importError.value = getApiErrorMessage(
        error,
        'Не удалось импортировать вопросы'
      )
    } finally {
      importing.value = false
    }
  }

  return {
    importing,
    importDialogVisible,
    importError,
    wordFiles,
    fileInputKey,
    openImportDialog,
    closeImportDialog,
    onWordFiles,
    importWord,
  }
}
