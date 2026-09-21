<script setup>
defineProps({
  title: { type: String, default: '' },
  description: { type: String, default: '' },
  compact: { type: Boolean, default: false },
})
</script>

<template>
  <div class="st-ui-empty" :class="{ 'st-ui-empty--compact': compact }">
    <strong v-if="title" class="st-ui-empty__title">{{ title }}</strong>
    <p v-if="description" class="st-ui-empty__description">{{ description }}</p>
    <div v-if="$slots.default" class="st-ui-empty__content"><slot /></div>
    <div v-if="$slots.actions" class="st-ui-empty__actions"><slot name="actions" /></div>
  </div>
</template>

<style scoped>
.st-ui-empty {
  padding: 26px 16px;
  display: grid;
  justify-items: center;
  gap: 6px;
  color: var(--st-text-secondary);
  background: color-mix(in srgb, var(--st-surface-muted) 72%, var(--st-surface));
  border: 1px dashed color-mix(in srgb, var(--st-border) 86%, var(--st-text-muted));
  border-radius: var(--st-radius-card);
  font-size: 13px;
  text-align: center;
}

.st-ui-empty--compact {
  padding: 16px;
}

.st-ui-empty__title {
  color: var(--st-text);
  font-size: 14px;
  font-weight: 700;
}

.st-ui-empty__description {
  margin: 0;
  color: var(--st-text-secondary);
  line-height: 1.5;
}

.st-ui-empty__content {
  width: 100%;
  color: var(--st-text-secondary);
}

.st-ui-empty__actions {
  margin-top: 4px;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}
</style>
