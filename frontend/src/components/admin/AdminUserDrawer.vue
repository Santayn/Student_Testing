<script setup>
import AdminPersonEditor from '@/components/admin/AdminPersonEditor.vue'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiCheckbox,
  UiDrawer,
  UiEmptyState,
  UiSelect,
  UiTag,
} from '@/components/ui'

defineProps({
  modelValue: { type: Boolean, default: false },
  userForm: { type: Object, required: true },
  roles: { type: Array, default: () => [] },
  formError: { type: String, default: '' },
  userSaving: { type: Boolean, default: false },
  selectedUserName: { type: String, default: '—' },
  selectedPerson: { type: Object, default: null },
  selectedPersonName: { type: String, default: '—' },
  availablePersonOptions: { type: Array, default: () => [] },
  selectedRolePermissionNames: { type: Array, default: () => [] },
  selectedUserPermissionNames: { type: Array, default: () => [] },
  personCreatorVisible: { type: Boolean, default: false },
  personCreateError: { type: String, default: '' },
  personCreateMessage: { type: String, default: '' },
  creatingPerson: { type: Boolean, default: false },
  personEditorVisible: { type: Boolean, default: false },
  personEditDraft: { type: Object, required: true },
  personEditError: { type: String, default: '' },
  personEditMessage: { type: String, default: '' },
  updatingPerson: { type: Boolean, default: false },
  personEditorDirty: { type: Boolean, default: false },
})

const emit = defineEmits([
  'update:modelValue',
  'request-person-selection-change',
  'request-open-person-creator',
  'open-person-editor',
  'cancel-person-creator',
  'create-person',
  'request-cancel-person-editor',
  'update-person',
  'request-close',
  'save',
])
</script>

<template>
  <UiDrawer
    :model-value="modelValue"
    :title="`Пользователь ${userForm.login || ''}`"
    width="46rem"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="admin-user-drawer">
      <UiAlert
        v-if="formError"
        variant="danger"
        :message="formError"
      />

      <UiCard
        title="1. Учётная запись"
        description="Логин изменяется только через отдельный серверный сценарий и здесь доступен только для просмотра."
        compact
      >
        <dl class="admin-user-drawer__summary">
          <div>
            <dt>Логин</dt>
            <dd>{{ userForm.login || '—' }}</dd>
          </div>

          <div>
            <dt>Текущий профиль</dt>
            <dd>{{ selectedUserName }}</dd>
          </div>
        </dl>

        <UiCheckbox
          v-model="userForm.active"
          label="Учётная запись активна"
          description="Отключённый пользователь не должен получать обычный доступ к системе."
          :disabled="userSaving"
        />
      </UiCard>

      <UiCard
        title="2. Профиль пользователя"
        description="Один профиль Person может быть привязан только к одной учётной записи."
        compact
      >
        <UiSelect
          :model-value="userForm.personId"
          label="Профиль"
          :options="availablePersonOptions"
          placeholder="Без профиля"
          :filter="true"
          filter-placeholder="Поиск по ФИО или email"
          :clearable="true"
          :disabled="userSaving || creatingPerson || updatingPerson"
          @update:model-value="emit('request-person-selection-change', $event)"
        />

        <AdminPersonEditor
          :user-form="userForm"
          :selected-person="selectedPerson"
          :selected-person-name="selectedPersonName"
          :user-saving="userSaving"
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
          @request-open-person-creator="emit('request-open-person-creator')"
          @open-person-editor="emit('open-person-editor')"
          @cancel-person-creator="emit('cancel-person-creator')"
          @create-person="emit('create-person')"
          @request-cancel-person-editor="emit('request-cancel-person-editor')"
          @update-person="emit('update-person')"
        />
      </UiCard>

      <UiCard
        title="3. Доступ · Роли"
        description="Выберите одну или несколько ролей. Изменения отправятся только после нажатия «Сохранить»."
        compact
      >
        <div class="admin-user-role-picker">
          <UiCheckbox
            v-for="role in roles"
            :key="role.id"
            v-model="userForm.roleIds"
            mode="multiple"
            :value="Number(role.id)"
            :label="role.name"
            :description="role.description || 'Без описания'"
            :disabled="userSaving"
          />
        </div>

        <UiAlert
          v-if="!roles.length"
          variant="warning"
          message="В системе пока нет доступных ролей."
        />
      </UiCard>

      <UiCard
        title="Итоговые права · Только для просмотра"
        description="Права вычисляются из выбранных ролей. На этом экране они не редактируются напрямую."
        compact
      >
        <div
          v-if="selectedRolePermissionNames.length"
          class="admin-user-permissions"
        >
          <UiTag
            v-for="permission in selectedRolePermissionNames"
            :key="permission"
            :value="permission"
            variant="info"
          />
        </div>

        <UiEmptyState
          v-else
          description="Выбранные роли не содержат перечисленных прав."
          compact
        />

        <p
          v-if="selectedUserPermissionNames.length"
          class="admin-user-drawer__hint"
        >
          Текущие эффективные права пользователя могут дополнительно включать индивидуальные назначения, которые backend возвращает вместе с правами ролей.
        </p>
      </UiCard>
    </div>

    <template #footer>
      <div class="admin-user-drawer__footer">
        <UiButton
          variant="secondary"
          label="Отмена"
          :disabled="userSaving || updatingPerson || creatingPerson"
          @click="emit('request-close')"
        />

        <UiButton
          variant="primary"
          label="Сохранить изменения"
          :loading="userSaving"
          loading-text="Сохранение..."
          :disabled="userSaving || updatingPerson || creatingPerson || personEditorDirty || !roles.length"
          @click="emit('save')"
        />
      </div>
    </template>
  </UiDrawer>
</template>

<style scoped>
.admin-user-drawer {
  display: grid;
  gap: 14px;
}

.admin-user-drawer__summary {
  margin: 0;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 180px), 1fr));
  gap: 10px;
}

.admin-user-drawer__summary > div {
  min-width: 0;
  display: grid;
  gap: 3px;
}

.admin-user-drawer__summary dt {
  color: var(--st-text-secondary);
  font-size: 11px;
  font-weight: var(--st-font-weight-bold);
  line-height: 1.35;
}

.admin-user-drawer__summary dd {
  margin: 0;
  color: var(--st-text);
  font-size: 13px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.admin-user-drawer__hint {
  margin: 10px 0 0;
  color: var(--st-text-secondary);
  font-size: 12px;
  line-height: 1.55;
}

.admin-user-drawer__footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

.admin-user-role-picker {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 220px), 1fr));
  gap: 8px;
}

@media (max-width: 640px) {
  .admin-user-drawer__footer {
    align-items: stretch;
    flex-direction: column;
  }

  .admin-user-drawer__footer > * {
    width: 100%;
  }

}
</style>
