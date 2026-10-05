<script setup>
defineProps({
  label: {
    type: String,
    default: '',
  },
  value: {
    type: [String, Number],
    default: '',
  },
})
</script>

<template>
  <div class="st-ui-stat">
    <span class="st-ui-stat__label">
      <slot name="label">{{ label }}</slot>
    </span>

    <strong class="st-ui-stat__value">
      <slot>{{ value }}</slot>
    </strong>
  </div>
</template>

<style scoped>
.st-ui-stat {
  min-width: 0;
  padding: 13px 14px;

  display: grid;
  gap: 4px;

  color: var(--st-text);
  background: var(--st-surface-muted);

  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-md);
}

.st-ui-stat__label {
  color: var(--st-text-secondary);

  font-size: var(--st-font-xs);
  font-weight: var(--st-font-weight-bold);
  line-height: var(--st-line-normal);
}

.st-ui-stat__value {
  min-width: 0;

  font-size: var(--st-font-xl);
  font-weight: var(--st-font-weight-bold);
  line-height: var(--st-line-tight);
  overflow-wrap: anywhere;
}
</style>
