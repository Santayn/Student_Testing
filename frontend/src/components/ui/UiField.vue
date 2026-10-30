<script setup>
import { isMotionPresetFor, motionClassFor } from '@/config/motionPresets'
defineProps({
  id: { type: String, required: true },
  label: { type: String, default: '' },
  hint: { type: String, default: '' },
  error: { type: String, default: '' },
  required: { type: Boolean, default: false },
  motion: {
    type: String,
    default: 'default',
    validator: (value) => isMotionPresetFor('field', value),
  },
})
</script>

<template>
  <div class="st-ui-field" :class="[{ 'st-ui-field--error': error }, motionClassFor('field', motion)]">
    <label v-if="label" class="st-ui-field__label" :for="id">
      {{ label }}
      <span v-if="required" class="st-ui-field__required" aria-hidden="true">*</span>
    </label>

    <slot />

    <p v-if="error" :id="`${id}-error`" class="st-ui-field__message st-ui-field__message--error">
      {{ error }}
    </p>
    <p v-else-if="hint" :id="`${id}-hint`" class="st-ui-field__message">
      {{ hint }}
    </p>
  </div>
</template>
