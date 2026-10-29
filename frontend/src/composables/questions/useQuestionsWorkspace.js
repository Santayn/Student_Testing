import {
  computed,
  onMounted,
  reactive,
  ref,
  watch,
} from 'vue'

import {
  useRoute,
} from 'vue-router'

import {
  getApiErrorMessage,
} from '@/api'

import {
  useQuestionEditorState,
} from '@/composables/questions/useQuestionEditorState'

import {
  useQuestionImport,
} from '@/composables/questions/useQuestionImport'

import {
  useQuestionMutations,
} from '@/composables/questions/useQuestionMutations'

import {
  useQuestionDrawerWorkspace,
} from '@/composables/questions/useQuestionDrawerWorkspace'

import {
  useQuestionFilters,
} from '@/composables/questions/useQuestionFilters'

import {
  useQuestionOptions,
} from '@/composables/questions/useQuestionOptions'

import {
  useQuestionTopicContext,
} from '@/composables/questions/useQuestionTopicContext'

import {
  useQuestionsList,
} from '@/composables/questions/useQuestionsList'

import {
  useTeacherSubjects,
} from '@/composables/teacher/useTeacherSubjects'

import {
  normalizeMatchingPairs,
} from '@/utils/matchingPairs'

import {
  focusFirstInvalidField,
} from '@/utils/formErrorLifecycle'

export function useQuestionsWorkspace() {
  const route = useRoute()

  const {
    loadingSubjects,
    selectedMembershipId,
    selectedSubjectId,
    selectedMembership,
    selectedSubject,
    membershipOptions,
    ensureSelectedMembershipActive,
    loadTeacherSubjects,
  } = useTeacherSubjects()

  const initialized = ref(false)
  const formError = ref('')
  const formFieldErrors = ref({})
  const questionFormElement = ref(null)

  const notice = ref({
    type: 'info',
    message: '',
  })

  const {
    selectedTopicId,
    selectedImportTopicId,
    topicOptions,
    currentTopic,
    loadTopics: loadTopicContext,
  } = useQuestionTopicContext({
    route,
    selectedMembership,
    notice,
  })

  const {
    questions,
    loading,
    loadQuestions,
    resetQuestions,
  } = useQuestionsList({
    selectedTopicId,
    notice,
  })

  const {
    importing,
    importDialogVisible,
    importError,
    wordFiles,
    fileInputKey,
    openImportDialog,
    closeImportDialog,
    onWordFiles,
    importWord,
  } = useQuestionImport({
    selectedMembership,
    topicOptions,
    selectedTopicId,
    selectedImportTopicId,
    ensureSelectedMembershipActive,
    loadQuestions,
    notice,
  })

  const questionTypeOptions = [
    {
      value: 1,
      label: 'Один вариант ответа',
    },
    {
      value: 2,
      label: 'Несколько вариантов ответа',
    },
    {
      value: 3,
      label: 'Соответствие колонок',
    },
    {
      value: 4,
      label: 'Текстовый ответ',
    },
  ]

  const questionTypeFilterOptions = [
    { value: 'all', label: 'Все типы' },
    ...questionTypeOptions,
  ]

  const statusOptions = [
    { value: 'all', label: 'Все статусы' },
    { value: 'active', label: 'Активные' },
    { value: 'hidden', label: 'Скрытые' },
  ]

  const sortOptions = [
    { value: 'ordinal', label: 'По порядку' },
    { value: 'points-desc', label: 'Сначала больше баллов' },
    { value: 'type', label: 'По типу вопроса' },
    { value: 'text', label: 'По тексту А–Я' },
  ]

  const {
    form,
    isOpen: questionDrawerOpen,
    isCreate,
    dirty: questionDirty,
    saving: savingQuestion,
    confirmCloseVisible,
    openCreate,
    openEdit,
    closeImmediately,
    discardAndClose,
    continueEditing,
    beginSaving,
    finishSaving,
    failSaving,
    isSelectableType,
    isMatchingType,
    isTextType,
    drawerTitle,
    validation: questionValidation,
    validationMessage: questionValidationMessage,
  } = useQuestionEditorState({
    selectedMembership,
    selectedTopicId,
  })

  const {
    options,
    loadingOptions,
    savingOption,
    optionForm,
    optionDraftDirty,
    resetOptionForm,
    editOption,
    clearOptions,
    loadOptions,
    saveOption,
  } = useQuestionOptions({
    form,
    isSelectableType,
    ensureSelectedMembershipActive,
    notice,
    formError,
  })

  const canCreateQuestion = computed(() => {
    return Boolean(
      selectedMembership.value &&
      selectedTopicId.value
    )
  })

  const contextHint = computed(() => {
    if (!selectedMembership.value) {
      return 'Выберите предмет преподавателя, затем тему для работы с банком вопросов.'
    }

    if (!selectedTopicId.value) {
      return selectedSubject.value
        ? `Предмет «${selectedSubject.value.name}». Выберите тему.`
        : 'Выберите тему.'
    }

    return currentTopic.value
      ? `Предмет «${selectedSubject.value?.name ?? ''}», тема «${currentTopic.value.name}».`
      : 'Открыта выбранная тема предмета.'
  })

  function questionTypeLabel(type) {
    switch (Number(type)) {
      case 1:
        return 'Один вариант'
      case 2:
        return 'Несколько вариантов'
      case 3:
        return 'Соответствие'
      case 4:
        return 'Текстовый ответ'
      default:
        return `Тип ${type}`
    }
  }

  function questionMatchingPairs(question) {
    return normalizeMatchingPairs(
      question?.matchingPairs
    )
  }

  function questionDisplayAnswer(question) {
    if (Number(question.type) === 3) {
      return ''
    }

    return question.correctAnswer || ''
  }

  const {
    searchQuery,
    typeFilter,
    statusFilter,
    sortMode,
    questionStats,
    hasActiveFilters,
    filteredQuestions,
    filterResultText,
    resetFilters,
  } = useQuestionFilters({
    questions,
    selectedTopicId,
    questionTypeLabel,
    questionDisplayAnswer,
  })

  function routeQuery(topicId = null) {
    const query = {}

    if (selectedSubjectId.value) {
      query.subjectId = selectedSubjectId.value
    }

    if (selectedMembership.value) {
      query.subjectMembershipId = selectedMembership.value.id
    }

    if (topicId || selectedTopicId.value) {
      query.topicId = topicId || selectedTopicId.value
    }

    return query
  }

  const {
    openCreateQuestion,
    editQuestion,
    requestQuestionDrawerClose,
    handleQuestionDrawerVisibility,
    discardQuestionDrawer,
    closeQuestionDrawerImmediately,
    handleQuestionTypeChange,
  } = useQuestionDrawerWorkspace({
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
  })

  async function loadTopics() {
    resetQuestions()
    clearOptions()
    closeQuestionDrawerImmediately()

    await loadTopicContext()
  }

  async function focusQuestionFormErrors() {
    await focusFirstInvalidField(
      questionFormElement.value
    )
  }

  const {
    togglingQuestionId,
    saveQuestion,
    toggleQuestionActive,
  } = useQuestionMutations({
    form,
    formError,
    selectedTopicId,
    questions,
    notice,
    questionValidation,
    questionValidationMessage,
    formFieldErrors,
    focusFormErrors:
      focusQuestionFormErrors,
    beginSaving,
    saving: savingQuestion,
    finishSaving,
    failSaving,
    ensureSelectedMembershipActive,
    loadQuestions,
    openEdit,
    loadOptions,
    clearOptions,
  })

  watch(
    selectedMembershipId,
    () => {
      if (!initialized.value) {
        return
      }

      closeQuestionDrawerImmediately()
      closeImportDialog()
      resetFilters()
      loadTopics()
    }
  )

  watch(
    selectedTopicId,
    async (value) => {
      selectedImportTopicId.value = value

      if (!initialized.value) {
        return
      }

      closeQuestionDrawerImmediately()
      resetFilters()
      await loadQuestions()
    }
  )

  onMounted(async () => {
    try {
      await loadTeacherSubjects({
        preferredSubjectId: route.query.subjectId,
        preferredMembershipId:
          route.query.subjectMembershipId,
      })

      initialized.value = true

      if (selectedSubjectId.value) {
        await loadTopics()
      }

      if (!membershipOptions.value.length) {
        notice.value = {
          type: 'info',
          message:
            'Нет предметов преподавателя для работы с вопросами.',
        }
      }
    } catch (error) {
      notice.value = {
        type: 'danger',
        message: getApiErrorMessage(
          error,
          error.message
        ),
      }
    }
  })
  function setQuestionFormElement(element) {
    questionFormElement.value = element
  }

  // Reactive shell owns every ref and mutation. Switching presentation chunks
  // does not recreate form state, API requests or the unsaved-change lifecycle.
  return reactive({
    routeQuery, contextHint, notice, loadingSubjects,
    selectedMembershipId, selectedTopicId, selectedMembership,
    selectedSubject, membershipOptions, topicOptions,
    questions, loading, canCreateQuestion, questionStats,
    searchQuery, typeFilter, statusFilter, sortMode,
    questionTypeOptions, questionTypeFilterOptions, statusOptions, sortOptions,
    hasActiveFilters, filteredQuestions, filterResultText, resetFilters,
    questionTypeLabel, questionMatchingPairs, questionDisplayAnswer,
    openImportDialog, importDialogVisible, importError,
    importing, selectedImportTopicId, wordFiles, fileInputKey,
    onWordFiles, closeImportDialog, importWord,
    openCreateQuestion, editQuestion, toggleQuestionActive, togglingQuestionId,
    form, formError, formFieldErrors, questionDrawerOpen, drawerTitle,
    isCreate, isSelectableType, isMatchingType, isTextType,
    savingQuestion, savingOption, confirmCloseVisible,
    handleQuestionDrawerVisibility, handleQuestionTypeChange,
    requestQuestionDrawerClose, discardQuestionDrawer, continueEditing,
    options, loadingOptions, optionForm, resetOptionForm, editOption,
    saveOption, saveQuestion,
    setQuestionFormElement,
  })
}

