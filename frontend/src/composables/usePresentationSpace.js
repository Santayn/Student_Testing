import { computed, onScopeDispose, ref, watch } from 'vue'
import {
  CONTAINER_BREAKPOINTS,
  resolvePresentationMode,
} from '@/config/presentationModes'

/**
 * Observes the CONTENT width of an actual workspace, not the browser window.
 * No view is selected until the container has been measured, preventing
 * downloading the wrong responsive chunk during first render.
 */
export function usePresentationSpace(
  containerRef,
  breakpoints = CONTAINER_BREAKPOINTS
) {
  const width = ref(null)
  const mode = computed(() =>
    resolvePresentationMode(width.value, breakpoints)
  )
  const ready = computed(() => mode.value !== null)

  let observer = null
  let fallbackListener = null

  function setWidth(nextWidth) {
    width.value =
      typeof nextWidth === 'number' &&
      Number.isFinite(nextWidth) &&
      nextWidth > 0
        ? nextWidth
        : null
  }

  function cleanup() {
    observer?.disconnect()
    observer = null
    if (fallbackListener && typeof window !== 'undefined') {
      window.removeEventListener('resize', fallbackListener)
    }
    fallbackListener = null
  }

  watch(
    containerRef,
    (element) => {
      cleanup()
      setWidth(null)
      if (!element) return

      const measure = () => {
        setWidth(element.getBoundingClientRect().width)
      }
      measure()

      if (typeof ResizeObserver !== 'undefined') {
        observer = new ResizeObserver((entries) => {
          const entry = entries.find((item) => item.target === element)
          if (entry) setWidth(entry.contentRect.width)
        })
        observer.observe(element)
      } else if (typeof window !== 'undefined') {
        fallbackListener = measure
        window.addEventListener('resize', measure, { passive: true })
      }
    },
    { immediate: true, flush: 'post' }
  )

  onScopeDispose(cleanup)

  return { width, mode, ready }
}
