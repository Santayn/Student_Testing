<script setup>
import { computed } from 'vue'
import Drawer from 'primevue/drawer'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '' },
  position: {
    type: String,
    default: 'right',
    validator: (value) => ['left', 'right', 'top', 'bottom', 'full'].includes(value),
  },
  modal: { type: Boolean, default: true },
  dismissable: { type: Boolean, default: false },
  closeOnEscape: { type: Boolean, default: true },
  closable: { type: Boolean, default: true },
  blockScroll: { type: Boolean, default: true },
  width: { type: String, default: '38rem' },
  height: { type: String, default: '24rem' },
  mobileFullScreen: { type: Boolean, default: true },
})

const emit = defineEmits([
  'update:modelValue',
  'show',
  'after-show',
  'hide',
  'after-hide',
  'before-hide',
])

const panelStyle = computed(() => {
  if (props.position === 'left' || props.position === 'right') {
    return {
      width: props.width,
      maxWidth: 'calc(100vw - 0.75rem)',
    }
  }

  if (props.position === 'top' || props.position === 'bottom') {
    return {
      height: props.height,
      maxHeight: 'calc(100dvh - 0.75rem)',
    }
  }

  return undefined
})
</script>

<template>
  <Drawer
    :visible="modelValue"
    :class="[
      'st-ui-drawer',
      { 'st-ui-drawer--mobile-full': mobileFullScreen },
    ]"
    :header="title || undefined"
    :position="position"
    :modal="modal"
    :dismissable="dismissable"
    :show-close-icon="closable"
    :close-on-escape="closeOnEscape"
    :block-scroll="blockScroll"
    :style="panelStyle"
    @update:visible="emit('update:modelValue', $event)"
    @show="emit('show')"
    @after-show="emit('after-show')"
    @hide="emit('hide')"
    @after-hide="emit('after-hide')"
    @before-hide="emit('before-hide')"
  >
    <template v-if="$slots.header" #header><slot name="header" /></template>
    <slot />
    <template v-if="$slots.footer" #footer><slot name="footer" /></template>
  </Drawer>
</template>
