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
    value: statusText,
    sortValue: (row) => {
      if (row.correct) {
        return 2
      }

      return isPartial(row)
        ? 1
        : 0
    },
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

function isPartial(row) {
  return (
    row?.gradingStatus === 'partial' ||
    (
      !row?.correct &&
      Number(row?.awardedPoints) > 0
    )
  )
}

function statusText(row) {
  if (row?.correct) {
    return 'Верно'
  }

  return isPartial(row)
    ? 'Частично зачтено'
    : 'Неверно'
}

function statusClass(row) {
  if (row?.correct) {
    return 'result-attempt__status--success'
  }

  return isPartial(row)
    ? 'result-attempt__status--warning'
    : 'result-attempt__status--danger'
}

function pointsText(row) {
  const awarded = Number(row?.awardedPoints)
  const total = Number(row?.questionPoints)

  if (
    Number.isFinite(awarded) &&
    Number.isFinite(total) &&
    total > 0
  ) {
    return `${awarded} из ${total} балл.`
  }

  return ''
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
          <span class="result-attempt__status-line">
            <strong
              class="result-attempt__status"
              :class="statusClass(row)"
            >
              {{ statusText(row) }}
            </strong>

            <span
              v-if="row.gradingNote"
              class="result-attempt__review-icon"
              :title="row.gradingNote"
              :aria-label="row.gradingNote"
              role="img"
            >
              !
            </span>
          </span>

          <small
            v-if="row.gradingNote"
            class="result-attempt__grading-note"
          >
            {{ row.gradingNote }}
          </small>

          <small
            v-else-if="isPartial(row)"
            class="result-attempt__grading-note"
          >
            Автоматически зачтено частично, проверьте вручную.
          </small>

          <small
            v-if="pointsText(row)"
            class="result-attempt__points"
          >
            {{ pointsText(row) }}
          </small>
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

.result-attempt__status-line {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.result-attempt__review-icon {
  width: 18px;
  height: 18px;

  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;

  color: #854d0e;
  background: #fef3c7;

  border: 1px solid #f59e0b;
  border-radius: 50%;

  font-size: 12px;
  font-weight: 800;
  line-height: 1;
}

.result-attempt__status--success {
  color: var(--success);
}

.result-attempt__status--danger {
  color: var(--danger);
}

.result-attempt__status--warning {
  color: #a16207;
}

.result-attempt__grading-note,
.result-attempt__points {
  display: block;
  max-width: 220px;
  margin-top: 4px;

  color: var(--text-secondary);

  font-size: 12px;
  line-height: 1.35;
}

.result-attempt__grading-note {
  color: #854d0e;
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
