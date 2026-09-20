<script setup>
import Toast from 'primevue/toast'

defineProps({
  position: { type: String, default: 'top-right' },
  group: { type: String, default: undefined },
})
</script>

<template>
  <Toast :position="position" :group="group" />
</template>
