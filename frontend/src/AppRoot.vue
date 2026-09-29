<script setup>
import App from './App.vue'
import AppRuntimeErrorBoundary from '@/components/runtime/AppRuntimeErrorBoundary.vue'
</script>

<template>
  <AppRuntimeErrorBoundary>
    <App />
  </AppRuntimeErrorBoundary>
</template>
