<script setup>
import {
  computed,
  onBeforeUnmount,
  onMounted,
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
  apiFieldError,
} from '@/utils/apiErrorPresentation'

import {
  clearFormFieldError,
  focusFirstInvalidField,
} from '@/utils/formErrorLifecycle'

import {
  UiActionMenu,
  UiAlert,
  UiButton,
  UiCard,
  UiDialog,
  UiEmptyState,
  UiLoadingState,
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
} from '@/composables/teacher/useTeacherSubjects'

import {
  useTeacherTopicsData,
} from '@/composables/teacher/useTeacherTopicsData'

import {
  useTeacherTopicMutations,
} from '@/composables/teacher/useTeacherTopicMutations'

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

const initialized = ref(false)
const topicFormElement = ref(null)

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

function openEditTopic(topic) {
  formError.value = ''
  openEdit(topic)
}

const {
  descriptionOptions,
  sortOptions,
  topics,
  loading,
  notice,
  searchQuery,
  descriptionFilter,
  sortMode,
  canEdit,
  contextHint,
  hasActiveFilters,
  filteredTopics,
  filterResultText,
  routeQuery,
  nextOrdinal,
  resetFilters,
  loadTopics,
  dispose: disposeTopicsData,
} = useTeacherTopicsData({
  route,
  selectedMembership,
  selectedSubjectId,
  selectedSubject,
  onOpenRouteTopic: openEditTopic,
})

function topicActionItems(topic) {
  return [
    {
      label: 'Изменить',
      icon: 'pi pi-pencil',
      command: () => openEditTopic(topic),
    },
    {
      label: 'Удалить',
      icon: 'pi pi-trash',
      danger: true,
      command: () => requestDeleteTopic(topic),
    },
  ]
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
  formFieldErrors.value = {}
  openCreate({
    ordinal: nextOrdinal(),
  })
}

const topicDialogTitle = computed(() =>
  isCreate.value
    ? 'Создание темы'
    : 'Редактирование темы'
)

async function focusTopicFormErrors() {
  await focusFirstInvalidField(
    topicFormElement.value
  )
}

const {
  deleteTarget,
  deleteConfirmVisible,
  deletingId,
  deleteError,
  formError,
  formFieldErrors,
  requestDeleteTopic,
  closeDeleteDialog,
  saveTopic,
  deleteTopic,
} = useTeacherTopicMutations({
  form,
  selectedMembership,
  topics,
  notice,
  ensureSelectedMembershipActive,
  beginSaving,
  saving,
  finishSaving,
  failSaving,
  loadTopics,
  focusFormErrors:
    focusTopicFormErrors,
})

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

onBeforeUnmount(disposeTopicsData)

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
    subtitle="Создавайте, редактируйте и находите темы выбранного предмета."
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

        <UiLoadingState
          v-if="loading"
          compact
          label="Загрузка тем..."
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

            <p
              v-if="topic.description"
              class="teacher-entity-card__description"
            >
              {{ topic.description }}
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

              <UiActionMenu
                :items="topicActionItems(topic)"
                :aria-label="`Действия темы «${topic.name}»`"
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
        ref="topicFormElement"
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
            @update:model-value="clearFormFieldError(formFieldErrors, formError, 'ordinal')"
            :error="apiFieldError({ fieldErrors: formFieldErrors }, 'ordinal')"
            label="Порядок"
            type="number"
            min="1"
            step="1"
            :disabled="!canEdit || saving"
            required
          />

          <UiInput
            v-model="form.name"
            @update:model-value="clearFormFieldError(formFieldErrors, formError, 'name')"
            :error="apiFieldError({ fieldErrors: formFieldErrors }, 'name')"
            label="Название темы"
            maxlength="200"
            :disabled="!canEdit || saving"
            required
          />
        </div>

        <UiTextarea
          v-model="form.description"
          @update:model-value="clearFormFieldError(formFieldErrors, formError, 'description')"
            :error="apiFieldError({ fieldErrors: formFieldErrors }, 'description')"
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
