<script setup>
import WorkspacePageShell from '@/components/layout/WorkspacePageShell.vue'

defineProps({
  title: {
    type: String,
    required: true,
  },
  subtitle: {
    type: String,
    default: '',
  },
  narrow: {
    type: Boolean,
    default: false,
  },
})
</script>

<template>
  <WorkspacePageShell
    :title="title"
    :subtitle="subtitle"
    :narrow="narrow"
    narrow-width="880px"
  >
    <template
      v-if="$slots.actions"
      #actions
    >
      <slot name="actions" />
    </template>

    <slot />
  </WorkspacePageShell>
</template>
