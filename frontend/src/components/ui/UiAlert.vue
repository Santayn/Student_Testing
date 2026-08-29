<script setup>
defineProps({
  variant: {
    type: String,
    default: 'info',
    validator: (value) =>
      [
        'info',
        'success',
        'warning',
        'danger',
        'error',
      ].includes(value),
  },

  title: {
    type: String,
    default: '',
  },

  message: {
    type: String,
    default: '',
  },

  closable: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits([
  'close',
])
</script>

<template>
  <div
    class="ui-alert"
    :class="[
      `ui-alert--${
        variant === 'error'
          ? 'danger'
          : variant
      }`,
    ]"
    role="status"
  >
    <div class="ui-alert__content">
      <strong
        v-if="title"
        class="ui-alert__title"
      >
        {{ title }}
      </strong>

      <div class="ui-alert__message">
        <slot>
          {{ message }}
        </slot>
      </div>
    </div>

    <button
      v-if="closable"
      class="ui-alert__close"
      type="button"
      aria-label="Закрыть сообщение"
      @click="emit('close')"
    >
      ×
    </button>
  </div>
</template>

<style scoped>
.ui-alert {
  padding: 11px 13px;

  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;

  border: 1px solid var(--border);
  border-radius: 9px;

  font-size: 13px;
  line-height: 1.45;
}

.ui-alert--info {
  color: var(--brand);
  background: var(--brand-soft);
  border-color: var(--brand);
}

.ui-alert--success {
  color: var(--success);
  background: var(--success-soft);
  border-color: var(--success);
}

.ui-alert--warning {
  color: var(--warning);
  background: var(--warning-soft);
  border-color: var(--warning-border);
}

.ui-alert--danger {
  color: var(--danger);
  background: var(--danger-soft);
  border-color: var(--danger-border);
}

.ui-alert__content {
  min-width: 0;

  display: grid;
  gap: 3px;
}

.ui-alert__title {
  font-weight: 800;
}

.ui-alert__message {
  overflow-wrap: anywhere;
}

.ui-alert__close {
  padding: 0;

  color: inherit;
  background: transparent;
  border: 0;

  font: inherit;
  font-size: 18px;
  line-height: 1;

  cursor: pointer;
}
</style>
