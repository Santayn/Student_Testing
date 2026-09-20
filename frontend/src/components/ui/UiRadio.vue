<script setup>
import { computed } from 'vue'
import RadioButton from 'primevue/radiobutton'

let radioSequence = 0
const props = defineProps({
  modelValue: { type: [String, Number, Boolean], default: null },
  value: { type: [String, Number, Boolean], required: true },
  id: { type: String, default: '' },
  label: { type: String, default: '' },
  description: { type: String, default: '' },
  name: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue', 'change'])
const generatedId = `ui-radio-${++radioSequence}`
const controlId = computed(() => props.id || generatedId)
</script>

<template>
  <label
    class="st-ui-choice"
    :class="{ 'st-ui-choice--disabled': disabled }"
    :for="controlId"
  >
    <RadioButton
      :input-id="controlId"
      :model-value="modelValue"
      class="st-ui-choice__control"
      :value="value"
      :name="name || undefined"
      :disabled="disabled"
      @update:model-value="emit('update:modelValue', $event)"
      @change="emit('change', $event)"
    />

    <span class="st-ui-choice__copy">
      <span v-if="label || $slots.default" class="st-ui-choice__label">
        <slot>{{ label }}</slot>
      </span>
      <span v-if="description || $slots.description" class="st-ui-choice__description">
        <slot name="description">{{ description }}</slot>
      </span>
    </span>
  </label>
</template>
