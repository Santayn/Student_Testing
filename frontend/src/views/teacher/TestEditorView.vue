<script setup>
import {
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
  UiTextarea,
  UiUnsavedChangesConfirm,
} from '@/components/ui'

import TeacherPageShell from '@/components/teacher/TeacherPageShell.vue'

import {
  useTeacherSubjects,
} from '@/composables/teacher/useTeacherSubjects'

import {
  useUnsavedNavigationGuard,
} from '@/composables/shared/useUnsavedNavigationGuard'

import {
  useTestEditorState,
} from '@/composables/tests/useTestEditorState'

import {
  useTestEditorSaveFlow,
} from '@/composables/tests/useTestEditorSaveFlow'

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
const selectedTopicId = ref('')
const groupTargets = ref([])
const selectedGroupIds = ref([])
const questions = ref([])

const loadingContext = ref(false)
const loadingQuestions = ref(false)
const initialized = ref(false)

const contextRequest = createLatestRequestGuard()
const questionsRequest = createLatestRequestGuard()

const notice = ref({
  type: 'info',
  message: '',
})

const {
  form,
  editorDirty,
  statusOptions,
  topicOptions,
  activeQuestions,
  ruleSummary,
  questionTypeLabel,
  setDefaultDates,
  routeQuery,
  validationError,
  markEditorClean,
  markInitialContextClean,
} = useTestEditorState({
  selectedMembershipId,
  selectedSubjectId,
  selectedMembership,
  selectedTopicId,
  selectedGroupIds,
  topics,
  questions,
})

const {
  confirmVisible:
    navigationConfirmVisible,
  continueEditing:
    continueNavigationEditing,
  discardAndNavigate,
} = useUnsavedNavigationGuard(
  editorDirty
)

async function buildGroupTargets(assignments) {
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
    return []
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

  return groupIds
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
          'Группа без названия',
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
}

async function loadSubjectContext() {
  const requestId =
    contextRequest.begin()

  questionsRequest.invalidate()
  loadingQuestions.value = false

  topics.value = []
  selectedTopicId.value = ''
  groupTargets.value = []
  selectedGroupIds.value = []
  questions.value = []

  const membershipId = Number(
    selectedMembership.value?.id ?? 0
  )

  if (!membershipId) {
    loadingContext.value = false
    return
  }

  loadingContext.value = true

  try {
    const [topicsResponse, assignmentsResponse] =
      await Promise.all([
        topicsApi.getAll({
          subjectMembershipId:
            membershipId,
        }),
        teachingApi.getAssignments({
          subjectMembershipId:
            membershipId,
          status: 1,
        }),
      ])

    const nextTopics =
      listFromResponse(topicsResponse)
        .sort(
          (left, right) =>
            Number(left.ordinal ?? 0) -
            Number(right.ordinal ?? 0)
        )

    const nextGroupTargets =
      await buildGroupTargets(
        listFromResponse(
          assignmentsResponse
        )
      )

    if (
      !contextRequest.isCurrent(
        requestId
      )
    ) {
      return
    }

    topics.value = nextTopics
    groupTargets.value =
      nextGroupTargets
    selectedGroupIds.value = []

    const preferredTopicId =
      route.query.topicId

    if (
      preferredTopicId &&
      nextTopics.some(
        (item) =>
          String(item.id) ===
          String(preferredTopicId)
      )
    ) {
      selectedTopicId.value =
        String(preferredTopicId)
    } else if (
      nextTopics.length === 1
    ) {
      selectedTopicId.value =
        String(nextTopics[0].id)
    }
  } catch (error) {
    if (
      !contextRequest.isCurrent(
        requestId
      )
    ) {
      return
    }

    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось загрузить контекст теста'
      ),
    }
  } finally {
    if (
      contextRequest.isCurrent(
        requestId
      )
    ) {
      loadingContext.value = false
    }
  }
}

async function loadQuestions() {
  const requestId =
    questionsRequest.begin()

  questions.value = []

  const topicId = Number(
    selectedTopicId.value || 0
  )

  if (!topicId) {
    loadingQuestions.value = false
    return
  }

  loadingQuestions.value = true

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
        'Не удалось загрузить вопросы темы'
      ),
    }
  } finally {
    if (
      questionsRequest.isCurrent(
        requestId
      )
    ) {
      loadingQuestions.value = false
    }
  }
}

const {
  saving,
  createTest,
} = useTestEditorSaveFlow({
  form,
  selectedTopicId,
  selectedGroupIds,
  groupTargets,
  validationError,
  ensureSelectedMembershipActive,
  markEditorClean,
  notice,
})

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
  markEditorClean()

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
  } finally {
    markInitialContextClean()
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
              :disabled="loadingSubjects || !membershipOptions.length"
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
                v-else-if="loadingContext"
                description="Загрузка групп..."
                compact
              />

              <UiEmptyState
                v-else-if="!groupTargets.length"
                description="Для этого предмета пока нет активных назначений на учебные группы."
                compact
              />

              <div
                v-else
                class="teacher-selection-grid"
              >
                <UiCheckbox
                  mode="multiple"
                  v-for="target in groupTargets"
                  :key="target.groupId"
                  v-model="selectedGroupIds"
                  :value="target.groupId"
                  :label="target.groupName"
                  :description="`Назначений: ${target.assignmentIds.length}`"
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
          <div class="teacher-grid--3 teacher-grid">
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

        <UiCard title="Публикация и доступ">
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
        :description="`Всего: ${questions.length}. Активных: ${activeQuestions.length}.`"
      >
        <UiEmptyState
          v-if="loadingQuestions"
          description="Загрузка вопросов..."
          compact
        />

        <UiEmptyState
          v-else-if="!selectedTopicId"
          description="Выберите тему, чтобы проверить доступный банк вопросов."
          compact
        />

        <UiEmptyState
          v-else-if="!questions.length"
          description="Список вопросов пуст."
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

            <div class="teacher-entity-card__meta">
              <span>Баллы: {{ question.points }}</span>
              <span>ID: {{ question.id }}</span>
            </div>
          </article>
        </div>
      </UiCard>
    </div>
  </TeacherPageShell>

  <UiUnsavedChangesConfirm
    v-model="navigationConfirmVisible"
    title="Есть несохранённый тест"
    message="Если покинуть страницу сейчас, несохранённые параметры теста будут потеряны."
    continue-label="Продолжить редактирование"
    discard-label="Покинуть без сохранения"
    :busy="saving"
    @continue="continueNavigationEditing"
    @discard="discardAndNavigate"
  />
</template>
