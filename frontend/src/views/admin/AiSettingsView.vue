<script setup>
import { onMounted } from 'vue'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiEmptyState,
  UiSelect,
  UiTag,
} from '@/components/ui'

import AdminPageShell from '@/components/admin/AdminPageShell.vue'

import { useAiEngineSettings } from '@/composables/admin/useAiEngineSettings'

const {
  features,
  loading,
  savingFeature,
  error,
  load,
  setEngine,
} = useAiEngineSettings()

function engineOptions(feature) {
  return [
    { value: 'api', label: 'Внешний API', disabled: !feature.apiAvailable && feature.engine !== 'api' },
    { value: 'local', label: 'Собственная модель', disabled: !feature.localConfigured && feature.engine !== 'local' },
  ]
}

function apiState(feature) {
  return feature.apiAvailable
    ? { label: 'API настроен', variant: 'success' }
    : { label: 'API не настроен', variant: 'secondary' }
}

function localState(feature) {
  if (feature.localReady) {
    return { label: feature.localModel ? `Модель ${feature.localModel} готова` : 'Модель готова', variant: 'success' }
  }
  if (feature.localConfigured) {
    return { label: 'Модель не загружена', variant: 'warning' }
  }
  return { label: 'Модель не подключена', variant: 'secondary' }
}

function percent(value) {
  const number = Number(value)
  return Number.isFinite(number) ? `${Math.round(number * 100)}%` : '—'
}

function metricsLine(feature) {
  const metrics = feature.localMetrics
  if (!metrics) return ''
  if (metrics.score_within_0_25 !== undefined) {
    return `Тест модели: балл совпал с разметкой ±25% — ${percent(metrics.score_within_0_25)}, `
      + `критерии верно — ${percent(metrics.criterion_accuracy)}`
  }
  if (metrics.line_accuracy !== undefined) {
    return `Тест модели: строки размечены верно — ${percent(metrics.line_accuracy)}`
  }
  return ''
}

onMounted(load)
</script>

<template>
  <AdminPageShell title="Настройки ИИ">
    <template #actions>
      <UiButton
        size="sm"
        icon="pi pi-refresh"
        label="Обновить"
        :loading="loading"
        loading-text="Обновление..."
        @click="load"
      />
    </template>

    <UiAlert
      v-if="error"
      variant="danger"
      :message="error"
      closable
      @close="error = ''"
    />

    <UiEmptyState
      v-if="!loading && !features.length && !error"
      description="Нет данных."
      compact
    />

    <UiCard
      v-for="feature in features"
      :key="feature.feature"
      :title="feature.title"
    >
      <div class="ai-settings__row">
        <UiSelect
          :model-value="feature.engine"
          label="Движок"
          :options="engineOptions(feature)"
          :disabled="Boolean(savingFeature)"
          class="ai-settings__select"
          @update:model-value="(engine) => engine !== feature.engine && setEngine(feature.feature, engine)"
        />
        <div class="ai-settings__states">
          <UiTag :variant="apiState(feature).variant" :value="apiState(feature).label" />
          <UiTag :variant="localState(feature).variant" :value="localState(feature).label" />
        </div>
      </div>
      <p
        v-if="metricsLine(feature)"
        class="ai-settings__metrics"
      >
        {{ metricsLine(feature) }}
      </p>
    </UiCard>
  </AdminPageShell>
</template>

<style scoped>
.ai-settings__row {
  display: flex;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 16px;
}

.ai-settings__select {
  width: min(100%, 260px);
}

.ai-settings__states {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-bottom: 6px;
}

.ai-settings__metrics {
  margin: 12px 0 0;
  color: var(--st-text-muted);
  font-size: 13px;
}
</style>
