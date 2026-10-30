<script setup>
import { motionTransitionName, isMotionPresetFor } from '@/config/motionPresets'
import { useMotionPreferences } from '@/composables/motion/useMotionPreferences'

const props = defineProps({
  show: { type: Boolean, default: true },
  preset: {
    type: String,
    default: 'fade',
    validator: (value) => isMotionPresetFor('transition', value),
  },
  tag: { type: String, default: 'div' },
  appear: { type: Boolean, default: false },
})
const { prefersReducedMotion } = useMotionPreferences()
</script>

<template>
  <Transition :name="prefersReducedMotion ? '' : motionTransitionName(props.preset)" :appear="appear">
    <component
      :is="tag"
      v-if="show"
      class="st-ui-motion"
      :class="{ 'st-ui-motion--expand': preset === 'expand' }"
    >
      <div v-if="preset === 'expand'" class="st-ui-motion__inner"><slot /></div>
      <slot v-else />
    </component>
  </Transition>
</template>
