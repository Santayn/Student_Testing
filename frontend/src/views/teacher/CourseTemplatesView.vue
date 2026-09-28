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
  coursesApi,
  getApiErrorMessage,
} from '@/api'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiDialog,
  UiEmptyState,
  UiFilterBar,
  UiSelect,
  UiUnsavedChangesConfirm,
} from '@/components/ui'

import TeacherPageShell from '@/components/teacher/TeacherPageShell.vue'
import CourseTemplateEditorDrawer from '@/components/teacher/CourseTemplateEditorDrawer.vue'
import CourseVersionEditorDrawer from '@/components/teacher/CourseVersionEditorDrawer.vue'

import {
  useTeacherSubjects,
} from '@/composables/useTeacherSubjects'

import {
  useAuthStore,
} from '@/stores/auth'

import {
  canCreateCourseTemplate,
} from '@/utils/courseTemplateContext'

import {
  useCourseTemplatesData,
} from '@/composables/useCourseTemplatesData'

import {
  useCourseTemplateEditors,
} from '@/composables/useCourseTemplateEditors'

const route = useRoute()
const authStore = useAuthStore()

const {
  loadingSubjects,
  selectedMembershipId,
  selectedSubjectId,
  selectedSubject,
  selectedMembership,
  membershipOptions,
  ensureSelectedMembershipActive,
  loadTeacherSubjects,
} = useTeacherSubjects()

const initialized = ref(false)
const publishingVersionId = ref(null)

const deleteTarget = ref(null)
const deleteConfirmVisible = ref(false)
const deletingTemplateId = ref(null)
const deleteError = ref('')

const notice = ref({
  type: 'info',
  message: '',
})

let courseTemplateEditors = null

function closeVersionDrawerForData() {
  courseTemplateEditors?.closeVersionDrawerImmediately()
}

function openRouteVersionForData(version) {
  courseTemplateEditors?.openEditVersion(version)
}

const {
  templates,
  versions,
  selectedTemplateId,
  selectedTemplate,
  loading,
  loadingVersions,
  handledRouteVersionKey,
  templateSearchQuery,
  templateVisibilityFilter,
  templateSortMode,
  versionSearchQuery,
  versionPublicationFilter,
  versionSortMode,
  templateVisibilityOptions,
  templateSortOptions,
  versionPublicationOptions,
  versionSortOptions,
  hasActiveTemplateFilters,
  filteredTemplates,
  hasActiveVersionFilters,
  filteredVersions,
  templateFilterResultText,
  versionFilterResultText,
  resetTemplateFilters,
  resetVersionFilters,
  loadTemplates,
  selectTemplate,
  loadVersions,
} = useCourseTemplatesData({
  selectedSubjectId,
  selectedMembership,
  authStore,
  route,
  notice,
  beforeTemplateSelection: closeVersionDrawerForData,
  onOpenRouteVersion: openRouteVersionForData,
})

const templateCreationAllowed = computed(() => {
  return canCreateCourseTemplate({
    isAdmin: authStore.isAdminMode,
  })
})

const canWorkWithTemplates = computed(() => {
  return Boolean(selectedMembership.value && selectedSubjectId.value)
})

courseTemplateEditors = useCourseTemplateEditors({
  versions,
  selectedTemplate,
  selectedSubjectId,
  canWorkWithTemplates,
  templateCreationAllowed,
  notice,
})

const {
  templateOverlay,
  versionOverlay,
  templateFormError,
  versionFormError,
  templateDrawerTitle,
  versionDrawerTitle,
  closeTemplateDrawerImmediately,
  closeVersionDrawerImmediately,
  openCreateTemplate,
  openEditTemplate,
  requestTemplateDrawerClose,
  handleTemplateDrawerVisibility,
  discardTemplateDrawer,
  openCreateVersion,
  openEditVersion,
  requestVersionDrawerClose,
  handleVersionDrawerVisibility,
  discardVersionDrawer,
  templateValidationMessage,
  versionValidationMessage,
} = courseTemplateEditors

const contextHint = computed(() => {
  if (!selectedMembership.value) {
    return 'Выберите предмет преподавателя. Шаблоны задают структуру курса, а версии используются при работе с лекциями.'
  }

  if (!selectedSubject.value) {
    return `Выбрано назначение #${selectedMembership.value.id}.`
  }

  return `Предмет «${selectedSubject.value.name}». Шаблонов курса: ${templates.value.length}.`
})

function routeQuery(versionId = null) {
  const query = {}

  if (selectedSubjectId.value) {
    query.subjectId = selectedSubjectId.value
  }

  if (selectedMembership.value) {
    query.subjectMembershipId = selectedMembership.value.id
  }

  if (selectedTemplateId.value) {
    query.templateId = selectedTemplateId.value
  }

  if (versionId) {
    query.versionId = versionId
  }

  return query
}

async function saveTemplate() {
  templateFormError.value = templateValidationMessage()

  if (templateFormError.value) {
    return
  }

  if (
    !templateOverlay.form.id &&
    !templateCreationAllowed.value
  ) {
    templateFormError.value = 'Новый шаблон должен создать сам преподаватель.'
    return
  }

  const payload = {
    subjectId: Number(selectedSubjectId.value),
    name: String(templateOverlay.form.name).trim(),
    publicVisible: Boolean(templateOverlay.form.publicVisible),
  }

  const editingId = templateOverlay.form.id

  templateOverlay.beginSaving()

  try {
    await ensureSelectedMembershipActive()

    const response = editingId
      ? await coursesApi.updateTemplate(editingId, payload)
      : await coursesApi.createTemplate(payload)

    const savedId = response?.data?.id ?? editingId ?? null

    notice.value = {
      type: 'success',
      message: editingId ? 'Шаблон обновлён.' : 'Шаблон создан.',
    }

    templateOverlay.finishSaving({ close: true })
    templateFormError.value = ''
    await loadTemplates({ preferredTemplateId: savedId })
  } catch (error) {
    templateFormError.value = getApiErrorMessage(
      error,
      editingId
        ? 'Не удалось обновить шаблон'
        : 'Не удалось создать шаблон'
    )
    templateOverlay.failSaving()
  }
}

async function deleteTemplate() {
  const template = deleteTarget.value

  if (!template || deletingTemplateId.value !== null) {
    return
  }

  deletingTemplateId.value = template.id
  deleteError.value = ''

  try {
    await ensureSelectedMembershipActive()
    await coursesApi.removeTemplate(template.id)

    if (Number(templateOverlay.form.id) === Number(template.id)) {
      closeTemplateDrawerImmediately()
    }

    if (Number(selectedTemplateId.value) === Number(template.id)) {
      selectedTemplateId.value = null
      versions.value = []
      closeVersionDrawerImmediately()
    }

    notice.value = {
      type: 'success',
      message: 'Шаблон удалён.',
    }

    deleteConfirmVisible.value = false
    deleteTarget.value = null
    await loadTemplates()
  } catch (error) {
    deleteError.value = getApiErrorMessage(
      error,
      'Не удалось удалить шаблон'
    )
  } finally {
    deletingTemplateId.value = null
  }
}

async function saveVersion() {
  versionFormError.value = versionValidationMessage()

  if (versionFormError.value) {
    return
  }

  const editingId = versionOverlay.form.id

  const basePayload = {
    versionNumber: Number(versionOverlay.form.versionNumber),
    title: String(versionOverlay.form.title).trim(),
    description: String(versionOverlay.form.description ?? '').trim() || null,
    changeNotes: String(versionOverlay.form.changeNotes ?? '').trim() || null,
  }

  versionOverlay.beginSaving()

  try {
    await ensureSelectedMembershipActive()

    if (editingId) {
      await coursesApi.updateVersion(editingId, basePayload)
    } else {
      await coursesApi.createVersion(
        selectedTemplateId.value,
        {
          ...basePayload,
          published: Boolean(versionOverlay.form.published),
        }
      )
    }

    notice.value = {
      type: 'success',
      message: editingId ? 'Версия обновлена.' : 'Версия создана.',
    }

    versionOverlay.finishSaving({ close: true })
    versionFormError.value = ''
    await loadVersions()
  } catch (error) {
    versionFormError.value = getApiErrorMessage(
      error,
      editingId
        ? 'Не удалось обновить версию'
        : 'Не удалось создать версию'
    )
    versionOverlay.failSaving()
  }
}

async function publishVersion(version) {
  publishingVersionId.value = version.id

  try {
    await ensureSelectedMembershipActive()

    if (version.published) {
      await coursesApi.unpublishVersion(version.id)

      notice.value = {
        type: 'success',
        message: 'Публикация версии снята.',
      }
    } else {
      await coursesApi.publishVersion(version.id)

      notice.value = {
        type: 'success',
        message: 'Версия опубликована.',
      }
    }

    await loadVersions()
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось изменить публикацию версии'
      ),
    }
  } finally {
    publishingVersionId.value = null
  }
}

watch(
  selectedMembershipId,
  () => {
    if (!initialized.value) {
      return
    }

    closeTemplateDrawerImmediately()
    closeVersionDrawerImmediately()
    closeDeleteDialog()
    resetTemplateFilters()
    resetVersionFilters()
    handledRouteVersionKey.value = ''
    loadTemplates()
  }
)

onMounted(async () => {
  try {
    await loadTeacherSubjects({
      preferredSubjectId: route.query.subjectId,
      preferredMembershipId: route.query.subjectMembershipId,
    })

    initialized.value = true

    if (selectedSubjectId.value) {
      await loadTemplates({ openRouteVersion: true })
    }

    if (!membershipOptions.value.length) {
      notice.value = {
        type: 'info',
        message: 'Нет предметов преподавателя для работы с шаблонами курса.',
      }
    }
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(error, error.message),
    }
  }
})
</script>

<template>
  <TeacherPageShell
    title="Шаблоны курса"
    subtitle="Просматривайте структуру курса на рабочей странице, а шаблоны и версии редактируйте в боковой панели."
  >
    <template #actions>
      <UiButton
        v-if="templateCreationAllowed"
        variant="primary"
        :disabled="!canWorkWithTemplates"
        @click="openCreateTemplate"
      >
        Добавить шаблон
      </UiButton>
    </template>

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
      <div class="teacher-grid teacher-template-context-grid">
        <UiSelect
          v-model="selectedMembershipId"
          label="Предмет преподавателя"
          :options="membershipOptions"
          placeholder="Выберите предмет"
          :disabled="loadingSubjects || !membershipOptions.length"
        />

        <div class="teacher-template-context-actions">
          <UiButton
            variant="secondary"
            :disabled="!selectedMembership"
            :to="selectedMembership ? {
              name: 'teacher-topics',
              query: routeQuery(),
            } : undefined"
          >
            Темы предмета
          </UiButton>
        </div>
      </div>

      <UiAlert
        v-if="authStore.isAdminMode"
        class="teacher-template-admin-hint"
        variant="info"
        message="Администратор может просматривать и редактировать существующие шаблоны выбранного преподавателя. Создание шаблона от имени преподавателя backend не поддерживает."
      />
    </UiCard>

    <UiCard
      title="Шаблоны курса"
      description="Выберите шаблон, чтобы открыть его версии. Создание и редактирование выполняются в боковой панели."
    >
      <UiFilterBar
        v-model="templateSearchQuery"
        search-placeholder="Название или ID шаблона"
        :result-text="templateFilterResultText"
        :reset-disabled="!hasActiveTemplateFilters"
        @reset="resetTemplateFilters"
      >
        <template #filters>
          <UiSelect
            v-model="templateVisibilityFilter"
            :options="templateVisibilityOptions"
            aria-label="Видимость шаблона"
          />

          <UiSelect
            v-model="templateSortMode"
            :options="templateSortOptions"
            aria-label="Сортировка шаблонов"
          />
        </template>

        <template #actions>
          <UiButton
            v-if="templateCreationAllowed"
            size="sm"
            variant="primary"
            :disabled="!canWorkWithTemplates"
            @click="openCreateTemplate"
          >
            Добавить шаблон
          </UiButton>
        </template>
      </UiFilterBar>

      <UiEmptyState
        v-if="loading"
        description="Загрузка шаблонов..."
        compact
      />

      <UiEmptyState
        v-else-if="!selectedSubjectId"
        description="Выберите предмет преподавателя, чтобы открыть его шаблоны курса."
        compact
      />

      <UiEmptyState
        v-else-if="!templates.length"
        description="У выбранного предмета пока нет шаблонов курса."
        compact
      />

      <UiEmptyState
        v-else-if="!filteredTemplates.length"
        description="По выбранным фильтрам шаблоны не найдены."
        compact
      />

      <div
        v-else
        class="teacher-entity-list teacher-template-list"
      >
        <article
          v-for="courseTemplate in filteredTemplates"
          :key="courseTemplate.id"
          class="teacher-entity-card"
          :class="{
            'teacher-entity-card--selected':
              Number(selectedTemplateId) === Number(courseTemplate.id),
          }"
        >
          <div class="teacher-entity-card__header">
            <div class="teacher-entity-card__heading">
              <span class="teacher-entity-card__eyebrow">
                Шаблон #{{ courseTemplate.id }}
              </span>

              <h3 class="teacher-entity-card__title">
                {{ courseTemplate.name }}
              </h3>
            </div>

            <span
              class="teacher-status"
              :class="{
                'teacher-status--success': courseTemplate.publicVisible,
              }"
            >
              {{ courseTemplate.publicVisible ? 'Виден' : 'Черновик' }}
            </span>
          </div>

          <div class="teacher-entity-card__meta">
            <span>
              {{ Number(selectedTemplateId) === Number(courseTemplate.id)
                ? `${versions.length} версий загружено`
                : 'Откройте шаблон, чтобы посмотреть версии' }}
            </span>
          </div>

          <div class="teacher-entity-card__actions">
            <UiButton
              :variant="
                Number(selectedTemplateId) === Number(courseTemplate.id)
                  ? 'primary'
                  : 'secondary'
              "
              size="sm"
              @click="selectTemplate(courseTemplate.id)"
            >
              {{ Number(selectedTemplateId) === Number(courseTemplate.id) ? 'Версии открыты' : 'Открыть версии' }}
            </UiButton>

            <UiButton
              size="sm"
              @click="openEditTemplate(courseTemplate)"
            >
              Изменить
            </UiButton>

            <UiButton
              variant="danger"
              size="sm"
              :loading="deletingTemplateId === courseTemplate.id"
              loading-text="Удаление..."
              @click="requestDeleteTemplate(courseTemplate)"
            >
              Удалить
            </UiButton>
          </div>
        </article>
      </div>
    </UiCard>

    <UiCard
      title="Версии шаблона"
      :description="
        selectedTemplate
          ? `Шаблон «${selectedTemplate.name}». Версии редактируются в той же боковой панели без вложенных окон.`
          : 'Выберите шаблон курса выше.'
      "
    >
      <UiFilterBar
        v-model="versionSearchQuery"
        search-placeholder="Название, номер или описание версии"
        :result-text="versionFilterResultText"
        :reset-disabled="!hasActiveVersionFilters"
        :show-search="Boolean(selectedTemplate)"
        :show-reset="Boolean(selectedTemplate)"
        @reset="resetVersionFilters"
      >
        <template v-if="selectedTemplate" #filters>
          <UiSelect
            v-model="versionPublicationFilter"
            :options="versionPublicationOptions"
            aria-label="Публикация версии"
          />

          <UiSelect
            v-model="versionSortMode"
            :options="versionSortOptions"
            aria-label="Сортировка версий"
          />
        </template>

        <template #actions>
          <UiButton
            size="sm"
            variant="primary"
            :disabled="!selectedTemplate"
            @click="openCreateVersion"
          >
            Добавить версию
          </UiButton>
        </template>
      </UiFilterBar>

      <UiEmptyState
        v-if="loadingVersions"
        description="Загрузка версий..."
        compact
      />

      <UiEmptyState
        v-else-if="!selectedTemplateId"
        description="Выберите шаблон курса выше."
        compact
      />

      <UiEmptyState
        v-else-if="!versions.length"
        description="У выбранного шаблона пока нет версий."
        compact
      />

      <UiEmptyState
        v-else-if="!filteredVersions.length"
        description="По выбранным фильтрам версии не найдены."
        compact
      />

      <div
        v-else
        class="teacher-entity-list teacher-version-list"
      >
        <article
          v-for="version in filteredVersions"
          :key="version.id"
          class="teacher-entity-card"
        >
          <div class="teacher-entity-card__header">
            <div class="teacher-entity-card__heading">
              <span class="teacher-entity-card__eyebrow">
                Версия {{ version.versionNumber }}
              </span>

              <h3 class="teacher-entity-card__title">
                {{ version.title }}
              </h3>
            </div>

            <span
              class="teacher-status"
              :class="{
                'teacher-status--success': version.published,
              }"
            >
              {{ version.published ? 'Опубликована' : 'Черновик' }}
            </span>
          </div>

          <p class="teacher-entity-card__description">
            {{ version.description || 'Описание пока не добавлено.' }}
          </p>

          <div
            v-if="version.changeNotes"
            class="teacher-version-notes"
          >
            <span class="teacher-muted">Что изменилось</span>
            <p>{{ version.changeNotes }}</p>
          </div>

          <div class="teacher-entity-card__actions">
            <UiButton
              size="sm"
              @click="openEditVersion(version)"
            >
              Изменить
            </UiButton>

            <UiButton
              :variant="version.published ? 'secondary' : 'primary'"
              size="sm"
              :loading="publishingVersionId === version.id"
              loading-text="Обновление..."
              @click="publishVersion(version)"
            >
              {{ version.published ? 'Снять публикацию' : 'Опубликовать' }}
            </UiButton>

            <UiButton
              size="sm"
              variant="secondary"
              :to="{
                name: 'teacher-lectures',
                query: routeQuery(version.id),
              }"
            >
              Лекции
            </UiButton>
          </div>
        </article>
      </div>
    </UiCard>

    <CourseTemplateEditorDrawer
      :open="templateOverlay.isOpen.value"
      :title="templateDrawerTitle"
      :form="templateOverlay.form"
      :saving="templateOverlay.saving.value"
      :form-error="templateFormError"
      :subject-label="selectedSubject?.name || `Предмет #${selectedSubjectId}`"
      @update:open="handleTemplateDrawerVisibility"
      @close="requestTemplateDrawerClose"
      @save="saveTemplate"
    />

    <CourseVersionEditorDrawer
      :open="versionOverlay.isOpen.value"
      :title="versionDrawerTitle"
      :form="versionOverlay.form"
      :is-create="versionOverlay.isCreate.value"
      :saving="versionOverlay.saving.value"
      :form-error="versionFormError"
      :template-label="selectedTemplate?.name || 'Не выбран'"
      @update:open="handleVersionDrawerVisibility"
      @close="requestVersionDrawerClose"
      @save="saveVersion"
    />

    <UiUnsavedChangesConfirm
      v-model="templateOverlay.confirmCloseVisible.value"
      :busy="templateOverlay.saving.value"
      @continue="templateOverlay.continueEditing"
      @discard="discardTemplateDrawer"
    />

    <UiUnsavedChangesConfirm
      v-model="versionOverlay.confirmCloseVisible.value"
      :busy="versionOverlay.saving.value"
      @continue="versionOverlay.continueEditing"
      @discard="discardVersionDrawer"
    />

    <UiDialog
      :model-value="deleteConfirmVisible"
      title="Удалить шаблон курса?"
      width="32rem"
      :closable="deletingTemplateId === null"
      :close-on-escape="deletingTemplateId === null"
      :dismissable-mask="false"
      @update:model-value="($event) => !$event && closeDeleteDialog()"
    >
      <div class="teacher-stack">
        <p class="teacher-confirm-text">
          Шаблон «{{ deleteTarget?.name }}» будет удалён. Доступность связанных версий зависит от правил backend.
        </p>

        <UiAlert
          v-if="deleteError"
          variant="danger"
          :message="deleteError"
        />
      </div>

      <template #footer>
        <div class="teacher-drawer-footer">
          <UiButton
            variant="secondary"
            :disabled="deletingTemplateId !== null"
            @click="closeDeleteDialog"
          >
            Отмена
          </UiButton>

          <UiButton
            variant="danger"
            :loading="deletingTemplateId !== null"
            loading-text="Удаление..."
            @click="deleteTemplate"
          >
            Удалить
          </UiButton>
        </div>
      </template>
    </UiDialog>
  </TeacherPageShell>
</template>

<style scoped>
.teacher-template-context-grid {
  align-items: end;
}

.teacher-template-context-actions {
  display: flex;
  align-items: flex-end;
}

.teacher-template-admin-hint {
  margin-top: 12px;
}

.teacher-template-list,
.teacher-version-list {
  margin-top: 14px;
}

.teacher-version-notes {
  padding: 10px 12px;

  display: grid;
  gap: 4px;

  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 8px;
}

.teacher-version-notes p,
.teacher-confirm-text {
  margin: 0;
  color: var(--st-text-secondary);
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.teacher-drawer-footer {
  width: 100%;

  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 720px) {
  .teacher-template-context-actions,
  .teacher-template-context-actions > * {
    width: 100%;
  }

  .teacher-drawer-footer,
  .teacher-drawer-footer > * {
    width: 100%;
  }
}
</style>
