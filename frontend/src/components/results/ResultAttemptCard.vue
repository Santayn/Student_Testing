<script setup>
import { computed } from 'vue'

import {
  UiEmptyState,
  UiTable,
} from '@/components/ui'

const props = defineProps({
  attempt: {
    type: Object,
    required: true,
  },

  mode: {
    type: String,
    default: 'student',
  },

  open: {
    type: Boolean,
    default: false,
  },

  best: {
    type: Boolean,
    default: false,
  },
})

const columns = [
  {
    key: 'displayIndex',
    label: '#',
    sortable: false,
  },
  {
    key: 'questionText',
    label: 'Вопрос',
  },
  {
    key: 'givenAnswer',
    label: 'Ответ студента',
  },
  {
    key: 'correctAnswer',
    label: 'Правильный ответ',
  },
  {
    key: 'status',
    label: 'Результат',
    value: (row) =>
      row.correct
        ? 'Верно'
        : 'Неверно',
    sortValue: (row) =>
      row.correct
        ? 1
        : 0,
  },
]

const stats = computed(() => {
  return (
    props.attempt.stats ?? {
      total: 0,
      right: 0,
      percent: 0,
    }
  )
})

const testName = computed(() => {
  return (
    props.attempt.testName ||
    `Тест #${
      props.attempt.testId ??
      '?'
    }`
  )
})

const metaText = computed(() => {
  const parts = []

  if (props.mode === 'teacher') {
    parts.push(
      props.attempt.studentName ||
      `Студент #${
        props.attempt.studentId ??
        '?'
      }`
    )
  } else {
    parts.push('Ваша попытка')
  }

  if (
    props.attempt.attemptOrdinal
  ) {
    parts.push(
      `Попытка ${
        props.attempt
          .attemptOrdinal
      }`
    )
  }

  if (
    props.attempt.completedAt
  ) {
    parts.push(
      formatDateTime(
        props.attempt
          .completedAt
      )
    )
  }

  return parts.join(' · ')
})

const rows = computed(() => {
  const source =
    Array.isArray(
      props.attempt.results
    )
      ? props.attempt.results
      : []

  return source.map(
    (row, index) => ({
      ...row,
      displayIndex:
        index + 1,
    })
  )
})

function formatDateTime(value) {
  try {
    return new Intl.DateTimeFormat(
      'ru-RU',
      {
        dateStyle: 'short',
        timeStyle: 'short',
      }
    ).format(
      new Date(value)
    )
  } catch {
    return value || ''
  }
}
</script>

<template>
  <details
    class="result-attempt"
    :open="open"
  >
    <summary class="result-attempt__summary">
      <div class="result-attempt__title">
        <strong>
          {{ testName }}
        </strong>

        <small>
          {{ metaText }}
        </small>
      </div>

      <div class="result-attempt__stats">
        <span
          v-if="best"
          class="result-attempt__badge result-attempt__badge--success"
        >
          Лучшая попытка
        </span>

        <span
          class="result-attempt__badge"
          :class="{
            'result-attempt__badge--success':
              Number(stats.right) > 0 &&
              Number(stats.right) ===
                Number(stats.total),
          }"
        >
          {{
            stats.right ?? 0
          }}
          из
          {{
            stats.total ?? 0
          }}
        </span>

        <span class="result-attempt__badge">
          {{
            stats.percent ?? 0
          }}%
        </span>
      </div>
    </summary>

    <div class="result-attempt__body">
      <UiEmptyState
        v-if="!rows.length"
        description="Для этой попытки нет доступных ответов."
        compact
      />

      <UiTable
        v-else
        :columns="columns"
        :rows="rows"
        empty-message="Для этой попытки нет доступных ответов."
      >
        <template #cell-questionText="{ row }">
          {{
            row.questionText ||
            '—'
          }}
        </template>

        <template #cell-givenAnswer="{ row }">
          {{
            row.givenAnswer ||
            '—'
          }}
        </template>

        <template #cell-correctAnswer="{ row }">
          {{
            row.correctAnswer ||
            '—'
          }}
        </template>

        <template #cell-status="{ row }">
          <strong
            class="result-attempt__status"
            :class="
              row.correct
                ? 'result-attempt__status--success'
                : 'result-attempt__status--danger'
            "
          >
            {{
              row.correct
                ? 'Верно'
                : 'Неверно'
            }}
          </strong>
        </template>
      </UiTable>
    </div>
  </details>
</template>

<style scoped>
.result-attempt {
  overflow: hidden;

  color: var(--text);
  background: var(--surface);

  border: 1px solid var(--border);
  border-radius: 12px;
}

.result-attempt__summary {
  padding: 14px 16px;

  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;

  cursor: pointer;

  list-style-position: inside;
}

.result-attempt__summary:hover {
  background: var(--surface-secondary);
}

.result-attempt__title {
  min-width: 0;

  display: grid;
  gap: 4px;
}

.result-attempt__title small {
  color: var(--text-secondary);

  line-height: 1.4;
}

.result-attempt__stats {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 7px;
}

.result-attempt__badge {
  padding: 4px 8px;

  color: var(--text);
  background: var(--surface-secondary);

  border: 1px solid var(--border);
  border-radius: 999px;

  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.result-attempt__badge--success {
  color: var(--success);
  background: var(--success-soft);
  border-color: var(--success);
}

.result-attempt__body {
  padding: 0 14px 14px;
}

.result-attempt__status--success {
  color: var(--success);
}

.result-attempt__status--danger {
  color: var(--danger);
}

@media (max-width: 720px) {
  .result-attempt__summary {
    align-items: flex-start;
    flex-direction: column;
  }

  .result-attempt__stats {
    justify-content: flex-start;
  }
}
</style>
