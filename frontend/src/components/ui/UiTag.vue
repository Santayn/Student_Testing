<script setup>
import { computed } from 'vue'
import Tag from 'primevue/tag'

const props = defineProps({
  value: { type: [String, Number], default: '' },
  variant: {
    type: String,
    default: 'secondary',
    validator: (value) => ['secondary', 'info', 'success', 'warning', 'danger'].includes(value),
  },
  icon: { type: String, default: '' },
  rounded: { type: Boolean, default: true },
})

const severity = computed(() => props.variant === 'warning' ? 'warn' : props.variant)
</script>

<template>
  <Tag
    class="st-ui-tag"
    :value="value || undefined"
    :severity="severity"
    :icon="icon || undefined"
    :rounded="rounded"
  >
    <slot />
  </Tag>
</template>
