<script setup>
import { computed } from 'vue'
import InputText from 'primevue/inputtext'
import UiField from './UiField.vue'

defineOptions({ inheritAttrs: false })
let inputSequence = 0

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  id: { type: String, default: '' },
  label: { type: String, default: '' },
  hint: { type: String, default: '' },
  error: { type: String, default: '' },
  type: { type: String, default: 'text' },
  required: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  readonly: { type: Boolean, default: false },
  size: {
    type: String,
    default: 'md',
    validator: (value) => ['sm', 'md', 'lg'].includes(value),
  },
})

const emit = defineEmits(['update:modelValue', 'change', 'blur', 'focus'])
const generatedId = `ui-input-${++inputSequence}`
const controlId = computed(() => props.id || generatedId)
const describedBy = computed(() => {
  if (props.error) return `${controlId.value}-error`
  if (props.hint) return `${controlId.value}-hint`
  return undefined
})
</script>

<template>
  <UiField :id="controlId" :label="label" :hint="hint" :error="error" :required="required">
    <InputText
      :id="controlId"
      :model-value="modelValue"
      class="st-ui-control"
      :class="[
        `st-ui-control--${size}`,
        { 'st-ui-control--invalid': error },
      ]"
      :type="type"
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
