<script setup>
import {
  computed,
  onMounted,
  ref,
  watch,
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
  UiSelect,
  UiTag,
} from '@/components/ui'

import {
  facultiesApi,
  getApiErrorMessage,
} from '@/api'

import {
  useAdminFacultySubjectsData,
} from '@/composables/useAdminFacultySubjectsData'

const {
  sortOptions,
  faculties,
  subjects,
  assignedSubjects,
  facultyId,
  searchQuery,
  sortMode,
  loadingBase,
  loadingAssigned,
  loading,
  notice,
  facultyOptions,
  selectedFaculty,
  availableSubjects,
  hasActiveFilters,
  filteredAssignedSubjects,
  filteredAvailableSubjects,
  filterResultText,
  resetFilters,
  showNotice,
  clearNotice,
  loadBaseData,
  loadAssignedSubjects: loadFacultyAssignedSubjects,
} = useAdminFacultySubjectsData()

const mutatingSubjectId = ref(null)

const removeTarget = ref(null)
const removeConfirmVisible = ref(false)
const removeError = ref('')

const saving = computed(() => {
  return mutatingSubjectId.value !== null
})

function relationErrorMessage(error, fallback) {
  const message = getApiErrorMessage(
    error,
    fallback
  )

  const normalized = message.toLowerCase()

  if (
    normalized.includes(
      'subject is already linked to faculty'
    )
  ) {
    return 'Этот предмет уже назначен факультету.'
  }

  if (
    normalized.includes(
      'teaching assignments exist'
    )
  ) {
    return (
      'Нельзя убрать предмет: он уже используется ' +
      'в учебной нагрузке этого факультета.'
    )
  }

  return message
}

function closeRemoveConfirm() {
  if (saving.value) {
    return
  }

  removeConfirmVisible.value = false
  removeTarget.value = null
  removeError.value = ''
}

function requestRemoveSubject(subject) {
  removeTarget.value = subject
  removeError.value = ''
  removeConfirmVisible.value = true
}

async function loadAssignedSubjects() {
  closeRemoveConfirm()
  return loadFacultyAssignedSubjects()
}

async function addSubject(subject) {
  const targetFacultyId =
    Number(facultyId.value)

  const subjectId = Number(subject?.id)

  if (
    saving.value ||
    !Number.isInteger(targetFacultyId) ||
    targetFacultyId <= 0 ||
    !Number.isInteger(subjectId) ||
    subjectId <= 0
  ) {
    return
  }

  mutatingSubjectId.value = subjectId
  clearNotice()

  try {
    await facultiesApi.addSubject(
            targetFacultyId,
            subjectId
          )

    if (
      Number(facultyId.value) !==
      targetFacultyId
    ) {
      return
    }

    showNotice(
      'success',
      `Предмет «${subject.name}» добавлен факультету.`
    )

    await loadAssignedSubjects()
  } catch (error) {
    if (
      Number(facultyId.value) !==
      targetFacultyId
    ) {
      return
    }

    showNotice(
      'error',
      relationErrorMessage(
        error,
        'Не удалось добавить предмет факультету.'
      )
    )
  } finally {
    if (mutatingSubjectId.value === subjectId) {
      mutatingSubjectId.value = null
    }
  }
}

async function removeSubject() {
  const targetFacultyId =
    Number(facultyId.value)

  const target = removeTarget.value
  const subjectId = Number(target?.id)

  if (
    saving.value ||
    !Number.isInteger(targetFacultyId) ||
    targetFacultyId <= 0 ||
    !Number.isInteger(subjectId) ||
    subjectId <= 0
  ) {
    return
  }

  mutatingSubjectId.value = subjectId
  removeError.value = ''
  clearNotice()

  try {
    await facultiesApi.removeSubject(
            targetFacultyId,
            subjectId
          )

    if (
      Number(facultyId.value) !==
      targetFacultyId
    ) {
      return
    }

    removeConfirmVisible.value = false
    removeTarget.value = null

    showNotice(
      'success',
      `Предмет «${target.name}» больше не связан с факультетом.`
    )

    await loadAssignedSubjects()
  } catch (error) {
    if (
      Number(facultyId.value) !==
      targetFacultyId
    ) {
      return
    }

    removeError.value =
      relationErrorMessage(
        error,
        'Не удалось убрать предмет из факультета.'
      )
  } finally {
    if (mutatingSubjectId.value === subjectId) {
      mutatingSubjectId.value = null
    }
  }
}

watch(
  facultyId,
  loadAssignedSubjects
)

onMounted(loadBaseData)
</script>

<template>
  <AdminPageShell
    title="Предметы факультетов"
    description="Управляйте предметами, которые доступны выбранному факультету."
  >
    <AdminNotice
      :type="notice.type"
      :message="notice.message"
      @close="clearNotice"
    />

    <UiCard
      title="Факультет"
      description="Выберите факультет, для которого нужно настроить список предметов."
    >
      <label class="faculty-subjects__faculty-field">
        <span>Факультет</span>

        <UiSelect
          v-model="facultyId"
          :disabled="loading || saving"
          :options="facultyOptions"
          option-label="label"
          option-value="value"
          placeholder="Выберите факультет"
        />
      </label>

      <div
        v-if="selectedFaculty"
        class="faculty-subjects__context"
      >
        <UiTag
          v-if="selectedFaculty.code"
          :value="selectedFaculty.code"
        />

        <UiTag
          variant="info"
          :value="`Назначено предметов: ${assignedSubjects.length}`"
        />
      </div>

      <UiEmptyState
        v-else-if="!loadingBase && !faculties.length"
        description="Факультеты ещё не созданы. Сначала добавьте факультет."
        compact
      />
    </UiCard>

    <template v-if="selectedFaculty">
      <UiFilterBar
        v-model="searchQuery"
        aria-label="Фильтры предметов факультета"
        search-placeholder="Поиск по названию или описанию"
        :result-text="filterResultText"
        :reset-disabled="!hasActiveFilters"
        @reset="resetFilters"
      >
        <template #filters>
          <UiSelect
            v-model="sortMode"
            :options="sortOptions"
            option-label="label"
            option-value="value"
            aria-label="Сортировка предметов"
          />
        </template>
      </UiFilterBar>

      <div class="faculty-subjects__columns">
        <UiCard
          title="Назначенные предметы"
          description="Предметы, которые уже доступны этому факультету."
        >
          <UiEmptyState
            v-if="loadingAssigned"
            description="Загрузка предметов факультета..."
            compact
          />

          <UiEmptyState
            v-else-if="!assignedSubjects.length"
            description="Факультету пока не назначено ни одного предмета."
            compact
          />

          <UiEmptyState
            v-else-if="!filteredAssignedSubjects.length"
            description="Среди назначенных предметов ничего не найдено."
            compact
          />

          <div
            v-else
            class="faculty-subjects__list"
          >
            <article
              v-for="subject in filteredAssignedSubjects"
              :key="subject.id"
              class="faculty-subjects__item"
            >
              <div class="faculty-subjects__item-copy">
                <h3>{{ subject.name }}</h3>

                <p>
                  {{
                    subject.description ||
                    'Описание не указано.'
                  }}
                </p>
              </div>

              <UiButton
                variant="danger"
                size="sm"
                label="Убрать"
                icon="pi pi-times"
                :loading="mutatingSubjectId === Number(subject.id)"
                loading-text="Удаление..."
                :disabled="saving"
                @click="requestRemoveSubject(subject)"
              />
            </article>
          </div>
        </UiCard>

        <UiCard
          title="Доступные предметы"
          description="Предметы из справочника, которые ещё не связаны с факультетом."
        >
          <UiEmptyState
            v-if="loadingAssigned"
            description="Загрузка доступных предметов..."
            compact
          />

          <UiEmptyState
            v-else-if="!subjects.length"
            description="В справочнике пока нет предметов."
            compact
          />

          <UiEmptyState
            v-else-if="!availableSubjects.length"
            description="Все предметы уже назначены этому факультету."
            compact
          />

          <UiEmptyState
            v-else-if="!filteredAvailableSubjects.length"
            description="Среди доступных предметов ничего не найдено."
            compact
          />

          <div
            v-else
            class="faculty-subjects__list"
          >
            <article
              v-for="subject in filteredAvailableSubjects"
              :key="subject.id"
              class="faculty-subjects__item"
            >
              <div class="faculty-subjects__item-copy">
                <h3>{{ subject.name }}</h3>

                <p>
                  {{
                    subject.description ||
                    'Описание не указано.'
                  }}
                </p>
              </div>

              <UiButton
                variant="primary"
                size="sm"
                label="Добавить"
                icon="pi pi-plus"
                :loading="mutatingSubjectId === Number(subject.id)"
                loading-text="Добавление..."
                :disabled="saving"
                @click="addSubject(subject)"
              />
            </article>
          </div>
        </UiCard>
      </div>
    </template>

    <UiDialog
      v-model="removeConfirmVisible"
      title="Убрать предмет из факультета?"
      width="30rem"
      :closable="!saving"
      :close-on-escape="!saving"
      @update:model-value="(visible) => {
        if (!visible) closeRemoveConfirm()
      }"
    >
      <div class="faculty-subjects__confirm">
        <p>
          Предмет
          <strong>«{{ removeTarget?.name }}»</strong>
          перестанет быть связан с факультетом
          <strong>«{{ selectedFaculty?.name }}»</strong>.
        </p>

        <UiAlert
          variant="warning"
          message="Если предмет уже используется в учебной нагрузке, удалить эту связь не получится."
        />

        <UiAlert
          v-if="removeError"
          variant="danger"
          :message="removeError"
        />
      </div>

      <template #footer>
        <div class="faculty-subjects__dialog-actions">
          <UiButton
            variant="ghost"
            label="Отмена"
            :disabled="saving"
            @click="closeRemoveConfirm"
          />

          <UiButton
            variant="danger"
            label="Убрать предмет"
            icon="pi pi-times"
            :loading="saving"
            loading-text="Удаление..."
            @click="removeSubject"
          />
        </div>
      </template>
    </UiDialog>
  </AdminPageShell>
</template>

<style scoped>
.faculty-subjects__faculty-field {
  max-width: 520px;

  display: grid;
  gap: 7px;
}

.faculty-subjects__faculty-field > span {
  color: var(--st-text);

  font-size: 13px;
  font-weight: 700;
}

.faculty-subjects__context {
  margin-top: 12px;

  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.faculty-subjects__columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  align-items: start;
}

.faculty-subjects__list {
  display: grid;
  gap: 10px;
}

.faculty-subjects__item {
  min-width: 0;
  padding: 14px;

  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;

  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 10px;
}

.faculty-subjects__item-copy {
  min-width: 0;
}

.faculty-subjects__item h3,
.faculty-subjects__item p,
.faculty-subjects__confirm p {
  margin: 0;
}

.faculty-subjects__item h3 {
  color: var(--st-text);

  font-size: 15px;
  line-height: 1.35;
}

.faculty-subjects__item p {
  margin-top: 5px;

  color: var(--st-text-secondary);

  font-size: 13px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.faculty-subjects__confirm {
  display: grid;
  gap: 14px;
}

.faculty-subjects__confirm p {
  color: var(--st-text-secondary);

  line-height: 1.55;
}

.faculty-subjects__dialog-actions {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 960px) {
  .faculty-subjects__columns {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .faculty-subjects__faculty-field {
    max-width: none;
  }

  .faculty-subjects__item {
    align-items: stretch;
    flex-direction: column;
  }

  .faculty-subjects__item :deep(.st-ui-button) {
    width: 100%;
  }

  .faculty-subjects__dialog-actions {
    flex-direction: column-reverse;
  }

  .faculty-subjects__dialog-actions :deep(.st-ui-button) {
    width: 100%;
  }
}
</style>
