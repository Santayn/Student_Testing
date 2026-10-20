<script setup>
import {
  computed,
  ref,
  watch,
} from 'vue'

import {
  UiButton,
  UiInput,
  UiTag,
  UiTextarea,
} from '@/components/ui'

import { REVIEW_STATUS_LABELS } from '@/composables/results/useReviewQueue'

const props = defineProps({
  item: { type: Object, required: true },
  busy: { type: Boolean, default: false },
  aiAvailable: { type: Boolean, default: false },
})

const emit = defineEmits(['grade', 'accept', 'regrade'])

const points = ref('')
const comment = ref('')

watch(
  () => props.item,
  (item) => {
    points.value = item?.awardedPoints ?? ''
    comment.value = item?.teacherComment ?? ''
  },
  { immediate: true }
)

const statusVariant = computed(() => {
  switch (props.item.reviewStatus) {
    case 'needs_teacher':
      return 'warning'
    case 'ai_pending':
      return 'info'
    case 'teacher_confirmed':
      return 'success'
    default:
      return 'secondary'
  }
})

const confidenceText = computed(() => {
  const value = Number(props.item.aiConfidence)
  return Number.isFinite(value) && props.item.aiConfidence !== null
    ? `${Math.round(value * 100)}%`
    : ''
})

const pointsError = computed(() => {
  const value = Number(points.value)
  const max = Number(props.item.questionPoints ?? 0)
  if (points.value === '' || !Number.isFinite(value)) {
    return 'Укажите баллы.'
  }
  if (value < 0 || value > max) {
    return `Баллы должны быть от 0 до ${formatNumber(max)}.`
  }
  return ''
})

const canAccept = computed(() => (
  props.item.reviewStatus === 'needs_teacher' || props.item.reviewStatus === 'ai_graded'
))

function formatNumber(value) {
  const number = Number(value)
  return Number.isFinite(number)
    ? new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 2 }).format(number)
    : '0'
}

function formatDateTime(value) {
  try {
    return new Intl.DateTimeFormat('ru-RU', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
  } catch {
    return ''
  }
}

function submitGrade() {
  if (!pointsError.value) {
    emit('grade', { points: points.value, comment: comment.value })
  }
}
</script>

<template>
  <article class="review-card">
    <header class="review-card__header">
      <div class="review-card__title">
        <strong>{{ item.studentName }}</strong>
        <small>
          {{ item.testName }}
          <template v-if="item.completedAt"> · {{ formatDateTime(item.completedAt) }}</template>
        </small>
      </div>
      <div class="review-card__tags">
        <UiTag
          v-if="item.suspicious"
          variant="danger"
          value="Похоже на попытку обмануть ИИ"
        />
        <UiTag
          :variant="statusVariant"
          :value="REVIEW_STATUS_LABELS[item.reviewStatus] || item.reviewStatus"
        />
        <UiTag :value="`${formatNumber(item.awardedPoints)} из ${formatNumber(item.questionPoints)}`" />
      </div>
    </header>

    <div class="review-card__grid">
      <div class="review-card__block review-card__block--wide">
        <span>Вопрос</span>
        <strong>{{ item.questionText }}</strong>
      </div>
      <div class="review-card__block review-card__block--wide review-card__block--answer">
        <span>Ответ студента</span>
        <strong>{{ item.answerText || '—' }}</strong>
      </div>
      <div class="review-card__block">
        <span>Эталон</span>
        <strong>{{ item.correctAnswer || '—' }}</strong>
      </div>
      <div class="review-card__block">
        <span>Критерии</span>
        <strong class="review-card__pre">{{ item.gradingCriteria || 'Не заданы' }}</strong>
      </div>
    </div>

    <section
      v-if="item.gradedBy === 'ai' || item.aiFeedback || item.matched?.length || item.missing?.length"
      class="review-card__ai"
      aria-label="Оценка ИИ"
    >
      <div class="review-card__ai-heading">
        <i class="pi pi-sparkles" aria-hidden="true" />
        <strong>Оценка ИИ</strong>
        <span v-if="confidenceText">уверенность {{ confidenceText }}</span>
      </div>
      <p v-if="item.aiFeedback">{{ item.aiFeedback }}</p>
      <ul
        v-if="item.matched?.length"
        class="review-card__points review-card__points--ok"
      >
        <li v-for="point in item.matched" :key="`m-${point}`">{{ point }}</li>
      </ul>
      <ul
        v-if="item.missing?.length"
        class="review-card__points review-card__points--missing"
      >
        <li v-for="point in item.missing" :key="`x-${point}`">{{ point }}</li>
      </ul>
    </section>

    <form
      class="review-card__form"
      @submit.prevent="submitGrade"
    >
      <UiInput
        v-model="points"
        label="Баллы"
        type="number"
        min="0"
        :max="item.questionPoints"
        step="0.25"
        :error="pointsError"
        :disabled="busy"
      />
      <UiTextarea
        v-model="comment"
        label="Комментарий студенту"
        :rows="2"
        auto-resize
        maxlength="2000"
        :disabled="busy"
      />
      <div class="review-card__actions">
        <UiButton
          type="submit"
          variant="primary"
          size="sm"
          label="Сохранить оценку"
          :loading="busy"
          :disabled="Boolean(pointsError)"
        />
        <UiButton
          v-if="canAccept"
          variant="secondary"
          size="sm"
          label="Подтвердить как есть"
          :disabled="busy"
          @click="emit('accept')"
        />
        <UiButton
          v-if="aiAvailable && item.questionType === 4 && item.reviewStatus !== 'ai_pending'"
          variant="ghost"
          size="sm"
          icon="pi pi-refresh"
          label="Перепроверить ИИ"
          :disabled="busy"
          @click="emit('regrade')"
        />
      </div>
    </form>
  </article>
</template>

<style scoped>
.review-card {
  min-width: 0;
  padding: 16px;
  display: grid;
  gap: 14px;
  background: var(--st-surface);
  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-card);
  box-shadow: var(--st-shadow-card);
}

.review-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.review-card__title {
  min-width: 0;
  display: grid;
  gap: 3px;
}

.review-card__title small {
  color: var(--st-text-secondary);
}

.review-card__tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.review-card__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.review-card__block {
  min-width: 0;
  padding: 10px 12px;
  display: grid;
  gap: 4px;
  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 8px;
}

.review-card__block--wide {
  grid-column: 1 / -1;
}

.review-card__block--answer {
  border-color: var(--st-primary);
}

.review-card__block span {
  color: var(--st-text-secondary);
  font-size: 11px;
  font-weight: var(--st-font-weight-bold);
}

.review-card__block strong {
  line-height: 1.45;
  overflow-wrap: anywhere;
  font-weight: var(--st-font-weight-medium);
}

.review-card__pre {
  white-space: pre-line;
}

.review-card__ai {
  padding: 12px;
  display: grid;
  gap: 6px;
  background: var(--st-primary-soft);
  border-radius: 8px;
}

.review-card__ai p {
  margin: 0;
  line-height: 1.5;
}

.review-card__ai-heading {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--st-primary-soft-text);
}

.review-card__ai-heading span {
  color: var(--st-text-secondary);
  font-size: 12px;
}

.review-card__points {
  margin: 0;
  padding-left: 1.2rem;
  display: grid;
  gap: 2px;
  font-size: 13px;
}

.review-card__points--ok li::marker {
  content: '✓  ';
  color: var(--st-success-text);
}

.review-card__points--missing li::marker {
  content: '✗  ';
  color: var(--st-danger-text);
}

.review-card__form {
  display: grid;
  grid-template-columns: minmax(120px, 160px) minmax(0, 1fr);
  align-items: start;
  gap: 10px;
}

.review-card__actions {
  grid-column: 1 / -1;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 720px) {
  .review-card__header {
    flex-direction: column;
  }

  .review-card__tags {
    justify-content: flex-start;
  }

  .review-card__grid,
  .review-card__form {
    grid-template-columns: 1fr;
  }
}
</style>
