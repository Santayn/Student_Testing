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
  facultiesApi,
  getApiErrorMessage,
} from '@/api'

import {
  listFromResponse,
} from '@/utils/apiData'

const faculties = ref([])
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
  { value: 'code-asc', label: 'Код А–Я' },
]

const {
  form,
  model: facultyDialogModel,
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
    code: '',
    description: '',
  }),
  mapEntity: (faculty) => ({
    id: faculty?.id ?? null,
    name: faculty?.name ?? '',
    code: faculty?.code ?? '',
    description: faculty?.description ?? '',
  }),
})

const hasActiveFilters = computed(() => {
  return Boolean(searchQuery.value.trim()) ||
    descriptionFilter.value !== 'all' ||
    sortMode.value !== 'name-asc'
})

const filteredFaculties = computed(() => {
  const query = searchQuery.value
    .trim()
    .toLocaleLowerCase('ru-RU')

  const result = faculties.value.filter((faculty) => {
    const description = String(
      faculty.description ?? ''
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
      faculty.name,
      faculty.code,
      faculty.description,
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

    if (sortMode.value === 'code-asc') {
      return String(left.code ?? '').localeCompare(
        String(right.code ?? ''),
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
  return `Показано: ${filteredFaculties.value.length} из ${faculties.value.length}`
})

const facultyDialogTitle = computed(() => {
  return isCreate.value
    ? 'Новый факультет'
    : 'Редактирование факультета'
})

const canSubmit = computed(() => {
  return !facultyFormValidationMessage() && !saving.value
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

function openCreateFaculty() {
  formError.value = ''
  openCreate()
}

function openEditFaculty(faculty) {
  formError.value = ''
  openEdit(faculty)
}

function requestDeleteFaculty(faculty) {
  deleteTarget.value = faculty
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

function facultyFormValidationMessage() {
  const name = String(form.name ?? '').trim()
  const code = String(form.code ?? '').trim()
  const description = String(
    form.description ?? ''
  ).trim()

  if (!name) {
    return 'Введите название факультета.'
  }

  if (name.length > 200) {
    return 'Название факультета не может быть длиннее 200 символов.'
  }

  if (!code) {
    return 'Введите код факультета.'
  }

  if (code.length > 50) {
    return 'Код факультета не может быть длиннее 50 символов.'
  }

  if (description.length > 1000) {
    return 'Описание факультета не может быть длиннее 1000 символов.'
  }

  const normalizedCode = code.toLocaleLowerCase('ru-RU')
  const duplicate = faculties.value.find((faculty) => {
    return String(faculty.code ?? '')
      .trim()
      .toLocaleLowerCase('ru-RU') === normalizedCode &&
      String(faculty.id) !== String(form.id ?? '')
  })

  if (duplicate) {
    return `Факультет с кодом «${code}» уже существует.`
  }

  return ''
}

async function loadFaculties() {
  loading.value = true

  try {
    const response = await facultiesApi.getAll()
    faculties.value = listFromResponse(response)
  } catch (error) {
    showNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось загрузить факультеты'
      )
    )
  } finally {
    loading.value = false
  }
}

async function saveFaculty() {
  const validationMessage = facultyFormValidationMessage()

  if (validationMessage) {
    formError.value = validationMessage
    return
  }

  beginSaving()
  formError.value = ''
  clearNotice()

  const payload = {
    name: String(form.name).trim(),
    code: String(form.code).trim(),
    description:
      String(form.description ?? '').trim() || null,
  }

  try {
    const editingId = form.id

    if (editingId) {
      await facultiesApi.update(
        editingId,
        payload
      )
    } else {
      await facultiesApi.create(payload)
    }

    await loadFaculties()
    finishSaving({ close: true })

    showNotice(
      'success',
      editingId
        ? 'Факультет обновлён.'
        : 'Факультет создан.'
    )
  } catch (error) {
    failSaving()
    formError.value = getApiErrorMessage(
      error,
      'Не удалось сохранить факультет'
    )
  }
}

async function deleteFaculty() {
  const faculty = deleteTarget.value

  if (!faculty || deletingId.value !== null) {
    return
  }

  deletingId.value = faculty.id
  deleteError.value = ''
  clearNotice()

  try {
    await facultiesApi.remove(faculty.id)
    await loadFaculties()

    deleteConfirmVisible.value = false
    deleteTarget.value = null

    showNotice(
      'success',
      'Факультет удалён.'
    )
  } catch (error) {
    deleteError.value = getApiErrorMessage(
      error,
      'Не удалось удалить факультет'
    )
  } finally {
    deletingId.value = null
  }
}

onMounted(loadFaculties)
</script>

<template>
  <AdminPageShell
    title="Факультеты"
    description="Просматривайте и находите факультеты в рабочем списке. Создание и редактирование открываются поверх страницы и не сбрасывают текущие фильтры."
  >
    <template #actions>
      <UiButton
        size="sm"
        icon="pi pi-refresh"
        label="Обновить"
        :loading="loading"
        loading-text="Обновление..."
        @click="loadFaculties"
      />
    </template>

    <AdminNotice
      :type="notice.type"
      :message="notice.message"
      @close="clearNotice"
    />

    <UiCard
      title="Список факультетов"
      description="Поиск выполняется по названию, коду и описанию."
    >
      <div class="admin-faculties-workspace">
        <UiFilterBar
          v-model="searchQuery"
          search-placeholder="Название, код или описание"
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
              label="Добавить факультет"
              @click="openCreateFaculty"
            />
          </template>
        </UiFilterBar>

        <UiEmptyState
          v-if="loading"
          description="Загрузка факультетов..."
          compact
        />

        <UiEmptyState
          v-else-if="!faculties.length"
          description="Факультеты ещё не созданы. Добавьте первый факультет кнопкой выше."
          compact
        />

        <UiEmptyState
          v-else-if="!filteredFaculties.length"
          description="По текущему поиску и фильтрам факультеты не найдены."
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
          class="admin-faculty-grid"
        >
          <article
            v-for="faculty in filteredFaculties"
            :key="faculty.id"
            class="admin-faculty-card"
          >
            <div class="admin-faculty-card__heading">
              <span class="admin-faculty-card__code">
                {{ faculty.code }}
              </span>

              <h2 class="admin-faculty-card__title">
                {{ faculty.name }}
              </h2>
            </div>

            <p class="admin-faculty-card__description">
              {{ faculty.description || 'Описание пока не добавлено.' }}
            </p>

            <div class="admin-faculty-card__actions">
              <UiButton
                size="sm"
                icon="pi pi-pencil"
                label="Изменить"
                @click="openEditFaculty(faculty)"
              />

              <UiButton
                variant="danger"
                size="sm"
                icon="pi pi-trash"
                label="Удалить"
                :loading="deletingId === faculty.id"
                loading-text="Удаление..."
                @click="requestDeleteFaculty(faculty)"
              />
            </div>
          </article>
        </div>
      </div>
    </UiCard>

    <UiDialog
      v-model="facultyDialogModel"
      :title="facultyDialogTitle"
      width="38rem"
      :dismissable-mask="false"
    >
      <form
        class="admin-faculty-form"
        @submit.prevent="saveFaculty"
      >
        <UiAlert
          v-if="formError"
          variant="danger"
          :message="formError"
        />

        <UiInput
          v-model="form.name"
          label="Название факультета"
          maxlength="200"
          :disabled="saving"
          required
        />

        <UiInput
          v-model="form.code"
          label="Код факультета"
          hint="Код должен быть уникальным. Например: fit."
          maxlength="50"
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

        <div class="admin-faculty-form__actions">
          <UiButton
            variant="secondary"
            label="Отмена"
            :disabled="saving"
            @click="requestClose"
          />

          <UiButton
            type="submit"
            variant="primary"
            :label="isCreate ? 'Создать факультет' : 'Сохранить изменения'"
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
      title="Удалить факультет?"
      width="31rem"
      :closable="deletingId === null"
      :close-on-escape="deletingId === null"
      :dismissable-mask="false"
    >
      <div class="admin-faculty-delete">
        <UiAlert
          v-if="deleteError"
          variant="danger"
          :message="deleteError"
        />

        <p>
          Факультет
          <strong>«{{ deleteTarget?.name }}»</strong>
          будет удалён. Если факультет нельзя удалить из-за связанных групп, предметов или других данных, причина останется в этом окне.
        </p>
      </div>

      <template #footer>
        <div class="admin-faculty-delete__actions">
          <UiButton
            variant="secondary"
            label="Отмена"
            :disabled="deletingId !== null"
            @click="closeDeleteDialog"
          />

          <UiButton
            variant="danger"
            label="Удалить факультет"
            :loading="deletingId !== null"
            loading-text="Удаление..."
            @click="deleteFaculty"
          />
        </div>
      </template>
    </UiDialog>
  </AdminPageShell>
</template>

<style scoped>
.admin-faculties-workspace,
.admin-faculty-form,
.admin-faculty-delete {
  display: grid;
  gap: 14px;
}

.admin-faculty-grid {
  display: grid;
  grid-template-columns:
    repeat(auto-fit, minmax(min(100%, 300px), 1fr));
  gap: 12px;
}

.admin-faculty-card {
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

.admin-faculty-card__heading {
  min-width: 0;

  display: grid;
  gap: 6px;
}

.admin-faculty-card__code {
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
  overflow-wrap: anywhere;
}

.admin-faculty-card__title {
  margin: 0;

  font-size: 18px;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.admin-faculty-card__description {
  margin: 0;

  color: var(--st-text-secondary);

  font-size: 13px;
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.admin-faculty-card__actions,
.admin-faculty-form__actions,
.admin-faculty-delete__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

.admin-faculty-card__actions {
  margin-top: auto;
  padding-top: 2px;
}

.admin-faculty-delete p {
  margin: 0;

  color: var(--st-text-secondary);

  font-size: 14px;
  line-height: 1.65;
  overflow-wrap: anywhere;
}

.admin-faculty-delete strong {
  color: var(--st-text);
}

@media (max-width: 640px) {
  .admin-faculty-card__actions,
  .admin-faculty-form__actions,
  .admin-faculty-delete__actions {
    align-items: stretch;
    flex-direction: column;
  }

  .admin-faculty-card__actions > *,
  .admin-faculty-form__actions > *,
  .admin-faculty-delete__actions > * {
    width: 100%;
  }
}
</style>
