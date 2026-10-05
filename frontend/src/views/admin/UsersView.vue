<script setup>
import {
  computed,
  onMounted,
  ref,
} from 'vue'

import AdminNotice from '@/components/admin/AdminNotice.vue'
import AdminPageShell from '@/components/admin/AdminPageShell.vue'
import AdminUserDrawer from '@/components/admin/AdminUserDrawer.vue'

import {
  UiButton,
  UiEmptyState,
  UiLoadingState,
  UiFilterBar,
  UiSelect,
  UiTag,
  UiUnsavedChangesConfirm,
  useOverlayForm,
} from '@/components/ui'

import {
  getApiErrorMessage,
  usersApi,
} from '@/api'

import {
  emptyAdminPersonDraft,
  useAdminPersonEditor,
} from '@/composables/admin/users/useAdminPersonEditor'
import {
  useAdminUsersData,
} from '@/composables/admin/users/useAdminUsersData'

const {
  ACTIVE_OPTIONS,
  PROFILE_OPTIONS,
  SORT_OPTIONS,
  roles,
  users,
  people,
  loading,
  notice,
  searchQuery,
  roleFilter,
  activeFilter,
  profileFilter,
  sortMode,
  roleOptions,
  filteredUsers,
  hasActiveFilters,
  showNotice,
  clearNotice,
  personById,
  fullName,
  userFullName,
  userEmail,
  userPhone,
  roleIdsForUser,
  roleNamesForUser,
  permissionNames,
  availablePersonOptionsFor,
  resetFilters,
  loadData,
} = useAdminUsersData()

const formError = ref('')

function mapUserToForm(user) {
  return {
    id: Number(user?.id) || null,
    login: user?.login ?? '',
    active: user?.active !== false,
    personId:
      user?.personId == null
        ? null
        : Number(user.personId),
    roleIds: roleIdsForUser(user),
    newPerson: emptyAdminPersonDraft(),
  }
}

const {
  form: userForm,
  model: userDrawerModel,
  saving: userSaving,
  confirmCloseVisible,
  openEdit: openUserForm,
  requestClose: requestCloseUserDrawer,
  discardAndClose,
  continueEditing,
  beginSaving,
  finishSaving,
  failSaving,
  markClean,
} = useOverlayForm({
  mapEntity: mapUserToForm,
})

const selectedUser = computed(() => {
  return users.value.find(
    (user) =>
      Number(user.id) === Number(userForm.id)
  ) ?? null
})

const selectedPerson = computed(() => {
  return personById(userForm.personId)
})


const safeUserDrawerModel = computed({
  get: () => userDrawerModel.value,
  set: (nextValue) => {
    if (nextValue) {
      userDrawerModel.value = true
      return
    }

    requestCloseUserDrawerSafely()
  },
})

const selectedRoleItems = computed(() => {
  const selectedIds = new Set(
    (userForm.roleIds ?? []).map(Number)
  )

  return roles.value.filter(
    (role) => selectedIds.has(Number(role.id))
  )
})

const selectedRolePermissionNames = computed(() => {
  return [...new Set(
    selectedRoleItems.value.flatMap(
      (role) =>
        Array.isArray(role.permissions)
          ? role.permissions
              .map((permission) =>
                permission?.name
              )
              .filter(Boolean)
          : []
    )
  )].sort((left, right) =>
    String(left).localeCompare(
      String(right),
      'ru'
    )
  )
})

function roleIdsEqual(left, right) {
  const normalize = (values) =>
    [...new Set(
      (values ?? [])
        .map(Number)
        .filter(Number.isFinite)
    )].sort((a, b) => a - b)

  return JSON.stringify(normalize(left)) ===
    JSON.stringify(normalize(right))
}

const {
  personCreatorVisible,
  personCreateError,
  personCreateMessage,
  creatingPerson,
  personEditorVisible,
  personEditError,
  personEditMessage,
  updatingPerson,
  personEditDraft,
  personEditGuardVisible,
  personEditorDirty,
  resetPersonCreator,
  resetPersonEditor,
  requestOpenPersonCreator,
  openPersonEditor,
  cancelPersonCreator,
  requestCancelPersonEditor,
  requestPersonSelectionChange,
  createPersonFromDrawer,
  updatePersonFromDrawer,
  runAfterPersonEditorGuard,
  continuePersonEditing,
  discardPersonEditAndContinue,
} = useAdminPersonEditor({
  userForm,
  selectedPerson,
  people,
  fullName,
  usersApi,
  getApiErrorMessage,
})

function requestCloseUserDrawerSafely() {
  return runAfterPersonEditorGuard(() => {
    requestCloseUserDrawer()
  })
}

function discardUserChangesAndClose() {
  resetPersonEditor()
  resetPersonCreator()
  discardAndClose()
}

function openUserDrawer(user) {
  formError.value = ''
  resetPersonCreator()
  resetPersonEditor()
  openUserForm(user)
}

async function saveUser() {
  if (
    userSaving.value ||
    updatingPerson.value ||
    creatingPerson.value ||
    !userForm.id
  ) {
    return
  }

  formError.value = ''

  if (personEditorDirty.value) {
    formError.value =
      'Сначала сохраните или отмените изменения профиля Person.'
    return
  }

  const targetUserId = Number(userForm.id)

  const normalizedRoleIds = [
    ...new Set(
      (userForm.roleIds ?? [])
        .map(Number)
        .filter(Number.isFinite)
    ),
  ]

  if (!normalizedRoleIds.length) {
    formError.value =
      'У пользователя должна остаться хотя бы одна роль.'
    return
  }

  const original = users.value.find(
    (user) =>
      Number(user.id) === targetUserId
  )

  if (!original) {
    formError.value =
      'Пользователь больше не найден. Обновите список.'
    return
  }

  const nextPersonId =
    userForm.personId == null ||
    userForm.personId === ''
      ? null
      : Number(userForm.personId)

  const nextActive = Boolean(
    userForm.active
  )

  const currentPersonId =
    original.personId == null
      ? null
      : Number(original.personId)

  const personChanged =
    nextPersonId !== currentPersonId
  const rolesChanged = !roleIdsEqual(
    normalizedRoleIds,
    roleIdsForUser(original)
  )
  const activeChanged =
    nextActive !==
    Boolean(original.active)

  if (
    !personChanged &&
    !rolesChanged &&
    !activeChanged
  ) {
    finishSaving({ close: true })
    return
  }

  beginSaving()
  let completedSteps = 0

  try {
    if (personChanged) {
      await usersApi.updatePersonBinding(
        targetUserId,
        nextPersonId
      )
      completedSteps += 1
    }

    if (rolesChanged) {
      await usersApi.updateRoles(
        targetUserId,
        {
          roleIds: normalizedRoleIds,
        }
      )
      completedSteps += 1
    }

    if (activeChanged) {
      await usersApi.setActive(
        targetUserId,
        nextActive
      )
      completedSteps += 1
    }

    await loadData({ clearMessage: false })

    finishSaving({ close: true })

    showNotice(
      'success',
      `Пользователь ${original.login} обновлён.`
    )
  } catch (error) {
    failSaving()

    await loadData({ clearMessage: false })

    const refreshed = users.value.find(
      (user) =>
        Number(user.id) === targetUserId
    )

    if (refreshed) {
      markClean(mapUserToForm(refreshed))
    }

    const message = getApiErrorMessage(
      error,
      'Не удалось сохранить пользователя.'
    )

    formError.value = completedSteps
      ? `${message} Часть предыдущих изменений уже была применена; форма синхронизирована с сервером.`
      : message
  }
}

onMounted(loadData)
</script>

<template>
  <AdminPageShell
    title="Пользователи"
    description="Поиск пользователей, управление активностью, привязкой профиля и назначенными ролями через явное сохранение изменений."
  >
    <template #actions>
      <UiButton
        type="button"
        variant="secondary"
        icon="pi pi-refresh"
        label="Обновить"
        :loading="loading"
        loading-text="Обновление..."
        :disabled="loading"
        @click="loadData()"
      />
    </template>

    <AdminNotice
      :type="notice.type"
      :message="notice.message"
      @close="clearNotice"
    />

    <div class="admin-users-workspace">
      <UiFilterBar
        v-model="searchQuery"
        search-placeholder="Логин, ФИО, email, телефон или роль"
        :result-count="filteredUsers.length"
        :reset-disabled="!hasActiveFilters"
        @reset="resetFilters"
      >
        <template #filters>
          <UiSelect
            v-model="roleFilter"
            :options="roleOptions"
            placeholder="Все роли"
            aria-label="Фильтр по роли"
          />

          <UiSelect
            v-model="activeFilter"
            :options="ACTIVE_OPTIONS"
            aria-label="Фильтр по активности"
          />

          <UiSelect
            v-model="profileFilter"
            :options="PROFILE_OPTIONS"
            aria-label="Фильтр по профилю"
          />

          <UiSelect
            v-model="sortMode"
            :options="SORT_OPTIONS"
            aria-label="Сортировка пользователей"
          />
        </template>
      </UiFilterBar>

      <UiLoadingState
        v-if="loading"
        label="Загрузка пользователей..."
      />

      <UiEmptyState
        v-else-if="!filteredUsers.length"
        :description="
          hasActiveFilters
            ? 'Пользователи по выбранным фильтрам не найдены.'
            : 'Пользователи пока не найдены.'
        "
      />

      <div
        v-else
        class="admin-user-grid"
      >
        <article
          v-for="user in filteredUsers"
          :key="user.id"
          class="admin-user-card"
        >
          <header class="admin-user-card__header">
            <div class="admin-user-card__identity">
              <div class="admin-user-card__status-row">
                <UiTag
                  :variant="user.active ? 'success' : 'secondary'"
                  :value="user.active ? 'Активен' : 'Отключён'"
                />

                <UiTag
                  :variant="user.personId ? 'info' : 'warning'"
                  :value="user.personId ? 'Профиль привязан' : 'Без профиля'"
                />
              </div>

              <h2 class="admin-user-card__login">
                {{ user.login }}
              </h2>

              <p class="admin-user-card__name">
                {{ userFullName(user) }}
              </p>
            </div>
          </header>

          <dl class="admin-user-card__details">
            <div>
              <dt>Email</dt>
              <dd>{{ userEmail(user) }}</dd>
            </div>

            <div>
              <dt>Телефон</dt>
              <dd>{{ userPhone(user) }}</dd>
            </div>
          </dl>

          <div class="admin-user-card__roles">
            <span class="admin-user-card__section-label">
              Роли
            </span>

            <div
              v-if="roleNamesForUser(user).length"
              class="admin-user-card__tags"
            >
              <UiTag
                v-for="roleName in roleNamesForUser(user)"
                :key="roleName"
                :value="roleName"
              />
            </div>

            <span
              v-else
              class="admin-user-card__empty-copy"
            >
              Роли не назначены
            </span>
          </div>

          <div class="admin-user-card__actions">
            <UiButton
              variant="secondary"
              icon="pi pi-pencil"
              label="Настроить"
              @click="openUserDrawer(user)"
            />
          </div>
        </article>
      </div>
    </div>

    <AdminUserDrawer
      v-model="safeUserDrawerModel"
      :user-form="userForm"
      :roles="roles"
      :form-error="formError"
      :user-saving="userSaving"
      :selected-user-name="userFullName(selectedUser)"
      :selected-person="selectedPerson"
      :selected-person-name="selectedPerson ? fullName(selectedPerson) : '—'"
      :available-person-options="availablePersonOptionsFor(userForm.id)"
      :selected-role-permission-names="selectedRolePermissionNames"
      :selected-user-permission-names="permissionNames(selectedUser)"
      :person-creator-visible="personCreatorVisible"
      :person-create-error="personCreateError"
      :person-create-message="personCreateMessage"
      :creating-person="creatingPerson"
      :person-editor-visible="personEditorVisible"
      :person-edit-draft="personEditDraft"
      :person-edit-error="personEditError"
      :person-edit-message="personEditMessage"
      :updating-person="updatingPerson"
      :person-editor-dirty="personEditorDirty"
      @request-person-selection-change="requestPersonSelectionChange"
      @request-open-person-creator="requestOpenPersonCreator"
      @open-person-editor="openPersonEditor"
      @cancel-person-creator="cancelPersonCreator"
      @create-person="createPersonFromDrawer"
      @request-cancel-person-editor="requestCancelPersonEditor"
      @update-person="updatePersonFromDrawer"
      @request-close="requestCloseUserDrawerSafely"
      @save="saveUser"
    />

    <UiUnsavedChangesConfirm
      v-model="confirmCloseVisible"
      :busy="userSaving"
      @continue="continueEditing"
      @discard="discardUserChangesAndClose"
    />

    <UiUnsavedChangesConfirm
      v-model="personEditGuardVisible"
      title="Есть несохранённые изменения профиля"
      message="Изменения Person ещё не сохранены. Их можно продолжить редактировать или отбросить."
      continue-label="Продолжить редактирование"
      discard-label="Отбросить изменения"
      :busy="updatingPerson"
      @continue="continuePersonEditing"
      @discard="discardPersonEditAndContinue"
    />
  </AdminPageShell>
</template>

<style scoped>
.admin-users-workspace {
  display: grid;
  gap: 14px;
}

.admin-user-grid {
  display: grid;
  grid-template-columns:
    repeat(auto-fit, minmax(min(100%, 320px), 1fr));
  gap: 12px;
}

.admin-user-card {
  min-width: 0;
  padding: 15px;

  display: grid;
  align-content: start;
  gap: 14px;

  color: var(--st-text);
  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 12px;
}

.admin-user-card__header,
.admin-user-card__identity,
.admin-user-card__roles {
  min-width: 0;
  display: grid;
  gap: 7px;
}

.admin-user-card__status-row,
.admin-user-card__tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
}

.admin-user-card__login {
  margin: 0;

  font-size: 18px;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.admin-user-card__name {
  margin: 0;

  color: var(--st-text-secondary);

  font-size: 13px;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.admin-user-card__details {
  margin: 0;

  display: grid;
  grid-template-columns:
    repeat(auto-fit, minmax(min(100%, 180px), 1fr));
  gap: 10px;
}

.admin-user-card__details > div {
  min-width: 0;
  display: grid;
  gap: 3px;
}

.admin-user-card__details dt,
.admin-user-card__section-label {
  color: var(--st-text-secondary);

  font-size: 11px;
  font-weight: var(--st-font-weight-bold);
  line-height: 1.35;
}

.admin-user-card__details dd {
  margin: 0;

  color: var(--st-text);

  font-size: 13px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.admin-user-card__empty-copy {
  color: var(--st-text-secondary);

  font-size: 12px;
  line-height: 1.55;
}

.admin-user-card__actions {
  margin-top: auto;
  padding-top: 2px;

  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 640px) {
  .admin-user-card__actions {
    align-items: stretch;
    flex-direction: column;
  }

  .admin-user-card__actions > * {
    width: 100%;
  }
}
</style>
