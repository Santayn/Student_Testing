<script setup>
import { UiCard, UiFilterBar, UiButton, UiSelect, UiSearchInput } from '@/components/ui'
import QuestionsList from './QuestionsList.vue'
defineProps({ compact: { type: Boolean, default: false } })
import { toRefs } from 'vue'
import { useQuestionsWorkspaceContext } from '@/composables/questions/questionsWorkspaceContext'
const {
  questionStats,
  searchQuery,
  filterResultText,
  hasActiveFilters,
  resetFilters,
  typeFilter,
  questionTypeFilterOptions,
  statusFilter,
  statusOptions,
  sortMode,
  sortOptions,
  topicOptions,
  canCreateQuestion,
  openImportDialog,
  openCreateQuestion,
} = toRefs(useQuestionsWorkspaceContext())
</script>
<template>
<UiCard title="Банк вопросов" :description="questionStats">
  <div class="teacher-stack">
<template v-if="compact">
<div class="questions-compact-controls">
  <div class="questions-compact-actions">
    <UiButton size="sm" icon="pi pi-file-import" label="Импорт Word"
      :disabled="!topicOptions.length" @click="openImportDialog" />
    <UiButton size="sm" variant="primary" icon="pi pi-plus" label="Добавить вопрос"
      :disabled="!canCreateQuestion" @click="openCreateQuestion" />
  </div>
  <UiSearchInput v-model="searchQuery" label="Поиск вопросов"
    placeholder="Текст, ответ или номер вопроса" />
  <details class="questions-compact-more">
    <summary>Тип, статус и сортировка</summary>
    <div class="questions-compact-fields">
      <UiSelect v-model="typeFilter" label="Тип" :options="questionTypeFilterOptions" size="sm" />
      <UiSelect v-model="statusFilter" label="Статус" :options="statusOptions" size="sm" />
      <UiSelect v-model="sortMode" label="Сортировка" :options="sortOptions" size="sm" />
      <UiButton variant="ghost" size="sm" label="Сбросить фильтры"
        :disabled="!hasActiveFilters" @click="resetFilters" />
    </div>
  </details>
  <p class="questions-compact-result" role="status">{{ filterResultText }}</p>
</div>
</template>
<template v-else>
<UiFilterBar
          v-model="searchQuery"
          search-placeholder="Текст, ответ или номер вопроса"
          :result-text="filterResultText"
          :reset-disabled="!hasActiveFilters"
          @reset="resetFilters"
        >
          <template #filters>
            <UiSelect
              v-model="typeFilter"
              label="Тип"
              :options="questionTypeFilterOptions"
              size="sm"
            />

            <UiSelect
              v-model="statusFilter"
              label="Статус"
              :options="statusOptions"
              size="sm"
            />

            <UiSelect
              v-model="sortMode"
              label="Сортировка"
              :options="sortOptions"
              size="sm"
            />
          </template>

          <template #actions>
            <UiButton
              size="sm"
              icon="pi pi-file-import"
              label="Импорт Word"
              :disabled="!topicOptions.length"
              @click="openImportDialog"
            />

            <UiButton
              variant="primary"
              size="sm"
              icon="pi pi-plus"
              label="Добавить вопрос"
              :disabled="!canCreateQuestion"
              @click="openCreateQuestion"
            />
          </template>
        </UiFilterBar>
</template>
<QuestionsList />
  </div>
</UiCard>
</template>
<style scoped>

.questions-compact-controls { display: grid; min-width: 0; gap: 12px; }
.questions-compact-actions { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 8px; }
.questions-compact-more { border: 1px solid var(--st-border); border-radius: 10px; padding: 10px 12px; }
.questions-compact-more summary { cursor: pointer; color: var(--st-text); font-weight: var(--st-font-weight-bold); }
.questions-compact-fields { padding-top: 12px; display: grid; gap: 10px; }
.questions-compact-result { margin: 0; font-size: 13px; color: var(--st-text-secondary); }
@media (max-width: 400px) { .questions-compact-actions { grid-template-columns: minmax(0, 1fr); } }

</style>
