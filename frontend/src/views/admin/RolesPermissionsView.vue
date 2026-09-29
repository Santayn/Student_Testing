<script setup>
import {
  onMounted,
  ref,
} from 'vue'

import AdminNotice from '@/components/admin/AdminNotice.vue'
import AdminPageShell from '@/components/admin/AdminPageShell.vue'
import AdminRolePermissionsDrawer from '@/components/admin/AdminRolePermissionsDrawer.vue'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiDialog,
  UiEmptyState,
  UiLoadingState,
  UiFilterBar,
  UiInput,
  UiSelect,
  UiTag,
  UiTextarea,
  UiUnsavedChangesConfirm,
  useOverlayForm,
} from '@/components/ui'

import {
  useAdminRolesPermissionsData,
} from '@/composables/admin/roles-permissions/useAdminRolesPermissionsData'

import {
  useAdminRolePermissionsEditor,
} from '@/composables/admin/roles-permissions/useAdminRolePermissionsEditor'

import {
  useAdminRolesPermissionsMutations,
} from '@/composables/admin/roles-permissions/useAdminRolesPermissionsMutations'

const {
  ROLE_PERMISSION_FILTER_OPTIONS,
  ROLE_SORT_OPTIONS,
  PERMISSION_USAGE_OPTIONS,
  PERMISSION_SORT_OPTIONS,
  roles,
  permissions,
  loading,
  notice,
  roleSearch,
  rolePermissionFilter,
  roleSortMode,
  permissionSearch,
  permissionUsageFilter,
  permissionSortMode,
  filteredRoles,
  filteredPermissions,
  roleFiltersActive,
  permissionFiltersActive,
  showNotice,
  clearNotice,
  normalize,
  rolePermissions,
  rolePermissionIds,
  permissionUsageCount,
  resetRoleFilters,
  resetPermissionFilters,
  loadData: loadRolesPermissionsData,
} = useAdminRolesPermissionsData()

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

const {
  rolePermissionsError,
  permissionDrawerSearch,
  selectedRoleId,
  rolePermissionsOverlay,
  drawerRole,
  drawerPermissions,
  openRolePermissions,
  onSelectedRoleRefreshed,
  onSelectedRoleMissing,
  requestRolePermissionsClose,
  discardRolePermissionsAndClose,
} = useAdminRolePermissionsEditor({
  roles,
  permissions,
  rolePermissionIds,
})

function loadData(options = {}) {
  return loadRolesPermissionsData({
    ...options,
    selectedRoleId: selectedRoleId.value,
    onSelectedRoleRefreshed,
    onSelectedRoleMissing,
  })
}

const {
  roleFormError,
  permissionFormError,
  openCreateRole,
  openCreatePermission,
  createRole,
  createPermission,
  saveRolePermissions,
} = useAdminRolesPermissionsMutations({
  roles,
  permissions,
  normalize,
  showNotice,
  loadData,
  roleCreateOverlay,
  permissionCreateOverlay,
  rolePermissionsOverlay,
  selectedRoleId,
  rolePermissionIds,
  rolePermissionsError,
})

const permissionDirectoryOpen = ref(false)

onMounted(loadData)
</script>

<template>
  <AdminPageShell
    title="Роли и права"
    description="Управляйте ролями и наборами прав доступа."
  >
    <template #actions>
      <UiButton
        variant="secondary"
        icon="pi pi-key"
        label="Создать право"
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

    <div class="admin-access-directory-toggle">
      <UiButton
        variant="secondary"
        size="sm"
        icon="pi pi-key"
        :label="permissionDirectoryOpen ? 'Скрыть справочник прав' : 'Показать справочник прав'"
        @click="permissionDirectoryOpen = !permissionDirectoryOpen"
      />
    </div>

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

        <UiLoadingState
          v-if="loading"
          label="Загрузка ролей..."
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
                v-for="permission in rolePermissions(role).slice(0, 3)"
                :key="permission.id"
                :value="permission.name"
              />

              <UiTag
                v-if="rolePermissions(role).length > 3"
                :value="`+${rolePermissions(role).length - 3}`"
              />
            </div>

            <p
              v-else
              class="admin-access-muted"
            >
              Для роли пока не назначены права.
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
      v-if="permissionDirectoryOpen"
      title="Справочник прав"
      description="Доступные права платформы и количество ролей, в которых они используются."
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

        <UiLoadingState
          v-if="loading"
          label="Загрузка permissions..."
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
      title="Новое право"
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
          label="Название права"
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
          message="После создания право можно назначать ролям. Изменение или удаление существующего права текущим API не предусмотрено."
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
            label="Создать право"
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

    <AdminRolePermissionsDrawer
      :model-value="rolePermissionsOverlay.isOpen.value"
      :role="drawerRole"
      :permissions="permissions"
      :filtered-permissions="drawerPermissions"
      :search="permissionDrawerSearch"
      :permission-ids="rolePermissionsOverlay.form.permissionIds"
      :saving="rolePermissionsOverlay.saving.value"
      :dirty="rolePermissionsOverlay.dirty.value"
      :error="rolePermissionsError"
      :confirm-close-visible="rolePermissionsOverlay.confirmCloseVisible.value"
      @update:model-value="(value) => {
        if (!value) requestRolePermissionsClose()
      }"
      @update:search="permissionDrawerSearch = $event"
      @update:permission-ids="rolePermissionsOverlay.form.permissionIds = $event"
      @update:confirm-close-visible="rolePermissionsOverlay.confirmCloseVisible.value = $event"
      @request-close="requestRolePermissionsClose"
      @save="saveRolePermissions"
      @discard="discardRolePermissionsAndClose"
      @continue="rolePermissionsOverlay.continueEditing"
    />
  </AdminPageShell>
</template>

<style scoped>
.admin-access-workspace,
.admin-access-form {
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

.admin-access-directory-toggle {
  display: flex;
  justify-content: flex-end;
}

</style>
