import {
  ensureMatchingPairRows,
} from '@/utils/matchingPairs'

export function useQuestionDrawerWorkspace({
  canCreateQuestion,
  questions,
  form,
  formError,
  savingQuestion,
  savingOption,
  questionDirty,
  optionDraftDirty,
  confirmCloseVisible,
  openCreate,
  openEdit,
  closeImmediately,
  discardAndClose,
  resetOptionForm,
  clearOptions,
  loadOptions,
  notice,
} = {}) {
  function nextQuestionOrdinal() {
    return questions.value.reduce(
      (max, question) =>
        Math.max(max, Number(question.ordinal ?? 0)),
      0
    ) + 1
  }

  function clearQuestionDrawerState() {
    formError.value = ''
    clearOptions()
  }

  function openCreateQuestion() {
    if (!canCreateQuestion.value) {
      notice.value = {
        type: 'danger',
        message: 'Выберите предмет и тему.',
      }
      return
    }

    clearQuestionDrawerState()
    openCreate({
      ordinal: nextQuestionOrdinal(),
    })
  }

  async function editQuestion(question) {
    clearQuestionDrawerState()
    openEdit(question)

    if (
      Number(question.type) === 1 ||
      Number(question.type) === 2
    ) {
      await loadOptions(question.id)
    }
  }

  function requestQuestionDrawerClose() {
    if (savingQuestion.value || savingOption.value) {
      return false
    }

    if (questionDirty.value || optionDraftDirty.value) {
      confirmCloseVisible.value = true
      return false
    }

    closeImmediately()
    clearQuestionDrawerState()
    return true
  }

  function handleQuestionDrawerVisibility(nextValue) {
    if (nextValue) {
      return
    }

    requestQuestionDrawerClose()
  }

  function discardQuestionDrawer() {
    resetOptionForm()
    discardAndClose()
    clearQuestionDrawerState()
  }

  function closeQuestionDrawerImmediately() {
    closeImmediately()
    clearQuestionDrawerState()
  }

  function handleQuestionTypeChange(value) {
    if (Number(value) === 3) {
      form.matchingPairs = ensureMatchingPairRows(
        form.matchingPairs,
        2
      )
    }

    if (!form.id) {
      clearOptions()
      return
    }

    if (Number(value) === 1 || Number(value) === 2) {
      loadOptions(form.id)
    } else {
      clearOptions()
    }
  }

  return {
    openCreateQuestion,
    editQuestion,
    requestQuestionDrawerClose,
    handleQuestionDrawerVisibility,
    discardQuestionDrawer,
    closeQuestionDrawerImmediately,
    handleQuestionTypeChange,
  }
}
