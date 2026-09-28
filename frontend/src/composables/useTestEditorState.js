import {
  computed,
  ref,
} from 'vue'

export const TEST_STATUS_OPTIONS = [
  { value: 1, label: 'Черновик' },
  { value: 2, label: 'Активно' },
  { value: 3, label: 'Закрыто' },
  { value: 4, label: 'Приостановлено' },
]

function localDateTimeValue(date) {
  const offset =
    date.getTimezoneOffset() * 60_000

  return new Date(
    date.getTime() - offset
  )
    .toISOString()
    .slice(0, 16)
}

export function useTestEditorState({
  selectedMembershipId,
  selectedSubjectId,
  selectedMembership,
  selectedTopicId,
  selectedGroupIds,
  topics,
  questions,
}) {
  const form = ref({
    title: '',
    description: '',
    questionCount: 1,
    attemptsAllowed: 1,
    textQuestionCount: 0,
    singleAnswerQuestionCount: 0,
    multipleAnswerQuestionCount: 0,
    matchingQuestionCount: 0,
    status: 2,
    availableFrom: '',
    availableUntil: '',
  })

  const editorBaseline = ref(null)

  const editorState = computed(() => {
    const groupIds = selectedGroupIds.value
      .map((id) => Number(id))
      .filter(Number.isFinite)
      .sort((left, right) => left - right)

    return {
      context: {
        membershipId:
          String(
            selectedMembershipId.value ?? ''
          ),
        topicId:
          String(
            selectedTopicId.value ?? ''
          ),
        groupIds,
      },
      form: {
        title:
          String(form.value.title ?? ''),
        description:
          String(
            form.value.description ?? ''
          ),
        questionCount:
          Number(form.value.questionCount),
        attemptsAllowed:
          Number(
            form.value.attemptsAllowed
          ),
        textQuestionCount:
          Number(
            form.value.textQuestionCount
          ),
        singleAnswerQuestionCount:
          Number(
            form.value
              .singleAnswerQuestionCount
          ),
        multipleAnswerQuestionCount:
          Number(
            form.value
              .multipleAnswerQuestionCount
          ),
        matchingQuestionCount:
          Number(
            form.value
              .matchingQuestionCount
          ),
        status: Number(form.value.status),
        availableFrom:
          String(
            form.value.availableFrom ?? ''
          ),
        availableUntil:
          String(
            form.value.availableUntil ?? ''
          ),
      },
    }
  })

  function cloneEditorState() {
    return JSON.parse(
      JSON.stringify(editorState.value)
    )
  }

  const editorDirty = computed(() => {
    return (
      editorBaseline.value !== null &&
      JSON.stringify(editorState.value) !==
        JSON.stringify(editorBaseline.value)
    )
  })

  function markEditorClean() {
    editorBaseline.value =
      cloneEditorState()
  }

  function markInitialContextClean() {
    const nextBaseline =
      cloneEditorState()

    if (editorBaseline.value?.form) {
      nextBaseline.form =
        editorBaseline.value.form
    }

    editorBaseline.value =
      nextBaseline
  }

  const statusOptions =
    TEST_STATUS_OPTIONS

  const topicOptions = computed(() => {
    return topics.value.map(
      (topic) => ({
        value: topic.id,
        label:
          `${topic.ordinal}. ${topic.name}`,
      })
    )
  })

  const activeQuestions = computed(() => {
    return questions.value.filter(
      (question) => question.active
    )
  })

  const questionCounts = computed(() => {
    const active = activeQuestions.value

    return {
      total: active.length,
      single: active.filter(
        (item) =>
          Number(item.type) === 1
      ).length,
      multiple: active.filter(
        (item) =>
          Number(item.type) === 2
      ).length,
      matching: active.filter(
        (item) =>
          Number(item.type) === 3
      ).length,
      text: active.filter(
        (item) =>
          Number(item.type) === 4
      ).length,
    }
  })

  const fixedQuestionCount = computed(() => {
    return (
      Number(
        form.value.textQuestionCount
      ) +
      Number(
        form.value.singleAnswerQuestionCount
      ) +
      Number(
        form.value.multipleAnswerQuestionCount
      ) +
      Number(
        form.value.matchingQuestionCount
      )
    )
  })

  const ruleSummary = computed(() => {
    if (!selectedTopicId.value) {
      return 'Выберите тему. После этого можно точно настроить состав вопросов.'
    }

    const automatic = Math.max(
      0,
      Number(form.value.questionCount) -
        fixedQuestionCount.value
    )

    return (
      `Активных вопросов: ${questionCounts.value.total}. ` +
      `Фиксировано по типам: ${fixedQuestionCount.value}. ` +
      `Остальные случайно: ${automatic}.`
    )
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

  function setDefaultDates(now = new Date()) {
    const until = new Date(
      now.getTime() +
        30 * 24 * 60 * 60 * 1000
    )

    form.value.availableFrom =
      localDateTimeValue(now)

    form.value.availableUntil =
      localDateTimeValue(until)
  }

  function routeQuery() {
    const query = {}

    if (selectedSubjectId.value) {
      query.subjectId =
        selectedSubjectId.value
    }

    if (selectedMembership.value) {
      query.subjectMembershipId =
        selectedMembership.value.id
    }

    if (selectedTopicId.value) {
      query.topicId =
        selectedTopicId.value
    }

    return query
  }

  function validationError() {
    if (
      !selectedMembership.value ||
      !selectedTopicId.value
    ) {
      return 'Выберите предмет и тему.'
    }

    if (!selectedGroupIds.value.length) {
      return 'Выберите хотя бы одну группу.'
    }

    const total =
      Number(form.value.questionCount)

    if (total < 1) {
      return 'Количество вопросов должно быть больше нуля.'
    }

    if (
      fixedQuestionCount.value > total
    ) {
      return 'Сумма вопросов по типам не может превышать общее количество.'
    }

    if (
      questionCounts.value.total < total
    ) {
      return 'В теме недостаточно активных вопросов.'
    }

    if (
      questionCounts.value.text <
        Number(
          form.value.textQuestionCount
        ) ||
      questionCounts.value.single <
        Number(
          form.value.singleAnswerQuestionCount
        ) ||
      questionCounts.value.multiple <
        Number(
          form.value.multipleAnswerQuestionCount
        ) ||
      questionCounts.value.matching <
        Number(
          form.value.matchingQuestionCount
        )
    ) {
      return 'В теме недостаточно вопросов выбранных типов.'
    }

    if (!form.value.title.trim()) {
      return 'Введите название теста.'
    }

    if (
      !form.value.availableFrom ||
      !form.value.availableUntil
    ) {
      return 'Укажите период доступности теста.'
    }

    if (
      new Date(form.value.availableFrom) >=
      new Date(form.value.availableUntil)
    ) {
      return 'Дата окончания должна быть позже даты начала.'
    }

    return ''
  }

  return {
    form,
    editorDirty,
    statusOptions,
    topicOptions,
    activeQuestions,
    questionCounts,
    fixedQuestionCount,
    ruleSummary,
    questionTypeLabel,
    setDefaultDates,
    routeQuery,
    validationError,
    markEditorClean,
    markInitialContextClean,
  }
}
