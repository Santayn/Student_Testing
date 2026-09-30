<script setup>
import {
  computed,
  onMounted,
  ref,
} from 'vue'

import AdminGroupMembersDrawer from '@/components/admin/AdminGroupMembersDrawer.vue'
import AdminNotice from '@/components/admin/AdminNotice.vue'
import AdminPageShell from '@/components/admin/AdminPageShell.vue'

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
  UiUnsavedChangesConfirm,
  useOverlayForm,
} from '@/components/ui'

import {
  API_ERROR_CODES,
  groupsApi,
} from '@/api'

import {
  apiFieldError,
  presentApiError,
} from '@/utils/apiErrorPresentation'

import {
  clearFormFieldError,
  focusFirstInvalidField,
  FORM_FIELD_ERROR_SUMMARY,
  setFormFieldError,
} from '@/utils/formErrorLifecycle'

import {
  useAdminGroupsData,
} from '@/composables/admin/groups/useAdminGroupsData'

import {
  useAdminGroupMembers,
} from '@/composables/admin/groups/useAdminGroupMembers'



const {
  sortOptions,
  faculties,
  groups,
  loading,
  notice,
  searchQuery,
  facultyFilter,
  sortMode,
  facultyOptions,
  facultyFilterOptions,
  hasActiveFilters,
  filteredGroups,
  filterResultText,
  showNotice,
  clearNotice,
  facultyName,
  resetFilters,
  loadData,
} = useAdminGroupsData()

const {
  membersDrawerVisible,
  membersGroup,
  membersLoading,
  memberSearch,
  addingPersonId,
  memberNotice,
  memberRemoveTarget,
  memberRemoveConfirmVisible,
  removingMembershipId,
  memberRemoveError,
  currentStudentMemberships,
  availableStudents,
  filteredCurrentStudentMemberships,
  filteredAvailableStudents,
  membersDrawerTitle,
  membersBusy,
  memberResultText,
  personById,
  personName,
  personContact,
  pausedStudentMembership,
  availableStudentActionLabel,
  clearMemberNotice,
  resetMemberSearch,
  openGroupMembers,
  resetMembersDrawer,
  addStudentToGroup,
  requestRemoveMember,
  closeMemberRemoveDialog,
  removeStudentFromGroup,
} = useAdminGroupMembers()

const formError = ref('')
const formFieldErrors = ref({})
const groupFormElement = ref(null)

const deleteTarget = ref(null)
const deleteConfirmVisible = ref(false)
const deletingId = ref(null)
const deleteError = ref('')


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

const groupDialogTitle = computed(() => {
  return isCreate.value
    ? 'Новая группа'
    : 'Редактирование группы'
})

const canSubmit = computed(() => {
  return !groupFormValidationMessage() && !saving.value
})

function groupActionItems(group) {
  return [
    {
      label: 'Изменить',
      icon: 'pi pi-pencil',
      command: () => openEditGroup(group),
    },
    {
      label: 'Удалить',
      icon: 'pi pi-trash',
      danger: true,
      command: () => requestDeleteGroup(group),
    },
  ]
}

function openCreateGroup() {
  formError.value = ''
  formFieldErrors.value = {}
  openCreate()
}

function openEditGroup(group) {
  formError.value = ''
  formFieldErrors.value = {}
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
    return { field: 'name', message: 'Введите название группы.' }
  }

  if (name.length > 200) {
    return { field: 'name', message: 'Название группы не может быть длиннее 200 символов.' }
  }

  if (!code) {
    return { field: 'code', message: 'Введите код группы.' }
  }

  if (code.length > 50) {
    return { field: 'code', message: 'Код группы не может быть длиннее 50 символов.' }
  }

  if (!Number.isFinite(facultyId) || facultyId <= 0) {
    return { field: 'facultyId', message: 'Выберите факультет.' }
  }

  if (
    !faculties.value.some(
      (faculty) => Number(faculty.id) === facultyId
    )
  ) {
    return { field: 'facultyId', message: 'Выбранный факультет больше недоступен. Обновите страницу и выберите другой.' }
  }

  const normalizedCode = code.toLocaleLowerCase('ru-RU')
  const duplicate = groups.value.find((group) => {
    return String(group.code ?? '')
      .trim()
      .toLocaleLowerCase('ru-RU') === normalizedCode &&
      String(group.id) !== String(form.id ?? '')
  })

  if (duplicate) {
    return { field: 'code', message: `Группа с кодом «${code}» уже существует.` }
  }

  return null
}

async function saveGroup() {
  if (saving.value) {
    return
  }

  const validation = groupFormValidationMessage()

  if (validation) {
    setFormFieldError(
      formFieldErrors,
      formError,
      validation.field,
      validation.message
    )

    await focusFirstInvalidField(
      groupFormElement.value
    )
    return
  }

  beginSaving()
  formError.value = ''
  formFieldErrors.value = {}
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
    const presentation = presentApiError(
      error,
      {
        context: 'form',
        fallback: 'Не удалось сохранить группу',
        forbiddenMessage:
          'Недостаточно прав для сохранения изменений.',
      }
    )

    formFieldErrors.value =
      presentation.fieldErrors

    formError.value =
      presentation.channel === 'field'
        ? FORM_FIELD_ERROR_SUMMARY
        : presentation.message
    failSaving()

    if (
      presentation.channel === 'field'
    ) {
      await focusFirstInvalidField(
        groupFormElement.value
      )
    }
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
    deleteError.value = presentApiError(
      error,
      {
        context: 'delete',
        fallback: 'Не удалось удалить группу',
        codeMessages: {
          [API_ERROR_CODES.GROUP_HAS_DEPENDENCIES]:
            'Группа используется назначениями, участниками, результатами или другими связанными данными и не может быть удалена.',
        },
        conflictMessage:
          'Группа используется назначениями, участниками, результатами или другими связанными данными и не может быть удалена.',
        forbiddenMessage:
          'Недостаточно прав для удаления группы.',
        notFoundMessage:
          'Группа уже удалена или больше недоступна.',
      }
    ).message
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
        variant="secondary"
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

        <UiLoadingState
          v-if="loading"
          compact
          label="Загрузка групп..."
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
                variant="secondary"
                size="sm"
                icon="pi pi-users"
                label="Состав группы"
                @click="openGroupMembers(group)"
              />

              <UiActionMenu
                :items="groupActionItems(group)"
                :aria-label="`Действия группы «${group.name}»`"
              />
            </div>
          </article>
        </div>
      </div>
    </UiCard>

    <AdminGroupMembersDrawer
      v-model="membersDrawerVisible"
      :title="membersDrawerTitle"
      :busy="membersBusy"
      :loading="membersLoading"
      :notice="memberNotice"
      :search="memberSearch"
      :result-text="memberResultText"
      :current-memberships="currentStudentMemberships"
      :filtered-current-memberships="filteredCurrentStudentMemberships"
      :available-students="availableStudents"
      :filtered-available-students="filteredAvailableStudents"
      :adding-person-id="addingPersonId"
      :removing-membership-id="removingMembershipId"
      :remove-confirm-visible="memberRemoveConfirmVisible"
      :remove-error="memberRemoveError"
      :remove-target="memberRemoveTarget"
      :group="membersGroup"
      :person-by-id="personById"
      :person-name="personName"
      :person-contact="personContact"
      :paused-student-membership="pausedStudentMembership"
      :available-student-action-label="availableStudentActionLabel"
      @after-hide="resetMembersDrawer"
      @clear-notice="clearMemberNotice"
      @update:search="memberSearch = $event"
      @reset-search="resetMemberSearch"
      @add-student="addStudentToGroup"
      @request-remove="requestRemoveMember"
      @close-remove="closeMemberRemoveDialog"
      @confirm-remove="removeStudentFromGroup"
    />

    <UiDialog
      v-model="groupDialogModel"
      :title="groupDialogTitle"
      width="38rem"
      :dismissable-mask="false"
    >
      <form
        ref="groupFormElement"
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
          @update:model-value="clearFormFieldError(formFieldErrors, formError, 'name')"
          :error="apiFieldError({ fieldErrors: formFieldErrors }, 'name')"
          label="Название группы"
          maxlength="200"
          :disabled="saving"
          required
        />

        <UiInput
          v-model="form.code"
          @update:model-value="clearFormFieldError(formFieldErrors, formError, 'code')"
          :error="apiFieldError({ fieldErrors: formFieldErrors }, 'code')"
          label="Код группы"
          maxlength="50"
          :disabled="saving"
          required
        />

        <UiSelect
          v-model="form.facultyId"
          @update:model-value="clearFormFieldError(formFieldErrors, formError, 'facultyId', 'faculty')"
          :error="apiFieldError({ fieldErrors: formFieldErrors }, 'facultyId', 'faculty')"
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
  font-weight: var(--st-font-weight-bold);
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
  font-weight: var(--st-font-weight-bold);
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
