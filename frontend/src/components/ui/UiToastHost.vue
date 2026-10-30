<script setup>
import Toast from 'primevue/toast'
import { isMotionPresetFor, motionClassFor } from '@/config/motionPresets'

defineProps({
  position: { type: String, default: 'top-right' },
  group: { type: String, default: undefined },
  motion: {
    type: String,
    default: 'default',
    validator: (value) => isMotionPresetFor('toast', value),
  },
})
</script>

<template>
  <Toast
    class="st-ui-toast-host"
    :class="motionClassFor('toast', motion)"
    :position="position"
    :group="group"
  />
</template>
