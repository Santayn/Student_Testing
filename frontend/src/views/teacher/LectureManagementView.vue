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
  lecturesApi,
} from '@/api'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiDialog,
  UiEmptyState,
  UiLoadingState,
  UiFilterBar,
  UiSelect,
  UiUnsavedChangesConfirm,
  useOverlayForm,
} from '@/components/ui'

import LectureEditorDrawer from '@/components/teacher/LectureEditorDrawer.vue'
import TeacherPageShell from '@/components/teacher/TeacherPageShell.vue'

import {
  useTeacherSubjects,
} from '@/composables/teacher/useTeacherSubjects'

import {
  useLectureSaveFlow,
} from '@/composables/lectures/useLectureSaveFlow'

import {
  useLectureManagementData,
} from '@/composables/lectures/useLectureManagementData'

import {
  useLectureMaterials,
} from '@/composables/lectures/useLectureMaterials'

import {
  useLectureDrawerWorkspace,
} from '@/composables/lectures/useLectureDrawerWorkspace'

import {
  useLectureDelete,
} from '@/composables/lectures/useLectureDelete'

import {
  clearFormFieldError,
} from '@/utils/formErrorLifecycle'

const route = useRoute()

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
const formError = ref('')
const formFieldErrors = ref({})
const lectureEditorDrawer = ref(null)
const notice = ref({
  type: 'info',
  message: '',
})

let lectureEditorActions = null

async function openRouteLecture(lecture) {
  await lectureEditorActions?.openEditLecture(lecture)
}

const {
  lectures,
  availableTests,
  lectureTestsById,
  loading,
  searchQuery,
  visibilityFilter,
  testFilter,
  sortMode,
  visibilityOptions,
  testFilterOptions,
  sortOptions,
  hasActiveFilters,
  filteredLectures,
  filterResultText,
  lectureTests,
  lectureTestSummary,
  resetFilters,
  loadLectures,
  resetRouteLectureHandling,
  dispose: disposeLectureData,
} = useLectureManagementData({
  selectedMembership,
  selectedSubjectId,
  route,
  notice,
  onOpenRouteLecture: openRouteLecture,
})

function lectureToForm(lecture = null) {
  return {
    id: lecture?.id ?? null,
    title: lecture?.title ?? '',
    description: lecture?.description ?? '',
    publicVisible: lecture ? Boolean(lecture.publicVisible) : true,
    testIds: lecture
      ? lectureTests(lecture.id).map((test) => Number(test.id))
      : [],
  }
}

const {
  form,
  isOpen: lectureDrawerOpen,
  isCreate,
  mode: lectureFormMode,
  dirty: lectureDirty,
  saving,
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
  createDefault: () => lectureToForm(),
  mapEntity: lectureToForm,
})

const lectureId = computed(() => form.id)

const {
  materials,
  pendingFiles,
  fileInputKey,
  loadingMaterials,
  pendingFilesDirty,
  materialDeleteTarget,
  materialDeleteConfirmVisible,
  deletingMaterialId,
  materialDeleteError,
  setPendingFiles,
  removePendingFile,
  requestDeleteMaterial,
  closeMaterialDeleteDialog,
  resetMaterials,
  loadMaterials,
  deleteMaterial,
  downloadMaterial,
} = useLectureMaterials({
  lectureId,
  lecturesApi,
  ensureSelectedMembershipActive,
  formError,
  getApiErrorMessage,
})

async function focusLectureFormErrors() {
  await lectureEditorDrawer.value
    ?.focusFirstInvalidField?.()
}

const {
  partialCreatePending,
  resetPartialCreate,
  saveLecture,
} = useLectureSaveFlow({
  form,
  formError,
  formFieldErrors,
  focusFormErrors:
    focusLectureFormErrors,
  lectureFormMode,
  selectedSubject,
  selectedMembership,
  ensureSelectedMembershipActive,
  lectures,
  lectureTestsById,
  pendingFiles,
  fileInputKey,
  notice,
  beginSaving,
  saving,
  finishSaving,
  failSaving,
  loadLectures,
  clearLectureDrawerState,
  lecturesApi,
  getApiErrorMessage,
})

const canEdit = computed(() => Boolean(selectedMembership.value))

const contextHint = computed(() => {
  if (!selectedMembership.value) {
    return 'Выберите предмет преподавателя. Лекции останутся на рабочей странице, а создание и редактирование открываются в боковой панели.'
  }

  if (!selectedSubject.value) {
    return 'Выбрано назначение преподавателя.'
  }

  return `Предмет «${selectedSubject.value.name}». Лекций в выбранном назначении: ${lectures.value.length}.`
})

const drawerTitle = computed(() => {
  return isCreate.value ? 'Новая лекция' : 'Редактирование лекции'
})

function routeQuery(lectureId = null) {
  const query = {}

  if (selectedSubjectId.value) {
    query.subjectId = selectedSubjectId.value
  }

  if (selectedMembership.value) {
    query.subjectMembershipId = selectedMembership.value.id
  }

  if (lectureId) {
    query.lectureId = lectureId
  }

  return query
}

function clearLectureDrawerState() {
  resetPartialCreate()
  resetMaterials()
  formError.value = ''
  formFieldErrors.value = {}
}

const {
  openCreateLecture,
  openEditLecture,
  requestLectureDrawerClose,
  handleLectureDrawerVisibility,
  discardLectureDrawer,
  closeLectureDrawerImmediately,
} = useLectureDrawerWorkspace({
  canEdit,
  saving,
  lectureDirty,
  pendingFilesDirty,
  partialCreatePending,
  confirmCloseVisible,
  openCreate,
  openEdit,
  closeImmediately,
  discardAndClose,
  clearLectureDrawerState,
  loadMaterials,
  notice,
})

lectureEditorActions = {
  openEditLecture,
}

const {
  deleteTarget,
  deleteConfirmVisible,
  deletingId,
  deleteError,
  requestDeleteLecture,
  closeDeleteDialog,
  deleteLecture,
} = useLectureDelete({
  form,
  lecturesApi,
  ensureSelectedMembershipActive,
  closeLectureDrawerImmediately,
  loadLectures,
  notice,
  getApiErrorMessage,
})

watch(
  selectedMembershipId,
  () => {
    if (!initialized.value) {
      return
    }

    closeLectureDrawerImmediately()
    closeDeleteDialog()
    resetFilters()
    resetRouteLectureHandling()
    loadLectures({ openRouteLecture: true })
  }
)

onBeforeUnmount(disposeLectureData)

onMounted(async () => {
  try {
    await loadTeacherSubjects({
      preferredSubjectId: route.query.subjectId,
      preferredMembershipId: route.query.subjectMembershipId,
    })

    initialized.value = true

    if (selectedSubjectId.value) {
      await loadLectures({ openRouteLecture: true })
    }

    if (!membershipOptions.value.length) {
      notice.value = {
        type: 'info',
        message: 'Нет предметов преподавателя для управления лекциями.',
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
    title="Лекции предмета"
    subtitle="Просматривайте и фильтруйте лекции выбранного предмета. Создание, редактирование, материалы и связи с тестами открываются в боковой панели."
  >
    <UiAlert
      v-if="notice.message"
      :variant="notice.type"
      :message="notice.message"
      closable
      @close="notice.message = ''"
    />

    <UiCard
      title="Контекст лекций"
      :description="contextHint"
    >
      <div class="teacher-lecture-context">
        <UiSelect
          v-model="selectedMembershipId"
          label="Предмет преподавателя"
          :options="membershipOptions"
          placeholder="Выберите предмет"
          :disabled="loadingSubjects || !membershipOptions.length"
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
            :to="{
              name: 'teacher-questions',
              query: routeQuery(),
            }"
          >
            Все вопросы
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
      title="Лекции"
      :description="selectedSubject ? `Предмет: ${selectedSubject.name}.` : 'Предмет не выбран.'"
    >
      <div class="teacher-stack">
        <UiFilterBar
          v-model="searchQuery"
          search-placeholder="Название, описание, тест, ID или номер лекции"
          :result-text="filterResultText"
          :reset-disabled="!hasActiveFilters"
          @reset="resetFilters"
        >
          <template #filters>
            <UiSelect
              v-model="visibilityFilter"
              label="Публикация"
              :options="visibilityOptions"
              size="sm"
            />

            <UiSelect
              v-model="testFilter"
              label="Связанные тесты"
              :options="testFilterOptions"
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
              label="Добавить лекцию"
              :disabled="!canEdit"
              @click="openCreateLecture"
            />
          </template>
        </UiFilterBar>

        <UiLoadingState
          v-if="loading"
          compact
          label="Загрузка лекций..."
        />

        <UiEmptyState
          v-else-if="!selectedMembership"
          description="Выберите предмет преподавателя, чтобы открыть его лекции."
          compact
        />

        <UiEmptyState
          v-else-if="!lectures.length"
          description="Для выбранного предмета пока нет лекций. Добавьте первую лекцию кнопкой выше."
          compact
        />

        <UiEmptyState
          v-else-if="!filteredLectures.length"
          description="По текущему поиску и фильтрам лекции не найдены."
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
            v-for="lecture in filteredLectures"
            :key="lecture.id"
            class="teacher-entity-card"
            :class="{
              'teacher-entity-card--selected':
                lectureDrawerOpen && Number(form.id) === Number(lecture.id),
            }"
          >
            <div class="teacher-entity-card__header">
              <div class="teacher-entity-card__heading">
                <span class="teacher-entity-card__eyebrow">
                  Лекция {{ lecture.ordinal }} · ID {{ lecture.id }}
                </span>

                <h3 class="teacher-entity-card__title">
                  {{ lecture.title }}
                </h3>
              </div>

              <span
                class="teacher-status"
                :class="{
                  'teacher-status--success': lecture.publicVisible,
                }"
              >
                {{ lecture.publicVisible ? 'Опубликована' : 'Скрыта' }}
              </span>
            </div>

            <p class="teacher-entity-card__description">
              {{ lecture.description || 'Описание пока не добавлено.' }}
            </p>

            <div class="teacher-lecture-tests-preview">
              <span class="teacher-muted">Связанные тесты</span>
              <strong>{{ lectureTestSummary(lecture.id) }}</strong>
            </div>

            <div class="teacher-entity-card__actions">
              <UiButton
                size="sm"
                icon="pi pi-pencil"
                label="Изменить"
                @click="openEditLecture(lecture)"
              />

              <UiButton
                variant="danger"
                size="sm"
                icon="pi pi-trash"
                label="Удалить"
                :loading="deletingId === lecture.id"
                loading-text="Удаление..."
                @click="requestDeleteLecture(lecture)"
              />
            </div>
          </article>
        </div>
      </div>
    </UiCard>

    <LectureEditorDrawer
      ref="lectureEditorDrawer"
      :open="lectureDrawerOpen"
      :title="drawerTitle"
      :form="form"
      :is-create="isCreate"
      :saving="saving"
      :form-error="formError"
      :form-field-errors="formFieldErrors"
      :available-tests="availableTests"
      :materials="materials"
      :pending-files="pendingFiles"
      :file-input-key="fileInputKey"
      :loading-materials="loadingMaterials"
      :deleting-material-id="deletingMaterialId"
      @update:open="handleLectureDrawerVisibility"
      @dismiss-error="formError = ''"
      @field-change="clearFormFieldError(formFieldErrors, formError, $event)"
      @files-change="setPendingFiles"
      @remove-pending-file="removePendingFile"
      @download-material="downloadMaterial"
      @request-delete-material="requestDeleteMaterial"
      @close="requestLectureDrawerClose"
      @save="saveLecture"
    />

    <UiUnsavedChangesConfirm
      v-model="confirmCloseVisible"
      :busy="saving"
      @continue="continueEditing"
      @discard="discardLectureDrawer"
    />

    <UiDialog
      v-model="deleteConfirmVisible"
      title="Удалить лекцию?"
      width="31rem"
      :close-on-escape="deletingId === null"
      :closable="deletingId === null"
    >
      <div class="teacher-stack">
        <p class="teacher-lecture-dialog-copy">
          Лекция «{{ deleteTarget?.title }}» будет удалена. Это действие нельзя отменить.
        </p>

        <UiAlert
          v-if="deleteError"
          variant="danger"
          :message="deleteError"
        />
      </div>

      <template #footer>
        <div class="teacher-actions teacher-actions--mobile-stack">
          <UiButton
            variant="secondary"
            label="Отмена"
            :disabled="deletingId !== null"
            @click="closeDeleteDialog"
          />

          <UiButton
            variant="danger"
            :loading="deletingId !== null"
            loading-text="Удаление..."
            label="Удалить лекцию"
            @click="deleteLecture"
          />
        </div>
      </template>
    </UiDialog>

    <UiDialog
      v-model="materialDeleteConfirmVisible"
      title="Удалить материал?"
      width="31rem"
      :close-on-escape="deletingMaterialId === null"
      :closable="deletingMaterialId === null"
    >
      <div class="teacher-stack">
        <p class="teacher-lecture-dialog-copy">
          Файл «{{ materialDeleteTarget?.fileName || 'Материал без названия' }}» будет удалён из лекции.
        </p>

        <UiAlert
          v-if="materialDeleteError"
          variant="danger"
          :message="materialDeleteError"
        />
      </div>

      <template #footer>
        <div class="teacher-actions teacher-actions--mobile-stack">
          <UiButton
            variant="secondary"
            label="Отмена"
            :disabled="deletingMaterialId !== null"
            @click="closeMaterialDeleteDialog"
          />

          <UiButton
            variant="danger"
            :loading="deletingMaterialId !== null"
            loading-text="Удаление..."
            label="Удалить файл"
            @click="deleteMaterial"
          />
        </div>
      </template>
    </UiDialog>
  </TeacherPageShell>
</template>

<style scoped>
.teacher-lecture-context {
  min-width: 0;
  display: grid;
  gap: 14px;
}

.teacher-lecture-tests-preview {
  min-width: 0;
  padding: 9px 10px;
  display: grid;
  gap: 3px;
  background: var(--st-surface);
  border: 1px solid var(--st-border);
  border-radius: 8px;
}

.teacher-lecture-tests-preview strong {
  min-width: 0;
  color: var(--st-text);
  font-size: 12px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.teacher-lecture-dialog-copy {
  margin: 0;
  color: var(--st-text-secondary);
  line-height: 1.55;
}
</style>
