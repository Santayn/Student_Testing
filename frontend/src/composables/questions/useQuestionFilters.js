import {
  computed,
  ref,
} from 'vue'

/**
 * Client-side presentation state for the question bank. Changing filters must
 * never refetch questions or mutate the selected topic.
 */
export function useQuestionFilters({
  questions,
  selectedTopicId,
  questionTypeLabel,
  questionDisplayAnswer,
}) {
  const searchQuery = ref('')
  const typeFilter = ref('all')
  const statusFilter = ref('all')
  const sortMode = ref('ordinal')

  const activeQuestions = computed(() => {
    return questions.value.filter(
      (question) => question.active
    )
  })

  const questionStats = computed(() => {
    if (!selectedTopicId.value) {
      return 'Выберите тему, чтобы открыть банк вопросов.'
    }

    const active = activeQuestions.value

    return (
      `Всего ${questions.value.length}. ` +
      `Активных ${active.length}. ` +
      `Скрытых ${questions.value.length - active.length}.`
    )
  })

  const hasActiveFilters = computed(() => {
    return Boolean(searchQuery.value.trim()) ||
      typeFilter.value !== 'all' ||
      statusFilter.value !== 'all' ||
      sortMode.value !== 'ordinal'
  })

  const filteredQuestions = computed(() => {
    const query = searchQuery.value
      .trim()
      .toLocaleLowerCase('ru-RU')

    const result = questions.value.filter((question) => {
      if (
        typeFilter.value !== 'all' &&
        Number(question.type) !== Number(typeFilter.value)
      ) {
        return false
      }

      if (
        statusFilter.value === 'active' &&
        !question.active
      ) {
        return false
      }

      if (
        statusFilter.value === 'hidden' &&
        question.active
      ) {
        return false
      }

      if (!query) {
        return true
      }

      const haystack = [
        question.id,
        question.ordinal,
        question.question,
        question.correctAnswer,
        questionDisplayAnswer(question),
        questionTypeLabel(question.type),
      ]
        .filter(
          (value) =>
            value !== null &&
            value !== undefined
        )
        .join(' ')
        .toLocaleLowerCase('ru-RU')

      return haystack.includes(query)
    })

    return [...result].sort((left, right) => {
      if (sortMode.value === 'points-desc') {
        return (
          Number(right.points ?? 0) - Number(left.points ?? 0) ||
          Number(left.ordinal ?? 0) - Number(right.ordinal ?? 0)
        )
      }

      if (sortMode.value === 'type') {
        return (
          questionTypeLabel(left.type).localeCompare(
            questionTypeLabel(right.type),
            'ru'
          ) ||
          Number(left.ordinal ?? 0) - Number(right.ordinal ?? 0)
        )
      }

      if (sortMode.value === 'text') {
        return String(left.question ?? '').localeCompare(
          String(right.question ?? ''),
          'ru'
        )
      }

      return Number(left.ordinal ?? 0) - Number(right.ordinal ?? 0)
    })
  })

  const filterResultText = computed(() => {
    if (!selectedTopicId.value) {
      return 'Сначала выберите тему.'
    }

    return (
      `Показано: ${filteredQuestions.value.length} ` +
      `из ${questions.value.length}`
    )
  })

  function resetFilters() {
    searchQuery.value = ''
    typeFilter.value = 'all'
    statusFilter.value = 'all'
    sortMode.value = 'ordinal'
  }

  return {
    searchQuery,
    typeFilter,
    statusFilter,
    sortMode,
    questionStats,
    hasActiveFilters,
    filteredQuestions,
    filterResultText,
    resetFilters,
  }
}
