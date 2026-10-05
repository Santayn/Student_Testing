import { ref } from 'vue'

export function useLectureDelete({
  form,
  lecturesApi,
  ensureSelectedMembershipActive,
  closeLectureDrawerImmediately,
  loadLectures,
  notice,
  getApiErrorMessage,
}) {
  const deleteTarget = ref(null)
  const deleteConfirmVisible = ref(false)
  const deletingId = ref(null)
  const deleteError = ref('')

  function requestDeleteLecture(lecture) {
    deleteTarget.value = lecture
    deleteError.value = ''
    deleteConfirmVisible.value = true
  }

  function closeDeleteDialog() {
    if (deletingId.value !== null) {
      return false
    }

    deleteConfirmVisible.value = false
    deleteTarget.value = null
    deleteError.value = ''
    return true
  }

  async function deleteLecture() {
    const lecture = deleteTarget.value

    if (!lecture || deletingId.value !== null) {
      return false
    }

    deletingId.value = lecture.id
    deleteError.value = ''

    try {
      await ensureSelectedMembershipActive()
      await lecturesApi.remove(lecture.id)

      if (Number(form.id) === Number(lecture.id)) {
        closeLectureDrawerImmediately()
      }

      notice.value = {
        type: 'success',
        message: 'Лекция удалена.',
      }

      deleteConfirmVisible.value = false
      deleteTarget.value = null
      await loadLectures()
      return true
    } catch (error) {
      deleteError.value = getApiErrorMessage(
        error,
        'Не удалось удалить лекцию'
      )
      return false
    } finally {
      deletingId.value = null
    }
  }

  return {
    deleteTarget,
    deleteConfirmVisible,
    deletingId,
    deleteError,
    requestDeleteLecture,
    closeDeleteDialog,
    deleteLecture,
  }
}
