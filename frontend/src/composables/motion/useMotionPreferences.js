import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

/** Reactive OS preference for components that require JS motion decisions.
 * Regular UI animation is handled entirely in CSS with the same media query.
 */
export function useMotionPreferences() {
  const prefersReducedMotion = ref(false)
  let media = null

  function update(event) {
    prefersReducedMotion.value = Boolean(event.matches)
  }

  onMounted(() => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return

    media = window.matchMedia('(prefers-reduced-motion: reduce)')
    update(media)
    if (typeof media.addEventListener === 'function') {
      media.addEventListener('change', update)
    } else {
      media.addListener?.(update)
    }
  })

  onBeforeUnmount(() => {
    if (!media) return
    if (typeof media.removeEventListener === 'function') {
      media.removeEventListener('change', update)
    } else {
      media.removeListener?.(update)
    }
  })

  return {
    prefersReducedMotion,
    motionAllowed: computed(() => !prefersReducedMotion.value),
  }
}
