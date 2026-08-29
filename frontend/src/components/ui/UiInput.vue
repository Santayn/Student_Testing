<script setup>
import { computed } from 'vue'

import UiField from './UiField.vue'

defineOptions({
  inheritAttrs: false,
})

let inputSequence = 0

const props = defineProps({
  modelValue: {
    type: [String, Number],
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

  type: {
    type: String,
    default: 'text',
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

  size: {
    type: String,
    default: 'md',
    validator: (value) =>
      ['sm', 'md', 'lg'].includes(value),
  },
})

const emit = defineEmits([
  'update:modelValue',
  'change',
  'blur',
  'focus',
])

const generatedId =
  `ui-input-${++inputSequence}`

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

function updateValue(event) {
  emit(
    'update:modelValue',
    event.target.value
  )
}
</script>

<template>
  <UiField
    :id="controlId"
    :label="label"
    :hint="hint"
    :error="error"
    :required="required"
  >
    <input
      :id="controlId"
      class="ui-control"
      :class="[
        `ui-control--${size}`,
        {
          'ui-control--error': error,
        },
      ]"
      :value="modelValue"
      :type="type"
      :required="required"
      :disabled="disabled"
      :readonly="readonly"
      :aria-invalid="
        error ? 'true' : undefined
      "
      :aria-describedby="describedBy"
      v-bind="$attrs"
      @input="updateValue"
      @change="
        emit('change', $event)
      "
      @blur="
        emit('blur', $event)
      "
      @focus="
        emit('focus', $event)
      "
    >
  </UiField>
</template>

<style scoped>
.ui-control {
  width: 100%;

  color: var(--text);
  background: var(--surface);

  border: 1px solid var(--border);
  border-radius: 9px;

  font: inherit;

  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease,
    background-color 0.15s ease;
}

.ui-control--sm {
  min-height: 36px;
  padding: 7px 9px;

  font-size: 13px;
}

.ui-control--md {
  min-height: 42px;
  padding: 8px 10px;

  font-size: 14px;
}

.ui-control--lg {
  min-height: 46px;
  padding: 10px 12px;

  font-size: 15px;
}

.ui-control:focus {
  outline: 2px solid var(--focus-ring);
  outline-offset: 0;

  border-color: var(--brand);
}

.ui-control--error {
  border-color: var(--danger);
}

.ui-control:disabled {
  opacity: 0.58;
  cursor: not-allowed;
}

.ui-control:read-only {
  background: var(--surface-secondary);
}
</style>
