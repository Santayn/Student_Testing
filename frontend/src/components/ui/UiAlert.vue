<script setup>
import { computed } from 'vue'
import Message from 'primevue/message'

const props = defineProps({
  variant: {
    type: String,
    default: 'info',
    validator: (value) => ['info', 'success', 'warning', 'danger', 'error'].includes(value),
  },
  title: { type: String, default: '' },
  message: { type: String, default: '' },
  closable: { type: Boolean, default: false },
})

const emit = defineEmits(['close'])
const severity = computed(() => {
  if (props.variant === 'warning') return 'warn'
  if (props.variant === 'danger') return 'error'
  return props.variant
})
</script>

<template>
  <Message
    class="st-ui-alert"
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
