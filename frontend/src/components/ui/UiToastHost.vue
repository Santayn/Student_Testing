<script setup>
import Toast from 'primevue/toast'

defineProps({
  position: { type: String, default: 'top-right' },
  group: { type: String, default: undefined },
})
</script>

<template>
  <Toast class="st-ui-toast-host" :position="position" :group="group" />
</template>
