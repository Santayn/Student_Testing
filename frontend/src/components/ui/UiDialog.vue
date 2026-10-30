<script setup>
import { computed } from 'vue'
import Dialog from 'primevue/dialog'
import { isMotionPresetFor, motionClassFor } from '@/config/motionPresets'
import { glassClasses, isGlassSurface } from '@/config/glassPresets'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '' },
  modal: { type: Boolean, default: true },
  dismissableMask: { type: Boolean, default: false },
  closeOnEscape: { type: Boolean, default: true },
  closable: { type: Boolean, default: true },
  blockScroll: { type: Boolean, default: true },
  draggable: { type: Boolean, default: false },
  maximizable: { type: Boolean, default: false },
  width: { type: String, default: '32rem' },
  surface: { type: String, default: 'default', validator: isGlassSurface },
  motion: {
    type: String,
    default: 'default',
    validator: (value) => isMotionPresetFor('dialog', value),
  },
  breakpoints: {
    type: Object,
    default: () => ({
      '640px': 'calc(100vw - 1rem)',
    }),
  },
})

const emit = defineEmits(['update:modelValue', 'show', 'hide'])
const motionClass = computed(() => motionClassFor('dialog', props.motion))
const glassClass = computed(() => glassClasses(props.surface, 'none', 'normal'))
</script>

<template>
  <Dialog
    :visible="modelValue"
    class="st-ui-dialog"
    :class="[motionClass, glassClass]"
    :header="title || undefined"
    :modal="modal"
    :dismissable-mask="dismissableMask"
    :close-on-escape="closeOnEscape"
    :closable="closable"
    :block-scroll="blockScroll"
    :draggable="draggable"
    :maximizable="maximizable"
    :breakpoints="breakpoints"
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
