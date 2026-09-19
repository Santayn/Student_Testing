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
  testsApi,
  topicsApi,
} from '@/api'

import {
  UiAlert,
  UiButton,
  UiCard,
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

const tests = ref([])
const topics = ref([])
const questions = ref([])
const selectedTestId = ref('')
const selectedTopicId = ref('')
const currentTest = ref(null)

const loading = ref(false)
const loadingTests = ref(false)
const loadingQuestions = ref(false)
const saving = ref(false)
const deletingId = ref(null)
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
})

const selectedTest = computed(() => {
  return tests.value.find(
    (test) =>
      String(test.id) ===
      String(selectedTestId.value)
  ) ?? currentTest.value
})

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
    return 'Выберите тест и тему, чтобы изменить правила отбора.'
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

const testColumns = [
  {
    key: 'id',
    label: 'ID',
  },
  {
    key: 'title',
    label: 'Тест',
  },
  {
    key: 'questionCount',
    label: 'Вопросов',
  },
  {
    key: 'attemptsAllowed',
    label: 'Попыток',
  },
  {
    key: 'actions',
    label: 'Действия',
    sortable: false,
  },
]

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

function resetForm() {
  currentTest.value = null
  selectedTestId.value = ''
  selectedTopicId.value = ''
  questions.value = []
  form.value = {
    title: '',
    description: '',
    questionCount: 1,
    attemptsAllowed: 1,
    textQuestionCount: 0,
    singleAnswerQuestionCount: 0,
    multipleAnswerQuestionCount: 0,
    matchingQuestionCount: 0,
  }
}

function selectionRulePayload() {
  return [
    {
      courseLectureId: null,
      topicId:
        Number(selectedTopicId.value),
      questionCount:
        Number(form.value.questionCount),
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
      ordinal: 1,
    },
  ]
}

function validationError() {
  if (!selectedTestId.value) {
    return 'Выберите тест.'
  }

  if (!selectedTopicId.value) {
    return 'Выберите тему.'
  }

  if (!form.value.title.trim()) {
    return 'Введите название теста.'
  }

  const total =
    Number(form.value.questionCount)

  if (total < 1) {
    return 'Количество вопросов должно быть больше нуля.'
  }

  if (
    Number(form.value.attemptsAllowed) < 1
  ) {
    return 'Количество попыток должно быть больше нуля.'
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

  return ''
}

async function loadSubjectContext() {
  topics.value = []
  tests.value = []
  resetForm()

  if (!selectedMembership.value) {
    return
  }

  loading.value = true

  try {
    const [topicsResponse, testsResponse] =
      await Promise.all([
        topicsApi.getAll({
          subjectMembershipId:
            selectedMembership.value.id,
        }),
        testsApi.getAll({
          subjectId:
            selectedSubjectId.value,
        }),
      ])

    topics.value =
      listFromResponse(topicsResponse)
        .sort(
          (left, right) =>
            Number(left.ordinal ?? 0) -
            Number(right.ordinal ?? 0)
        )

    tests.value =
      listFromResponse(testsResponse)

    const preferredTestId =
      route.query.testId

    if (
      preferredTestId &&
      tests.value.some(
        (item) =>
          String(item.id) ===
          String(preferredTestId)
      )
    ) {
      await selectTest(preferredTestId)
    }
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось загрузить тесты предмета'
      ),
    }
  } finally {
    loading.value = false
  }
}

async function reloadTests() {
  if (!selectedSubjectId.value) {
    tests.value = []
    return
  }

  loadingTests.value = true

  try {
    const response =
      await testsApi.getAll({
        subjectId:
          selectedSubjectId.value,
      })

    tests.value =
      listFromResponse(response)
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось обновить список тестов'
      ),
    }
  } finally {
    loadingTests.value = false
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

async function selectTest(testId) {
  selectedTestId.value =
    String(testId)
  loading.value = true

  try {
    const [testResponse, rulesResponse] =
      await Promise.all([
        testsApi.getById(testId),
        testsApi.getSelectionRules(testId),
      ])

    const test = testResponse.data
    const rules =
      listFromResponse(rulesResponse)
    const primaryRule =
      rules[0] ?? null

    currentTest.value = test
    form.value.title =
      test.title ?? ''
    form.value.description =
      test.description ?? ''
    form.value.questionCount =
      Number(test.questionCount ?? 1)
    form.value.attemptsAllowed =
      Number(test.attemptsAllowed ?? 1)
    form.value.textQuestionCount =
      Number(
        primaryRule?.textQuestionCount ??
          0
      )
    form.value.singleAnswerQuestionCount =
      Number(
        primaryRule
          ?.singleAnswerQuestionCount ??
          0
      )
    form.value.multipleAnswerQuestionCount =
      Number(
        primaryRule
          ?.multipleAnswerQuestionCount ??
          0
      )
    form.value.matchingQuestionCount =
      Number(
        primaryRule
          ?.matchingQuestionCount ??
          0
      )

    selectedTopicId.value =
      primaryRule?.topicId
        ? String(primaryRule.topicId)
        : ''

    await loadQuestions()
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось загрузить тест'
      ),
    }
  } finally {
    loading.value = false
  }
}

async function saveTest() {
  const errorMessage =
    validationError()

  if (errorMessage) {
    notice.value = {
      type: 'danger',
      message: errorMessage,
    }
    return
  }

  saving.value = true

  try {
    const testId =
      Number(selectedTestId.value)

    await testsApi.updateSelectionRules(
      testId,
      selectionRulePayload()
    )

    const response =
      await testsApi.update(testId, {
        title: form.value.title.trim(),
        description:
          form.value.description.trim() ||
          null,
        duration:
          currentTest.value?.duration ??
          null,
        attemptsAllowed:
          Number(
            form.value.attemptsAllowed
          ),
        questionCount:
          Number(
            form.value.questionCount
          ),
      })

    currentTest.value =
      response.data

    await reloadTests()

    notice.value = {
      type: 'success',
      message: 'Параметры теста обновлены.',
    }
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось обновить тест'
      ),
    }
  } finally {
    saving.value = false
  }
}

async function deleteTest(testId) {
  const target =
    tests.value.find(
      (test) =>
        Number(test.id) ===
        Number(testId)
    ) ?? selectedTest.value

  const name =
    target?.title
      ? ` «${target.title}»`
      : ` #${testId}`

  if (
    !window.confirm(
      `Удалить тест${name}?`
    )
  ) {
    return
  }

  deletingId.value =
    Number(testId)

  try {
    await testsApi.delete(testId)

    if (
      String(selectedTestId.value) ===
      String(testId)
    ) {
      resetForm()
    }

    await reloadTests()

    notice.value = {
      type: 'success',
      message: 'Тест удалён.',
    }
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось удалить тест'
      ),
    }
  } finally {
    deletingId.value = null
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
          'Нет предметов преподавателя для управления тестами.',
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
    title="Тесты"
    subtitle="Управление созданными тестами: параметры, правила отбора вопросов и удаление."
  >
    <template #actions>
      <UiButton
        :to="{
          name: 'teacher-test-create',
          query: {
            subjectId: selectedSubjectId,
            subjectMembershipId:
              selectedMembershipId,
          },
        }"
      >
        Создать тест
      </UiButton>
    </template>

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
          title="Тесты предмета"
          description="Выберите предмет и тест для изменения."
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

            <UiTable
              :columns="testColumns"
              :rows="tests"
              :loading="
                loading ||
                loadingTests
              "
              empty-message="Для выбранного предмета тестов пока нет."
              :default-sort="{
                key: 'title',
                direction: 'asc',
              }"
            >
              <template #cell-title="{ row }">
                <strong>{{ row.title }}</strong>
              </template>

              <template #cell-actions="{ row }">
                <div class="teacher-actions">
                  <UiButton
                    size="sm"
                    :variant="
                      String(row.id) ===
                      String(selectedTestId)
                        ? 'primary'
                        : 'secondary'
                    "
                    @click="selectTest(row.id)"
                  >
                    Изменить
                  </UiButton>

                  <UiButton
                    size="sm"
                    variant="danger"
                    :loading="
                      deletingId ===
                      Number(row.id)
                    "
                    loading-text="Удаление..."
                    @click="deleteTest(row.id)"
                  >
                    Удалить
                  </UiButton>
                </div>
              </template>
            </UiTable>
          </div>
        </UiCard>

        <UiCard
          title="Параметры теста"
          :description="
            selectedTest
              ? `Тест #${selectedTest.id}`
              : 'Выберите тест в таблице.'
          "
        >
          <UiEmptyState
            v-if="!selectedTestId"
            description="Выберите тест для редактирования параметров."
            compact
          />

          <div
            v-else
            class="teacher-stack"
          >
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
            </div>
          </div>
        </UiCard>

        <UiCard
          title="Правила отбора вопросов"
          :description="ruleSummary"
        >
          <UiEmptyState
            v-if="!selectedTestId"
            description="Выберите тест, чтобы изменить правила отбора."
            compact
          />

          <div
            v-else
            class="teacher-stack"
          >
            <UiSelect
              v-model="selectedTopicId"
              label="Тема"
              :options="topicOptions"
              placeholder="Выберите тему"
              :disabled="!topicOptions.length"
            />

            <div class="teacher-publication-fields">
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

            <div class="teacher-actions">
              <UiButton
                variant="primary"
                size="lg"
                :loading="saving"
                loading-text="Сохранение..."
                @click="saveTest"
              >
                Сохранить изменения
              </UiButton>

              <UiButton
                variant="danger"
                size="lg"
                :loading="
                  deletingId ===
                  Number(selectedTestId)
                "
                loading-text="Удаление..."
                @click="deleteTest(selectedTestId)"
              >
                Удалить тест
              </UiButton>
            </div>
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
              {{
                row.active
                  ? 'Активен'
                  : 'Скрыт'
              }}
            </span>
          </template>
        </UiTable>
      </UiCard>
    </div>
  </TeacherPageShell>
</template>
