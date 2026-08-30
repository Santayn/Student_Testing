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
  groupsApi,
  questionsApi,
  teachingApi,
  testsApi,
  topicsApi,
} from '@/api'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiCheckbox,
  UiEmptyState,
  UiInput,
  UiSelect,
  UiTable,
  UiTextarea,
} from '@/components/ui'

import TeacherPageShell from '@/components/teacher/TeacherPageShell.vue'

import {
  useTeacherSubjects,
} from '@/composables/useTeacherSubjects'

import {
  listFromResponse,
} from '@/utils/apiData'

const route = useRoute()

const {
  loadingSubjects,
  selectedMembershipId,
  selectedSubjectId,
  selectedMembership,
  membershipOptions,
  loadTeacherSubjects,
} = useTeacherSubjects()

const topics = ref([])
const selectedTopicId = ref('')
const groupTargets = ref([])
const selectedGroupIds = ref([])
const questions = ref([])

const loadingContext = ref(false)
const loadingQuestions = ref(false)
const saving = ref(false)
const initialized = ref(false)

const notice = ref({
  type: 'info',
  message: '',
})

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

const statusOptions = [
  { value: 1, label: 'Черновик' },
  { value: 2, label: 'Активно' },
  { value: 3, label: 'Закрыто' },
  { value: 4, label: 'Приостановлено' },
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

const questionColumns = [
  {
    key: 'ordinal',
    label: '#',
  },
  {
    key: 'type',
    label: 'Тип',
    value: (row) =>
      questionTypeLabel(row.type),
  },
  {
    key: 'question',
    label: 'Вопрос',
  },
  {
    key: 'active',
    label: 'Статус',
    value: (row) =>
      row.active
        ? 'Активен'
        : 'Скрыт',
  },
]

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

function localDateTimeValue(date) {
  const offset =
    date.getTimezoneOffset() * 60_000

  return new Date(
    date.getTime() - offset
  )
    .toISOString()
    .slice(0, 16)
}

function setDefaultDates() {
  const now = new Date()
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

async function loadGroups(assignments) {
  const activeAssignments =
    assignments.filter(
      (item) =>
        Number(item.status) === 1
    )

  const groupIds = [
    ...new Set(
      activeAssignments
        .map(
          (item) =>
            Number(item.groupId)
        )
        .filter(Boolean)
    ),
  ]

  if (!groupIds.length) {
    groupTargets.value = []
    selectedGroupIds.value = []
    return
  }

  const responses =
    await Promise.all(
      groupIds.map(
        (groupId) =>
          groupsApi.getById(groupId)
      )
    )

  const groupsById = new Map(
    responses
      .map((response) => response.data)
      .filter(Boolean)
      .map(
        (group) => [
          Number(group.id),
          group,
        ]
      )
  )

  groupTargets.value =
    groupIds
      .map((groupId) => {
        const related =
          activeAssignments.filter(
            (item) =>
              Number(item.groupId) ===
              Number(groupId)
          )

        const group =
          groupsById.get(
            Number(groupId)
          )

        return {
          groupId,
          groupName:
            group?.name ??
            `Группа #${groupId}`,
          assignmentIds: [
            ...new Set(
              related.map(
                (item) =>
                  Number(item.id)
              )
            ),
          ],
        }
      })
      .sort(
        (left, right) =>
          String(left.groupName)
            .localeCompare(
              String(right.groupName),
              'ru'
            )
      )

  selectedGroupIds.value = []
}

async function loadSubjectContext() {
  topics.value = []
  selectedTopicId.value = ''
  groupTargets.value = []
  selectedGroupIds.value = []
  questions.value = []

  if (!selectedMembership.value) {
    return
  }

  loadingContext.value = true

  try {
    const [topicsResponse, assignmentsResponse] =
      await Promise.all([
        topicsApi.getAll({
          subjectMembershipId:
            selectedMembership.value.id,
        }),
        teachingApi.getAssignments({
          subjectMembershipId:
            selectedMembership.value.id,
          status: 1,
        }),
      ])

    topics.value =
      listFromResponse(topicsResponse)
        .sort(
          (left, right) =>
            Number(left.ordinal ?? 0) -
            Number(right.ordinal ?? 0)
        )

    await loadGroups(
      listFromResponse(
        assignmentsResponse
      )
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

  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось загрузить контекст теста'
      ),
    }
  } finally {
    loadingContext.value = false
  }
}

async function loadQuestions() {
  questions.value = []

  if (!selectedTopicId.value) {
    return
  }

  loadingQuestions.value = true

  try {
    const response =
      await questionsApi.getAll({
        topicId:
          Number(selectedTopicId.value),
      })

    questions.value =
      listFromResponse(response)
        .sort(
          (left, right) =>
            Number(left.ordinal ?? 0) -
            Number(right.ordinal ?? 0)
        )
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось загрузить вопросы темы'
      ),
    }
  } finally {
    loadingQuestions.value = false
  }
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

  if (
    !form.value.title.trim()
  ) {
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

async function createTest() {
  const errorMessage =
    validationError()

  if (errorMessage) {
    notice.value = {
      type: 'danger',
      message: errorMessage,
    }
    return
  }

  const assignmentIds = [
    ...new Set(
      groupTargets.value
        .filter(
          (target) =>
            selectedGroupIds.value.some(
              (id) =>
                Number(id) ===
                Number(target.groupId)
            )
        )
        .flatMap(
          (target) =>
            target.assignmentIds
        )
    ),
  ]

  if (!assignmentIds.length) {
    notice.value = {
      type: 'danger',
      message:
        'Не удалось определить учебные назначения для выбранных групп.',
    }
    return
  }

  saving.value = true

  try {
    const testResponse =
      await testsApi.create({
        title: form.value.title.trim(),
        description:
          form.value.description.trim() ||
          null,
        duration: null,
        attemptsAllowed:
          Number(
            form.value.attemptsAllowed
          ) || 1,
        questionCount:
          Number(
            form.value.questionCount
          ),
        selectionRules: [
          {
            courseLectureId: null,
            topicId:
              Number(
                selectedTopicId.value
              ),
            questionCount:
              Number(
                form.value.questionCount
              ),
            textQuestionCount:
              Number(
                form.value
                  .textQuestionCount
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
            ordinal: 1,
          },
        ],
      })

    const test = testResponse.data

    const availableFromUtc =
      new Date(
        form.value.availableFrom
      ).toISOString()

    const availableUntilUtc =
      new Date(
        form.value.availableUntil
      ).toISOString()

    await Promise.all(
      assignmentIds.map(
        (assignmentId) =>
          testsApi.createAssignments(
            test.id,
            {
              scope: 4,
              courseVersionId: null,
              courseLectureId: null,
              teachingAssignmentId:
                Number(assignmentId),
              availableFromUtc,
              availableUntilUtc,
              status:
                Number(form.value.status),
            }
          )
      )
    )

    notice.value = {
      type: 'success',
      message:
        `Тест #${test.id} создан и назначен выбранным группам.`,
    }
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось создать тест'
      ),
    }
  } finally {
    saving.value = false
  }
}

watch(
  selectedMembershipId,
  () => {
    if (initialized.value) {
      loadSubjectContext()
    }
  }
)

watch(
  selectedTopicId,
  () => {
    if (initialized.value) {
      loadQuestions()
    }
  }
)

onMounted(async () => {
  setDefaultDates()

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
      await loadSubjectContext()
    }

    if (!membershipOptions.value.length) {
      notice.value = {
        type: 'info',
        message:
          'Нет предметов преподавателя для создания тестов.',
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
    title="Создание теста"
    subtitle="Тест создаётся на уровне предмета, собирается из вопросов выбранной темы и назначается активным учебным группам преподавателя."
  >
    <UiAlert
      v-if="notice.message"
      :variant="notice.type"
      :message="notice.message"
      closable
      @close="notice.message = ''"
    />

    <div class="teacher-layout">
      <div class="teacher-stack">
        <UiCard
          title="Контекст теста"
          description="Выберите предмет, группы и тему."
        >
          <div class="teacher-stack">
            <UiSelect
              v-model="selectedMembershipId"
              label="Предмет"
              :options="membershipOptions"
              placeholder="Выберите предмет"
              :disabled="
                loadingSubjects ||
                !membershipOptions.length
              "
            />

            <div>
              <span class="teacher-field-label">
                Группы, которым назначается тест
              </span>

              <UiEmptyState
                v-if="!selectedSubjectId"
                description="Сначала выберите предмет."
                compact
              />

              <UiEmptyState
                v-else-if="
                  loadingContext
                "
                description="Загрузка групп..."
                compact
              />

              <UiEmptyState
                v-else-if="
                  !groupTargets.length
                "
                description="Для этого предмета пока нет активных назначений на учебные группы."
                compact
              />

              <div
                v-else
                class="teacher-selection-grid"
              >
                <UiCheckbox
                  v-for="target in groupTargets"
                  :key="target.groupId"
                  v-model="selectedGroupIds"
                  :value="target.groupId"
                  :label="target.groupName"
                  :description="
                    `Назначений: ${target.assignmentIds.length}`
                  "
                />
              </div>
            </div>

            <div class="teacher-grid">
              <UiSelect
                v-model="selectedTopicId"
                label="Тема"
                :options="topicOptions"
                placeholder="Выберите тему"
                :disabled="!topicOptions.length"
              />

              <div class="teacher-inline-actions">
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
                    name: 'teacher-questions',
                    query: routeQuery(),
                  }"
                >
                  Вопросы темы
                </UiButton>
              </div>
            </div>
          </div>
        </UiCard>

        <UiCard title="Параметры теста">
          <div class="teacher-stack">
            <UiInput
              v-model="form.title"
              label="Название теста"
              maxlength="200"
              required
            />

            <UiTextarea
              v-model="form.description"
              label="Описание"
              maxlength="4000"
            />
          </div>
        </UiCard>

        <UiCard
          title="Правила отбора вопросов"
          :description="ruleSummary"
        >
          <div class="teacher-publication-fields">
            <UiInput
              v-model="form.questionCount"
              label="Всего вопросов"
              type="number"
              min="1"
              step="1"
              required
            />

            <UiInput
              v-model="form.attemptsAllowed"
              label="Попыток"
              type="number"
              min="1"
              step="1"
              required
            />

            <UiInput
              v-model="form.textQuestionCount"
              label="Текстовых"
              type="number"
              min="0"
              step="1"
            />

            <UiInput
              v-model="form.singleAnswerQuestionCount"
              label="С одним ответом"
              type="number"
              min="0"
              step="1"
            />

            <UiInput
              v-model="form.multipleAnswerQuestionCount"
              label="С несколькими ответами"
              type="number"
              min="0"
              step="1"
            />

            <UiInput
              v-model="form.matchingQuestionCount"
              label="На соответствие"
              type="number"
              min="0"
              step="1"
            />
          </div>
        </UiCard>

        <UiCard title="Публикация">
          <div class="teacher-stack">
            <div class="teacher-grid--3 teacher-grid">
              <UiSelect
                v-model="form.status"
                label="Статус назначения"
                :options="statusOptions"
              />

              <UiInput
                v-model="form.availableFrom"
                label="Доступен с"
                type="datetime-local"
                required
              />

              <UiInput
                v-model="form.availableUntil"
                label="Доступен до"
                type="datetime-local"
                required
              />
            </div>

            <UiButton
              variant="primary"
              size="lg"
              :loading="saving"
              loading-text="Создание теста..."
              @click="createTest"
            >
              Создать тест
            </UiButton>
          </div>
        </UiCard>
      </div>

      <UiCard
        title="Вопросы выбранной темы"
        :description="
          `Всего: ${questions.length}. Активных: ${activeQuestions.length}.`
        "
      >
        <UiTable
          :columns="questionColumns"
          :rows="questions"
          :loading="loadingQuestions"
          empty-message="Список вопросов пуст."
          :default-sort="{
            key: 'ordinal',
            direction: 'asc',
          }"
        >
          <template #cell-question="{ row }">
            <strong>{{ row.question }}</strong>
          </template>

          <template #cell-active="{ row }">
            <span
              class="teacher-status"
              :class="{
                'teacher-status--success':
                  row.active,
              }"
            >
              {{ row.active ? 'Активен' : 'Скрыт' }}
            </span>
          </template>
        </UiTable>
      </UiCard>
    </div>
  </TeacherPageShell>
</template>
