<script setup>
import {
  UiAlert,
  UiButton,
  UiCheckbox,
  UiDrawer,
  UiInput,
} from '@/components/ui'

defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '' },
  form: { type: Object, required: true },
  saving: { type: Boolean, default: false },
  formError: { type: String, default: '' },
  subjectLabel: { type: String, default: '' },
})

const emit = defineEmits([
  'update:open',
  'close',
  'save',
])
</script>

<template>
  <UiDrawer
    :model-value="open"
    :title="title"
    width="34rem"
    @update:model-value="emit('update:open', $event)"
  >
    <div class="teacher-stack">
      <div class="teacher-overlay-context">
        <span class="teacher-muted">Предмет</span>
        <strong>{{ subjectLabel }}</strong>
      </div>

      <UiAlert
        v-if="formError"
        variant="danger"
        :message="formError"
      />

      <UiInput
        v-model="form.name"
        label="Название шаблона"
        placeholder="Например: Базовый поток"
        maxlength="200"
        required
      />

      <UiCheckbox
        v-model="form.publicVisible"
        label="Публиковать шаблон"
      />

      <p class="teacher-muted">
        Шаблон остаётся привязан к выбранному предмету. Версии создаются отдельно после сохранения шаблона.
      </p>
    </div>

    <template #footer>
      <div class="teacher-drawer-footer">
        <UiButton
          variant="secondary"
          :disabled="saving"
          @click="emit('close')"
        >
          Отмена
        </UiButton>

        <UiButton
          variant="primary"
          :loading="saving"
          loading-text="Сохранение..."
          @click="emit('save')"
        >
          Сохранить
        </UiButton>
      </div>
    </template>
  </UiDrawer>
</template>

<style scoped>
.teacher-overlay-context {
  padding: 10px 12px;

  display: grid;
  gap: 4px;

  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 9px;
}

.teacher-overlay-context strong {
  overflow-wrap: anywhere;
}

.teacher-drawer-footer {
  width: 100%;

  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 720px) {
  .teacher-drawer-footer,
  .teacher-drawer-footer > * {
    width: 100%;
  }
}
</style>
