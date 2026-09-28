<script setup>
import {
  UiButton,
  UiEmptyState,
  UiFileInput,
} from '@/components/ui'

defineProps({
  lectureId: { type: [Number, String], default: null },
  materials: { type: Array, default: () => [] },
  pendingFiles: { type: Array, default: () => [] },
  fileInputKey: { type: Number, default: 0 },
  loadingMaterials: { type: Boolean, default: false },
  deletingMaterialId: { type: [Number, String], default: null },
  saving: { type: Boolean, default: false },
})

const emit = defineEmits([
  'files-change',
  'remove-pending-file',
  'download-material',
  'request-delete-material',
])
</script>

<template>
  <section class="teacher-lecture-form-section">
    <div class="teacher-lecture-form-section__heading">
      <span class="teacher-muted">Материалы</span>
      <strong>
        {{ lectureId ? `${materials.length} загружено` : 'Будут загружены после создания' }}
      </strong>
    </div>

    <UiFileInput
      :key="fileInputKey"
      label="Добавить файлы"
      hint="Можно выбрать несколько файлов. Они загрузятся вместе с сохранением лекции."
      multiple
      :disabled="saving"
      @files-change="emit('files-change', $event)"
    />

    <div
      v-if="pendingFiles.length"
      class="teacher-file-list"
    >
      <div
        v-for="(file, index) in pendingFiles"
        :key="`${file.name}-${index}`"
        class="teacher-file-item"
      >
        <span>{{ file.name }}</span>

        <UiButton
          size="sm"
          variant="secondary"
          label="Убрать"
          :disabled="saving"
          @click="emit('remove-pending-file', index)"
        />
      </div>
    </div>

    <template v-if="lectureId">
      <div class="teacher-divider" />

      <UiEmptyState
        v-if="loadingMaterials"
        description="Загрузка материалов..."
        compact
      />

      <UiEmptyState
        v-else-if="!materials.length"
        description="Загруженных материалов пока нет."
        compact
      />

      <div
        v-else
        class="teacher-file-list"
      >
        <div
          v-for="material in materials"
          :key="material.id"
          class="teacher-file-item"
        >
          <span>{{ material.fileName || `Материал #${material.id}` }}</span>

          <div class="teacher-inline-actions">
            <UiButton
              size="sm"
              variant="secondary"
              icon="pi pi-download"
              label="Скачать"
              @click="emit('download-material', material)"
            />

            <UiButton
              size="sm"
              variant="danger"
              icon="pi pi-trash"
              label="Удалить"
              :loading="deletingMaterialId === material.id"
              loading-text="Удаление..."
              @click="emit('request-delete-material', material)"
            />
          </div>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
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

@media (max-width: 640px) {
  .teacher-lecture-form-section {
    padding: 12px;
  }
}
</style>
