<script setup>
import {
  UiAlert,
} from '@/components/ui'

defineProps({
  type: {
    type: String,
    default: 'info',
  },

  message: {
    type: String,
    default: '',
  },
})

const emit = defineEmits([
  'close',
])
</script>

<template>
  <UiAlert
    v-if="message"
    :variant="type"
    :message="message"
    closable
    @close="emit('close')"
  />
</template>
