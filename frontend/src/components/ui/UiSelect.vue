<script setup>
import { computed } from 'vue'
import Select from 'primevue/select'
import UiField from './UiField.vue'

defineOptions({ inheritAttrs: false })
let selectSequence = 0

const props = defineProps({
  modelValue: { type: [String, Number, Boolean, Object], default: null },
  options: { type: Array, default: () => [] },
  optionLabel: { type: [String, Function], default: 'label' },
  optionValue: { type: [String, Function], default: 'value' },
  optionDisabled: { type: [String, Function], default: 'disabled' },
  id: { type: String, default: '' },
  label: { type: String, default: '' },
  hint: { type: String, default: '' },
  error: { type: String, default: '' },
  placeholder: { type: String, default: '' },
  required: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  filter: { type: Boolean, default: false },
  clearable: { type: Boolean, default: false },
  size: {
    type: String,
    default: 'md',
    validator: (value) => ['sm', 'md', 'lg'].includes(value),
  },
})

const emit = defineEmits(['update:modelValue', 'change', 'blur', 'focus'])
const generatedId = `ui-select-${++selectSequence}`
const controlId = computed(() => props.id || generatedId)
const describedBy = computed(() => {
  if (props.error) return `${controlId.value}-error`
  if (props.hint) return `${controlId.value}-hint`
  return undefined
})

function getValue(option, resolver, fallback) {
  if (typeof resolver === 'function') return resolver(option)
  if (option !== null && typeof option === 'object') return option?.[resolver]
  return fallback
}

const normalizedOptions = computed(() => props.options.map((option, index) => ({
  __key: `${String(getValue(option, props.optionValue, option))}-${index}`,
  label: getValue(option, props.optionLabel, option),
  value: getValue(option, props.optionValue, option),
  disabled: Boolean(getValue(option, props.optionDisabled, false)),
  raw: option,
})))
</script>

<template>
  <UiField :id="controlId" :label="label" :hint="hint" :error="error" :required="required">
    <Select
      :input-id="controlId"
      :model-value="modelValue"
      :options="normalizedOptions"
      option-label="label"
      option-value="value"
      option-disabled="disabled"
      class="st-ui-control"
      :class="[
        `st-ui-control--${size}`,
        { 'st-ui-control--invalid': error },
      ]"
      :placeholder="placeholder"
      :disabled="disabled"
      :filter="filter"
      :show-clear="clearable"
      :aria-invalid="error ? 'true' : undefined"
      :aria-required="required ? 'true' : undefined"
      :aria-describedby="describedBy"
      fluid
      v-bind="$attrs"
      @update:model-value="emit('update:modelValue', $event)"
      @change="emit('change', $event)"
      @focus="emit('focus', $event)"
      @blur="emit('blur', $event)"
    >
      <template v-if="$slots.option" #option="slotProps">
        <slot name="option" :option="slotProps.option.raw" />
      </template>
      <template v-if="$slots.value" #value="slotProps">
        <slot name="value" :value="slotProps.value" :placeholder="slotProps.placeholder" />
      </template>
    </Select>
  </UiField>
</template>
