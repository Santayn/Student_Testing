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
  UiUnsavedChangesConfirm,
  useOverlayForm,
} from '@/components/ui'

import {
  facultiesApi,
  getApiErrorMessage,
  groupsApi,
} from '@/api'

import {
  listFromResponse,
} from '@/utils/apiData'

const faculties = ref([])
const groups = ref([])
const loading = ref(false)

const notice = ref({
  type: 'info',
  message: '',
})

const searchQuery = ref('')
const facultyFilter = ref('all')
const sortMode = ref('name-asc')

const formError = ref('')

const deleteTarget = ref(null)
const deleteConfirmVisible = ref(false)
const deletingId = ref(null)
const deleteError = ref('')

const sortOptions = [
  { value: 'name-asc', label: 'Название А–Я' },
  { value: 'name-desc', label: 'Название Я–А' },
  { value: 'code-asc', label: 'Код А–Я' },
  { value: 'faculty-asc', label: 'Факультет А–Я' },
]

const {
  form,
  model: groupDialogModel,
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
    facultyId: '',
  }),
  mapEntity: (group) => ({
    id: group?.id ?? null,
    name: group?.name ?? '',
    code: group?.code ?? '',
    facultyId: group?.facultyId == null
      ? ''
      : String(group.facultyId),
  }),
})

const facultyOptions = computed(() => {
  return faculties.value
    .map((faculty) => ({
      value: String(faculty.id),
      label: faculty.name,
    }))
    .sort((left, right) =>
      left.label.localeCompare(right.label, 'ru')
    )
})

const facultyFilterOptions = computed(() => [
  { value: 'all', label: 'Все факультеты' },
  ...facultyOptions.value,
])

const hasActiveFilters = computed(() => {
  return Boolean(searchQuery.value.trim()) ||
    facultyFilter.value !== 'all' ||
    sortMode.value !== 'name-asc'
})

const filteredGroups = computed(() => {
  const query = searchQuery.value
    .trim()
    .toLocaleLowerCase('ru-RU')

  const result = groups.value.filter((group) => {
    if (
      facultyFilter.value !== 'all' &&
      String(group.facultyId) !== facultyFilter.value
    ) {
      return false
    }

    if (!query) {
      return true
    }

    const haystack = [
      group.name,
      group.code,
      facultyName(group.facultyId),
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

    if (sortMode.value === 'faculty-asc') {
      return facultyName(left.facultyId).localeCompare(
        facultyName(right.facultyId),
        'ru'
      ) || String(left.name ?? '').localeCompare(
        String(right.name ?? ''),
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
  return `Показано: ${filteredGroups.value.length} из ${groups.value.length}`
})

const groupDialogTitle = computed(() => {
  return isCreate.value
    ? 'Новая группа'
    : 'Редактирование группы'
})

const canSubmit = computed(() => {
  return !groupFormValidationMessage() && !saving.value
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

function facultyName(facultyId) {
  const faculty = faculties.value.find(
    (item) => Number(item.id) === Number(facultyId)
  )

  return faculty?.name ?? 'Факультет не найден'
}

function resetFilters() {
  searchQuery.value = ''
  facultyFilter.value = 'all'
  sortMode.value = 'name-asc'
}

function openCreateGroup() {
  formError.value = ''
  openCreate()
}

function openEditGroup(group) {
  formError.value = ''
  openEdit(group)
}

function requestDeleteGroup(group) {
  deleteTarget.value = group
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

function groupFormValidationMessage() {
  const name = String(form.name ?? '').trim()
  const code = String(form.code ?? '').trim()
  const facultyId = Number(form.facultyId)

  if (!name) {
    return 'Введите название группы.'
  }

  if (name.length > 200) {
    return 'Название группы не может быть длиннее 200 символов.'
  }

  if (!code) {
    return 'Введите код группы.'
  }

  if (code.length > 50) {
    return 'Код группы не может быть длиннее 50 символов.'
  }

  if (!Number.isFinite(facultyId) || facultyId <= 0) {
    return 'Выберите факультет.'
  }

  if (
    !faculties.value.some(
      (faculty) => Number(faculty.id) === facultyId
    )
  ) {
    return 'Выбранный факультет больше недоступен. Обновите страницу и выберите другой.'
  }

  const normalizedCode = code.toLocaleLowerCase('ru-RU')
  const duplicate = groups.value.find((group) => {
    return String(group.code ?? '')
      .trim()
      .toLocaleLowerCase('ru-RU') === normalizedCode &&
      String(group.id) !== String(form.id ?? '')
  })

  if (duplicate) {
    return `Группа с кодом «${code}» уже существует.`
  }

  return ''
}

async function loadData() {
  loading.value = true

  try {
    const [
      facultiesResponse,
      groupsResponse,
    ] = await Promise.all([
      facultiesApi.getAll(),
      groupsApi.getAll(),
    ])

    faculties.value = listFromResponse(facultiesResponse)
    groups.value = listFromResponse(groupsResponse)
  } catch (error) {
    showNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось загрузить группы'
      )
    )
  } finally {
    loading.value = false
  }
}

async function saveGroup() {
  const validationMessage = groupFormValidationMessage()

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
    facultyId: Number(form.facultyId),
  }

  try {
    const editingId = form.id

    if (editingId) {
      await groupsApi.update(
        editingId,
        payload
      )
    } else {
      await groupsApi.create(payload)
    }

    await loadData()
    finishSaving({ close: true })

    showNotice(
      'success',
      editingId
        ? 'Группа обновлена.'
        : 'Группа создана.'
    )
  } catch (error) {
    failSaving()
    formError.value = getApiErrorMessage(
      error,
      'Не удалось сохранить группу'
    )
  }
}

async function deleteGroup() {
  const group = deleteTarget.value

  if (!group || deletingId.value !== null) {
    return
  }

  deletingId.value = group.id
  deleteError.value = ''
  clearNotice()

  try {
    await groupsApi.remove(group.id)
    await loadData()

    deleteConfirmVisible.value = false
    deleteTarget.value = null

    showNotice(
      'success',
      'Группа удалена.'
    )
  } catch (error) {
    deleteError.value = getApiErrorMessage(
      error,
      'Не удалось удалить группу'
    )
  } finally {
    deletingId.value = null
  }
}

onMounted(loadData)
</script>

<template>
  <AdminPageShell
    title="Группы"
    description="Просматривайте учебные группы, находите их по названию, коду или факультету и редактируйте без ухода со страницы."
  >
    <template #actions>
      <UiButton
        size="sm"
        icon="pi pi-refresh"
        label="Обновить"
        :loading="loading"
        loading-text="Обновление..."
        @click="loadData"
      />
    </template>

    <AdminNotice
      :type="notice.type"
      :message="notice.message"
      @close="clearNotice"
    />

    <UiCard
      title="Учебные группы"
      description="Каждая группа относится к факультету. Поиск работает по названию, коду группы и названию факультета."
    >
      <div class="admin-groups-workspace">
        <UiFilterBar
          v-model="searchQuery"
          search-placeholder="Название, код или факультет"
          :result-text="filterResultText"
          :reset-disabled="!hasActiveFilters"
          @reset="resetFilters"
        >
          <template #filters>
            <UiSelect
              v-model="facultyFilter"
              label="Факультет"
              :options="facultyFilterOptions"
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
              label="Добавить группу"
              :disabled="!faculties.length"
              @click="openCreateGroup"
            />
          </template>
        </UiFilterBar>

        <UiAlert
          v-if="!loading && !faculties.length"
          variant="warning"
          message="Сначала создайте хотя бы один факультет — без факультета учебную группу создать нельзя."
        />

        <UiEmptyState
          v-if="loading"
          description="Загрузка групп..."
          compact
        />

        <UiEmptyState
          v-else-if="!groups.length"
          description="Группы ещё не созданы. Добавьте первую группу кнопкой выше."
          compact
        />

        <UiEmptyState
          v-else-if="!filteredGroups.length"
          description="По текущему поиску и фильтрам группы не найдены."
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
          class="admin-group-grid"
        >
          <article
            v-for="group in filteredGroups"
            :key="group.id"
            class="admin-group-card"
          >
            <div class="admin-group-card__heading">
              <span class="admin-group-card__code">
                {{ group.code }}
              </span>

              <h2 class="admin-group-card__title">
                {{ group.name }}
              </h2>
            </div>

            <div class="admin-group-card__faculty">
              <span>Факультет</span>
              <strong>{{ facultyName(group.facultyId) }}</strong>
            </div>

            <div class="admin-group-card__actions">
              <UiButton
                size="sm"
                icon="pi pi-pencil"
                label="Изменить"
                @click="openEditGroup(group)"
              />

              <UiButton
                variant="danger"
                size="sm"
                icon="pi pi-trash"
                label="Удалить"
                :loading="deletingId === group.id"
                loading-text="Удаление..."
                @click="requestDeleteGroup(group)"
              />
            </div>
          </article>
        </div>
      </div>
    </UiCard>

    <UiDialog
      v-model="groupDialogModel"
      :title="groupDialogTitle"
      width="38rem"
      :dismissable-mask="false"
    >
      <form
        class="admin-group-form"
        @submit.prevent="saveGroup"
      >
        <UiAlert
          v-if="formError"
          variant="danger"
          :message="formError"
        />

        <UiInput
          v-model="form.name"
          label="Название группы"
          maxlength="200"
          :disabled="saving"
          required
        />

        <UiInput
          v-model="form.code"
          label="Код группы"
          maxlength="50"
          :disabled="saving"
          required
        />

        <UiSelect
          v-model="form.facultyId"
          label="Факультет"
          :options="facultyOptions"
          placeholder="Выберите факультет"
          :disabled="saving"
          required
        />

        <div class="admin-group-form__actions">
          <UiButton
            variant="secondary"
            label="Отмена"
            :disabled="saving"
            @click="requestClose"
          />

          <UiButton
            type="submit"
            variant="primary"
            :label="isCreate ? 'Создать группу' : 'Сохранить изменения'"
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
      title="Удалить группу?"
      width="31rem"
      :closable="deletingId === null"
      :close-on-escape="deletingId === null"
      :dismissable-mask="false"
    >
      <div class="admin-group-delete">
        <UiAlert
          v-if="deleteError"
          variant="danger"
          :message="deleteError"
        />

        <p>
          Группа
          <strong>«{{ deleteTarget?.name }}»</strong>
          будет удалена. Если группа связана со студентами, учебной нагрузкой, тестами или другими данными и удалить её нельзя, причина останется в этом окне.
        </p>
      </div>

      <template #footer>
        <div class="admin-group-delete__actions">
          <UiButton
            variant="secondary"
            label="Отмена"
            :disabled="deletingId !== null"
            @click="closeDeleteDialog"
          />

          <UiButton
            variant="danger"
            label="Удалить группу"
            :loading="deletingId !== null"
            loading-text="Удаление..."
            @click="deleteGroup"
          />
        </div>
      </template>
    </UiDialog>
  </AdminPageShell>
</template>

<style scoped>
.admin-groups-workspace,
.admin-group-form,
.admin-group-delete {
  display: grid;
  gap: 14px;
}

.admin-group-grid {
  display: grid;
  grid-template-columns:
    repeat(auto-fit, minmax(min(100%, 300px), 1fr));
  gap: 12px;
}

.admin-group-card {
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

.admin-group-card__heading {
  min-width: 0;

  display: grid;
  gap: 7px;
}

.admin-group-card__code {
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

.admin-group-card__title {
  margin: 0;

  font-size: 18px;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.admin-group-card__faculty {
  min-width: 0;
  padding: 10px 11px;

  display: grid;
  gap: 4px;

  background: var(--st-surface);
  border: 1px solid var(--st-border);
  border-radius: 9px;
}

.admin-group-card__faculty span {
  color: var(--st-text-secondary);

  font-size: 11px;
  font-weight: 700;
}

.admin-group-card__faculty strong {
  overflow-wrap: anywhere;
}

.admin-group-card__actions,
.admin-group-form__actions,
.admin-group-delete__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

.admin-group-card__actions {
  margin-top: auto;
  padding-top: 2px;
}

.admin-group-delete p {
  margin: 0;

  color: var(--st-text-secondary);

  font-size: 14px;
  line-height: 1.65;
  overflow-wrap: anywhere;
}

.admin-group-delete strong {
  color: var(--st-text);
}

@media (max-width: 640px) {
  .admin-group-card__actions,
  .admin-group-form__actions,
  .admin-group-delete__actions {
    align-items: stretch;
    flex-direction: column;
  }

  .admin-group-card__actions > *,
  .admin-group-form__actions > *,
  .admin-group-delete__actions > * {
    width: 100%;
  }
}
</style>
