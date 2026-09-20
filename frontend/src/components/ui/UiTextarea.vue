<script setup>
import { computed } from 'vue'
import Textarea from 'primevue/textarea'
import UiField from './UiField.vue'

defineOptions({ inheritAttrs: false })
let textareaSequence = 0

const props = defineProps({
  modelValue: { type: String, default: '' },
  id: { type: String, default: '' },
  label: { type: String, default: '' },
  hint: { type: String, default: '' },
  error: { type: String, default: '' },
  required: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  readonly: { type: Boolean, default: false },
  rows: { type: Number, default: 4 },
  autoResize: { type: Boolean, default: false },
  size: {
    type: String,
    default: 'md',
    validator: (value) => ['sm', 'md', 'lg'].includes(value),
  },
})

const emit = defineEmits(['update:modelValue', 'change', 'blur', 'focus'])
const generatedId = `ui-textarea-${++textareaSequence}`
const controlId = computed(() => props.id || generatedId)
const describedBy = computed(() => {
  if (props.error) return `${controlId.value}-error`
  if (props.hint) return `${controlId.value}-hint`
  return undefined
})
</script>

<template>
  <UiField :id="controlId" :label="label" :hint="hint" :error="error" :required="required">
    <Textarea
      :id="controlId"
      :model-value="modelValue"
      class="st-ui-control"
      :class="[
        `st-ui-control--${size}`,
        { 'st-ui-control--invalid': error },
      ]"
      :rows="rows"
      :auto-resize="autoResize"
      :required="required"
      :disabled="disabled"
      :readonly="readonly"
      :aria-invalid="error ? 'true' : undefined"
      :aria-describedby="describedBy"
      v-bind="$attrs"
      @update:model-value="emit('update:modelValue', $event)"
      @change="emit('change', $event)"
      @blur="emit('blur', $event)"
      @focus="emit('focus', $event)"
    />
  </UiField>
</template>
