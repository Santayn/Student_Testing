<script setup>
import {
  UiAlert,
  UiButton,
  UiCheckbox,
  UiDrawer,
  UiInput,
  UiTextarea,
} from '@/components/ui'

defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '' },
  form: { type: Object, required: true },
  isCreate: { type: Boolean, default: false },
  saving: { type: Boolean, default: false },
  formError: { type: String, default: '' },
  templateLabel: { type: String, default: '' },
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
    width="42rem"
    @update:model-value="emit('update:open', $event)"
  >
    <div class="teacher-stack">
      <div class="teacher-overlay-context">
        <span class="teacher-muted">Шаблон</span>
        <strong>{{ templateLabel }}</strong>
      </div>

      <UiAlert
        v-if="formError"
        variant="danger"
        :message="formError"
      />

      <div class="teacher-grid">
        <UiInput
          v-model="form.versionNumber"
          label="Номер версии"
          type="number"
          min="1"
          step="1"
          required
        />

        <UiInput
          v-model="form.title"
          label="Название версии"
          maxlength="200"
          required
        />
      </div>

      <UiTextarea
        v-model="form.description"
        label="Описание"
        maxlength="2000"
        placeholder="Что входит в эту версию курса"
      />

      <UiTextarea
        v-model="form.changeNotes"
        label="Что изменилось"
        maxlength="2000"
        placeholder="Кратко опишите изменения относительно предыдущей версии"
      />

      <UiCheckbox
        v-if="isCreate"
        v-model="form.published"
        label="Опубликовать сразу после создания"
      />

      <UiAlert
        v-else
        variant="info"
        message="Статус публикации существующей версии изменяется отдельной кнопкой в карточке версии."
      />
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
          Сохранить версию
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
