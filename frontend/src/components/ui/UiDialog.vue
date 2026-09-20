<script setup>
import Dialog from 'primevue/dialog'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '' },
  modal: { type: Boolean, default: true },
  dismissableMask: { type: Boolean, default: false },
  closeOnEscape: { type: Boolean, default: true },
  closable: { type: Boolean, default: true },
  width: { type: String, default: '32rem' },
})

const emit = defineEmits(['update:modelValue', 'show', 'hide'])
</script>

<template>
  <Dialog
    :visible="modelValue"
    class="st-ui-dialog"
    :header="title || undefined"
    :modal="modal"
    :dismissable-mask="dismissableMask"
    :close-on-escape="closeOnEscape"
    :closable="closable"
    :style="{ width, maxWidth: 'calc(100vw - 2rem)' }"
    @update:visible="emit('update:modelValue', $event)"
    @show="emit('show')"
    @hide="emit('hide')"
  >
    <template v-if="$slots.header" #header><slot name="header" /></template>
    <slot />
    <template v-if="$slots.footer" #footer><slot name="footer" /></template>
  </Dialog>
</template>
