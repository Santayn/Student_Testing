import { ref, onMounted, onBeforeUnmount, watch } from 'vue'

// Hysteresis prevents the bottom bar from flickering for minor finger movements.
export function useScrollNavigation(route) {
  const visible = ref(true)
  let previous = 0
  let distance = 0
  let direction = 0
  let scheduled = false
  let frameId = null

  function sample() {
    scheduled = false
    frameId = null
    const y = Math.max(0, window.scrollY || 0)
    const delta = y - previous
    previous = y

    if (y < 40) {
      visible.value = true
      distance = 0
      direction = 0
      return
    }
    if (Math.abs(delta) < 2) return

    const nextDirection = Math.sign(delta)
    if (direction !== nextDirection) distance = 0
    direction = nextDirection
    distance += Math.abs(delta)

    // Keep navigation visible while an input or an accessible modal is active.
    const active = document.activeElement
    if (
      active?.matches?.('input, textarea, select, [contenteditable="true"]') ||
      document.querySelector('[role="dialog"][aria-modal="true"]')
    ) {
      visible.value = true
      return
    }

    if (direction > 0 && distance >= 76) {
      visible.value = false
      distance = 0
    } else if (direction < 0 && distance >= 24) {
      visible.value = true
      distance = 0
    }
  }

  function scrollListener() {
    if (scheduled) return
    scheduled = true
    frameId = requestAnimationFrame(sample)
  }

  watch(() => route.fullPath, () => {
    visible.value = true
    distance = 0
    direction = 0
    if (typeof window !== 'undefined') previous = window.scrollY || 0
  })

  onMounted(() => {
    previous = window.scrollY || 0
    window.addEventListener('scroll', scrollListener, { passive: true })
  })
  onBeforeUnmount(() => {
    window.removeEventListener('scroll', scrollListener)
    if (frameId !== null) cancelAnimationFrame(frameId)
  })

  return { visible }
}
