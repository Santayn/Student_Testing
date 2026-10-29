<script setup>
import { computed, defineAsyncComponent, ref, useAttrs } from 'vue'
import { usePresentationSpace } from '@/composables/usePresentationSpace'
import { getPresentationLoader } from '@/config/presentationRegistry'
import PresentationChunkLoading from './PresentationChunkLoading.vue'
import PresentationChunkError from './PresentationChunkError.vue'

defineOptions({ inheritAttrs: false })

const props = defineProps({
  viewKey: { type: String, required: true },
  // Only for controlled previews / special workflows. Normally measured.
  mode: { type: String, default: null },
})

const attrs = useAttrs()
const root = ref(null)
const { mode: containerMode, ready } = usePresentationSpace(root)

const activeMode = computed(() => props.mode || containerMode.value)

// Cache async component wrappers. Re-rendering must not recreate loaders.
const asyncComponents = new WeakMap()

function componentFor(loader) {
  if (!loader) return null
  if (!asyncComponents.has(loader)) {
    asyncComponents.set(
      loader,
      defineAsyncComponent({
        loader,
        loadingComponent: PresentationChunkLoading,
        errorComponent: PresentationChunkError,
        delay: 150,
      })
    )
  }
  return asyncComponents.get(loader)
}

const activeComponent = computed(() =>
  componentFor(getPresentationLoader(props.viewKey, activeMode.value))
)
</script>

<template>
  <div
    ref="root"
    class="st-responsive-presentation"
    :data-presentation-mode="activeMode || 'unmeasured'"
  >
    <component
      :is="activeComponent"
      v-if="activeComponent"
      v-bind="attrs"
    >
      <slot />
    </component>

    <slot v-else-if="!ready && !mode" name="measuring">
      <PresentationChunkLoading />
    </slot>

    <slot v-else name="missing" :mode="activeMode">
      <div role="alert">
        Для этого рабочего пространства представление не зарегистрировано.
      </div>
    </slot>
  </div>
</template>

<style scoped>
.st-responsive-presentation {
  width: 100%;
  min-width: 0;
  container-type: inline-size;
}
</style>
