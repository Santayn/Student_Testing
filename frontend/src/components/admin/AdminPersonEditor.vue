<script setup>
import {
  UiAlert,
  UiButton,
  UiInput,
} from '@/components/ui'

defineProps({
  userForm: { type: Object, required: true },
  selectedPerson: { type: Object, default: null },
  selectedPersonName: { type: String, default: '—' },
  userSaving: { type: Boolean, default: false },
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
  'request-open-person-creator',
  'open-person-editor',
  'cancel-person-creator',
  'create-person',
  'request-cancel-person-editor',
  'update-person',
])
</script>

<template>
  <div class="admin-person-editor">
    <div class="admin-user-person-actions">
      <UiButton
        v-if="!personCreatorVisible"
        type="button"
        variant="secondary"
        icon="pi pi-user-plus"
        label="Создать новый профиль"
        :disabled="userSaving || creatingPerson || updatingPerson"
        @click="emit('request-open-person-creator')"
      />

      <UiButton
        v-if="selectedPerson && !personCreatorVisible && !personEditorVisible"
        type="button"
        variant="secondary"
        icon="pi pi-user-edit"
        label="Изменить профиль"
        :disabled="userSaving || creatingPerson || updatingPerson"
        @click="emit('open-person-editor')"
      />
    </div>

    <UiAlert
      v-if="personCreateMessage"
      variant="success"
      :message="personCreateMessage"
    />

    <UiAlert
      v-if="personEditMessage"
      variant="success"
      :message="personEditMessage"
    />

    <div
      v-if="personCreatorVisible"
      class="admin-user-person-creator"
    >
      <div>
        <h3 class="admin-user-person-creator__title">
          Новый профиль
        </h3>

        <p class="admin-user-person-creator__description">
          Профиль создаётся сразу на сервере. Его привязка к текущей учётной записи произойдёт только после сохранения пользователя.
        </p>
      </div>

      <UiAlert
        v-if="personCreateError"
        variant="danger"
        :message="personCreateError"
      />

      <div class="admin-user-person-creator__grid">
        <UiInput
          v-model="userForm.newPerson.lastName"
          label="Фамилия"
          maxlength="100"
          required
          :disabled="creatingPerson || userSaving"
        />

        <UiInput
          v-model="userForm.newPerson.firstName"
          label="Имя"
          maxlength="100"
          required
          :disabled="creatingPerson || userSaving"
        />

        <UiInput
          v-model="userForm.newPerson.dateOfBirth"
          type="date"
          label="Дата рождения"
          min="1900-01-01"
          hint="Если дата неизвестна, будет сохранено 01.01.1900."
          :disabled="creatingPerson || userSaving"
        />

        <UiInput
          v-model="userForm.newPerson.email"
          type="email"
          label="Email"
          maxlength="255"
          required
          :disabled="creatingPerson || userSaving"
        />

        <UiInput
          v-model="userForm.newPerson.phone"
          type="tel"
          label="Телефон"
          maxlength="50"
          :disabled="creatingPerson || userSaving"
        />
      </div>

      <div class="admin-user-person-creator__actions">
        <UiButton
          type="button"
          variant="secondary"
          label="Отмена создания"
          :disabled="creatingPerson || userSaving"
          @click="emit('cancel-person-creator')"
        />

        <UiButton
          type="button"
          variant="primary"
          icon="pi pi-user-plus"
          label="Создать профиль"
          :loading="creatingPerson"
          loading-text="Создание..."
          :disabled="creatingPerson || userSaving"
          @click="emit('create-person')"
        />
      </div>
    </div>

    <div
      v-if="personEditorVisible && selectedPerson"
      class="admin-user-person-creator admin-user-person-editor"
    >
      <div>
        <h3 class="admin-user-person-creator__title">
          Редактирование профиля
        </h3>

        <p class="admin-user-person-creator__description">
          Изменения Person сохраняются отдельно и сразу становятся видны во всех разделах системы, где используется этот человек.
        </p>
      </div>

      <UiAlert
        v-if="personEditError"
        variant="danger"
        :message="personEditError"
      />

      <div class="admin-user-person-creator__grid">
        <UiInput
          v-model="personEditDraft.lastName"
          label="Фамилия"
          maxlength="100"
          required
          :disabled="updatingPerson || userSaving"
        />

        <UiInput
          v-model="personEditDraft.firstName"
          label="Имя"
          maxlength="100"
          required
          :disabled="updatingPerson || userSaving"
        />

        <UiInput
          v-model="personEditDraft.dateOfBirth"
          type="date"
          label="Дата рождения"
          min="1900-01-01"
          hint="Если очистить поле, backend оставит текущую дату без изменений."
          :disabled="updatingPerson || userSaving"
        />

        <UiInput
          v-model="personEditDraft.email"
          type="email"
          label="Email"
          maxlength="255"
          required
          :disabled="updatingPerson || userSaving"
        />

        <UiInput
          v-model="personEditDraft.phone"
          type="tel"
          label="Телефон"
          maxlength="50"
          :disabled="updatingPerson || userSaving"
        />
      </div>

      <div class="admin-user-person-creator__actions">
        <UiButton
          type="button"
          variant="secondary"
          label="Отмена редактирования"
          :disabled="updatingPerson || userSaving"
          @click="emit('request-cancel-person-editor')"
        />

        <UiButton
          type="button"
          variant="primary"
          icon="pi pi-save"
          label="Сохранить профиль"
          :loading="updatingPerson"
          loading-text="Сохранение..."
          :disabled="updatingPerson || userSaving || !personEditorDirty"
          @click="emit('update-person')"
        />
      </div>
    </div>

    <dl
      v-if="selectedPerson && !personEditorVisible"
      class="admin-user-drawer__summary"
    >
      <div>
        <dt>ФИО</dt>
        <dd>{{ selectedPersonName }}</dd>
      </div>

      <div>
        <dt>Email</dt>
        <dd>{{ selectedPerson.email || '—' }}</dd>
      </div>

      <div>
        <dt>Телефон</dt>
        <dd>{{ selectedPerson.phone || '—' }}</dd>
      </div>
    </dl>
  </div>
</template>

<style scoped>
.admin-person-editor {
  display: grid;
  gap: 12px;
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
  font-weight: 700;
  line-height: 1.35;
}

.admin-user-drawer__summary dd {
  margin: 0;
  color: var(--st-text);
  font-size: 13px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.admin-user-person-actions,
.admin-user-person-creator__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

.admin-user-person-creator {
  padding: 13px;
  display: grid;
  gap: 12px;
  background: var(--st-surface);
  border: 1px solid var(--st-border);
  border-radius: 10px;
}

.admin-user-person-creator__title {
  margin: 0;
  color: var(--st-text);
  font-size: 15px;
  line-height: 1.4;
}

.admin-user-person-creator__description {
  margin: 4px 0 0;
  color: var(--st-text-secondary);
  font-size: 12px;
  line-height: 1.55;
}

.admin-user-person-creator__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

@media (max-width: 640px) {
  .admin-user-person-actions > *,
  .admin-user-person-creator__actions > * {
    width: 100%;
  }

  .admin-user-person-creator__grid {
    grid-template-columns: 1fr;
  }
}
</style>
