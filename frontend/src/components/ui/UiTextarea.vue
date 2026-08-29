<script setup>
import { computed } from 'vue'

import UiField from './UiField.vue'

defineOptions({
  inheritAttrs: false,
})

let textareaSequence = 0

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },

  id: {
    type: String,
    default: '',
  },

  label: {
    type: String,
    default: '',
  },

  hint: {
    type: String,
    default: '',
  },

  error: {
    type: String,
    default: '',
  },

  required: {
    type: Boolean,
    default: false,
  },

  disabled: {
    type: Boolean,
    default: false,
  },

  readonly: {
    type: Boolean,
    default: false,
  },

  rows: {
    type: Number,
    default: 4,
  },
})

const emit = defineEmits([
  'update:modelValue',
  'change',
  'blur',
  'focus',
])

const generatedId =
  `ui-textarea-${++textareaSequence}`

const controlId = computed(() => {
  return props.id || generatedId
})

const describedBy = computed(() => {
  if (props.error) {
    return `${controlId.value}-error`
  }

  if (props.hint) {
    return `${controlId.value}-hint`
  }

  return undefined
})
</script>

<template>
  <UiField
    :id="controlId"
    :label="label"
    :hint="hint"
    :error="error"
    :required="required"
  >
    <textarea
      :id="controlId"
      class="ui-textarea"
      :class="{
        'ui-textarea--error': error,
      }"
      :value="modelValue"
      :rows="rows"
      :required="required"
      :disabled="disabled"
      :readonly="readonly"
      :aria-invalid="
        error ? 'true' : undefined
      "
      :aria-describedby="describedBy"
      v-bind="$attrs"
      @input="
        emit(
          'update:modelValue',
          $event.target.value
        )
      "
      @change="
        emit('change', $event)
      "
      @blur="
        emit('blur', $event)
      "
      @focus="
        emit('focus', $event)
      "
    />
  </UiField>
</template>

<style scoped>
.ui-textarea {
  width: 100%;
  min-height: 96px;

  padding: 10px;

  color: var(--text);
  background: var(--surface);

  border: 1px solid var(--border);
  border-radius: 9px;

  font: inherit;
  font-size: 14px;
  line-height: 1.45;

  resize: vertical;

  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.ui-textarea:focus {
  outline: 2px solid var(--focus-ring);

  border-color: var(--brand);
}

.ui-textarea--error {
  border-color: var(--danger);
}

.ui-textarea:disabled {
  opacity: 0.58;
  cursor: not-allowed;
}

.ui-textarea:read-only {
  background: var(--surface-secondary);
}
</style>
