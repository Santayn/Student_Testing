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
  topicsApi,
} from '@/api'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiDialog,
  UiEmptyState,
  UiFilterBar,
  UiInput,
  UiSelect,
  UiTextarea,
  UiUnsavedChangesConfirm,
  useOverlayForm,
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
  selectedMembership,
  selectedSubjectId,
  selectedSubject,
  membershipOptions,
  ensureSelectedMembershipActive,
  loadTeacherSubjects,
} = useTeacherSubjects()

const topics = ref([])
const loading = ref(false)
const initialized = ref(false)

const searchQuery = ref('')
const descriptionFilter = ref('all')
const sortMode = ref('ordinal')

const deleteTarget = ref(null)
const deleteConfirmVisible = ref(false)
const deletingId = ref(null)
const deleteError = ref('')
const formError = ref('')
const handledRouteTopicKey = ref('')

const topicsRequest = createLatestRequestGuard()

const notice = ref({
  type: 'info',
  message: '',
})

const {
  form,
  model: topicDialogModel,
  isOpen: topicDialogOpen,
  isCreate,
  saving,
  confirmCloseVisible,
  openCreate,
  openEdit,
  requestClose,
  closeImmediately,
  discardAndClose,
  continueEditing,
  beginSaving,
  finishSaving,
  failSaving,
} = useOverlayForm({
  createDefault: () => ({
    id: null,
    ordinal: 1,
    name: '',
    description: '',
  }),
  mapEntity: (topic) => ({
    id: topic.id,
    ordinal: Number(topic.ordinal ?? 1),
    name: topic.name ?? '',
    description: topic.description ?? '',
  }),
})

const descriptionOptions = [
  { value: 'all', label: 'Все темы' },
  { value: 'with-description', label: 'С описанием' },
  { value: 'without-description', label: 'Без описания' },
]

const sortOptions = [
  { value: 'ordinal', label: 'По порядку' },
  { value: 'name-asc', label: 'Название А–Я' },
  { value: 'name-desc', label: 'Название Я–А' },
]

const canEdit = computed(() => {
  return Boolean(selectedMembership.value)
})

const contextHint = computed(() => {
  if (!selectedMembership.value) {
    return 'Выберите предмет преподавателя. Темы группируют вопросы банка и используются в правилах формирования тестов.'
  }

  if (!selectedSubject.value) {
    return `Выбрано назначение #${selectedMembership.value.id}.`
  }

  if (!topics.value.length) {
    return `У предмета «${selectedSubject.value.name}» в выбранном назначении пока нет тем.`
  }

  return `Предмет «${selectedSubject.value.name}». Тем в выбранном назначении: ${topics.value.length}.`
})

const hasActiveFilters = computed(() => {
  return Boolean(searchQuery.value.trim()) ||
    descriptionFilter.value !== 'all' ||
    sortMode.value !== 'ordinal'
})

const filteredTopics = computed(() => {
  const query = searchQuery.value
    .trim()
    .toLocaleLowerCase('ru-RU')

  const result = topics.value.filter((topic) => {
    const description = String(topic.description ?? '').trim()

    if (
      descriptionFilter.value === 'with-description' &&
      !description
    ) {
      return false
    }

    if (
      descriptionFilter.value === 'without-description' &&
      description
    ) {
      return false
    }

    if (!query) {
      return true
    }

    const haystack = [
      topic.ordinal,
      topic.name,
      topic.description,
    ]
      .filter((value) => value !== null && value !== undefined)
      .join(' ')
      .toLocaleLowerCase('ru-RU')

    return haystack.includes(query)
  })

  return [...result].sort((left, right) => {
    if (sortMode.value === 'name-asc') {
      return String(left.name ?? '').localeCompare(
        String(right.name ?? ''),
        'ru'
      )
    }

    if (sortMode.value === 'name-desc') {
      return String(right.name ?? '').localeCompare(
        String(left.name ?? ''),
        'ru'
      )
    }

    return Number(left.ordinal ?? 0) - Number(right.ordinal ?? 0)
  })
})

const filterResultText = computed(() => {
  if (!selectedMembership.value) {
    return 'Сначала выберите предмет преподавателя.'
  }

  return `Показано: ${filteredTopics.value.length} из ${topics.value.length}`
})

const topicDialogTitle = computed(() => {
  return isCreate.value ? 'Новая тема' : 'Редактирование темы'
})

function routeQuery(topicId = null) {
  const query = {}

  if (selectedSubjectId.value) {
    query.subjectId = selectedSubjectId.value
  }

  if (selectedMembership.value) {
    query.subjectMembershipId = selectedMembership.value.id
  }

  if (topicId) {
    query.topicId = topicId
  }

  return query
}

function nextOrdinal() {
  return topics.value.reduce(
    (max, topic) =>
      Math.max(max, Number(topic.ordinal ?? 0)),
    0
  ) + 1
}

function resetFilters() {
  searchQuery.value = ''
  descriptionFilter.value = 'all'
  sortMode.value = 'ordinal'
}

function openCreateTopic() {
  if (!canEdit.value) {
    notice.value = {
      type: 'danger',
      message: 'Выберите предмет преподавателя.',
    }
    return
  }

  formError.value = ''
  openCreate({
    ordinal: nextOrdinal(),
  })
}

function openEditTopic(topic) {
  formError.value = ''
  openEdit(topic)
}

function requestDeleteTopic(topic) {
  deleteTarget.value = topic
  deleteError.value = ''
  deleteConfirmVisible.value = true
}

function closeDeleteDialog() {
  if (deletingId.value !== null) {
    return
  }

  deleteConfirmVisible.value = false
  deleteTarget.value = null
  deleteError.value = ''
}

function topicFormValidationMessage() {
  const membership = selectedMembership.value
  const ordinal = Number(form.ordinal)
  const name = String(form.name ?? '').trim()
  const description = String(form.description ?? '').trim()

  if (!membership) {
    return 'Выберите предмет преподавателя.'
  }

  if (!Number.isInteger(ordinal) || ordinal <= 0) {
    return 'Порядковый номер темы должен быть целым числом больше нуля.'
  }

  if (!name) {
    return 'Введите название темы.'
  }

  if (name.length > 200) {
    return 'Название темы не может быть длиннее 200 символов.'
  }

  if (description.length > 2000) {
    return 'Описание темы не может быть длиннее 2000 символов.'
  }

  const duplicateOrdinal = topics.value.find(
    (topic) =>
      Number(topic.ordinal) === ordinal &&
      String(topic.id) !== String(form.id ?? '')
  )

  if (duplicateOrdinal) {
    return 'Тема с таким порядковым номером уже существует в выбранном назначении преподавателя.'
  }

  return ''
}

async function resolveRouteTopic(membershipId, nextTopics) {
  const topicId = route.query.topicId

  if (!topicId) {
    return null
  }

  const routeTopicKey = `${membershipId}:${topicId}`

  if (handledRouteTopicKey.value === routeTopicKey) {
    return null
  }

  handledRouteTopicKey.value = routeTopicKey

  let topic = nextTopics.find(
    (item) => String(item.id) === String(topicId)
  )

  if (topic) {
    return topic
  }

  try {
    const topicResponse = await topicsApi.getOne(topicId)
    const candidate = topicResponse.data

    if (
      candidate &&
      String(candidate.subjectMembershipId) === String(membershipId)
    ) {
      topic = candidate
    }
  } catch {
    /*
     * GET /topics/{id} возвращает 400, если темы нет.
     * Для workspace достаточно показать предупреждение.
     */
  }

  return topic
}

async function loadTopics({ openRouteTopic = false } = {}) {
  const requestId = topicsRequest.begin()
  const membershipId = Number(
    selectedMembership.value?.id ?? 0
  )

  if (!membershipId) {
    topics.value = []
    loading.value = false
    return
  }

  loading.value = true

  try {
    const response = await topicsApi.getAll({
      subjectMembershipId: membershipId,
    })

    const nextTopics = listFromResponse(response).sort(
      (left, right) =>
        Number(left.ordinal ?? 0) - Number(right.ordinal ?? 0)
    )

    let routeTopic = null

    if (openRouteTopic) {
      routeTopic = await resolveRouteTopic(
        membershipId,
        nextTopics
      )
    }

    if (!topicsRequest.isCurrent(requestId)) {
      return
    }

    topics.value = nextTopics

    if (openRouteTopic && route.query.topicId) {
      if (routeTopic) {
        openEditTopic(routeTopic)
      } else {
        notice.value = {
          type: 'warning',
          message:
            'Тема из ссылки не относится к выбранному назначению преподавателя или больше не существует.',
        }
      }
    }
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
  } finally {
    if (topicsRequest.isCurrent(requestId)) {
      loading.value = false
    }
  }
}

async function saveTopic() {
  formError.value = topicFormValidationMessage()

  if (formError.value) {
    return
  }

  const membership = selectedMembership.value
  const ordinal = Number(form.ordinal)
  const name = String(form.name ?? '').trim()
  const description = String(form.description ?? '').trim()

  const payload = {
    /*
     * subjectId и subjectMembershipId всегда берутся
     * из одного membership-контекста.
     */
    subjectId: Number(membership.subjectId),
    courseLectureId: null,
    subjectMembershipId: Number(membership.id),
    ordinal,
    name,
    description: description || null,
  }

  beginSaving()

  try {
    await ensureSelectedMembershipActive()

    if (form.id) {
      await topicsApi.update(form.id, payload)
      notice.value = {
        type: 'success',
        message: 'Тема обновлена.',
      }
    } else {
      await topicsApi.create(payload)
      notice.value = {
        type: 'success',
        message: 'Тема создана.',
      }
    }

    await loadTopics()
    finishSaving({ close: true })
  } catch (error) {
    formError.value = topicSaveErrorMessage(error)
    failSaving()
  }
}

async function deleteTopic() {
  const topic = deleteTarget.value

  if (!topic || deletingId.value !== null) {
    return
  }

  deletingId.value = topic.id
  deleteError.value = ''

  try {
    await ensureSelectedMembershipActive()
    await topicsApi.remove(topic.id)

    notice.value = {
      type: 'success',
      message: 'Тема удалена.',
    }

    deleteConfirmVisible.value = false
    deleteTarget.value = null
    await loadTopics()
  } catch (error) {
    deleteError.value = topicDeleteErrorMessage(error)
  } finally {
    deletingId.value = null
  }
}

function topicSaveErrorMessage(error) {
  return getApiErrorMessage(
    error,
    form.id
      ? 'Не удалось обновить тему.'
      : 'Не удалось создать тему.'
  )
}

function topicDeleteErrorMessage(error) {
  return getApiErrorMessage(
    error,
    'Не удалось удалить тему.'
  )
}

watch(
  selectedMembershipId,
  () => {
    if (!initialized.value) {
      return
    }

    if (topicDialogOpen.value) {
      closeImmediately()
    }

    closeDeleteDialog()
    resetFilters()
    loadTopics({ openRouteTopic: true })
  }
)

onMounted(async () => {
  try {
    await loadTeacherSubjects({
      preferredSubjectId: route.query.subjectId,
      preferredMembershipId: route.query.subjectMembershipId,
    })

    initialized.value = true

    if (selectedMembershipId.value) {
      await loadTopics({ openRouteTopic: true })
    }

    if (!membershipOptions.value.length) {
      notice.value = {
        type: 'info',
        message:
          'Нет активных назначений преподавателя на предметы для управления темами.',
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
    title="Темы предмета"
    subtitle="Просматривайте и находите темы выбранного предмета. Создание и редактирование открываются поверх workspace и не сбрасывают текущие фильтры."
  >
    <UiAlert
      v-if="notice.message"
      :variant="notice.type"
      :message="notice.message"
      closable
      @close="notice.message = ''"
    />

    <UiCard
      title="Контекст предмета"
      :description="contextHint"
    >
      <div class="teacher-topic-context">
        <UiSelect
          v-model="selectedMembershipId"
          label="Предмет преподавателя"
          :options="membershipOptions"
          placeholder="Выберите предмет"
          :disabled="loadingSubjects || !membershipOptions.length"
        />

        <div
          v-if="selectedMembership"
          class="teacher-inline-actions teacher-inline-actions--mobile-stack teacher-topic-context__actions"
        >
          <UiButton
            :to="{
              name: 'teacher-questions',
              query: routeQuery(),
            }"
          >
            Банк вопросов
          </UiButton>

          <UiButton
            :to="{
              name: 'teacher-test-create',
              query: routeQuery(),
            }"
          >
            Создать тест
          </UiButton>
        </div>
      </div>
    </UiCard>

    <UiCard
      title="Темы"
      :description="
        selectedSubject && selectedMembership
          ? `Рабочее пространство предмета «${selectedSubject.name}».`
          : 'Выберите предмет преподавателя, чтобы открыть список тем.'
      "
    >
      <div class="teacher-stack">
        <UiFilterBar
          v-model="searchQuery"
          search-placeholder="Название, описание или номер темы"
          :result-text="filterResultText"
          :reset-disabled="!hasActiveFilters"
          @reset="resetFilters"
        >
          <template #filters>
            <UiSelect
              v-model="descriptionFilter"
              label="Описание"
              :options="descriptionOptions"
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
              variant="primary"
              size="sm"
              icon="pi pi-plus"
              label="Добавить тему"
              :disabled="!canEdit"
              @click="openCreateTopic"
            />
          </template>
        </UiFilterBar>

        <UiEmptyState
          v-if="loading"
          description="Загрузка тем..."
          compact
        />

        <UiEmptyState
          v-else-if="!selectedMembership"
          description="Выберите предмет преподавателя, чтобы увидеть его темы."
          compact
        />

        <UiEmptyState
          v-else-if="!topics.length"
          description="Для выбранного назначения преподавателя пока нет тем. Добавьте первую тему кнопкой выше."
          compact
        />

        <UiEmptyState
          v-else-if="!filteredTopics.length"
          description="По текущему поиску и фильтрам темы не найдены."
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
            v-for="topic in filteredTopics"
            :key="topic.id"
            class="teacher-entity-card"
          >
            <div class="teacher-entity-card__header">
              <div class="teacher-entity-card__heading">
                <span class="teacher-entity-card__eyebrow">
                  Тема {{ topic.ordinal }}
                </span>
                <h3 class="teacher-entity-card__title">
                  {{ topic.name }}
                </h3>
              </div>
            </div>

            <p class="teacher-entity-card__description">
              {{ topic.description || 'Описание пока не добавлено.' }}
            </p>

            <div class="teacher-entity-card__actions">
              <UiButton
                size="sm"
                :to="{
                  name: 'teacher-questions',
                  query: routeQuery(topic.id),
                }"
              >
                Вопросы
              </UiButton>

              <UiButton
                size="sm"
                :to="{
                  name: 'teacher-test-create',
                  query: routeQuery(topic.id),
                }"
              >
                Создать тест
              </UiButton>

              <UiButton
                size="sm"
                icon="pi pi-pencil"
                label="Изменить"
                @click="openEditTopic(topic)"
              />

              <UiButton
                variant="danger"
                size="sm"
                icon="pi pi-trash"
                label="Удалить"
                :loading="deletingId === topic.id"
                loading-text="Удаление..."
                @click="requestDeleteTopic(topic)"
              />
            </div>
          </article>
        </div>
      </div>
    </UiCard>

    <UiDialog
      v-model="topicDialogModel"
      :title="topicDialogTitle"
      width="38rem"
      :dismissable-mask="false"
    >
      <form
        class="teacher-stack"
        @submit.prevent="saveTopic"
      >
        <UiAlert
          v-if="formError"
          variant="danger"
          :message="formError"
        />

        <UiAlert
          variant="info"
          :message="
            selectedSubject
              ? `Тема будет сохранена в предмете «${selectedSubject.name}».`
              : 'Тема будет сохранена в текущем назначении преподавателя.'
          "
        />

        <div class="teacher-grid">
          <UiInput
            v-model="form.ordinal"
            label="Порядок"
            type="number"
            min="1"
            step="1"
            :disabled="!canEdit || saving"
            required
          />

          <UiInput
            v-model="form.name"
            label="Название темы"
            maxlength="200"
            :disabled="!canEdit || saving"
            required
          />
        </div>

        <UiTextarea
          v-model="form.description"
          label="Описание"
          maxlength="2000"
          :rows="6"
          auto-resize
          :disabled="!canEdit || saving"
        />

        <div class="teacher-actions teacher-actions--mobile-stack teacher-topic-form__actions">
          <UiButton
            variant="secondary"
            label="Отмена"
            :disabled="saving"
            @click="requestClose"
          />

          <UiButton
            type="submit"
            variant="primary"
            :label="isCreate ? 'Создать тему' : 'Сохранить изменения'"
            :loading="saving"
            loading-text="Сохранение..."
            :disabled="!canEdit"
          />
        </div>
      </form>
    </UiDialog>

    <UiUnsavedChangesConfirm
      v-model="confirmCloseVisible"
      :busy="saving"
      @continue="continueEditing"
      @discard="discardAndClose"
    />

    <UiDialog
      v-model="deleteConfirmVisible"
      title="Удалить тему?"
      width="31rem"
      :closable="deletingId === null"
      :close-on-escape="deletingId === null"
      :dismissable-mask="false"
    >
      <div class="teacher-stack">
        <UiAlert
          v-if="deleteError"
          variant="danger"
          :message="deleteError"
        />

        <p class="teacher-topic-delete-copy">
          Тема
          <strong>«{{ deleteTarget?.name }}»</strong>
          будет удалена. Если backend запрещает удаление темы, которая уже используется вопросами или тестами, сообщение об этом останется в этом окне.
        </p>
      </div>

      <template #footer>
        <div class="teacher-actions teacher-actions--mobile-stack teacher-topic-delete-actions">
          <UiButton
            variant="secondary"
            label="Отмена"
            :disabled="deletingId !== null"
            @click="closeDeleteDialog"
          />

          <UiButton
            variant="danger"
            label="Удалить тему"
            :loading="deletingId !== null"
            loading-text="Удаление..."
            @click="deleteTopic"
          />
        </div>
      </template>
    </UiDialog>
  </TeacherPageShell>
</template>

<style scoped>
.teacher-topic-context {
  min-width: 0;

  display: grid;
  grid-template-columns:
    minmax(260px, 0.85fr)
    minmax(0, 1fr);
  gap: 14px;
  align-items: end;
}

.teacher-topic-context__actions {
  justify-content: flex-end;
}

.teacher-topic-form__actions,
.teacher-topic-delete-actions {
  justify-content: flex-end;
}

.teacher-topic-delete-copy {
  margin: 0;

  color: var(--st-text-secondary);

  font-size: 14px;
  line-height: 1.65;
  overflow-wrap: anywhere;
}

.teacher-topic-delete-copy strong {
  color: var(--st-text);
}

@media (max-width: 900px) {
  .teacher-topic-context {
    grid-template-columns: 1fr;
  }

  .teacher-topic-context__actions {
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .teacher-topic-context__actions,
  .teacher-topic-context__actions > * {
    width: 100%;
  }

  .teacher-topic-form__actions,
  .teacher-topic-delete-actions {
    width: 100%;
  }
}
</style>
