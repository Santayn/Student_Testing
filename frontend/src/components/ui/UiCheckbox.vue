<script setup>
import { computed, useId, useSlots } from 'vue'
import Checkbox from 'primevue/checkbox'

const props = defineProps({
  modelValue: { type: null, default: false },
  mode: {
    type: String,
    default: 'binary',
    validator: (value) => ['binary', 'multiple'].includes(value),
  },
  value: { type: null, default: undefined },
  trueValue: { type: null, default: true },
  falseValue: { type: null, default: false },
  id: { type: String, default: '' },
  label: { type: String, default: '' },
  description: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
  readonly: { type: Boolean, default: false },
  required: { type: Boolean, default: false },
  invalid: { type: Boolean, default: false },
  indeterminate: { type: Boolean, default: false },
})

const emit = defineEmits([
  'update:modelValue',
  'update:indeterminate',
  'change',
  'focus',
  'blur',
])

const slots = useSlots()
const generatedId = `ui-checkbox-${useId()}`
const controlId = computed(() => props.id || generatedId)
const descriptionId = computed(() => `${controlId.value}-description`)
const binary = computed(() => props.mode === 'binary')
const resolvedModelValue = computed(() => {
  if (props.mode === 'multiple') {
    return Array.isArray(props.modelValue) ? props.modelValue : []
  }

  return props.modelValue
})
const hasDescription = computed(
  () => Boolean(props.description || slots.description)
)
const inputPt = computed(() => ({
  input: {
    'aria-describedby': hasDescription.value
      ? descriptionId.value
      : undefined,
  },
}))
</script>

<template>
  <label
    class="st-ui-choice st-ui-checkbox ui-checkbox"
    :class="{
      'st-ui-choice--disabled': disabled,
      'st-ui-choice--readonly': readonly,
      'st-ui-choice--invalid': invalid,
      'st-ui-choice--indeterminate': indeterminate,
      'st-ui-choice--multiple': mode === 'multiple',
    }"
    :for="controlId"
  >
    <Checkbox
      :input-id="controlId"
      :model-value="resolvedModelValue"
      class="st-ui-choice__control"
      :value="value"
      :binary="binary"
      :true-value="trueValue"
      :false-value="falseValue"
      :disabled="disabled"
      :readonly="readonly"
      :required="required"
      :invalid="invalid"
      :indeterminate="indeterminate"
      :pt="inputPt"
      @update:model-value="emit('update:modelValue', $event)"
      @update:indeterminate="emit('update:indeterminate', $event)"
      @change="emit('change', $event)"
      @focus="emit('focus', $event)"
      @blur="emit('blur', $event)"
    />

    <span class="st-ui-choice__copy">
      <span v-if="label || $slots.default" class="st-ui-choice__label">
        <slot>{{ label }}</slot>
      </span>
      <span
        v-if="hasDescription"
        :id="descriptionId"
        class="st-ui-choice__description"
      >
        <slot name="description">{{ description }}</slot>
      </span>
    </span>
  </label>
</template>
