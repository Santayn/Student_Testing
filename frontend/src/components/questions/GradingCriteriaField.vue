<script setup>
import {
  computed,
  ref,
} from 'vue'

import { aiApi } from '@/api/ai.api'
import { getApiErrorMessage } from '@/api/error'

import {
  UiAlert,
  UiButton,
  UiTextarea,
} from '@/components/ui'

import { useAiStatus } from '@/composables/ai/useAiStatus'

const props = defineProps({
  modelValue: { type: String, default: '' },
  question: { type: String, default: '' },
  correctAnswer: { type: String, default: '' },
  points: { type: [Number, String], default: 1 },
  error: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue'])

const { aiStatus } = useAiStatus()

const suggesting = ref(false)
const suggestionError = ref('')

const canSuggest = computed(() => Boolean(aiStatus.value?.enabled))
const hasSource = computed(() =>
  Boolean(String(props.question ?? '').trim() && String(props.correctAnswer ?? '').trim())
)

async function suggest() {
  if (suggesting.value || !hasSource.value) {
    return
  }

  suggesting.value = true
  suggestionError.value = ''

  try {
    const response = await aiApi.suggestCriteria({
      question: String(props.question).trim(),
      correctAnswer: String(props.correctAnswer).trim(),
      points: Number(props.points) || 1,
    })
    const criteria = String(response?.data?.criteria ?? '').trim()
    if (criteria) {
      emit('update:modelValue', criteria)
    }
  } catch (requestError) {
    suggestionError.value = getApiErrorMessage(requestError, 'Не удалось получить критерии от ИИ.')
  } finally {
    suggesting.value = false
  }
}
</script>

<template>
  <div class="grading-criteria">
    <UiTextarea
      :model-value="modelValue"
      label="Критерии оценивания"
      hint="Ключевые пункты полного ответа и баллы за них — по ним ИИ и преподаватель оценивают ответ по смыслу."
      :error="error"
      :rows="4"
      auto-resize
      :disabled="disabled"
      maxlength="4000"
      @update:model-value="emit('update:modelValue', $event)"
    />

    <div
      v-if="canSuggest"
      class="grading-criteria__actions"
    >
      <UiButton
        variant="ghost"
        size="sm"
        icon="pi pi-sparkles"
        label="Предложить критерии"
        loading-text="ИИ составляет критерии…"
        :loading="suggesting"
        :disabled="disabled || !hasSource"
        @click="suggest"
      />
      <span
        v-if="!hasSource"
        class="grading-criteria__hint"
      >Сначала заполните вопрос и правильный ответ.</span>
    </div>

    <UiAlert
      v-if="suggestionError"
      variant="warning"
      :message="suggestionError"
    />
  </div>
</template>

<style scoped>
.grading-criteria {
  display: grid;
  gap: 8px;
}

.grading-criteria__actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.grading-criteria__hint {
  color: var(--st-text-secondary);
  font-size: 12px;
}
</style>
