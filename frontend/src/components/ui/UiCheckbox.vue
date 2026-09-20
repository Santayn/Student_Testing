<script setup>
import { computed } from 'vue'
import Checkbox from 'primevue/checkbox'

let checkboxSequence = 0
const props = defineProps({
  modelValue: { type: [Boolean, Array], default: false },
  value: { type: [String, Number, Boolean, Object], default: true },
  id: { type: String, default: '' },
  label: { type: String, default: '' },
  description: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue', 'change'])
const generatedId = `ui-checkbox-${++checkboxSequence}`
const controlId = computed(() => props.id || generatedId)
const binary = computed(() => !Array.isArray(props.modelValue))
</script>

<template>
  <label
    class="st-ui-choice"
    :class="{ 'st-ui-choice--disabled': disabled }"
    :for="controlId"
  >
    <Checkbox
      :input-id="controlId"
      :model-value="modelValue"
      class="st-ui-choice__control"
      :value="value"
      :binary="binary"
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
