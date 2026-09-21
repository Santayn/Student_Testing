<script setup>
import { computed, useId, useSlots } from 'vue'
import RadioButton from 'primevue/radiobutton'

const props = defineProps({
  modelValue: { type: null, default: null },
  value: { type: null, required: true },
  id: { type: String, default: '' },
  label: { type: String, default: '' },
  description: { type: String, default: '' },
  name: { type: String, required: true },
  disabled: { type: Boolean, default: false },
  readonly: { type: Boolean, default: false },
  invalid: { type: Boolean, default: false },
})

const emit = defineEmits([
  'update:modelValue',
  'change',
  'focus',
  'blur',
])

const slots = useSlots()
const generatedId = `ui-radio-${useId()}`
const controlId = computed(() => props.id || generatedId)
const descriptionId = computed(() => `${controlId.value}-description`)
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
    class="st-ui-choice st-ui-radio ui-radio"
    :class="{
      'st-ui-choice--disabled': disabled,
      'st-ui-choice--readonly': readonly,
      'st-ui-choice--invalid': invalid,
    }"
    :for="controlId"
  >
    <RadioButton
      :input-id="controlId"
      :model-value="modelValue"
      class="st-ui-choice__control"
      :value="value"
      :name="name"
      :disabled="disabled"
      :readonly="readonly"
      :invalid="invalid"
      :pt="inputPt"
      @update:model-value="emit('update:modelValue', $event)"
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
