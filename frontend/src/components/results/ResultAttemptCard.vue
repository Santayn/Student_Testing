<script setup>
import { computed } from 'vue'

import {
  sanitizeStudentAttempt,
} from '@/utils/resultContracts'

import {
  attemptScoreSummary,
  gradingStatusLabel,
  normalizeGradingStatus,
  resultItemScore,
} from '@/utils/resultScoring'

import {
  UiEmptyState,
  UiTag,
} from '@/components/ui'

const props = defineProps({
  attempt: { type: Object, required: true },
  mode: { type: String, default: 'student' },
  open: { type: Boolean, default: false },
  best: { type: Boolean, default: false },
})

const displayAttempt = computed(() => {
  if (props.mode === 'teacher') {
    return props.attempt
  }

  return sanitizeStudentAttempt(
    props.attempt
  )
})

const stats = computed(() => (
  displayAttempt.value.stats ?? {
    total: 0,
    right: 0,
    percent: 0,
  }
))

const score = computed(() => (
  attemptScoreSummary(
    displayAttempt.value
  )
))

const testName = computed(() => (
  displayAttempt.value.testName ||
  `Тест #${displayAttempt.value.testId ?? '?'}`
))

const metaText = computed(() => {
  const parts = []

  if (props.mode === 'teacher') {
    parts.push(
      displayAttempt.value.studentName ||
      `Студент #${displayAttempt.value.studentId ?? '?'}`
    )
  } else {
    parts.push('Ваша попытка')
  }

  if (displayAttempt.value.attemptOrdinal) {
    parts.push(`Попытка ${displayAttempt.value.attemptOrdinal}`)
  }

  if (displayAttempt.value.completedAt) {
    parts.push(
      formatDateTime(
        displayAttempt.value.completedAt
      )
    )
  }

  return parts.join(' · ')
})

const rows = computed(() => {
  const source = Array.isArray(
    displayAttempt.value.results
  )
    ? displayAttempt.value.results
    : []

  return source.map((row, index) => ({
    ...row,
    displayIndex: index + 1,
  }))
})

function formatScoreNumber(value) {
  const number = Number(value)

  if (!Number.isFinite(number)) {
    return '0'
  }

  return new Intl.NumberFormat(
    'ru-RU',
    { maximumFractionDigits: 2 }
  ).format(number)
}

function statusVariant(row) {
  const status = normalizeGradingStatus(row)

  if (status === 'correct') {
    return 'success'
  }

  if (status === 'partial') {
    return 'warning'
  }

  return 'danger'
}

function formatDateTime(value) {
  try {
    return new Intl.DateTimeFormat(
      'ru-RU',
      {
        dateStyle: 'short',
        timeStyle: 'short',
      }
    ).format(new Date(value))
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
        <strong>{{ testName }}</strong>
        <small>{{ metaText }}</small>
      </div>

      <div class="result-attempt__stats">
        <UiTag
          v-if="best"
          variant="success"
          value="Лучшая попытка"
        />
        <UiTag :value="`${stats.right ?? 0} из ${stats.total ?? 0}`" />
        <UiTag :value="`${formatScoreNumber(score.score)} из ${formatScoreNumber(score.maxScore)} баллов`" />
        <UiTag variant="info" :value="`${formatScoreNumber(score.percent)}%`" />
      </div>
    </summary>

    <div class="result-attempt__body">
      <UiEmptyState
        v-if="!rows.length"
        description="Для этой попытки нет доступных ответов."
        compact
      />

      <div
        v-else
        class="result-answer-list"
      >
        <article
          v-for="row in rows"
          :key="`${row.displayIndex}-${row.questionText}`"
          class="result-answer"
        >
          <div class="result-answer__heading">
            <span class="result-answer__number">{{ row.displayIndex }}</span>
            <div class="result-answer__question">
              <span>Вопрос</span>
              <strong>{{ row.questionText || '—' }}</strong>
            </div>

            <UiTag
              v-if="mode === 'teacher'"
              :variant="statusVariant(row)"
              :value="gradingStatusLabel(row)"
            />
          </div>

          <div
            class="result-answer__data"
            :class="{ 'result-answer__data--teacher': mode === 'teacher' }"
          >
            <div>
              <span>Ответ студента</span>
              <strong>{{ row.givenAnswer || '—' }}</strong>
            </div>

            <template v-if="mode === 'teacher'">
              <div>
                <span>Правильный ответ</span>
                <strong>{{ row.correctAnswer || '—' }}</strong>
              </div>

              <div>
                <span>Баллы</span>
                <strong>
                  {{ formatScoreNumber(resultItemScore(row).awardedPoints) }}
                  из
                  {{ formatScoreNumber(resultItemScore(row).questionPoints) }}
                </strong>
              </div>
            </template>
          </div>
        </article>
      </div>
    </div>
  </details>
</template>

<style scoped>
.result-attempt {
  overflow: hidden;
  color: var(--st-text);
  background: var(--st-surface);
  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-card);
  box-shadow: var(--st-shadow-card);
}

.result-attempt__summary {
  min-height: 58px;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  cursor: pointer;
  list-style-position: inside;
}

.result-attempt__summary:hover {
  background: var(--st-surface-muted);
}

.result-attempt__title {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.result-attempt__title strong {
  overflow-wrap: anywhere;
}

.result-attempt__title small {
  color: var(--st-text-secondary);
  line-height: 1.4;
}

.result-attempt__stats {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 7px;
}

.result-attempt__body {
  padding: 0 14px 14px;
}

.result-answer-list {
  display: grid;
  gap: 10px;
}

.result-answer {
  min-width: 0;
  padding: 14px;
  display: grid;
  gap: 12px;
  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-control);
}

.result-answer__heading {
  min-width: 0;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: start;
  gap: 10px;
}

.result-answer__number {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  color: var(--st-primary-soft-text);
  background: var(--st-primary-soft);
  border-radius: 10px;
  font-weight: 800;
}

.result-answer__question {
  min-width: 0;
  display: grid;
  gap: 3px;
}

.result-answer__question span,
.result-answer__data span {
  color: var(--st-text-secondary);
  font-size: 11px;
  font-weight: 700;
}

.result-answer__question strong,
.result-answer__data strong {
  color: var(--st-text);
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.result-answer__data {
  display: grid;
  gap: 10px;
}

.result-answer__data--teacher {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.result-answer__data > div {
  min-width: 0;
  padding: 10px;
  display: grid;
  gap: 4px;
  background: var(--st-surface);
  border: 1px solid var(--st-border);
  border-radius: 8px;
}

@media (max-width: 720px) {
  .result-attempt__summary {
    align-items: flex-start;
    flex-direction: column;
  }

  .result-attempt__stats {
    justify-content: flex-start;
  }

  .result-answer__heading {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .result-answer__heading :deep(.st-ui-tag) {
    grid-column: 1 / -1;
    justify-self: start;
  }

  .result-answer__data--teacher {
    grid-template-columns: 1fr;
  }
}
</style>
