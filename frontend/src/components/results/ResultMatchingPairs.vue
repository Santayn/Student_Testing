<script setup>
import { computed } from 'vue'

import {
  UiTag,
} from '@/components/ui'

import {
  matchingResultPairs,
} from '@/utils/matchingPairs'

const props = defineProps({
  givenAnswer: {
    type: String,
    default: '',
  },
  correctAnswer: {
    type: String,
    default: '',
  },
  teacher: {
    type: Boolean,
    default: false,
  },
})

const pairs = computed(() => (
  matchingResultPairs(
    props.givenAnswer,
    props.teacher ? props.correctAnswer : null
  )
))
</script>

<template>
  <div class="result-matching">
    <div class="result-matching__header">
      <strong>
        {{ teacher ? 'Соответствия студента' : 'Ваши соответствия' }}
      </strong>
      <span>{{ pairs.length }} пар</span>
    </div>

    <div class="result-matching__list">
      <article
        v-for="pair in pairs"
        :key="pair.ordinal"
        class="result-matching__pair"
        :class="{ 'result-matching__pair--teacher': teacher }"
      >
        <div class="result-matching__source">
          <span class="result-matching__number">{{ pair.ordinal }}</span>
          <div>
            <small>Элемент</small>
            <strong>{{ pair.left || '—' }}</strong>
          </div>
        </div>

        <i class="pi pi-arrow-right result-matching__arrow" aria-hidden="true" />

        <div class="result-matching__answer">
          <small>{{ teacher ? 'Ответ студента' : 'Выбрано' }}</small>
          <strong>{{ pair.givenRight || 'Не выбрано' }}</strong>
        </div>

        <template v-if="teacher">
          <div class="result-matching__correct">
            <small>Правильное соответствие</small>
            <strong>{{ pair.correctRight || '—' }}</strong>
          </div>

          <UiTag
            :variant="pair.matches ? 'success' : 'danger'"
            :value="pair.matches ? 'Верно' : 'Ошибка'"
          />
        </template>
      </article>
    </div>
  </div>
</template>

<style scoped>
.result-matching {
  min-width: 0;
  display: grid;
  gap: 10px;
}

.result-matching__header {
  min-width: 0;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.result-matching__header strong {
  color: var(--st-text);
  font-size: 13px;
}

.result-matching__header span,
.result-matching small {
  color: var(--st-text-secondary);
  font-size: 11px;
  font-weight: 700;
}

.result-matching__list {
  min-width: 0;
  display: grid;
  gap: 8px;
}

.result-matching__pair {
  min-width: 0;
  padding: 10px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  background: var(--st-surface);
  border: 1px solid var(--st-border);
  border-radius: 9px;
}

.result-matching__source,
.result-matching__answer,
.result-matching__correct {
  min-width: 0;
}

.result-matching__source {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.result-matching__source > div,
.result-matching__answer,
.result-matching__correct {
  display: grid;
  gap: 3px;
}

.result-matching__source strong,
.result-matching__answer strong,
.result-matching__correct strong {
  color: var(--st-text);
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.result-matching__number {
  width: 28px;
  height: 28px;
  flex: 0 0 28px;
  display: grid;
  place-items: center;
  color: var(--st-primary-soft-text);
  background: var(--st-primary-soft);
  border-radius: 8px;
  font-size: 12px;
  font-weight: 800;
}

.result-matching__arrow {
  color: var(--st-text-secondary);
}

.result-matching__pair--teacher {
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr) minmax(0, 1fr) auto;
}

@media (max-width: 900px) {
  .result-matching__pair,
  .result-matching__pair--teacher {
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .result-matching__arrow {
    justify-self: start;
    transform: rotate(90deg);
  }
}
</style>
