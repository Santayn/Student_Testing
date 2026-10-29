import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  resolvePresentationMode,
} from '@/config/presentationModes'

/**
 * Global viewport context only. Deliberately NOT persisted: a saved width
 * becomes stale as soon as the user rotates or resizes the window.
 * Local workspaces use usePresentationSpace() instead of this global width.
 */
export const useLayoutStore = defineStore('layout', () => {
  const width = ref(null)
  const height = ref(null)
  const initialized = ref(false)
  let listening = false

  const mode = computed(() =>
    resolvePresentationMode(width.value)
  )
  const isCompact = computed(() => mode.value === 'compact')
  const isMedium = computed(() => mode.value === 'medium')
  const isWide = computed(() => mode.value === 'wide')
  const isLandscape = computed(() =>
    width.value != null &&
    height.value != null &&
    width.value > height.value
  )

  function update() {
    if (typeof window === 'undefined') return

    // innerWidth is a layout viewport measure. It does not collapse merely
    // because a virtual keyboard opens (unlike visualViewport.height).
    width.value = window.innerWidth
    height.value = window.innerHeight
    initialized.value = true
  }

  function init() {
    if (typeof window === 'undefined') return

    update()
    if (!listening) {
      window.addEventListener('resize', update, { passive: true })
      listening = true
    }
  }

  function dispose() {
    if (typeof window !== 'undefined' && listening) {
      window.removeEventListener('resize', update)
    }
    listening = false
  }

  return {
    width,
    height,
    initialized,
    mode,
    isCompact,
    isMedium,
    isWide,
    isLandscape,
    init,
    dispose,
  }
})
