<script setup>
import { computed } from 'vue'

import {
  UiSelect,
  UiTag,
} from '@/components/ui'

import {
  assignMatchingRightIndex,
  matchingRightIndexForPrompt,
} from '@/utils/matchingPairs'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => [],
  },
  prompts: {
    type: Array,
    default: () => [],
  },
  options: {
    type: Array,
    default: () => [],
  },
  disabled: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits([
  'update:modelValue',
])

const normalizedPrompts = computed(() => (
  props.prompts.map((prompt, index) => ({
    ordinal: Number(prompt?.ordinal) || index + 1,
    text: String(prompt?.text ?? '').trim(),
  }))
))

const normalizedOptions = computed(() => (
  props.options.map((option, index) => ({
    index,
    text: String(option ?? '').trim(),
  }))
))

const completedCount = computed(() => (
  normalizedPrompts.value.filter((prompt) => (
    matchingRightIndexForPrompt(
      props.modelValue,
      prompt.ordinal
    ) !== null
  )).length
))

const complete = computed(() => (
  normalizedPrompts.value.length > 0 &&
  completedCount.value === normalizedPrompts.value.length
))

function selectedRightIndex(promptOrdinal) {
  return matchingRightIndexForPrompt(
    props.modelValue,
    promptOrdinal
  )
}

function selectOptions(promptOrdinal) {
  const current = selectedRightIndex(promptOrdinal)
  const usedByOtherPrompt = new Set(
    normalizedPrompts.value
      .filter((prompt) => prompt.ordinal !== Number(promptOrdinal))
      .map((prompt) => selectedRightIndex(prompt.ordinal))
      .filter((value) => value !== null)
  )

  return normalizedOptions.value.map((option) => ({
    label: option.text || `Вариант ${option.index + 1}`,
    value: option.index,
    disabled:
      usedByOtherPrompt.has(option.index) &&
      option.index !== current,
  }))
}

function updatePair(promptOrdinal, rightIndex) {
  emit(
    'update:modelValue',
    assignMatchingRightIndex(
      props.modelValue,
      promptOrdinal,
      rightIndex,
      normalizedOptions.value.length
    )
  )
}
</script>

<template>
  <section class="student-matching" aria-label="Сопоставление элементов">
    <div class="student-matching__intro">
      <div>
        <strong>Сопоставьте элементы</strong>
        <p>
          Для каждого элемента слева выберите подходящее соответствие.
          Каждый вариант справа можно использовать только один раз.
        </p>
      </div>

      <UiTag
        :variant="complete ? 'success' : 'info'"
        :value="`Сопоставлено ${completedCount} из ${normalizedPrompts.length}`"
      />
    </div>

    <div class="student-matching__list">
      <article
        v-for="prompt in normalizedPrompts"
        :key="prompt.ordinal"
        class="student-matching__pair"
      >
        <div class="student-matching__source">
          <span class="student-matching__number">
            {{ prompt.ordinal }}
          </span>

          <div>
            <small>Элемент</small>
            <strong>{{ prompt.text || '—' }}</strong>
          </div>
        </div>

        <i
          class="pi pi-arrow-right student-matching__arrow"
          aria-hidden="true"
        />

        <UiSelect
          :model-value="selectedRightIndex(prompt.ordinal)"
          :options="selectOptions(prompt.ordinal)"
          :label="`Соответствие для элемента ${prompt.ordinal}`"
          placeholder="Выберите соответствие"
          clearable
          :disabled="disabled"
          @update:model-value="updatePair(prompt.ordinal, $event)"
        />
      </article>
    </div>
  </section>
</template>

<style scoped>
.student-matching {
  min-width: 0;
  display: grid;
  gap: 12px;
}

.student-matching__intro {
  min-width: 0;
  padding: 12px 14px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  color: var(--st-text);
  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 10px;
}

.student-matching__intro > div {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.student-matching__intro strong {
  font-size: 14px;
}

.student-matching__intro p {
  margin: 0;
  color: var(--st-text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.student-matching__list {
  min-width: 0;
  display: grid;
  gap: 9px;
}

.student-matching__pair {
  min-width: 0;
  padding: 12px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(240px, 1.15fr);
  align-items: center;
  gap: 12px;
  background: var(--st-surface);
  border: 1px solid var(--st-border);
  border-radius: 10px;
}

.student-matching__source {
  min-width: 0;
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.student-matching__source > div {
  min-width: 0;
  display: grid;
  gap: 3px;
}

.student-matching__source small {
  color: var(--st-text-secondary);
  font-size: 11px;
  font-weight: 700;
}

.student-matching__source strong {
  color: var(--st-text);
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.student-matching__number {
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  display: grid;
  place-items: center;
  color: var(--st-primary-soft-text);
  background: var(--st-primary-soft);
  border-radius: 8px;
  font-size: 12px;
  font-weight: 800;
}

.student-matching__arrow {
  color: var(--st-text-secondary);
}

@media (max-width: 760px) {
  .student-matching__intro {
    display: grid;
  }

  .student-matching__pair {
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .student-matching__arrow {
    justify-self: start;
    transform: rotate(90deg);
  }
}
</style>
