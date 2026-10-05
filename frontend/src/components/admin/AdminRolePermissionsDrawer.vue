<script setup>
import {
  UiAlert,
  UiButton,
  UiCheckbox,
  UiDrawer,
  UiEmptyState,
  UiFilterBar,
  UiUnsavedChangesConfirm,
} from '@/components/ui'

defineProps({
  modelValue: { type: Boolean, default: false },
  role: { type: Object, default: null },
  permissions: { type: Array, default: () => [] },
  filteredPermissions: { type: Array, default: () => [] },
  search: { type: String, default: '' },
  permissionIds: { type: Array, default: () => [] },
  saving: { type: Boolean, default: false },
  dirty: { type: Boolean, default: false },
  error: { type: String, default: '' },
  confirmCloseVisible: { type: Boolean, default: false },
})

const emit = defineEmits([
  'update:modelValue',
  'update:search',
  'update:permissionIds',
  'update:confirmCloseVisible',
  'request-close',
  'save',
  'discard',
  'continue',
])
</script>

<template>
  <UiDrawer
    :model-value="modelValue"
    :title="role ? `Права роли «${role.name}»` : 'Права роли'"
    width="46rem"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="admin-role-permissions-drawer">
      <UiAlert
        v-if="error"
        variant="danger"
        :message="error"
      />

      <UiAlert
        variant="info"
        message="Сохранение полностью заменяет набор permissions этой роли. Пользователи с ролью получат обновлённый набор прав."
      />

      <UiFilterBar
        :model-value="search"
        search-placeholder="Найти permission"
        :result-count="filteredPermissions.length"
        :show-reset="false"
        @update:model-value="emit('update:search', $event)"
      />

      <UiEmptyState
        v-if="!permissions.length"
        description="В системе пока нет permissions."
      />

      <UiEmptyState
        v-else-if="!filteredPermissions.length"
        description="Permissions по поиску не найдены."
      />

      <div
        v-else
        class="admin-role-permissions-drawer__list"
      >
        <UiCheckbox
          v-for="permission in filteredPermissions"
          :key="permission.id"
          :model-value="permissionIds"
          mode="multiple"
          :value="Number(permission.id)"
          :label="permission.name"
          :description="permission.description || 'Описание не задано'"
          :disabled="saving"
          @update:model-value="emit('update:permissionIds', $event)"
        />
      </div>
    </div>

    <template #footer>
      <div class="admin-role-permissions-drawer__actions">
        <UiButton
          variant="ghost"
          label="Закрыть"
          :disabled="saving"
          @click="emit('request-close')"
        />

        <UiButton
          variant="primary"
          label="Сохранить права"
          :loading="saving"
          loading-text="Сохранение..."
          :disabled="!dirty"
          @click="emit('save')"
        />
      </div>
    </template>
  </UiDrawer>

  <UiUnsavedChangesConfirm
    :model-value="confirmCloseVisible"
    :busy="saving"
    @update:model-value="emit('update:confirmCloseVisible', $event)"
    @discard="emit('discard')"
    @continue="emit('continue')"
  />
</template>

<style scoped>
.admin-role-permissions-drawer {
  display: grid;
  gap: 16px;
}

.admin-role-permissions-drawer__list {
  display: grid;
  gap: 8px;
}

.admin-role-permissions-drawer__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 560px) {
  .admin-role-permissions-drawer__actions {
    align-items: stretch;
    flex-direction: column-reverse;
  }

  .admin-role-permissions-drawer__actions :deep(.st-ui-button) {
    width: 100%;
  }
}
</style>
