<script setup>
import { computed } from 'vue'

import {
  UiButton,
  UiInput,
} from '@/components/ui'

import {
  ensureMatchingPairRows,
} from '@/utils/matchingPairs'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => [],
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  minimumPairs: {
    type: Number,
    default: 2,
  },
})

const emit = defineEmits([
  'update:modelValue',
])

const pairs = computed(() => (
  ensureMatchingPairRows(
    props.modelValue,
    props.minimumPairs
  )
))

function emitPairs(nextPairs) {
  emit(
    'update:modelValue',
    nextPairs.map((pair, index) => ({
      ordinal: index + 1,
      left: pair.left ?? '',
      right: pair.right ?? '',
    }))
  )
}

function updatePair(index, field, value) {
  const nextPairs = pairs.value.map((pair) => ({ ...pair }))
  nextPairs[index][field] = value
  emitPairs(nextPairs)
}

function addPair() {
  emitPairs([
    ...pairs.value,
    {
      ordinal: pairs.value.length + 1,
      left: '',
      right: '',
    },
  ])
}

function removePair(index) {
  if (pairs.value.length <= props.minimumPairs) {
    return
  }

  emitPairs(
    pairs.value.filter((_, pairIndex) => pairIndex !== index)
  )
}
</script>

<template>
  <div class="matching-editor">
    <div class="matching-editor__intro">
      <div>
        <strong>Пары соответствия</strong>
        <p>
          Заполните элементы так, как они должны совпадать в правильном ответе.
          Во время теста варианты справа будут перемешаны.
        </p>
      </div>

      <UiButton
        size="sm"
        icon="pi pi-plus"
        label="Добавить пару"
        :disabled="disabled"
        @click="addPair"
      />
    </div>

    <div class="matching-editor__list">
      <article
        v-for="(pair, index) in pairs"
        :key="index"
        class="matching-editor__pair"
      >
        <div class="matching-editor__pair-header">
          <span class="matching-editor__number">
            {{ index + 1 }}
          </span>
          <strong>Пара {{ index + 1 }}</strong>

          <UiButton
            v-if="pairs.length > minimumPairs"
            class="matching-editor__remove"
            variant="ghost"
            size="sm"
            icon="pi pi-trash"
            label="Удалить"
            :disabled="disabled"
            @click="removePair(index)"
          />
        </div>

        <div class="matching-editor__fields">
          <UiInput
            :model-value="pair.left"
            label="Элемент слева"
            placeholder="Например: HTTP"
            maxlength="2000"
            :disabled="disabled"
            @update:model-value="updatePair(index, 'left', $event)"
          />

          <div class="matching-editor__arrow" aria-hidden="true">
            <i class="pi pi-arrow-right" />
          </div>

          <UiInput
            :model-value="pair.right"
            label="Правильное соответствие"
            placeholder="Например: протокол передачи гипертекста"
            maxlength="2000"
            :disabled="disabled"
            @update:model-value="updatePair(index, 'right', $event)"
          />
        </div>
      </article>
    </div>
  </div>
</template>

<style scoped>
.matching-editor {
  min-width: 0;
  display: grid;
  gap: 12px;
}

.matching-editor__intro {
  min-width: 0;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.matching-editor__intro > div {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.matching-editor__intro strong {
  color: var(--st-text);
  font-size: 14px;
}

.matching-editor__intro p {
  margin: 0;
  max-width: 58ch;
  color: var(--st-text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.matching-editor__list {
  min-width: 0;
  display: grid;
  gap: 10px;
}

.matching-editor__pair {
  min-width: 0;
  padding: 12px;
  display: grid;
  gap: 10px;
  background: var(--st-surface);
  border: 1px solid var(--st-border);
  border-radius: 10px;
}

.matching-editor__pair-header {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.matching-editor__number {
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

.matching-editor__remove {
  margin-left: auto;
}

.matching-editor__fields {
  min-width: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: end;
  gap: 10px;
}

.matching-editor__arrow {
  width: 34px;
  height: 40px;
  display: grid;
  place-items: center;
  color: var(--st-text-secondary);
}

@media (max-width: 640px) {
  .matching-editor__intro {
    align-items: stretch;
    flex-direction: column;
  }

  .matching-editor__intro > :last-child {
    width: 100%;
  }

  .matching-editor__fields {
    grid-template-columns: 1fr;
  }

  .matching-editor__arrow {
    width: 100%;
    height: 22px;
    transform: rotate(90deg);
  }
}
</style>
