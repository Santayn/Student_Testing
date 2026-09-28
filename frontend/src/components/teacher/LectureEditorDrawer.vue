<script setup>
import {
  UiAlert,
  UiButton,
  UiCheckbox,
  UiDrawer,
  UiEmptyState,
  UiInput,
  UiTextarea,
} from '@/components/ui'

import LectureMaterialsManager from '@/components/teacher/LectureMaterialsManager.vue'

// The parent owns the reactive form, dirty-state, save flow and API requests.
// This component edits its existing form object and emits user intent only.
defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '' },
  form: { type: Object, required: true },
  isCreate: { type: Boolean, default: false },
  saving: { type: Boolean, default: false },
  formError: { type: String, default: '' },
  availableTests: { type: Array, default: () => [] },
  materials: { type: Array, default: () => [] },
  pendingFiles: { type: Array, default: () => [] },
  fileInputKey: { type: Number, default: 0 },
  loadingMaterials: { type: Boolean, default: false },
  deletingMaterialId: { type: [Number, String], default: null },
})

const emit = defineEmits([
  'update:open',
  'dismiss-error',
  'files-change',
  'remove-pending-file',
  'download-material',
  'request-delete-material',
  'close',
  'save',
])
</script>

<template>
  <UiDrawer
      :model-value="open"
      :title="title"
      width="46rem"
      @update:model-value="emit('update:open', $event)"
    >
      <div class="teacher-stack teacher-lecture-drawer">
        <UiAlert
          v-if="formError"
          variant="danger"
          :message="formError"
          closable
          @close="emit('dismiss-error')"
        />

        <section class="teacher-lecture-form-section">
          <div class="teacher-lecture-form-section__heading">
            <span class="teacher-muted">Основные данные</span>
            <strong>
              {{ isCreate ? 'Новая лекция' : `Лекция #${form.id}` }}
            </strong>
          </div>

          <UiInput
            v-model="form.title"
            label="Название"
            maxlength="200"
            required
          />

          <UiTextarea
            v-model="form.description"
            label="Описание"
            maxlength="2000"
          />

          <UiCheckbox
            v-model="form.publicVisible"
            label="Публиковать лекцию для студентов"
          />
        </section>

        <section class="teacher-lecture-form-section">
          <div class="teacher-lecture-form-section__heading">
            <span class="teacher-muted">Связанные тесты</span>
            <strong>{{ form.testIds.length }} выбрано</strong>
          </div>

          <UiEmptyState
            v-if="!availableTests.length"
            description="Для предмета пока нет доступных тестов."
            compact
          />

          <div
            v-else
            class="teacher-selection-grid"
          >
            <UiCheckbox
              v-for="test in availableTests"
              :key="test.id"
              v-model="form.testIds"
              mode="multiple"
              :value="test.id"
              :label="test.title || `Тест #${test.id}`"
              :description="test.description || ''"
              :disabled="saving"
            />
          </div>
        </section>

        <LectureMaterialsManager
          :lecture-id="form.id"
          :materials="materials"
          :pending-files="pendingFiles"
          :file-input-key="fileInputKey"
          :loading-materials="loadingMaterials"
          :deleting-material-id="deletingMaterialId"
          :saving="saving"
          @files-change="emit('files-change', $event)"
          @remove-pending-file="emit('remove-pending-file', $event)"
          @download-material="emit('download-material', $event)"
          @request-delete-material="emit('request-delete-material', $event)"
        />
      </div>

      <template #footer>
        <div class="teacher-lecture-drawer__footer">
          <UiButton
            variant="secondary"
            label="Закрыть"
            :disabled="saving"
            @click="emit('close')"
          />

          <UiButton
            variant="primary"
            :loading="saving"
            loading-text="Сохранение..."
            :label="form.id ? 'Сохранить лекцию' : 'Создать лекцию'"
            @click="emit('save')"
          />
        </div>
      </template>
  </UiDrawer>
</template>

<style scoped>
.teacher-lecture-drawer {
  min-width: 0;
  padding-bottom: 4px;
}

.teacher-lecture-form-section {
  min-width: 0;
  padding: 14px;
  display: grid;
  gap: 14px;
  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 12px;
}

.teacher-lecture-form-section__heading {
  min-width: 0;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.teacher-lecture-form-section__heading strong {
  min-width: 0;
  color: var(--st-text);
  overflow-wrap: anywhere;
}

.teacher-lecture-drawer__footer {
  width: 100%;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 640px) {
  .teacher-lecture-form-section {
    padding: 12px;
  }

  .teacher-lecture-drawer__footer,
  .teacher-lecture-drawer__footer > * {
    width: 100%;
  }

  .teacher-lecture-drawer__footer {
    flex-direction: column-reverse;
  }
}
</style>
