<script setup>
import { computed } from 'vue'
import UiField from './UiField.vue'

defineOptions({ inheritAttrs: false })
let fileSequence = 0

const props = defineProps({
  id: { type: String, default: '' },
  label: { type: String, default: '' },
  hint: { type: String, default: '' },
  error: { type: String, default: '' },
  accept: { type: String, default: '' },
  multiple: { type: Boolean, default: false },
  required: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
})

const emit = defineEmits(['change', 'files-change'])
const generatedId = `ui-file-input-${++fileSequence}`
const controlId = computed(() => props.id || generatedId)
const describedBy = computed(() => {
  if (props.error) return `${controlId.value}-error`
  if (props.hint) return `${controlId.value}-hint`
  return undefined
})

function handleChange(event) {
  const files = Array.from(event.target.files ?? [])
  emit('files-change', files)
  emit('change', event)
}
</script>

<template>
  <UiField :id="controlId" :label="label" :hint="hint" :error="error" :required="required">
    <input
      :id="controlId"
      class="st-ui-file-input"
      :class="{ 'st-ui-file-input--invalid': error }"
      type="file"
      :accept="accept || undefined"
      :multiple="multiple"
      :required="required"
      :disabled="disabled"
      :aria-invalid="error ? 'true' : undefined"
      :aria-describedby="describedBy"
      v-bind="$attrs"
      @change="handleChange"
    >
  </UiField>
</template>

<style scoped>
.st-ui-file-input {
  width: 100%;
  min-height: var(--st-control-height);
  padding: 6px 8px;
  color: var(--st-text);
  background: var(--st-surface);
  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-control);
  font: inherit;
  font-size: 13px;
  transition:
    color 140ms ease,
    background-color 140ms ease,
    border-color 140ms ease,
    box-shadow 140ms ease;
}

.st-ui-file-input:not(:disabled):hover {
  border-color: color-mix(in srgb, var(--st-primary) 45%, var(--st-border));
}

.st-ui-file-input:focus-visible {
  outline: 0;
  border-color: var(--st-primary);
  box-shadow: var(--st-focus-shadow);
}

.st-ui-file-input--invalid {
  border-color: var(--st-danger);
}

.st-ui-file-input--invalid:not(:disabled):hover,
.st-ui-file-input--invalid:focus-visible {
  border-color: var(--st-danger);
}

.st-ui-file-input--invalid:focus-visible {
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--st-danger) 20%, transparent);
}

.st-ui-file-input:disabled {
  color: var(--st-text-muted);
  background: var(--st-surface-muted);
  border-color: var(--st-border);
  opacity: 1;
  cursor: not-allowed;
}

.st-ui-file-input::file-selector-button {
  margin-right: 10px;
  padding: 6px 10px;
  color: var(--st-text);
  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 7px;
  font: inherit;
  font-weight: 650;
  cursor: pointer;
  transition:
    color 140ms ease,
    background-color 140ms ease,
    border-color 140ms ease;
}

.st-ui-file-input:not(:disabled)::file-selector-button:hover {
  background: color-mix(in srgb, var(--st-primary-soft) 42%, var(--st-surface-muted));
  border-color: color-mix(in srgb, var(--st-primary) 45%, var(--st-border));
}

.st-ui-file-input:disabled::file-selector-button {
  color: var(--st-text-muted);
  background: color-mix(in srgb, var(--st-surface-muted) 82%, var(--st-surface));
  border-color: var(--st-border);
  cursor: not-allowed;
}

@media (max-width: 640px) {
  .st-ui-file-input {
    max-width: 100%;
    min-width: 0;
    min-height: 44px;
    box-sizing: border-box;
    overflow: hidden;
    font-size: 16px;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  .st-ui-file-input::file-selector-button {
    min-height: 32px;
    max-width: 100%;
    margin-right: 8px;
  }
}
</style>
