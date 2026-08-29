<script setup>
defineProps({
  title: {
    type: String,
    default: '',
  },

  description: {
    type: String,
    default: '',
  },

  compact: {
    type: Boolean,
    default: false,
  },
})
</script>

<template>
  <div
    class="ui-empty"
    :class="{
      'ui-empty--compact':
        compact,
    }"
  >
    <strong
      v-if="title"
      class="ui-empty__title"
    >
      {{ title }}
    </strong>

    <p
      v-if="description"
      class="ui-empty__description"
    >
      {{ description }}
    </p>

    <div
      v-if="$slots.default"
      class="ui-empty__content"
    >
      <slot />
    </div>

    <div
      v-if="$slots.actions"
      class="ui-empty__actions"
    >
      <slot name="actions" />
    </div>
  </div>
</template>

<style scoped>
.ui-empty {
  padding: 26px 16px;

  display: grid;
  justify-items: center;
  gap: 6px;

  color: var(--text-secondary);
  background: var(--surface-secondary);

  border: 1px dashed var(--border);
  border-radius: 9px;

  font-size: 13px;
  text-align: center;
}

.ui-empty--compact {
  padding: 16px;
}

.ui-empty__title {
  color: var(--text);

  font-size: 14px;
}

.ui-empty__description {
  margin: 0;

  line-height: 1.5;
}

.ui-empty__content {
  width: 100%;
}

.ui-empty__actions {
  margin-top: 4px;

  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}
</style>
