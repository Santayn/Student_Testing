<script setup>
import { computed } from 'vue'
import Button from 'primevue/button'

const props = defineProps({
  variant: {
    type: String,
    default: 'secondary',
    validator: (value) => ['primary', 'secondary', 'danger', 'success', 'ghost'].includes(value),
  },
  size: {
    type: String,
    default: 'md',
    validator: (value) => ['sm', 'md', 'lg'].includes(value),
  },
  type: { type: String, default: 'button' },
  label: { type: String, default: '' },
  icon: { type: String, default: '' },
  iconPos: { type: String, default: 'left' },
  loading: { type: Boolean, default: false },
  loadingText: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
  block: { type: Boolean, default: false },
  rounded: { type: Boolean, default: false },
  to: { type: [String, Object], default: null },
  href: { type: String, default: '' },
})

const emit = defineEmits(['click'])
const isDisabled = computed(() => props.disabled || props.loading)
const severity = computed(() => {
  if (props.variant === 'danger') return 'danger'
  if (props.variant === 'success') return 'success'
  if (props.variant === 'secondary') return 'secondary'
  return undefined
})
const outlined = computed(() => props.variant === 'secondary')
const text = computed(() => props.variant === 'ghost')
const visibleLabel = computed(() => {
  if (props.loading && props.loadingText) return props.loadingText
  return props.label
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
    :to="to"
    class="st-ui-link-button"
    :class="[
      `st-ui-link-button--${variant}`,
      `st-ui-link-button--${size}`,
      {
        'st-ui-link-button--block': block,
        'st-ui-link-button--disabled': isDisabled,
      },
    ]"
    :aria-disabled="isDisabled ? 'true' : undefined"
    @click="onClick"
  >
    <i v-if="icon" :class="icon" aria-hidden="true" />
    <span v-if="visibleLabel">{{ visibleLabel }}</span>
    <slot v-else />
  </RouterLink>

  <a
    v-else-if="href"
    :href="href"
    class="st-ui-link-button"
    :class="[
      `st-ui-link-button--${variant}`,
      `st-ui-link-button--${size}`,
      {
        'st-ui-link-button--block': block,
        'st-ui-link-button--disabled': isDisabled,
      },
    ]"
    :aria-disabled="isDisabled ? 'true' : undefined"
    @click="onClick"
  >
    <i v-if="icon" :class="icon" aria-hidden="true" />
    <span v-if="visibleLabel">{{ visibleLabel }}</span>
    <slot v-else />
  </a>

  <Button
    v-else
    class="st-ui-button"
    :class="[
      `st-ui-button--${variant}`,
      `st-ui-button--${size}`,
      { 'st-ui-button--block': block },
    ]"
    :type="type"
    :label="visibleLabel || undefined"
    :icon="icon || undefined"
    :icon-pos="iconPos"
    :severity="severity"
    :outlined="outlined"
    :text="text"
    :rounded="rounded"
    :loading="loading"
    :disabled="isDisabled"
    @click="onClick"
  >
    <template v-if="$slots.default && !visibleLabel" #default>
      <slot />
    </template>
  </Button>
</template>
