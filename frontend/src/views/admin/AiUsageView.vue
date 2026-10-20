<script setup>
import {
  computed,
  onMounted,
} from 'vue'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiInput,
  UiStat,
  UiStatGrid,
} from '@/components/ui'

import AdminPageShell from '@/components/admin/AdminPageShell.vue'

import { useAiUsageReport } from '@/composables/admin/useAiUsageReport'

const {
  from,
  to,
  report,
  loading,
  error,
  load,
} = useAiUsageReport()

const numberFormat = new Intl.NumberFormat('ru-RU')

function formatNumber(value) {
  return numberFormat.format(Number(value) || 0)
}

const usageFields = [
  { key: 'tokens', label: 'Токены' },
  { key: 'jobs', label: 'Задач' },
  { key: 'ocrPages', label: 'Страниц OCR' },
  { key: 'failed', label: 'С ошибкой' },
]

const sections = computed(() => [
  { key: 'type', title: 'По задачам', nameLabel: 'Задача', rows: report.value?.byType ?? [], rowKey: (row) => row.type, name: (row) => row.title, empty: 'За период ИИ не использовался.' },
  { key: 'user', title: 'По преподавателям', nameLabel: 'Преподаватель', rows: report.value?.byUser ?? [], rowKey: (row) => `u${row.personId}`, name: (row) => row.name, empty: 'Нет данных.' },
  { key: 'group', title: 'По группам', nameLabel: 'Группа', rows: report.value?.byGroup ?? [], rowKey: (row) => `g${row.groupId}`, name: (row) => row.name, empty: 'Нет проверенных ответов.' },
])

const limitText = computed(() => {
  const limit = Number(report.value?.monthlyTokenLimit) || 0
  if (!limit) {
    return 'без лимита'
  }
  const percent = Math.round((Number(report.value?.monthTokens) || 0) * 100 / limit)
  return `${formatNumber(report.value?.monthTokens)} из ${formatNumber(limit)} (${percent}%)`
})

onMounted(load)
</script>

<template>
  <AdminPageShell
    title="Расход ИИ"
  >
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

    <UiCard title="Период">
      <form
        class="ai-usage__period"
        @submit.prevent="load"
      >
        <UiInput
          v-model="from"
          label="С"
          type="date"
        />
        <UiInput
          v-model="to"
          label="По"
          type="date"
        />
        <UiButton
          type="submit"
          variant="primary"
          label="Показать"
          :loading="loading"
        />
      </form>
    </UiCard>

    <UiAlert
      v-if="error"
      variant="danger"
      :message="error"
    />

    <template v-if="report">
      <UiStatGrid>
        <UiStat label="Токены за период" :value="formatNumber(report.total.tokens)" />
        <UiStat label="Задач" :value="formatNumber(report.total.jobs)" />
        <UiStat label="Страниц OCR" :value="formatNumber(report.total.ocrPages)" />
        <UiStat label="Лимит месяца" :value="limitText" />
      </UiStatGrid>

      <UiCard
        v-for="section in sections"
        :key="section.key"
        :title="section.title"
      >
        <p
          v-if="!section.rows.length"
          class="ai-usage__empty"
        >
          {{ section.empty }}
        </p>
        <div
          v-else
          class="ai-usage__scroll"
          role="region"
          :aria-label="section.title"
          tabindex="0"
        >
          <table class="ai-usage__table">
            <thead>
              <tr>
                <th scope="col">{{ section.nameLabel }}</th>
                <th
                  v-for="field in usageFields"
                  :key="field.key"
                  scope="col"
                  class="ai-usage__number"
                >
                  {{ field.label }}
                </th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="row in section.rows"
                :key="section.rowKey(row)"
              >
                <th scope="row">{{ section.name(row) }}</th>
                <td
                  v-for="field in usageFields"
                  :key="field.key"
                  class="ai-usage__number"
                >
                  {{ formatNumber(row.usage?.[field.key]) }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </UiCard>
    </template>
  </AdminPageShell>
</template>

<style scoped>
.ai-usage__period {
  display: flex;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 12px;
}

.ai-usage__period > :not(:last-child) {
  width: min(100%, 200px);
}

.ai-usage__empty {
  margin: 0;
  color: var(--st-text-muted);
}

.ai-usage__scroll {
  max-width: 100%;
  overflow-x: auto;
}

.ai-usage__table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.ai-usage__table th,
.ai-usage__table td {
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid var(--st-border);
  white-space: nowrap;
}

.ai-usage__table thead th {
  color: var(--st-text-muted);
  font-weight: 600;
  font-size: 13px;
}

.ai-usage__table tbody th {
  font-weight: 500;
  white-space: normal;
  min-width: 160px;
}

.ai-usage__table .ai-usage__number {
  text-align: right;
  font-variant-numeric: tabular-nums;
}
</style>
