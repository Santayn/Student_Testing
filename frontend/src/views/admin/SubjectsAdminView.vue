<script setup>
import {
  computed,
  onMounted,
  ref,
} from 'vue'

import AdminNotice from '@/components/admin/AdminNotice.vue'
import AdminPageShell from '@/components/admin/AdminPageShell.vue'

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

import {
  getApiErrorMessage,
  subjectsApi,
} from '@/api'

import {
  listFromResponse,
} from '@/utils/apiData'

const subjects = ref([])
const loading = ref(false)

const notice = ref({
  type: 'info',
  message: '',
})

const searchQuery = ref('')
const descriptionFilter = ref('all')
const sortMode = ref('name-asc')

const formError = ref('')

const deleteTarget = ref(null)
const deleteConfirmVisible = ref(false)
const deletingId = ref(null)
const deleteError = ref('')

const descriptionOptions = [
  { value: 'all', label: 'Все' },
  { value: 'with-description', label: 'С описанием' },
  { value: 'without-description', label: 'Без описания' },
]

const sortOptions = [
  { value: 'name-asc', label: 'Название А–Я' },
  { value: 'name-desc', label: 'Название Я–А' },
]

const {
  form,
  model: subjectDialogModel,
  isCreate,
  saving,
  confirmCloseVisible,
  openCreate,
  openEdit,
  requestClose,
  discardAndClose,
  continueEditing,
  beginSaving,
  finishSaving,
  failSaving,
} = useOverlayForm({
  createDefault: () => ({
    id: null,
    name: '',
    description: '',
  }),
  mapEntity: (subject) => ({
    id: subject?.id ?? null,
    name: subject?.name ?? '',
    description: subject?.description ?? '',
  }),
})

const hasActiveFilters = computed(() => {
  return Boolean(searchQuery.value.trim()) ||
    descriptionFilter.value !== 'all' ||
    sortMode.value !== 'name-asc'
})

const filteredSubjects = computed(() => {
  const query = searchQuery.value
    .trim()
    .toLocaleLowerCase('ru-RU')

  const result = subjects.value.filter((subject) => {
    const description = String(
      subject.description ?? ''
    ).trim()

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
      subject.name,
      subject.description,
    ]
      .filter((value) => value !== null && value !== undefined)
      .join(' ')
      .toLocaleLowerCase('ru-RU')

    return haystack.includes(query)
  })

  return [...result].sort((left, right) => {
    if (sortMode.value === 'name-desc') {
      return String(right.name ?? '').localeCompare(
        String(left.name ?? ''),
        'ru'
      )
    }

    return String(left.name ?? '').localeCompare(
      String(right.name ?? ''),
      'ru'
    )
  })
})

const filterResultText = computed(() => {
  return `Показано: ${filteredSubjects.value.length} из ${subjects.value.length}`
})

const subjectDialogTitle = computed(() => {
  return isCreate.value
    ? 'Новый предмет'
    : 'Редактирование предмета'
})

const canSubmit = computed(() => {
  return !subjectFormValidationMessage() && !saving.value
})

function showNotice(type, message) {
  notice.value = {
    type,
    message,
  }
}

function clearNotice() {
  notice.value.message = ''
}

function resetFilters() {
  searchQuery.value = ''
  descriptionFilter.value = 'all'
  sortMode.value = 'name-asc'
}

function openCreateSubject() {
  formError.value = ''
  openCreate()
}

function openEditSubject(subject) {
  formError.value = ''
  openEdit(subject)
}

function requestDeleteSubject(subject) {
  deleteTarget.value = subject
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

function subjectFormValidationMessage() {
  const name = String(form.name ?? '').trim()
  const description = String(
    form.description ?? ''
  ).trim()

  if (!name) {
    return 'Введите название предмета.'
  }

  if (name.length > 200) {
    return 'Название предмета не может быть длиннее 200 символов.'
  }

  if (description.length > 1000) {
    return 'Описание предмета не может быть длиннее 1000 символов.'
  }

  const normalizedName = name.toLocaleLowerCase('ru-RU')
  const duplicate = subjects.value.find((subject) => {
    return String(subject.name ?? '')
      .trim()
      .toLocaleLowerCase('ru-RU') === normalizedName &&
      String(subject.id) !== String(form.id ?? '')
  })

  if (duplicate) {
    return `Предмет «${name}» уже существует.`
  }

  return ''
}

async function loadSubjects() {
  loading.value = true

  try {
    const response = await subjectsApi.getAll()
    subjects.value = listFromResponse(response)
  } catch (error) {
    showNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось загрузить предметы'
      )
    )
  } finally {
    loading.value = false
  }
}

async function saveSubject() {
  const validationMessage = subjectFormValidationMessage()

  if (validationMessage) {
    formError.value = validationMessage
    return
  }

  beginSaving()
  formError.value = ''
  clearNotice()

  const payload = {
    name: String(form.name).trim(),
    description:
      String(form.description ?? '').trim() || null,
  }

  try {
    const editingId = form.id

    if (editingId) {
      await subjectsApi.update(
        editingId,
        payload
      )
    } else {
      await subjectsApi.create(payload)
    }

    await loadSubjects()
    finishSaving({ close: true })

    showNotice(
      'success',
      editingId
        ? 'Предмет обновлён.'
        : 'Предмет создан.'
    )
  } catch (error) {
    failSaving()
    formError.value = getApiErrorMessage(
      error,
      'Не удалось сохранить предмет'
    )
  }
}

async function deleteSubject() {
  const subject = deleteTarget.value

  if (!subject || deletingId.value !== null) {
    return
  }

  deletingId.value = subject.id
  deleteError.value = ''
  clearNotice()

  try {
    await subjectsApi.remove(subject.id)
    await loadSubjects()

    deleteConfirmVisible.value = false
    deleteTarget.value = null

    showNotice(
      'success',
      'Предмет удалён.'
    )
  } catch (error) {
    deleteError.value = getApiErrorMessage(
      error,
      'Не удалось удалить предмет'
    )
  } finally {
    deletingId.value = null
  }
}

onMounted(loadSubjects)
</script>

<template>
  <AdminPageShell
    title="Предметы"
    description="Просматривайте и находите предметы в рабочем списке. Создание и редактирование открываются поверх страницы и не сбрасывают текущие фильтры."
  >
    <template #actions>
      <UiButton
        size="sm"
        icon="pi pi-refresh"
        label="Обновить"
        :loading="loading"
        loading-text="Обновление..."
        @click="loadSubjects"
      />
    </template>

    <AdminNotice
      :type="notice.type"
      :message="notice.message"
      @close="clearNotice"
    />

    <UiCard
      title="Справочник предметов"
      description="Поиск выполняется по названию и описанию. Привязка предметов к факультетам управляется в отдельном разделе."
    >
      <div class="admin-subjects-workspace">
        <UiFilterBar
          v-model="searchQuery"
          search-placeholder="Название или описание"
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
              label="Добавить предмет"
              @click="openCreateSubject"
            />
          </template>
        </UiFilterBar>

        <UiEmptyState
          v-if="loading"
          description="Загрузка предметов..."
          compact
        />

        <UiEmptyState
          v-else-if="!subjects.length"
          description="Предметы ещё не созданы. Добавьте первый предмет кнопкой выше."
          compact
        />

        <UiEmptyState
          v-else-if="!filteredSubjects.length"
          description="По текущему поиску и фильтрам предметы не найдены."
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
          class="admin-subject-grid"
        >
          <article
            v-for="subject in filteredSubjects"
            :key="subject.id"
            class="admin-subject-card"
          >
            <div class="admin-subject-card__heading">
              <span class="admin-subject-card__eyebrow">
                Учебный предмет
              </span>

              <h2 class="admin-subject-card__title">
                {{ subject.name }}
              </h2>
            </div>

            <p class="admin-subject-card__description">
              {{ subject.description || 'Описание пока не добавлено.' }}
            </p>

            <div class="admin-subject-card__actions">
              <UiButton
                size="sm"
                icon="pi pi-pencil"
                label="Изменить"
                @click="openEditSubject(subject)"
              />

              <UiButton
                variant="danger"
                size="sm"
                icon="pi pi-trash"
                label="Удалить"
                :loading="deletingId === subject.id"
                loading-text="Удаление..."
                @click="requestDeleteSubject(subject)"
              />
            </div>
          </article>
        </div>
      </div>
    </UiCard>

    <UiDialog
      v-model="subjectDialogModel"
      :title="subjectDialogTitle"
      width="38rem"
      :dismissable-mask="false"
    >
      <form
        class="admin-subject-form"
        @submit.prevent="saveSubject"
      >
        <UiAlert
          v-if="formError"
          variant="danger"
          :message="formError"
        />

        <UiInput
          v-model="form.name"
          label="Название предмета"
          maxlength="200"
          :disabled="saving"
          required
        />

        <UiTextarea
          v-model="form.description"
          label="Описание"
          maxlength="1000"
          :rows="6"
          auto-resize
          :disabled="saving"
        />

        <div class="admin-subject-form__actions">
          <UiButton
            variant="secondary"
            label="Отмена"
            :disabled="saving"
            @click="requestClose"
          />

          <UiButton
            type="submit"
            variant="primary"
            :label="isCreate ? 'Создать предмет' : 'Сохранить изменения'"
            :loading="saving"
            loading-text="Сохранение..."
            :disabled="!canSubmit"
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
      title="Удалить предмет?"
      width="31rem"
      :closable="deletingId === null"
      :close-on-escape="deletingId === null"
      :dismissable-mask="false"
    >
      <div class="admin-subject-delete">
        <UiAlert
          v-if="deleteError"
          variant="danger"
          :message="deleteError"
        />

        <p>
          Предмет
          <strong>«{{ deleteTarget?.name }}»</strong>
          будет удалён. Если удалить его нельзя из-за связанных факультетов, преподавателей, лекций, тестов или других данных, причина останется в этом окне.
        </p>
      </div>

      <template #footer>
        <div class="admin-subject-delete__actions">
          <UiButton
            variant="secondary"
            label="Отмена"
            :disabled="deletingId !== null"
            @click="closeDeleteDialog"
          />

          <UiButton
            variant="danger"
            label="Удалить предмет"
            :loading="deletingId !== null"
            loading-text="Удаление..."
            @click="deleteSubject"
          />
        </div>
      </template>
    </UiDialog>
  </AdminPageShell>
</template>

<style scoped>
.admin-subjects-workspace,
.admin-subject-form,
.admin-subject-delete {
  display: grid;
  gap: 14px;
}

.admin-subject-grid {
  display: grid;
  grid-template-columns:
    repeat(auto-fit, minmax(min(100%, 300px), 1fr));
  gap: 12px;
}

.admin-subject-card {
  min-width: 0;
  padding: 15px;

  display: grid;
  align-content: start;
  gap: 13px;

  color: var(--st-text);
  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 12px;
}

.admin-subject-card__heading {
  min-width: 0;

  display: grid;
  gap: 6px;
}

.admin-subject-card__eyebrow {
  width: fit-content;
  max-width: 100%;
  padding: 4px 8px;

  color: var(--st-primary);
  background: var(--st-primary-soft);
  border-radius: 999px;

  font-size: 11px;
  font-weight: 800;
  line-height: 1.25;
  letter-spacing: 0.04em;
}

.admin-subject-card__title {
  margin: 0;

  font-size: 18px;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.admin-subject-card__description {
  margin: 0;

  color: var(--st-text-secondary);

  font-size: 13px;
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.admin-subject-card__actions,
.admin-subject-form__actions,
.admin-subject-delete__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

.admin-subject-card__actions {
  margin-top: auto;
  padding-top: 2px;
}

.admin-subject-delete p {
  margin: 0;

  color: var(--st-text-secondary);

  font-size: 14px;
  line-height: 1.65;
  overflow-wrap: anywhere;
}

.admin-subject-delete strong {
  color: var(--st-text);
}

@media (max-width: 640px) {
  .admin-subject-card__actions,
  .admin-subject-form__actions,
  .admin-subject-delete__actions {
    align-items: stretch;
    flex-direction: column;
  }

  .admin-subject-card__actions > *,
  .admin-subject-form__actions > *,
  .admin-subject-delete__actions > * {
    width: 100%;
  }
}
</style>
