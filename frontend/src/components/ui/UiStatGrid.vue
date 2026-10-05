<script setup>
defineProps({
  minItemWidth: {
    type: String,
    default: '150px',
  },
})
</script>

<template>
  <div
    class="st-ui-stat-grid"
    :style="{
      '--ui-stat-min-width': minItemWidth,
    }"
  >
    <slot />
  </div>
</template>

<style scoped>
.st-ui-stat-grid {
  display: grid;
  grid-template-columns:
    repeat(auto-fit, minmax(min(100%, var(--ui-stat-min-width)), 1fr));
  gap: 10px;
}
</style>
