<script setup>
import { computed } from 'vue'
import Message from 'primevue/message'
import { isMotionPresetFor, motionClassFor } from '@/config/motionPresets'

const props = defineProps({
  variant: {
    type: String,
    default: 'info',
    validator: (value) => ['info', 'success', 'warning', 'danger', 'error'].includes(value),
  },
  title: { type: String, default: '' },
  message: { type: String, default: '' },
  closable: { type: Boolean, default: false },
  motion: {
    type: String,
    default: 'default',
    validator: (value) => isMotionPresetFor('alert', value),
  },
})

const emit = defineEmits(['close'])
const motionClass = computed(() => motionClassFor('alert', props.motion))
const severity = computed(() => {
  if (props.variant === 'warning') return 'warn'
  if (props.variant === 'danger') return 'error'
  return props.variant
})
</script>

<template>
  <Message
    class="st-ui-alert"
    :class="[`st-ui-alert--${variant}`, motionClass]"
    :severity="severity"
    :closable="closable"
    @close="emit('close')"
  >
    <div class="grid gap-1">
      <strong v-if="title">{{ title }}</strong>
      <div>
        <slot>{{ message }}</slot>
      </div>
    </div>
  </Message>
</template>
