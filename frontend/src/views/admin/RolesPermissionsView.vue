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
  UiCheckbox,
  UiDialog,
  UiDrawer,
  UiEmptyState,
  UiFilterBar,
  UiInput,
  UiSelect,
  UiTag,
  UiTextarea,
  UiUnsavedChangesConfirm,
  useOverlayForm,
} from '@/components/ui'

import {
  getApiErrorMessage,
  rolesApi,
} from '@/api'

import {
  listFromResponse,
} from '@/utils/apiData'

const roles = ref([])
const permissions = ref([])
const loading = ref(false)

const notice = ref({
  type: 'info',
  message: '',
})

const roleSearch = ref('')
const rolePermissionFilter = ref('all')
const roleSortMode = ref('name-asc')

const permissionSearch = ref('')
const permissionUsageFilter = ref('all')
const permissionSortMode = ref('name-asc')

const roleFormError = ref('')
const permissionFormError = ref('')
const rolePermissionsError = ref('')
const selectedRoleId = ref(null)
const permissionDrawerSearch = ref('')

const ROLE_PERMISSION_FILTER_OPTIONS = [
  { value: 'all', label: 'Все роли' },
  { value: 'with', label: 'С правами' },
  { value: 'without', label: 'Без прав' },
]

const ROLE_SORT_OPTIONS = [
  { value: 'name-asc', label: 'Название А–Я' },
  { value: 'name-desc', label: 'Название Я–А' },
  { value: 'permissions-desc', label: 'Сначала больше прав' },
]

const PERMISSION_USAGE_OPTIONS = [
  { value: 'all', label: 'Все permissions' },
  { value: 'used', label: 'Назначены ролям' },
  { value: 'unused', label: 'Не используются' },
]

const PERMISSION_SORT_OPTIONS = [
  { value: 'name-asc', label: 'Название А–Я' },
  { value: 'name-desc', label: 'Название Я–А' },
  { value: 'usage-desc', label: 'Сначала чаще используемые' },
]

const roleCreateOverlay = useOverlayForm({
  createDefault: () => ({
    name: '',
    description: '',
  }),
})

const permissionCreateOverlay = useOverlayForm({
  createDefault: () => ({
    name: '',
    description: '',
  }),
})

const rolePermissionsOverlay = useOverlayForm({
  createDefault: () => ({
    permissionIds: [],
  }),
  mapEntity: (role) => ({
    permissionIds: rolePermissionIds(role),
  }),
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

function normalize(value) {
  return String(value ?? '')
    .trim()
    .toLocaleLowerCase('ru-RU')
}

function rolePermissions(role) {
  return Array.isArray(role?.permissions)
    ? role.permissions
    : []
}

function rolePermissionIds(role) {
  return rolePermissions(role)
    .map((permission) => Number(permission?.id))
    .filter(Number.isFinite)
}

function selectedRole() {
  return roles.value.find(
    (role) =>
      Number(role.id) === Number(selectedRoleId.value)
  ) ?? null
}

function permissionUsageCount(permissionId) {
  const id = Number(permissionId)

  return roles.value.filter((role) =>
    rolePermissionIds(role).includes(id)
  ).length
}

const filteredRoles = computed(() => {
  const query = normalize(roleSearch.value)

  const result = roles.value.filter((role) => {
    const permissionCount = rolePermissionIds(role).length

    if (
      rolePermissionFilter.value === 'with' &&
      permissionCount === 0
    ) {
      return false
    }

    if (
      rolePermissionFilter.value === 'without' &&
      permissionCount > 0
    ) {
      return false
    }

    if (!query) {
      return true
    }

    const haystack = [
      role.name,
      role.description,
      ...rolePermissions(role).map(
        (permission) => permission?.name
      ),
    ]
      .filter(Boolean)
      .join(' ')
      .toLocaleLowerCase('ru-RU')

    return haystack.includes(query)
  })

  return [...result].sort((left, right) => {
    if (roleSortMode.value === 'permissions-desc') {
      const difference =
        rolePermissionIds(right).length -
        rolePermissionIds(left).length

      if (difference !== 0) {
        return difference
      }
    }

    const direction =
      roleSortMode.value === 'name-desc'
        ? -1
        : 1

    return direction * String(left.name ?? '')
      .localeCompare(
        String(right.name ?? ''),
        'ru'
      )
  })
})

const filteredPermissions = computed(() => {
  const query = normalize(permissionSearch.value)

  const result = permissions.value.filter((permission) => {
    const usageCount = permissionUsageCount(permission.id)

    if (
      permissionUsageFilter.value === 'used' &&
      usageCount === 0
    ) {
      return false
    }

    if (
      permissionUsageFilter.value === 'unused' &&
      usageCount > 0
    ) {
      return false
    }

    if (!query) {
      return true
    }

    return [
      permission.name,
      permission.description,
    ]
      .filter(Boolean)
      .join(' ')
      .toLocaleLowerCase('ru-RU')
      .includes(query)
  })

  return [...result].sort((left, right) => {
    if (permissionSortMode.value === 'usage-desc') {
      const difference =
        permissionUsageCount(right.id) -
        permissionUsageCount(left.id)

      if (difference !== 0) {
        return difference
      }
    }

    const direction =
      permissionSortMode.value === 'name-desc'
        ? -1
        : 1

    return direction * String(left.name ?? '')
      .localeCompare(
        String(right.name ?? ''),
        'ru'
      )
  })
})

const drawerPermissions = computed(() => {
  const query = normalize(permissionDrawerSearch.value)

  return [...permissions.value]
    .filter((permission) => {
      if (!query) {
        return true
      }

      return [
        permission.name,
        permission.description,
      ]
        .filter(Boolean)
        .join(' ')
        .toLocaleLowerCase('ru-RU')
        .includes(query)
    })
    .sort((left, right) =>
      String(left.name ?? '').localeCompare(
        String(right.name ?? ''),
        'ru'
      )
    )
})

const roleFiltersActive = computed(() => {
  return Boolean(roleSearch.value.trim()) ||
    rolePermissionFilter.value !== 'all' ||
    roleSortMode.value !== 'name-asc'
})

const permissionFiltersActive = computed(() => {
  return Boolean(permissionSearch.value.trim()) ||
    permissionUsageFilter.value !== 'all' ||
    permissionSortMode.value !== 'name-asc'
})

const drawerRole = computed(selectedRole)

function resetRoleFilters() {
  roleSearch.value = ''
  rolePermissionFilter.value = 'all'
  roleSortMode.value = 'name-asc'
}

function resetPermissionFilters() {
  permissionSearch.value = ''
  permissionUsageFilter.value = 'all'
  permissionSortMode.value = 'name-asc'
}

async function loadData({ preserveDrawer = true } = {}) {
  loading.value = true
  clearNotice()

  const drawerRoleId = preserveDrawer
    ? selectedRoleId.value
    : null

  try {
    const [
      rolesResponse,
      permissionsResponse,
    ] = await Promise.all([
      rolesApi.getAll(),
      rolesApi.getPermissions(),
    ])

    roles.value = listFromResponse(rolesResponse)
    permissions.value = listFromResponse(
      permissionsResponse
    )

    if (drawerRoleId != null) {
      const refreshedRole = roles.value.find(
        (role) =>
          Number(role.id) === Number(drawerRoleId)
      )

      if (refreshedRole) {
        rolePermissionsOverlay.markClean({
          permissionIds:
            rolePermissionIds(refreshedRole),
        })
      } else {
        selectedRoleId.value = null
        rolePermissionsOverlay.closeImmediately()
      }
    }
  } catch (error) {
    showNotice(
      'danger',
      getApiErrorMessage(
        error,
        'Не удалось загрузить роли и права.'
      )
    )
  } finally {
    loading.value = false
  }
}

function openCreateRole() {
  roleFormError.value = ''
  roleCreateOverlay.openCreate()
}

function openCreatePermission() {
  permissionFormError.value = ''
  permissionCreateOverlay.openCreate()
}

function openRolePermissions(role) {
  rolePermissionsError.value = ''
  permissionDrawerSearch.value = ''
  selectedRoleId.value = Number(role.id)
  rolePermissionsOverlay.openEdit(role)
}

function validateRoleForm() {
  const name = String(
    roleCreateOverlay.form.name ?? ''
  ).trim()
  const description = String(
    roleCreateOverlay.form.description ?? ''
  ).trim()

  if (!name) {
    return 'Введите название роли.'
  }

  if (name.length > 100) {
    return 'Название роли не должно превышать 100 символов.'
  }

  if (description.length > 500) {
    return 'Описание роли не должно превышать 500 символов.'
  }

  const duplicate = roles.value.some(
    (role) => normalize(role.name) === normalize(name)
  )

  if (duplicate) {
    return 'Роль с таким названием уже существует.'
  }

  return ''
}

function validatePermissionForm() {
  const name = String(
    permissionCreateOverlay.form.name ?? ''
  ).trim()
  const description = String(
    permissionCreateOverlay.form.description ?? ''
  ).trim()

  if (!name) {
    return 'Введите название permission.'
  }

  if (name.length > 100) {
    return 'Название permission не должно превышать 100 символов.'
  }

  if (description.length > 500) {
    return 'Описание permission не должно превышать 500 символов.'
  }

  const duplicate = permissions.value.some(
    (permission) =>
      normalize(permission.name) === normalize(name)
  )

  if (duplicate) {
    return 'Permission с таким названием уже существует.'
  }

  return ''
}

async function createRole() {
  if (roleCreateOverlay.saving.value) {
    return
  }

  const validationError = validateRoleForm()

  if (validationError) {
    roleFormError.value = validationError
    return
  }

  roleFormError.value = ''
  roleCreateOverlay.beginSaving()

  try {
    const response = await rolesApi.createRole({
      name: String(
        roleCreateOverlay.form.name
      ).trim(),
      description:
        String(
          roleCreateOverlay.form.description ?? ''
        ).trim() || null,
    })

    const createdRole = response?.data

    if (createdRole?.id != null) {
      roles.value = [
        ...roles.value,
        createdRole,
      ]
    } else {
      await loadData({ preserveDrawer: false })
    }

    roleCreateOverlay.finishSaving()

    showNotice(
      'success',
      createdRole?.name
        ? `Роль «${createdRole.name}» создана.`
        : 'Роль создана.'
    )
  } catch (error) {
    roleCreateOverlay.failSaving()
    roleFormError.value = getApiErrorMessage(
      error,
      'Не удалось создать роль.'
    )
  }
}

async function createPermission() {
  if (permissionCreateOverlay.saving.value) {
    return
  }

  const validationError = validatePermissionForm()

  if (validationError) {
    permissionFormError.value = validationError
    return
  }

  permissionFormError.value = ''
  permissionCreateOverlay.beginSaving()

  try {
    const response = await rolesApi.createPermission({
      name: String(
        permissionCreateOverlay.form.name
      ).trim(),
      description:
        String(
          permissionCreateOverlay.form.description ?? ''
        ).trim() || null,
    })

    const createdPermission = response?.data

    if (createdPermission?.id != null) {
      permissions.value = [
        ...permissions.value,
        createdPermission,
      ]
    } else {
      await loadData()
    }

    permissionCreateOverlay.finishSaving()

    showNotice(
      'success',
      createdPermission?.name
        ? `Permission «${createdPermission.name}» создан.`
        : 'Permission создан.'
    )
  } catch (error) {
    permissionCreateOverlay.failSaving()
    permissionFormError.value = getApiErrorMessage(
      error,
      'Не удалось создать permission.'
    )
  }
}

async function saveRolePermissions() {
  if (
    rolePermissionsOverlay.saving.value ||
    selectedRoleId.value == null
  ) {
    return
  }

  rolePermissionsError.value = ''
  rolePermissionsOverlay.beginSaving()

  const permissionIds = [
    ...new Set(
      (rolePermissionsOverlay.form.permissionIds ?? [])
        .map(Number)
        .filter(Number.isFinite)
    ),
  ]

  try {
    const response = await rolesApi.setPermissions(
      selectedRoleId.value,
      permissionIds
    )

    const updatedRole = response?.data

    roles.value = roles.value.map((role) =>
      Number(role.id) === Number(selectedRoleId.value)
        ? updatedRole ?? role
        : role
    )

    rolePermissionsOverlay.finishSaving({
      values: {
        permissionIds:
          rolePermissionIds(updatedRole),
      },
    })

    selectedRoleId.value = null

    showNotice(
      'success',
      updatedRole?.name
        ? `Права роли «${updatedRole.name}» сохранены.`
        : 'Права роли сохранены.'
    )
  } catch (error) {
    rolePermissionsOverlay.failSaving()
    rolePermissionsError.value = getApiErrorMessage(
      error,
      'Не удалось сохранить права роли.'
    )
  }
}

function requestRolePermissionsClose() {
  const closed = rolePermissionsOverlay.requestClose()

  if (closed) {
    selectedRoleId.value = null
    rolePermissionsError.value = ''
    permissionDrawerSearch.value = ''
  }
}

function discardRolePermissionsAndClose() {
  rolePermissionsOverlay.discardAndClose()
  selectedRoleId.value = null
  rolePermissionsError.value = ''
  permissionDrawerSearch.value = ''
}

onMounted(loadData)
</script>

<template>
  <AdminPageShell
    title="Роли и права"
    description="Создание ролей и permissions, а также явная настройка набора прав для каждой роли."
  >
    <template #actions>
      <UiButton
        variant="secondary"
        icon="pi pi-key"
        label="Создать permission"
        :disabled="loading"
        @click="openCreatePermission"
      />

      <UiButton
        variant="primary"
        icon="pi pi-plus"
        label="Создать роль"
        :disabled="loading"
        @click="openCreateRole"
      />
    </template>

    <AdminNotice
      :type="notice.type"
      :message="notice.message"
      @close="clearNotice"
    />

    <UiCard
      title="Роли"
      description="Каждая роль объединяет набор permissions, который получают назначенные ей пользователи."
    >
      <div class="admin-access-workspace">
        <UiFilterBar
          v-model="roleSearch"
          search-placeholder="Название, описание или permission"
          :result-count="filteredRoles.length"
          :reset-disabled="!roleFiltersActive"
          @reset="resetRoleFilters"
        >
          <template #filters>
            <UiSelect
              v-model="rolePermissionFilter"
              :options="ROLE_PERMISSION_FILTER_OPTIONS"
              aria-label="Фильтр ролей по наличию прав"
            />

            <UiSelect
              v-model="roleSortMode"
              :options="ROLE_SORT_OPTIONS"
              aria-label="Сортировка ролей"
            />
          </template>
        </UiFilterBar>

        <UiEmptyState
          v-if="loading"
          description="Загрузка ролей..."
        />

        <UiEmptyState
          v-else-if="!filteredRoles.length"
          description="Роли по выбранным условиям не найдены."
        />

        <div
          v-else
          class="admin-access-role-grid"
        >
          <article
            v-for="role in filteredRoles"
            :key="role.id"
            class="admin-access-role-card"
          >
            <div class="admin-access-role-card__header">
              <div>
                <h3>{{ role.name }}</h3>
                <p>
                  {{
                    role.description ||
                    'Описание роли не задано.'
                  }}
                </p>
              </div>

              <UiTag
                variant="info"
                :value="`${rolePermissionIds(role).length} прав`"
              />
            </div>

            <div
              v-if="rolePermissions(role).length"
              class="admin-access-tags"
            >
              <UiTag
                v-for="permission in rolePermissions(role).slice(0, 6)"
                :key="permission.id"
                :value="permission.name"
              />

              <UiTag
                v-if="rolePermissions(role).length > 6"
                :value="`+${rolePermissions(role).length - 6}`"
              />
            </div>

            <p
              v-else
              class="admin-access-muted"
            >
              Для роли пока не назначены permissions.
            </p>

            <div class="admin-access-role-card__actions">
              <UiButton
                variant="secondary"
                icon="pi pi-sliders-h"
                label="Настроить права"
                @click="openRolePermissions(role)"
              />
            </div>
          </article>
        </div>
      </div>
    </UiCard>

    <UiCard
      title="Справочник permissions"
      description="Доступные права платформы. Созданный permission можно назначить одной или нескольким ролям."
    >
      <div class="admin-access-workspace">
        <UiFilterBar
          v-model="permissionSearch"
          search-placeholder="Название или описание permission"
          :result-count="filteredPermissions.length"
          :reset-disabled="!permissionFiltersActive"
          @reset="resetPermissionFilters"
        >
          <template #filters>
            <UiSelect
              v-model="permissionUsageFilter"
              :options="PERMISSION_USAGE_OPTIONS"
              aria-label="Фильтр permissions по использованию"
            />

            <UiSelect
              v-model="permissionSortMode"
              :options="PERMISSION_SORT_OPTIONS"
              aria-label="Сортировка permissions"
            />
          </template>
        </UiFilterBar>

        <UiEmptyState
          v-if="loading"
          description="Загрузка permissions..."
        />

        <UiEmptyState
          v-else-if="!filteredPermissions.length"
          description="Permissions по выбранным условиям не найдены."
        />

        <div
          v-else
          class="admin-access-permission-grid"
        >
          <article
            v-for="permission in filteredPermissions"
            :key="permission.id"
            class="admin-access-permission-card"
          >
            <div class="admin-access-permission-card__header">
              <strong>{{ permission.name }}</strong>

              <UiTag
                :variant="permissionUsageCount(permission.id) ? 'success' : 'secondary'"
                :value="`В ролях: ${permissionUsageCount(permission.id)}`"
              />
            </div>

            <p>
              {{
                permission.description ||
                'Описание permission не задано.'
              }}
            </p>
          </article>
        </div>
      </div>
    </UiCard>

    <UiDialog
      v-model="roleCreateOverlay.model.value"
      title="Новая роль"
      width="34rem"
    >
      <div class="admin-access-form">
        <UiAlert
          v-if="roleFormError"
          variant="danger"
          :message="roleFormError"
        />

        <UiInput
          v-model="roleCreateOverlay.form.name"
          label="Название роли"
          placeholder="Например: CURATOR"
          required
          maxlength="100"
        />

        <UiTextarea
          v-model="roleCreateOverlay.form.description"
          label="Описание"
          placeholder="Кратко опишите назначение роли"
          :rows="4"
          maxlength="500"
        />

        <UiAlert
          variant="info"
          message="После создания к роли можно отдельно назначить permissions. Изменение имени или описания существующей роли текущим API не предусмотрено."
        />
      </div>

      <template #footer>
        <div class="admin-access-dialog-actions">
          <UiButton
            variant="ghost"
            label="Отмена"
            :disabled="roleCreateOverlay.saving.value"
            @click="roleCreateOverlay.requestClose"
          />

          <UiButton
            variant="primary"
            label="Создать роль"
            :loading="roleCreateOverlay.saving.value"
            loading-text="Создание..."
            @click="createRole"
          />
        </div>
      </template>
    </UiDialog>

    <UiUnsavedChangesConfirm
      v-model="roleCreateOverlay.confirmCloseVisible.value"
      :busy="roleCreateOverlay.saving.value"
      @discard="roleCreateOverlay.discardAndClose"
      @continue="roleCreateOverlay.continueEditing"
    />

    <UiDialog
      v-model="permissionCreateOverlay.model.value"
      title="Новый permission"
      width="34rem"
    >
      <div class="admin-access-form">
        <UiAlert
          v-if="permissionFormError"
          variant="danger"
          :message="permissionFormError"
        />

        <UiInput
          v-model="permissionCreateOverlay.form.name"
          label="Название permission"
          placeholder="Например: reports.manage"
          required
          maxlength="100"
        />

        <UiTextarea
          v-model="permissionCreateOverlay.form.description"
          label="Описание"
          placeholder="Что разрешает это право"
          :rows="4"
          maxlength="500"
        />

        <UiAlert
          variant="info"
          message="После создания permission можно назначать ролям. Изменение или удаление существующего permission текущим API не предусмотрено."
        />
      </div>

      <template #footer>
        <div class="admin-access-dialog-actions">
          <UiButton
            variant="ghost"
            label="Отмена"
            :disabled="permissionCreateOverlay.saving.value"
            @click="permissionCreateOverlay.requestClose"
          />

          <UiButton
            variant="primary"
            label="Создать permission"
            :loading="permissionCreateOverlay.saving.value"
            loading-text="Создание..."
            @click="createPermission"
          />
        </div>
      </template>
    </UiDialog>

    <UiUnsavedChangesConfirm
      v-model="permissionCreateOverlay.confirmCloseVisible.value"
      :busy="permissionCreateOverlay.saving.value"
      @discard="permissionCreateOverlay.discardAndClose"
      @continue="permissionCreateOverlay.continueEditing"
    />

    <UiDrawer
      :model-value="rolePermissionsOverlay.isOpen.value"
      :title="drawerRole ? `Права роли «${drawerRole.name}»` : 'Права роли'"
      width="46rem"
      @update:model-value="(value) => {
        if (!value) requestRolePermissionsClose()
      }"
    >
      <div class="admin-access-drawer">
        <UiAlert
          v-if="rolePermissionsError"
          variant="danger"
          :message="rolePermissionsError"
        />

        <UiAlert
          variant="info"
          message="Сохранение полностью заменяет набор permissions этой роли. Пользователи с ролью получат обновлённый набор прав."
        />

        <UiFilterBar
          v-model="permissionDrawerSearch"
          search-placeholder="Найти permission"
          :result-count="drawerPermissions.length"
          :show-reset="false"
        />

        <UiEmptyState
          v-if="!permissions.length"
          description="В системе пока нет permissions."
        />

        <UiEmptyState
          v-else-if="!drawerPermissions.length"
          description="Permissions по поиску не найдены."
        />

        <div
          v-else
          class="admin-access-permission-list"
        >
          <UiCheckbox
            v-for="permission in drawerPermissions"
            :key="permission.id"
            v-model="rolePermissionsOverlay.form.permissionIds"
            mode="multiple"
            :value="Number(permission.id)"
            :label="permission.name"
            :description="permission.description || 'Описание не задано'"
            :disabled="rolePermissionsOverlay.saving.value"
          />
        </div>
      </div>

      <template #footer>
        <div class="admin-access-dialog-actions">
          <UiButton
            variant="ghost"
            label="Закрыть"
            :disabled="rolePermissionsOverlay.saving.value"
            @click="requestRolePermissionsClose"
          />

          <UiButton
            variant="primary"
            label="Сохранить права"
            :loading="rolePermissionsOverlay.saving.value"
            loading-text="Сохранение..."
            :disabled="!rolePermissionsOverlay.dirty.value"
            @click="saveRolePermissions"
          />
        </div>
      </template>
    </UiDrawer>

    <UiUnsavedChangesConfirm
      v-model="rolePermissionsOverlay.confirmCloseVisible.value"
      :busy="rolePermissionsOverlay.saving.value"
      @discard="discardRolePermissionsAndClose"
      @continue="rolePermissionsOverlay.continueEditing"
    />
  </AdminPageShell>
</template>

<style scoped>
.admin-access-workspace,
.admin-access-form,
.admin-access-drawer {
  display: grid;
  gap: 16px;
}

.admin-access-role-grid,
.admin-access-permission-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.admin-access-role-card,
.admin-access-permission-card {
  min-width: 0;
  padding: 16px;

  display: grid;
  gap: 13px;

  color: var(--st-text);
  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 12px;
}

.admin-access-role-card__header,
.admin-access-permission-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.admin-access-role-card__header > div {
  min-width: 0;
}

.admin-access-role-card h3,
.admin-access-role-card p,
.admin-access-permission-card p {
  margin: 0;
}

.admin-access-role-card h3 {
  overflow-wrap: anywhere;
  font-size: 16px;
}

.admin-access-role-card p,
.admin-access-permission-card p,
.admin-access-muted {
  color: var(--st-text-secondary);
  font-size: 13px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.admin-access-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.admin-access-role-card__actions,
.admin-access-dialog-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

.admin-access-permission-card__header strong {
  min-width: 0;
  overflow-wrap: anywhere;
}

.admin-access-permission-list {
  display: grid;
  gap: 8px;
}

@media (max-width: 860px) {
  .admin-access-role-grid,
  .admin-access-permission-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .admin-access-role-card__header,
  .admin-access-permission-card__header {
    flex-direction: column;
  }

  .admin-access-role-card__actions,
  .admin-access-dialog-actions {
    align-items: stretch;
    flex-direction: column-reverse;
  }

  .admin-access-role-card__actions :deep(.st-ui-button),
  .admin-access-dialog-actions :deep(.st-ui-button) {
    width: 100%;
  }
}
</style>
