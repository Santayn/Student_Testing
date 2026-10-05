export function useLectureDrawerWorkspace({
  canEdit,
  saving,
  lectureDirty,
  pendingFilesDirty,
  partialCreatePending,
  confirmCloseVisible,
  openCreate,
  openEdit,
  closeImmediately,
  discardAndClose,
  clearLectureDrawerState,
  loadMaterials,
  notice,
}) {
  function openCreateLecture() {
    if (!canEdit.value) {
      notice.value = {
        type: 'danger',
        message: 'Выберите предмет преподавателя.',
      }
      return false
    }

    clearLectureDrawerState()
    openCreate()
    return true
  }

  async function openEditLecture(lecture) {
    clearLectureDrawerState()
    openEdit(lecture)
    await loadMaterials(lecture.id)
  }

  function requestLectureDrawerClose() {
    if (saving.value) {
      return false
    }

    if (
      lectureDirty.value ||
      pendingFilesDirty.value ||
      partialCreatePending.value
    ) {
      confirmCloseVisible.value = true
      return false
    }

    closeImmediately()
    clearLectureDrawerState()
    return true
  }

  function handleLectureDrawerVisibility(nextValue) {
    if (nextValue) {
      return true
    }

    return requestLectureDrawerClose()
  }

  function discardLectureDrawer() {
    discardAndClose()
    clearLectureDrawerState()
  }

  function closeLectureDrawerImmediately() {
    closeImmediately()
    clearLectureDrawerState()
  }

  return {
    openCreateLecture,
    openEditLecture,
    requestLectureDrawerClose,
    handleLectureDrawerVisibility,
    discardLectureDrawer,
    closeLectureDrawerImmediately,
  }
}
