<script setup>
import Toolbar from 'primevue/toolbar'

defineProps({
  ariaLabel: { type: String, default: 'Панель действий' },
})
</script>

<template>
  <Toolbar class="st-ui-toolbar" :aria-label="ariaLabel">
    <template #start><slot name="start"><slot /></slot></template>
    <template #center><slot name="center" /></template>
    <template #end><slot name="end" /></template>
  </Toolbar>
</template>
