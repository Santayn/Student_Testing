<script setup>
import { computed } from 'vue'

import UiField from './UiField.vue'

defineOptions({
  inheritAttrs: false,
})

let fileSequence = 0

const props = defineProps({
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

  accept: {
    type: String,
    default: '',
  },

  multiple: {
    type: Boolean,
    default: false,
  },

  required: {
    type: Boolean,
    default: false,
  },

  disabled: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits([
  'change',
  'files-change',
])

const generatedId =
  `ui-file-input-${++fileSequence}`

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

function handleChange(event) {
  const files = Array.from(
    event.target.files ?? []
  )

  emit('files-change', files)
  emit('change', event)
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
      class="ui-file-input"
      type="file"
      :accept="accept || undefined"
      :multiple="multiple"
      :required="required"
      :disabled="disabled"
      :aria-invalid="
        error ? 'true' : undefined
      "
      :aria-describedby="describedBy"
      v-bind="$attrs"
      @change="handleChange"
    >
  </UiField>
</template>

<style scoped>
.ui-file-input {
  width: 100%;
  min-height: 42px;

  padding: 7px 9px;

  color: var(--text);
  background: var(--surface);

  border: 1px solid var(--border);
  border-radius: 9px;

  font: inherit;
  font-size: 13px;
}

.ui-file-input:focus {
  outline: 2px solid var(--focus-ring);
  border-color: var(--brand);
}

.ui-file-input:disabled {
  opacity: 0.58;
  cursor: not-allowed;
}

.ui-file-input::file-selector-button {
  margin-right: 10px;
  padding: 6px 9px;

  color: var(--text);
  background: var(--surface-secondary);

  border: 1px solid var(--border);
  border-radius: 7px;

  font: inherit;
  font-weight: 700;

  cursor: pointer;
}
</style>
