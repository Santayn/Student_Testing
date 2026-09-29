<script setup>
defineProps({
  label: {
    type: String,
    default: 'Загрузка...',
  },
  compact: {
    type: Boolean,
    default: false,
  },
})
</script>

<template>
  <div
    class="st-ui-loading"
    :class="{
      'st-ui-loading--compact': compact,
    }"
    role="status"
    aria-live="polite"
    aria-busy="true"
  >
    <span
      class="st-ui-loading__spinner"
      aria-hidden="true"
    />

    <span class="st-ui-loading__label">
      {{ label }}
    </span>
  </div>
</template>

<style scoped>
.st-ui-loading {
  min-height: 112px;
  padding: 24px 16px;

  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;

  color: var(--st-text-secondary);
  background: color-mix(in srgb, var(--st-surface-muted) 54%, var(--st-surface));

  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-card);

  font-size: var(--st-font-sm);
  line-height: var(--st-line-normal);
}

.st-ui-loading--compact {
  min-height: 64px;
  padding: 14px 16px;
}

.st-ui-loading__spinner {
  width: 18px;
  height: 18px;
  flex: 0 0 18px;

  border: 2px solid color-mix(in srgb, var(--st-primary) 24%, var(--st-border));
  border-top-color: var(--st-primary);
  border-radius: 50%;

  animation: st-ui-loading-spin 720ms linear infinite;
}

@keyframes st-ui-loading-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .st-ui-loading__spinner {
    animation-duration: 1.6s;
  }
}
</style>
