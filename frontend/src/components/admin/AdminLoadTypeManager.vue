<script setup>
import {
  computed,
} from 'vue'

import {
  UiAlert,
  UiButton,
  UiDialog,
  UiEmptyState,
  UiInput,
  UiTextarea,
  UiUnsavedChangesConfirm,
} from '@/components/ui'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  closeConfirmVisible: {
    type: Boolean,
    default: false,
  },
  title: {
    type: String,
    default: 'Типы нагрузки',
  },
  editorTitle: {
    type: String,
    default: '',
  },
  form: {
    type: Object,
    required: true,
  },
  error: {
    type: String,
    default: '',
  },
  searchQuery: {
    type: String,
    default: '',
  },
  filteredLoadTypes: {
    type: Array,
    default: () => [],
  },
  saving: {
    type: Boolean,
    default: false,
  },
  isCreate: {
    type: Boolean,
    default: true,
  },
  dirty: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits([
  'update:modelValue',
  'update:closeConfirmVisible',
  'update:searchQuery',
  'start-new',
  'edit',
  'cancel',
  'save',
  'continue-editing',
  'discard',
])

const dialogModel = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const closeConfirmModel = computed({
  get: () => props.closeConfirmVisible,
  set: (value) => emit('update:closeConfirmVisible', value),
})

const searchModel = computed({
  get: () => props.searchQuery,
  set: (value) => emit('update:searchQuery', value),
})
</script>

<template>
  <UiDialog
    v-model="dialogModel"
    :title="title"
    width="52rem"
  >
    <div class="load-type-manager">
      <UiAlert
        v-if="error"
        variant="danger"
        :message="error"
      />

      <div class="load-type-manager__toolbar">
        <UiInput
          v-model="searchModel"
          label="Поиск"
          placeholder="Название или описание"
        />

        <UiButton
          variant="secondary"
          icon="pi pi-plus"
          label="Новый тип"
          :disabled="saving"
          @click="emit('start-new')"
        />
      </div>

      <div class="load-type-manager__layout">
        <section class="load-type-manager__list">
          <UiEmptyState
            v-if="!filteredLoadTypes.length"
            description="Типы нагрузки не найдены."
            compact
          />

          <template v-else>
            <button
              v-for="loadType in filteredLoadTypes"
              :key="loadType.id"
              type="button"
              class="load-type-item"
              :class="{
                'load-type-item--active':
                  Number(form.id) === Number(loadType.id),
              }"
              :disabled="saving"
              @click="emit('edit', loadType)"
            >
              <strong>{{ loadType.name }}</strong>
              <span>{{ loadType.description || 'Без описания' }}</span>
            </button>
          </template>
        </section>

        <section class="load-type-manager__editor">
          <h3>{{ editorTitle }}</h3>

          <UiInput
            v-model="form.name"
            label="Название"
            maxlength="100"
            required
            :disabled="saving"
          />

          <UiTextarea
            v-model="form.description"
            label="Описание"
            maxlength="1000"
            placeholder="Необязательное описание"
            :disabled="saving"
          />

          <p class="load-type-manager__hint">
            Тип нагрузки нельзя удалить через текущий backend API, но его название и описание можно изменить.
          </p>

          <div class="admin-actions admin-actions--end admin-actions--mobile-stack">
            <UiButton
              v-if="dirty"
              variant="secondary"
              label="Отменить изменения"
              :disabled="saving"
              @click="emit('cancel')"
            />

            <UiButton
              variant="primary"
              :label="isCreate ? 'Создать тип' : 'Сохранить тип'"
              :loading="saving"
              loading-text="Сохранение..."
              :disabled="saving"
              @click="emit('save')"
            />
          </div>
        </section>
      </div>
    </div>
  </UiDialog>

  <UiUnsavedChangesConfirm
    v-model="closeConfirmModel"
    :busy="saving"
    @continue="emit('continue-editing')"
    @discard="emit('discard')"
  />
</template>

<style scoped>
.load-type-manager {
  display: grid;
  gap: 16px;
}

.load-type-manager__toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
  gap: 10px;
}

.load-type-manager__layout {
  display: grid;
  grid-template-columns: minmax(220px, 0.8fr) minmax(0, 1.2fr);
  gap: 16px;
}

.load-type-manager__list,
.load-type-manager__editor {
  min-width: 0;

  display: grid;
  align-content: start;
  gap: 9px;
}

.load-type-manager__list {
  max-height: 430px;
  overflow-y: auto;
}

.load-type-manager__editor {
  padding: 14px;

  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 10px;
}

.load-type-manager__editor h3 {
  margin: 0 0 2px;

  color: var(--st-text);

  font-size: 16px;
}

.load-type-item {
  width: 100%;
  min-height: 56px;
  padding: 10px 12px;

  color: var(--st-text);
  background: var(--st-surface);
  border: 1px solid var(--st-border);
  border-radius: 9px;

  display: grid;
  gap: 4px;

  text-align: left;
  cursor: pointer;
}

.load-type-item:hover,
.load-type-item--active {
  border-color: var(--st-primary);
}

.load-type-item strong {
  font-size: 13px;
}

.load-type-item span,
.load-type-manager__hint {
  color: var(--st-text-secondary);

  font-size: 12px;
  line-height: 1.45;
}

.load-type-manager__hint {
  margin: 0;
}

@media (max-width: 900px) {
  .load-type-manager__layout {
    grid-template-columns: 1fr;
  }

  .load-type-manager__list {
    max-height: 240px;
  }
}

@media (max-width: 640px) {
  .load-type-manager__toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
