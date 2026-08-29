<script setup>
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
  <div
    v-if="message"
    class="admin-notice"
    :class="`admin-notice--${type}`"
    role="status"
  >
    <span>{{ message }}</span>

    <button
      class="admin-notice__close"
      type="button"
      aria-label="Закрыть сообщение"
      @click="emit('close')"
    >
      ×
    </button>
  </div>
</template>
