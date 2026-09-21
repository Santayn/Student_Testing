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
  UiEmptyState,
  UiFileInput,
  UiInput,
  UiSelect,
  UiTextarea,
} from '@/components/ui'

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

const route = useRoute()

const {
  loadingSubjects,
  selectedMembershipId,
  selectedSubjectId,
  selectedMembership,
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
const savingQuestion = ref(false)
const savingOption = ref(false)
const importing = ref(false)
const wordFiles = ref([])
const fileInputKey = ref(0)
const initialized = ref(false)

const topicsRequest = createLatestRequestGuard()
const questionsRequest = createLatestRequestGuard()
const optionsRequest = createLatestRequestGuard()

const notice = ref({
  type: 'info',
  message: '',
})

const questionForm = ref({
  id: null,
  question: '',
  type: 1,
  points: 1,
  ordinal: 1,
  correctAnswer: '',
  matchingPairsText: '',
  active: true,
})

const optionForm = ref({
  id: null,
  text: '',
  ordinal: 1,
  correct: false,
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

const topicOptions = computed(() => {
  return topics.value.map(
    (topic) => ({
      value: topic.id,
      label:
        `${topic.ordinal}. ${topic.name}`,
    })
  )
})

const currentQuestion = computed(() => {
  return questions.value.find(
    (item) =>
      Number(item.id) ===
      Number(questionForm.value.id)
  ) ?? null
})

const currentQuestionType = computed(() => {
  return Number(
    questionForm.value.type ?? 1
  )
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

const activeQuestions = computed(() => {
  return questions.value.filter(
    (question) => question.active
  )
})

const questionStats = computed(() => {
  if (!selectedTopicId.value) {
    return 'Вопросы не выбраны.'
  }

  const active =
    activeQuestions.value

  const single = active.filter(
    (item) =>
      Number(item.type) === 1
  ).length

  const multiple = active.filter(
    (item) =>
      Number(item.type) === 2
  ).length

  const matching = active.filter(
    (item) =>
      Number(item.type) === 3
  ).length

  const text = active.filter(
    (item) =>
      Number(item.type) === 4
  ).length

  return (
    `Всего ${questions.value.length}. ` +
    `Активных ${active.length}. ` +
    `С одним вариантом ${single}. ` +
    `С несколькими вариантами ${multiple}. ` +
    `На соответствие ${matching}. ` +
    `Текстовых ${text}.`
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

function questionDisplayAnswer(question) {
  if (Number(question.type) === 3) {
    return (
      Array.isArray(
        question.matchingPairs
      )
        ? question.matchingPairs
        : []
    )
      .map(
        (pair) =>
          `${pair.left || '?'} → ${pair.right || '-'}`
      )
      .join(' | ')
  }

  return question.correctAnswer || ''
}

function matchingPairsToText(pairs) {
  return (
    Array.isArray(pairs)
      ? [...pairs]
      : []
  )
    .sort(
      (left, right) =>
        Number(left.ordinal ?? 0) -
        Number(right.ordinal ?? 0)
    )
    .map(
      (pair) =>
        `${pair.left || ''} -> ${pair.right || ''}`.trim()
    )
    .filter(Boolean)
    .join('\n')
}

function parseMatchingPairsText(value) {
  return String(value || '')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line, index) => {
      const delimiter = line.match(
        /\s*(?:->|=>|=|\||;)\s*/
      )

      if (!delimiter) {
        return null
      }

      const left = line
        .slice(0, delimiter.index)
        .trim()

      const right = line
        .slice(
          delimiter.index +
            delimiter[0].length
        )
        .trim()

      if (!left || !right) {
        return null
      }

      return {
        ordinal: index + 1,
        left,
        right,
      }
    })
    .filter(Boolean)
}

function routeQuery(topicId = null) {
  const query = {}

  if (selectedSubjectId.value) {
    query.subjectId =
      selectedSubjectId.value
  }

  if (selectedMembership.value) {
    query.subjectMembershipId =
      selectedMembership.value.id
  }

  if (topicId || selectedTopicId.value) {
    query.topicId =
      topicId || selectedTopicId.value
  }

  return query
}

function resetQuestionForm() {
  const maxOrdinal =
    questions.value.reduce(
      (max, question) =>
        Math.max(
          max,
          Number(question.ordinal ?? 0)
        ),
      0
    )

  questionForm.value = {
    id: null,
    question: '',
    type: 1,
    points: 1,
    ordinal: maxOrdinal + 1,
    correctAnswer: '',
    matchingPairsText: '',
    active: true,
  }

  options.value = []
  resetOptionForm()
}

function resetOptionForm() {
  const maxOrdinal =
    options.value.reduce(
      (max, option) =>
        Math.max(
          max,
          Number(option.ordinal ?? 0)
        ),
      0
    )

  optionForm.value = {
    id: null,
    text: '',
    ordinal: maxOrdinal + 1,
    correct: false,
  }
}

async function loadTopics() {
  const requestId =
    topicsRequest.begin()

  questionsRequest.invalidate()
  optionsRequest.invalidate()
  loading.value = false
  loadingOptions.value = false

  topics.value = []
  selectedTopicId.value = ''
  selectedImportTopicId.value = ''
  questions.value = []
  options.value = []
  resetQuestionForm()
  resetOptionForm()

  const membershipId = Number(
    selectedMembership.value?.id ?? 0
  )

  if (!membershipId) {
    return
  }

  try {
    const response =
      await topicsApi.getAll({
        subjectMembershipId: membershipId,
      })

    if (
      !topicsRequest.isCurrent(
        requestId
      )
    ) {
      return
    }

    topics.value =
      listFromResponse(response)
        .sort(
          (left, right) =>
            Number(left.ordinal ?? 0) -
            Number(right.ordinal ?? 0)
        )

    const preferredTopicId =
      route.query.topicId

    if (
      preferredTopicId &&
      topics.value.some(
        (item) =>
          String(item.id) ===
          String(preferredTopicId)
      )
    ) {
      selectedTopicId.value =
        String(preferredTopicId)
    } else if (
      topics.value.length === 1
    ) {
      selectedTopicId.value =
        String(topics.value[0].id)
    }

    selectedImportTopicId.value =
      selectedTopicId.value
  } catch (error) {
    if (
      !topicsRequest.isCurrent(
        requestId
      )
    ) {
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
  const requestId =
    questionsRequest.begin()

  optionsRequest.invalidate()
  loadingOptions.value = false

  questions.value = []
  options.value = []
  resetQuestionForm()
  resetOptionForm()

  const topicId = Number(
    selectedTopicId.value || 0
  )

  if (!topicId) {
    loading.value = false
    return
  }

  loading.value = true

  try {
    const response =
      await questionsApi.getAll({
        topicId,
      })

    if (
      !questionsRequest.isCurrent(
        requestId
      )
    ) {
      return
    }

    questions.value =
      listFromResponse(response)
        .sort(
          (left, right) =>
            Number(left.ordinal ?? 0) -
            Number(right.ordinal ?? 0)
        )

    resetQuestionForm()
  } catch (error) {
    if (
      !questionsRequest.isCurrent(
        requestId
      )
    ) {
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
    if (
      questionsRequest.isCurrent(
        requestId
      )
    ) {
      loading.value = false
    }
  }
}

async function loadOptions(questionId) {
  const requestId =
    optionsRequest.begin()

  options.value = []
  resetOptionForm()

  if (
    !questionId ||
    !isSelectableType.value
  ) {
    loadingOptions.value = false
    return
  }

  loadingOptions.value = true

  try {
    const response =
      await questionsApi.getOptions(
        questionId
      )

    if (
      !optionsRequest.isCurrent(
        requestId
      )
    ) {
      return
    }

    options.value =
      listFromResponse(response)
        .sort(
          (left, right) =>
            Number(left.ordinal ?? 0) -
            Number(right.ordinal ?? 0)
        )

    resetOptionForm()
  } catch (error) {
    if (
      !optionsRequest.isCurrent(
        requestId
      )
    ) {
      return
    }

    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось загрузить варианты ответа'
      ),
    }
  } finally {
    if (
      optionsRequest.isCurrent(
        requestId
      )
    ) {
      loadingOptions.value = false
    }
  }
}

async function saveQuestion() {
  if (
    !selectedTopicId.value ||
    !questionForm.value.question.trim()
  ) {
    notice.value = {
      type: 'danger',
      message:
        'Выберите тему и заполните текст вопроса.',
    }
    return
  }

  const type =
    Number(questionForm.value.type)

  const basePayload = {
    courseLectureId: null,
    topicId:
      Number(selectedTopicId.value),
    type,
    question:
      questionForm.value.question.trim(),
    points:
      Number(questionForm.value.points) ||
      0,
    ordinal:
      Number(questionForm.value.ordinal) ||
      1,
    correctAnswer:
      type === 4
        ? questionForm.value
            .correctAnswer.trim() || null
        : null,
    matchingPairs:
      type === 3
        ? parseMatchingPairsText(
            questionForm.value
              .matchingPairsText
          )
        : [],
  }

  savingQuestion.value = true

  try {
    await ensureSelectedMembershipActive()

    let response

    if (questionForm.value.id) {
      response =
        await questionsApi.update(
          questionForm.value.id,
          {
            ...basePayload,
            active:
              questionForm.value.active,
          }
        )

      notice.value = {
        type: 'success',
        message: 'Вопрос обновлён.',
      }
    } else {
      response =
        await questionsApi.create({
          testId: null,
          ...basePayload,
        })

      notice.value = {
        type: 'success',
        message: 'Вопрос создан.',
      }
    }

    const saved = response.data

    await loadQuestions()

    if (saved?.id) {
      const question =
        questions.value.find(
          (item) =>
            Number(item.id) ===
            Number(saved.id)
        ) ?? saved

      await editQuestion(question)
    }
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось сохранить вопрос'
      ),
    }
  } finally {
    savingQuestion.value = false
  }
}

async function toggleQuestionActive(question) {
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
  }
}

function editOption(option) {
  optionForm.value = {
    id: option.id,
    text: option.text || '',
    ordinal: Number(
      option.ordinal ?? 1
    ),
    correct: Boolean(option.correct),
  }
}

async function saveOption() {
  if (
    !questionForm.value.id ||
    !isSelectableType.value ||
    !optionForm.value.text.trim()
  ) {
    notice.value = {
      type: 'danger',
      message:
        'Выберите вопрос с вариантами ответа и заполните текст варианта.',
    }
    return
  }

  const payload = {
    text: optionForm.value.text.trim(),
    ordinal:
      Number(optionForm.value.ordinal) ||
      1,
    correct:
      optionForm.value.correct,
  }

  savingOption.value = true

  try {
    await ensureSelectedMembershipActive()

    if (optionForm.value.id) {
      await questionsApi.updateOption(
        optionForm.value.id,
        payload
      )

      notice.value = {
        type: 'success',
        message:
          'Вариант ответа обновлён.',
      }
    } else {
      await questionsApi.createOption(
        questionForm.value.id,
        payload
      )

      notice.value = {
        type: 'success',
        message:
          'Вариант ответа создан.',
      }
    }

    await loadOptions(
      questionForm.value.id
    )
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось сохранить вариант ответа'
      ),
    }
  } finally {
    savingOption.value = false
  }
}

function onWordFiles(files) {
  wordFiles.value = files
}

async function importWord() {
  const file = wordFiles.value[0]

  if (
    !selectedImportTopicId.value ||
    !file
  ) {
    notice.value = {
      type: 'danger',
      message:
        'Выберите тему и файл .docx.',
    }
    return
  }

  importing.value = true

  try {
    await ensureSelectedMembershipActive()

    const response =
      await questionsApi.importFile(
        file,
        {
          topicId:
            Number(
              selectedImportTopicId.value
            ),
        }
      )

    const payload = response.data ?? {}

    notice.value = {
      type: 'success',
      message:
        `Импортировано вопросов: ${payload.importedQuestions ?? 0}. ` +
        `Вариантов ответа: ${payload.importedOptions ?? 0}.`,
    }

    wordFiles.value = []
    fileInputKey.value += 1

    if (
      String(selectedTopicId.value) ===
      String(selectedImportTopicId.value)
    ) {
      await loadQuestions()
    }
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось импортировать вопросы'
      ),
    }
  } finally {
    importing.value = false
  }
}

watch(
  selectedMembershipId,
  () => {
    if (initialized.value) {
      loadTopics()
    }
  }
)

watch(
  selectedTopicId,
  async (value) => {
    selectedImportTopicId.value = value

    if (initialized.value) {
      await loadQuestions()
    }
  }
)

watch(
  () => questionForm.value.type,
  () => {
    if (
      questionForm.value.id
    ) {
      loadOptions(
        questionForm.value.id
      )
    } else {
      options.value = []
      resetOptionForm()
    }
  }
)

onMounted(async () => {
  try {
    await loadTeacherSubjects({
      preferredSubjectId:
        route.query.subjectId,
      preferredMembershipId:
        route.query
          .subjectMembershipId,
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
    subtitle="Создание, редактирование и управление вариантами ответов в банке вопросов по темам предмета."
  >
    <UiAlert
      v-if="notice.message"
      :variant="notice.type"
      :message="notice.message"
      closable
      @close="notice.message = ''"
    />

    <div class="teacher-grid">
      <UiCard
        title="Контекст вопросов"
        description="Выберите предмет и тему."
      >
        <div class="teacher-stack">
          <div class="teacher-grid">
            <UiSelect
              v-model="selectedMembershipId"
              label="Предмет"
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
        title="Импорт из Word"
        description="Импортируйте .docx сразу в выбранную тему без отдельного экрана."
      >
        <div class="teacher-stack">
          <UiSelect
            v-model="selectedImportTopicId"
            label="Тема для импорта"
            :options="topicOptions"
            placeholder="Выберите тему"
            :disabled="!topicOptions.length"
          />

          <UiFileInput
            :key="fileInputKey"
            label="Файл .docx"
            accept=".docx,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            :disabled="!selectedImportTopicId"
            @files-change="onWordFiles"
          />

          <UiButton
            variant="primary"
            :loading="importing"
            loading-text="Импорт..."
            :disabled="!selectedImportTopicId || !wordFiles.length"
            @click="importWord"
          >
            Импортировать вопросы
          </UiButton>
        </div>
      </UiCard>
    </div>

    <div class="teacher-layout">
      <div class="teacher-stack">
        <UiCard
          :title="questionForm.id ? 'Редактирование вопроса' : 'Новый вопрос'"
        >
          <div class="teacher-stack">
            <UiTextarea
              v-model="questionForm.question"
              label="Текст вопроса"
              maxlength="2000"
              :disabled="!selectedTopicId"
              required
            />

            <div class="teacher-grid--3 teacher-grid">
              <UiSelect
                v-model="questionForm.type"
                label="Тип"
                :options="questionTypeOptions"
                :disabled="!selectedTopicId"
              />

              <UiInput
                v-model="questionForm.points"
                label="Баллы"
                type="number"
                min="0"
                step="0.01"
                :disabled="!selectedTopicId"
              />

              <UiInput
                v-model="questionForm.ordinal"
                label="Порядок"
                type="number"
                min="1"
                step="1"
                :disabled="!selectedTopicId"
              />
            </div>

            <UiInput
              v-if="isTextType"
              v-model="questionForm.correctAnswer"
              label="Правильный ответ"
              hint="Можно указать несколько допустимых вариантов через символ | или с новой строки."
              maxlength="2000"
              :disabled="!selectedTopicId"
            />

            <UiTextarea
              v-if="isMatchingType"
              v-model="questionForm.matchingPairsText"
              label="Пары для сопоставления"
              hint="Каждая строка — одна пара. Разделители: стрелка, =, | или ;."
              placeholder="Лекция → Учебный материал&#10;Тест → Проверка знаний"
              maxlength="4000"
              :disabled="!selectedTopicId"
            />

            <UiCheckbox
              v-model="questionForm.active"
              label="Активен"
              :disabled="!selectedTopicId"
            />

            <div class="teacher-actions teacher-actions--mobile-stack">
              <UiButton
                variant="primary"
                :loading="savingQuestion"
                loading-text="Сохранение..."
                :disabled="!selectedTopicId"
                @click="saveQuestion"
              >
                Сохранить вопрос
              </UiButton>

              <UiButton
                :disabled="!selectedTopicId"
                @click="resetQuestionForm"
              >
                Очистить
              </UiButton>
            </div>
          </div>
        </UiCard>

        <UiCard
          title="Варианты ответа"
          :description="
            !questionForm.id
              ? 'Сначала выберите вопрос в банке.'
              : !isSelectableType
                ? 'Для этого типа вопроса варианты ответа не используются.'
                : 'Редактируйте варианты прямо под вопросом.'
          "
        >
          <div class="teacher-stack">
            <UiEmptyState
              v-if="!questionForm.id"
              description="Выберите вопрос справа, чтобы работать с вариантами ответа."
              compact
            />

            <UiEmptyState
              v-else-if="!isSelectableType"
              description="У текстовых вопросов и вопросов на соответствие отдельного списка вариантов нет."
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
                description="У этого вопроса пока нет вариантов ответа."
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
                    @click="editOption(option)"
                  >
                    Изменить
                  </UiButton>
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
                  label="Правильный"
                />
              </div>

              <div class="teacher-actions teacher-actions--mobile-stack">
                <UiButton
                  variant="primary"
                  :loading="savingOption"
                  loading-text="Сохранение..."
                  @click="saveOption"
                >
                  Сохранить вариант
                </UiButton>

                <UiButton @click="resetOptionForm">
                  Очистить
                </UiButton>
              </div>
            </template>
          </div>
        </UiCard>
      </div>

      <UiCard
        title="Банк вопросов темы"
        :description="questionStats"
      >
        <UiEmptyState
          v-if="loading"
          description="Загрузка вопросов..."
          compact
        />

        <UiEmptyState
          v-else-if="!selectedTopicId"
          description="Выберите тему, чтобы открыть банк вопросов."
          compact
        />

        <UiEmptyState
          v-else-if="!questions.length"
          description="В выбранной теме пока нет вопросов."
          compact
        />

        <div
          v-else
          class="teacher-entity-list"
        >
          <article
            v-for="question in questions"
            :key="question.id"
            class="teacher-entity-card"
            :class="{
              'teacher-entity-card--selected':
                Number(questionForm.id) === Number(question.id),
            }"
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

            <p
              v-if="questionDisplayAnswer(question)"
              class="teacher-entity-card__description"
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
                @click="editQuestion(question)"
              >
                Изменить
              </UiButton>

              <UiButton
                size="sm"
                @click="toggleQuestionActive(question)"
              >
                {{ question.active ? 'Скрыть' : 'Активировать' }}
              </UiButton>
            </div>
          </article>
        </div>
      </UiCard>
    </div>
  </TeacherPageShell>
</template>
