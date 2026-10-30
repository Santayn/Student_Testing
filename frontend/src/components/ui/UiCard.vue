<script setup>
import { computed } from 'vue'
import Card from 'primevue/card'
import { isMotionPresetFor, motionClassFor } from '@/config/motionPresets'
import { glassClasses, isGlassSurface, isShinePreset, isShineIntensity } from '@/config/glassPresets'

const props = defineProps({
  title: { type: String, default: '' },
  description: { type: String, default: '' },
  compact: { type: Boolean, default: false },
  surface: { type: String, default: 'default', validator: isGlassSurface },
  shine: { type: String, default: 'none', validator: isShinePreset },
  shineIntensity: { type: String, default: 'normal', validator: isShineIntensity },
  motion: {
    type: String,
    default: 'default',
    validator: (value) => isMotionPresetFor('card', value),
  },
})
const motionClass = computed(() => motionClassFor('card', props.motion))
const glassClass = computed(() => glassClasses(props.surface, props.shine, props.shineIntensity))
</script>

<template>
  <Card class="st-ui-card" :class="[{ 'st-ui-card--compact': compact }, motionClass, glassClass]">
    <template v-if="title || description || $slots.header || $slots.actions" #title>
      <div class="st-ui-card__heading">
        <div class="st-ui-card__heading-copy">
          <slot name="header">
            <h2 v-if="title" class="st-ui-card__heading-title">{{ title }}</h2>
            <p v-if="description" class="st-ui-card__heading-description">{{ description }}</p>
          </slot>
        </div>
        <div v-if="$slots.actions" class="st-ui-card__actions">
          <slot name="actions" />
        </div>
      </div>
    </template>

    <template #content>
      <slot />
    </template>

    <template v-if="$slots.footer" #footer>
      <slot name="footer" />
    </template>
  </Card>
</template>
