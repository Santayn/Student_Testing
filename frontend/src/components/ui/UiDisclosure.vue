<script setup>
import { computed, useId } from 'vue'
import { isMotionPresetFor, motionClassFor, motionTransitionName } from '@/config/motionPresets'

const props = defineProps({
  title: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
  motion: {
    type: String,
    default: 'expand',
    validator: (value) => isMotionPresetFor('disclosure', value),
  },
})
const expanded = defineModel({ type: Boolean, default: false })
const contentId = `ui-disclosure-${useId()}`
const transitionName = computed(() =>
  ['default', 'expand'].includes(props.motion)
    ? motionTransitionName('expand')
    : ''
)

function toggle() {
  if (!props.disabled) expanded.value = !expanded.value
}
</script>

<template>
  <section class="st-ui-disclosure" :class="motionClassFor('disclosure', motion)">
    <button
      type="button"
      class="st-ui-disclosure__trigger"
      :disabled="disabled"
      :aria-expanded="expanded ? 'true' : 'false'"
      :aria-controls="contentId"
      @click="toggle"
    >
      <span class="st-ui-disclosure__title"><slot name="trigger">{{ title }}</slot></span>
      <i
        class="pi pi-chevron-down st-ui-disclosure__chevron"
        :class="{ 'st-ui-disclosure__chevron--expanded': expanded }"
        aria-hidden="true"
      />
    </button>
    <Transition :name="transitionName">
      <div
        v-show="expanded"
        :id="contentId"
        class="st-ui-motion st-ui-motion--expand st-ui-disclosure__content"
      >
        <div class="st-ui-motion__inner st-ui-disclosure__inner"><slot /></div>
      </div>
    </Transition>
  </section>
</template>
