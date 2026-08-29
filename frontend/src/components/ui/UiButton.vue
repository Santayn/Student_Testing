<script setup>
import { computed } from 'vue'

const props = defineProps({
  variant: {
    type: String,
    default: 'secondary',
    validator: (value) =>
      [
        'primary',
        'secondary',
        'danger',
        'success',
        'ghost',
      ].includes(value),
  },

  size: {
    type: String,
    default: 'md',
    validator: (value) =>
      ['sm', 'md', 'lg'].includes(value),
  },

  type: {
    type: String,
    default: 'button',
  },

  loading: {
    type: Boolean,
    default: false,
  },

  loadingText: {
    type: String,
    default: '',
  },

  disabled: {
    type: Boolean,
    default: false,
  },

  block: {
    type: Boolean,
    default: false,
  },

  to: {
    type: [String, Object],
    default: null,
  },

  href: {
    type: String,
    default: '',
  },
})

const emit = defineEmits([
  'click',
])

const isDisabled = computed(() => {
  return (
    props.disabled ||
    props.loading
  )
})

function onClick(event) {
  if (isDisabled.value) {
    event.preventDefault()
    event.stopPropagation()
    return
  }

  emit('click', event)
}
</script>

<template>
  <RouterLink
    v-if="to"
    class="ui-button"
    :class="[
      `ui-button--${variant}`,
      `ui-button--${size}`,
      {
        'ui-button--block': block,
        'ui-button--disabled':
          isDisabled,
      },
    ]"
    :to="to"
    :aria-disabled="
      isDisabled
        ? 'true'
        : undefined
    "
    @click="onClick"
  >
    <span
      v-if="loading"
      class="ui-button__spinner"
      aria-hidden="true"
    />

    <slot
      v-if="!loading || !loadingText"
    />

    <span
      v-else
    >
      {{ loadingText }}
    </span>
  </RouterLink>

  <a
    v-else-if="href"
    class="ui-button"
    :class="[
      `ui-button--${variant}`,
      `ui-button--${size}`,
      {
        'ui-button--block': block,
        'ui-button--disabled':
          isDisabled,
      },
    ]"
    :href="href"
    :aria-disabled="
      isDisabled
        ? 'true'
        : undefined
    "
    @click="onClick"
  >
    <span
      v-if="loading"
      class="ui-button__spinner"
      aria-hidden="true"
    />

    <slot
      v-if="!loading || !loadingText"
    />

    <span
      v-else
    >
      {{ loadingText }}
    </span>
  </a>

  <button
    v-else
    class="ui-button"
    :class="[
      `ui-button--${variant}`,
      `ui-button--${size}`,
      {
        'ui-button--block': block,
      },
    ]"
    :type="type"
    :disabled="isDisabled"
    @click="onClick"
  >
    <span
      v-if="loading"
      class="ui-button__spinner"
      aria-hidden="true"
    />

    <slot
      v-if="!loading || !loadingText"
    />

    <span
      v-else
    >
      {{ loadingText }}
    </span>
  </button>
</template>

<style scoped>
.ui-button {
  min-width: 0;

  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;

  border: 1px solid transparent;
  border-radius: 8px;

  font: inherit;
  font-weight: 700;
  text-decoration: none;

  cursor: pointer;

  transition:
    background-color 0.15s ease,
    border-color 0.15s ease,
    color 0.15s ease,
    transform 0.15s ease,
    opacity 0.15s ease;
}

.ui-button--sm {
  min-height: 32px;
  padding: 5px 9px;

  font-size: 12px;
}

.ui-button--md {
  min-height: 38px;
  padding: 7px 12px;

  font-size: 13px;
}

.ui-button--lg {
  min-height: 44px;
  padding: 9px 16px;

  font-size: 14px;
}

.ui-button--primary {
  color: var(--text-on-brand);
  background: var(--brand);
  border-color: var(--brand);
}

.ui-button--primary:hover:not(:disabled) {
  background: var(--brand-hover);
  border-color: var(--brand-hover);
}

.ui-button--secondary {
  color: var(--text);
  background: var(--surface-secondary);
  border-color: var(--border);
}

.ui-button--secondary:hover:not(:disabled) {
  border-color: var(--brand);
}

.ui-button--danger {
  color: var(--danger);
  background: var(--danger-soft);
  border-color: var(--danger-border);
}

.ui-button--success {
  color: var(--success);
  background: var(--success-soft);
  border-color: var(--success);
}

.ui-button--ghost {
  color: var(--text);
  background: transparent;
  border-color: transparent;
}

.ui-button--ghost:hover:not(:disabled) {
  background: var(--surface-secondary);
}

.ui-button:focus-visible {
  outline: 2px solid var(--focus-ring);
  outline-offset: 2px;
}

.ui-button:active:not(:disabled) {
  transform: scale(0.98);
}

.ui-button:disabled,
.ui-button--disabled {
  opacity: 0.52;
  cursor: default;
  pointer-events: none;
}

.ui-button--block {
  width: 100%;
}

.ui-button__spinner {
  width: 14px;
  height: 14px;

  flex: 0 0 auto;

  border: 2px solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;

  animation:
    ui-button-spin 0.7s linear infinite;
}

@keyframes ui-button-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
