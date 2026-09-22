<script setup>
import {
  computed,
  onMounted,
  ref,
  watch,
} from 'vue'

import {
  useRoute,
} from 'vue-router'

import {
  getApiErrorMessage,
  questionsApi,
  topicsApi,
} from '@/api'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiCheckbox,
  UiDialog,
  UiDrawer,
  UiEmptyState,
  UiFileInput,
  UiFilterBar,
  UiInput,
  UiSelect,
  UiTextarea,
  UiUnsavedChangesConfirm,
  useOverlayForm,
} from '@/components/ui'

import MatchingPairsEditor from '@/components/questions/MatchingPairsEditor.vue'
import TeacherPageShell from '@/components/teacher/TeacherPageShell.vue'

import {
  useTeacherSubjects,
} from '@/composables/useTeacherSubjects'

import {
  listFromResponse,
} from '@/utils/apiData'

import {
  createLatestRequestGuard,
} from '@/utils/latestRequest'

import {
  ensureMatchingPairRows,
  matchingPairsValidationMessage,
  normalizeMatchingPairs,
} from '@/utils/matchingPairs'

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

const topics = ref([])
const questions = ref([])
const options = ref([])

const selectedTopicId = ref('')
const selectedImportTopicId = ref('')

const loading = ref(false)
const loadingOptions = ref(false)
const savingOption = ref(false)
const togglingQuestionId = ref(null)
const importing = ref(false)
const initialized = ref(false)

const searchQuery = ref('')
const typeFilter = ref('all')
const statusFilter = ref('all')
const sortMode = ref('ordinal')

const importDialogVisible = ref(false)
const importError = ref('')
const wordFiles = ref([])
const fileInputKey = ref(0)
const formError = ref('')

const topicsRequest = createLatestRequestGuard()
const questionsRequest = createLatestRequestGuard()
const optionsRequest = createLatestRequestGuard()

const notice = ref({
  type: 'info',
  message: '',
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

function questionToForm(question) {
  return {
    id: question?.id ?? null,
    question: question?.question ?? '',
    type: Number(question?.type ?? 1),
    points: Number(question?.points ?? 1),
    ordinal: Number(question?.ordinal ?? 1),
    correctAnswer: question?.correctAnswer ?? '',
    matchingPairs: ensureMatchingPairRows(
      question?.matchingPairs,
      Number(question?.type ?? 1) === 3 ? 2 : 0
    ),
    active: question?.active !== false,
  }
}

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
} = useOverlayForm({
  createDefault: () => questionToForm(),
  mapEntity: questionToForm,
})

const optionForm = ref({
  id: null,
  text: '',
  ordinal: 1,
  correct: false,
})
const optionBaseline = ref('')

function normalizedOptionDraft(value) {
  return JSON.stringify({
    id: value?.id ?? null,
    text: String(value?.text ?? ''),
    ordinal: Number(value?.ordinal ?? 1),
    correct: Boolean(value?.correct),
  })
}

const optionDraftDirty = computed(() => {
  return normalizedOptionDraft(optionForm.value) !== optionBaseline.value
})

const topicOptions = computed(() => {
  return topics.value.map(
    (topic) => ({
      value: topic.id,
      label: `${topic.ordinal}. ${topic.name}`,
    })
  )
})

const currentTopic = computed(() => {
  return topics.value.find(
    (topic) =>
      String(topic.id) ===
      String(selectedTopicId.value)
  ) ?? null
})

const currentQuestionType = computed(() => {
  return Number(form.type ?? 1)
})

const isSelectableType = computed(() => {
  return (
    currentQuestionType.value === 1 ||
    currentQuestionType.value === 2
  )
})

const isMatchingType = computed(() => {
  return currentQuestionType.value === 3
})

const isTextType = computed(() => {
  return currentQuestionType.value === 4
})

const canCreateQuestion = computed(() => {
  return Boolean(
    selectedMembership.value &&
    selectedTopicId.value
  )
})

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
      .filter((value) => value !== null && value !== undefined)
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

  return `Показано: ${filteredQuestions.value.length} из ${questions.value.length}`
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

const drawerTitle = computed(() => {
  return isCreate.value
    ? 'Новый вопрос'
    : 'Редактирование вопроса'
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

function nextQuestionOrdinal() {
  return questions.value.reduce(
    (max, question) =>
      Math.max(max, Number(question.ordinal ?? 0)),
    0
  ) + 1
}

function resetFilters() {
  searchQuery.value = ''
  typeFilter.value = 'all'
  statusFilter.value = 'all'
  sortMode.value = 'ordinal'
}

function resetOptionForm() {
  const maxOrdinal = options.value.reduce(
    (max, option) =>
      Math.max(max, Number(option.ordinal ?? 0)),
    0
  )

  optionForm.value = {
    id: null,
    text: '',
    ordinal: maxOrdinal + 1,
    correct: false,
  }
  optionBaseline.value = normalizedOptionDraft(optionForm.value)
}

function editOption(option) {
  optionForm.value = {
    id: option.id,
    text: option.text || '',
    ordinal: Number(option.ordinal ?? 1),
    correct: Boolean(option.correct),
  }
  optionBaseline.value = normalizedOptionDraft(optionForm.value)
}

function clearQuestionDrawerState() {
  formError.value = ''
  options.value = []
  loadingOptions.value = false
  resetOptionForm()
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
    options.value = []
    resetOptionForm()
    return
  }

  if (Number(value) === 1 || Number(value) === 2) {
    loadOptions(form.id)
  } else {
    optionsRequest.invalidate()
    options.value = []
    loadingOptions.value = false
    resetOptionForm()
  }
}

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

function questionValidationMessage() {
  if (!selectedMembership.value || !selectedTopicId.value) {
    return 'Выберите предмет и тему.'
  }

  const question = String(form.question ?? '').trim()
  const points = Number(form.points)
  const ordinal = Number(form.ordinal)

  if (!question) {
    return 'Введите текст вопроса.'
  }

  if (question.length > 2000) {
    return 'Текст вопроса не может быть длиннее 2000 символов.'
  }

  if (!Number.isFinite(points) || points < 0) {
    return 'Количество баллов должно быть числом не меньше нуля.'
  }

  if (!Number.isInteger(ordinal) || ordinal <= 0) {
    return 'Порядковый номер должен быть целым числом больше нуля.'
  }

  if (isMatchingType.value) {
    const matchingError =
      matchingPairsValidationMessage(
        form.matchingPairs
      )

    if (matchingError) {
      return matchingError
    }
  }

  return ''
}

async function loadTopics() {
  const requestId = topicsRequest.begin()

  questionsRequest.invalidate()
  optionsRequest.invalidate()
  loading.value = false
  loadingOptions.value = false

  topics.value = []
  selectedTopicId.value = ''
  questions.value = []
  options.value = []
  closeQuestionDrawerImmediately()

  const membershipId = Number(
    selectedMembership.value?.id ?? 0
  )

  if (!membershipId) {
    return
  }

  try {
    const response = await topicsApi.getAll({
      subjectMembershipId: membershipId,
    })

    if (!topicsRequest.isCurrent(requestId)) {
      return
    }

    topics.value = listFromResponse(response).sort(
      (left, right) =>
        Number(left.ordinal ?? 0) -
        Number(right.ordinal ?? 0)
    )

    const preferredTopicId = route.query.topicId

    if (
      preferredTopicId &&
      topics.value.some(
        (item) =>
          String(item.id) ===
          String(preferredTopicId)
      )
    ) {
      selectedTopicId.value = String(preferredTopicId)
    } else if (topics.value.length === 1) {
      selectedTopicId.value = String(topics.value[0].id)
    }

    selectedImportTopicId.value = selectedTopicId.value
  } catch (error) {
    if (!topicsRequest.isCurrent(requestId)) {
      return
    }

    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось загрузить темы'
      ),
    }
  }
}

async function loadQuestions() {
  const requestId = questionsRequest.begin()
  const topicId = Number(selectedTopicId.value || 0)

  questions.value = []

  if (!topicId) {
    loading.value = false
    return
  }

  loading.value = true

  try {
    const response = await questionsApi.getAll({
      topicId,
    })

    if (!questionsRequest.isCurrent(requestId)) {
      return
    }

    questions.value = listFromResponse(response).sort(
      (left, right) =>
        Number(left.ordinal ?? 0) -
        Number(right.ordinal ?? 0)
    )
  } catch (error) {
    if (!questionsRequest.isCurrent(requestId)) {
      return
    }

    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось загрузить вопросы'
      ),
    }
  } finally {
    if (questionsRequest.isCurrent(requestId)) {
      loading.value = false
    }
  }
}

async function loadOptions(questionId) {
  const requestId = optionsRequest.begin()

  options.value = []
  resetOptionForm()

  if (!questionId || !isSelectableType.value) {
    loadingOptions.value = false
    return
  }

  loadingOptions.value = true

  try {
    const response = await questionsApi.getOptions(questionId)

    if (!optionsRequest.isCurrent(requestId)) {
      return
    }

    options.value = listFromResponse(response).sort(
      (left, right) =>
        Number(left.ordinal ?? 0) -
        Number(right.ordinal ?? 0)
    )

    resetOptionForm()
  } catch (error) {
    if (!optionsRequest.isCurrent(requestId)) {
      return
    }

    formError.value = getApiErrorMessage(
      error,
      'Не удалось загрузить варианты ответа'
    )
  } finally {
    if (optionsRequest.isCurrent(requestId)) {
      loadingOptions.value = false
    }
  }
}

async function saveQuestion() {
  const wasCreate = !form.id

  formError.value = questionValidationMessage()

  if (formError.value) {
    return
  }

  const type = Number(form.type)
  const basePayload = {
    courseLectureId: null,
    topicId: Number(selectedTopicId.value),
    type,
    question: String(form.question).trim(),
    points: Number(form.points) || 0,
    ordinal: Number(form.ordinal) || 1,
    correctAnswer:
      type === 4
        ? String(form.correctAnswer ?? '').trim() || null
        : null,
    matchingPairs:
      type === 3
        ? normalizeMatchingPairs(form.matchingPairs)
        : [],
  }

  beginSaving()

  try {
    await ensureSelectedMembershipActive()

    let response

    if (form.id) {
      response = await questionsApi.update(
        form.id,
        {
          ...basePayload,
          active: Boolean(form.active),
        }
      )

      notice.value = {
        type: 'success',
        message: 'Вопрос обновлён.',
      }
    } else {
      response = await questionsApi.create({
        testId: null,
        ...basePayload,
      })

      notice.value = {
        type: 'success',
        message:
          'Вопрос создан. Теперь можно добавить варианты ответа, если они нужны.',
      }
    }

    const savedId = Number(
      response.data?.id ?? form.id ?? 0
    )

    await loadQuestions()

    const saved =
      questions.value.find(
        (item) => Number(item.id) === savedId
      ) ?? response.data

    if (saved) {
      if (wasCreate) {
        openEdit(saved)
      } else {
        finishSaving({
          close: false,
          values: questionToForm(saved),
        })
      }

      if (
        Number(saved.type) === 1 ||
        Number(saved.type) === 2
      ) {
        await loadOptions(saved.id)
      } else {
        options.value = []
        resetOptionForm()
      }
    } else {
      finishSaving({ close: true })
      clearQuestionDrawerState()
    }
  } catch (error) {
    formError.value = getApiErrorMessage(
      error,
      form.id
        ? 'Не удалось обновить вопрос'
        : 'Не удалось создать вопрос'
    )
    failSaving()
  }
}

async function toggleQuestionActive(question) {
  if (togglingQuestionId.value !== null) {
    return
  }

  togglingQuestionId.value = question.id

  try {
    await ensureSelectedMembershipActive()

    await questionsApi.updateActive(
      question.id,
      {
        active: !question.active,
      }
    )

    await loadQuestions()

    notice.value = {
      type: 'success',
      message:
        question.active
          ? 'Вопрос скрыт.'
          : 'Вопрос активирован.',
    }
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось изменить статус вопроса'
      ),
    }
  } finally {
    togglingQuestionId.value = null
  }
}

async function saveOption() {
  const text = String(optionForm.value.text ?? '').trim()

  if (!form.id || !isSelectableType.value || !text) {
    formError.value =
      'Сохраните вопрос и заполните текст варианта ответа.'
    return
  }

  const ordinal = Number(optionForm.value.ordinal)

  if (!Number.isInteger(ordinal) || ordinal <= 0) {
    formError.value =
      'Порядок варианта должен быть целым числом больше нуля.'
    return
  }

  const payload = {
    text,
    ordinal,
    correct: Boolean(optionForm.value.correct),
  }

  savingOption.value = true
  formError.value = ''

  try {
    await ensureSelectedMembershipActive()

    if (optionForm.value.id) {
      await questionsApi.updateOption(
        optionForm.value.id,
        payload
      )

      notice.value = {
        type: 'success',
        message: 'Вариант ответа обновлён.',
      }
    } else {
      await questionsApi.createOption(
        form.id,
        payload
      )

      notice.value = {
        type: 'success',
        message: 'Вариант ответа создан.',
      }
    }

    await loadOptions(form.id)
  } catch (error) {
    formError.value = getApiErrorMessage(
      error,
      'Не удалось сохранить вариант ответа'
    )
  } finally {
    savingOption.value = false
  }
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
</script>

<template>
  <TeacherPageShell
    title="Вопросы предмета"
    subtitle="Работайте с банком вопросов выбранной темы: ищите и фильтруйте существующие вопросы, а создание и редактирование выполняйте в боковой панели."
  >
    <UiAlert
      v-if="notice.message"
      :variant="notice.type"
      :message="notice.message"
      closable
      @close="notice.message = ''"
    />

    <UiCard
      title="Контекст вопросов"
      :description="contextHint"
    >
      <div class="teacher-question-context">
        <div class="teacher-grid">
          <UiSelect
            v-model="selectedMembershipId"
            label="Предмет преподавателя"
            :options="membershipOptions"
            placeholder="Выберите предмет"
            :disabled="loadingSubjects || !membershipOptions.length"
          />

          <UiSelect
            v-model="selectedTopicId"
            label="Тема"
            :options="topicOptions"
            placeholder="Выберите тему"
            :disabled="!topicOptions.length"
          />
        </div>

        <div class="teacher-inline-actions teacher-inline-actions--mobile-stack">
          <UiButton
            :to="{
              name: 'teacher-topics',
              query: routeQuery(),
            }"
          >
            Темы предмета
          </UiButton>

          <UiButton
            v-if="selectedTopicId"
            :to="{
              name: 'teacher-test-create',
              query: routeQuery(),
            }"
          >
            Создать тест по теме
          </UiButton>
        </div>
      </div>
    </UiCard>

    <UiCard
      title="Банк вопросов"
      :description="questionStats"
    >
      <div class="teacher-stack">
        <UiFilterBar
          v-model="searchQuery"
          search-placeholder="Текст, ответ, ID или номер вопроса"
          :result-text="filterResultText"
          :reset-disabled="!hasActiveFilters"
          @reset="resetFilters"
        >
          <template #filters>
            <UiSelect
              v-model="typeFilter"
              label="Тип"
              :options="questionTypeFilterOptions"
              size="sm"
            />

            <UiSelect
              v-model="statusFilter"
              label="Статус"
              :options="statusOptions"
              size="sm"
            />

            <UiSelect
              v-model="sortMode"
              label="Сортировка"
              :options="sortOptions"
              size="sm"
            />
          </template>

          <template #actions>
            <UiButton
              size="sm"
              icon="pi pi-file-import"
              label="Импорт Word"
              :disabled="!topicOptions.length"
              @click="openImportDialog"
            />

            <UiButton
              variant="primary"
              size="sm"
              icon="pi pi-plus"
              label="Добавить вопрос"
              :disabled="!canCreateQuestion"
              @click="openCreateQuestion"
            />
          </template>
        </UiFilterBar>

        <UiEmptyState
          v-if="loading"
          description="Загрузка вопросов..."
          compact
        />

        <UiEmptyState
          v-else-if="!selectedMembership"
          description="Выберите предмет преподавателя."
          compact
        />

        <UiEmptyState
          v-else-if="!selectedTopicId"
          description="Выберите тему, чтобы открыть банк вопросов."
          compact
        />

        <UiEmptyState
          v-else-if="!questions.length"
          description="В выбранной теме пока нет вопросов. Добавьте первый вопрос кнопкой выше или импортируйте .docx."
          compact
        />

        <UiEmptyState
          v-else-if="!filteredQuestions.length"
          description="По текущему поиску и фильтрам вопросы не найдены."
          compact
        >
          <template #actions>
            <UiButton
              variant="secondary"
              size="sm"
              label="Сбросить фильтры"
              @click="resetFilters"
            />
          </template>
        </UiEmptyState>

        <div
          v-else
          class="teacher-entity-list"
        >
          <article
            v-for="question in filteredQuestions"
            :key="question.id"
            class="teacher-entity-card"
          >
            <div class="teacher-entity-card__header">
              <div class="teacher-entity-card__heading">
                <span class="teacher-entity-card__eyebrow">
                  {{ question.ordinal }} · {{ questionTypeLabel(question.type) }}
                </span>

                <h3 class="teacher-entity-card__title">
                  {{ question.question }}
                </h3>
              </div>

              <span
                class="teacher-status"
                :class="{
                  'teacher-status--success': question.active,
                }"
              >
                {{ question.active ? 'Активен' : 'Скрыт' }}
              </span>
            </div>

            <div
              v-if="Number(question.type) === 3 && questionMatchingPairs(question).length"
              class="teacher-matching-preview"
            >
              <div
                v-for="pair in questionMatchingPairs(question)"
                :key="`${question.id}-${pair.ordinal}`"
                class="teacher-matching-preview__pair"
              >
                <span class="teacher-matching-preview__number">{{ pair.ordinal }}</span>
                <span>{{ pair.left }}</span>
                <i class="pi pi-arrow-right" aria-hidden="true" />
                <strong>{{ pair.right }}</strong>
              </div>
            </div>

            <p
              v-else-if="questionDisplayAnswer(question)"
              class="teacher-entity-card__description teacher-question-answer-preview"
            >
              {{ questionDisplayAnswer(question) }}
            </p>

            <div class="teacher-entity-card__meta">
              <span>Баллы: {{ question.points }}</span>
              <span>ID: {{ question.id }}</span>
            </div>

            <div class="teacher-entity-card__actions">
              <UiButton
                size="sm"
                icon="pi pi-pencil"
                label="Изменить"
                @click="editQuestion(question)"
              />

              <UiButton
                size="sm"
                :loading="Number(togglingQuestionId) === Number(question.id)"
                :loading-text="question.active ? 'Скрытие...' : 'Активация...'"
                @click="toggleQuestionActive(question)"
              >
                {{ question.active ? 'Скрыть' : 'Активировать' }}
              </UiButton>
            </div>
          </article>
        </div>
      </div>
    </UiCard>

    <UiDrawer
      :model-value="questionDrawerOpen"
      :title="drawerTitle"
      width="46rem"
      @update:model-value="handleQuestionDrawerVisibility"
    >
      <div class="teacher-stack teacher-question-drawer">
        <UiAlert
          v-if="formError"
          variant="danger"
          :message="formError"
          closable
          @close="formError = ''"
        />

        <section class="teacher-question-form-section">
          <div class="teacher-question-form-section__heading">
            <span class="teacher-muted">Вопрос</span>
            <strong>
              {{ isCreate ? 'Новая запись банка' : `ID ${form.id}` }}
            </strong>
          </div>

          <UiTextarea
            v-model="form.question"
            label="Текст вопроса"
            maxlength="2000"
            required
          />

          <div class="teacher-grid--3 teacher-grid">
            <UiSelect
              v-model="form.type"
              label="Тип"
              :options="questionTypeOptions"
              @update:model-value="handleQuestionTypeChange"
            />

            <UiInput
              v-model="form.points"
              label="Баллы"
              type="number"
              min="0"
              step="0.01"
            />

            <UiInput
              v-model="form.ordinal"
              label="Порядок"
              type="number"
              min="1"
              step="1"
            />
          </div>

          <UiInput
            v-if="isTextType"
            v-model="form.correctAnswer"
            label="Правильный ответ"
            hint="Можно указать несколько допустимых вариантов через символ | или с новой строки."
            maxlength="2000"
          />

          <MatchingPairsEditor
            v-if="isMatchingType"
            v-model="form.matchingPairs"
            :disabled="savingQuestion"
          />

          <UiCheckbox
            v-if="!isCreate"
            v-model="form.active"
            label="Вопрос активен"
          />
        </section>

        <section
          v-if="isSelectableType"
          class="teacher-question-form-section"
        >
          <div class="teacher-question-form-section__heading">
            <span class="teacher-muted">Варианты ответа</span>
            <strong>
              {{ form.id ? `${options.length} вариантов` : 'После сохранения вопроса' }}
            </strong>
          </div>

          <UiEmptyState
            v-if="!form.id"
            description="Сначала сохраните вопрос. После получения ID управление вариантами станет доступно в этой же панели."
            compact
          />

          <template v-else>
            <UiEmptyState
              v-if="loadingOptions"
              description="Загрузка вариантов..."
              compact
            />

            <UiEmptyState
              v-else-if="!options.length"
              description="У вопроса пока нет вариантов ответа. Добавьте первый вариант ниже."
              compact
            />

            <div
              v-else
              class="teacher-choice-list"
            >
              <div
                v-for="option in options"
                :key="option.id"
                class="teacher-choice-row"
              >
                <div class="teacher-choice-row__copy">
                  <strong class="teacher-choice-row__title">
                    {{ option.ordinal }}. {{ option.text }}
                  </strong>
                  <span class="teacher-choice-row__meta">
                    {{ option.correct ? 'Правильный вариант' : 'Обычный вариант' }}
                  </span>
                </div>

                <UiButton
                  size="sm"
                  icon="pi pi-pencil"
                  label="Изменить"
                  @click="editOption(option)"
                />
              </div>
            </div>

            <div class="teacher-divider" />

            <UiInput
              v-model="optionForm.text"
              label="Текст варианта"
              maxlength="2000"
            />

            <div class="teacher-grid">
              <UiInput
                v-model="optionForm.ordinal"
                label="Порядок"
                type="number"
                min="1"
                step="1"
              />

              <UiCheckbox
                v-model="optionForm.correct"
                label="Правильный вариант"
              />
            </div>

            <div class="teacher-actions teacher-actions--mobile-stack">
              <UiButton
                variant="primary"
                :loading="savingOption"
                loading-text="Сохранение..."
                @click="saveOption"
              >
                {{ optionForm.id ? 'Сохранить вариант' : 'Добавить вариант' }}
              </UiButton>

              <UiButton
                :disabled="savingOption"
                @click="resetOptionForm"
              >
                Очистить вариант
              </UiButton>
            </div>
          </template>
        </section>
      </div>

      <template #footer>
        <div class="teacher-question-drawer__footer">
          <UiButton
            variant="secondary"
            label="Закрыть"
            :disabled="savingQuestion || savingOption"
            @click="requestQuestionDrawerClose"
          />

          <UiButton
            variant="primary"
            :loading="savingQuestion"
            loading-text="Сохранение..."
            :label="form.id ? 'Сохранить вопрос' : 'Создать вопрос'"
            :disabled="savingOption"
            @click="saveQuestion"
          />
        </div>
      </template>
    </UiDrawer>

    <UiUnsavedChangesConfirm
      v-model="confirmCloseVisible"
      :busy="savingQuestion || savingOption"
      @continue="continueEditing"
      @discard="discardQuestionDrawer"
    />

    <UiDialog
      v-model="importDialogVisible"
      title="Импорт вопросов из Word"
      width="34rem"
      :close-on-escape="!importing"
      :closable="!importing"
    >
      <div class="teacher-stack">
        <p class="teacher-muted teacher-question-dialog-copy">
          Выберите тему и .docx. Импорт не меняет текущие фильтры банка вопросов.
        </p>

        <UiAlert
          v-if="importError"
          variant="danger"
          :message="importError"
        />

        <UiSelect
          v-model="selectedImportTopicId"
          label="Тема для импорта"
          :options="topicOptions"
          placeholder="Выберите тему"
          :disabled="importing || !topicOptions.length"
        />

        <UiFileInput
          :key="fileInputKey"
          label="Файл .docx"
          accept=".docx,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
          :disabled="importing || !selectedImportTopicId"
          @files-change="onWordFiles"
        />
      </div>

      <template #footer>
        <div class="teacher-actions teacher-actions--mobile-stack">
          <UiButton
            variant="secondary"
            label="Отмена"
            :disabled="importing"
            @click="closeImportDialog"
          />

          <UiButton
            variant="primary"
            :loading="importing"
            loading-text="Импорт..."
            label="Импортировать"
            :disabled="!selectedImportTopicId || !wordFiles.length"
            @click="importWord"
          />
        </div>
      </template>
    </UiDialog>
  </TeacherPageShell>
</template>

<style scoped>
.teacher-question-context {
  min-width: 0;
  display: grid;
  gap: 14px;
}

.teacher-question-answer-preview {
  padding: 10px 12px;
  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 9px;
  white-space: pre-wrap;
}

.teacher-matching-preview {
  min-width: 0;
  display: grid;
  gap: 6px;
}

.teacher-matching-preview__pair {
  min-width: 0;
  padding: 8px 10px;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 8px;
}

.teacher-matching-preview__pair > span,
.teacher-matching-preview__pair > strong {
  min-width: 0;
  overflow-wrap: anywhere;
}

.teacher-matching-preview__pair > i {
  color: var(--st-text-secondary);
}

.teacher-matching-preview__number {
  width: 26px;
  height: 26px;
  display: grid;
  place-items: center;
  color: var(--st-primary-soft-text);
  background: var(--st-primary-soft);
  border-radius: 7px;
  font-size: 11px;
  font-weight: 800;
}

.teacher-question-drawer {
  min-width: 0;
  padding-bottom: 4px;
}

.teacher-question-form-section {
  min-width: 0;
  padding: 14px;
  display: grid;
  gap: 14px;
  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 12px;
}

.teacher-question-form-section__heading {
  min-width: 0;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.teacher-question-form-section__heading strong {
  min-width: 0;
  color: var(--st-text);
  overflow-wrap: anywhere;
}

.teacher-question-drawer__footer {
  width: 100%;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.teacher-question-dialog-copy {
  margin: 0;
  line-height: 1.55;
}

@media (max-width: 640px) {
  .teacher-matching-preview__pair {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .teacher-matching-preview__pair > i {
    grid-column: 2;
    transform: rotate(90deg);
  }

  .teacher-matching-preview__pair > strong {
    grid-column: 2;
  }

  .teacher-question-form-section {
    padding: 12px;
  }

  .teacher-question-drawer__footer,
  .teacher-question-drawer__footer > * {
    width: 100%;
  }

  .teacher-question-drawer__footer {
    flex-direction: column-reverse;
  }
}
</style>
