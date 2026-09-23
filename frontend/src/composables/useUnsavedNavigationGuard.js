import {
  onBeforeUnmount,
  onMounted,
  ref,
  unref,
} from 'vue'

import {
  onBeforeRouteLeave,
} from 'vue-router'

export function useUnsavedNavigationGuard(isDirty) {
  const confirmVisible = ref(false)

  let pendingDecision = null

  function hasUnsavedChanges() {
    return Boolean(unref(isDirty))
  }

  function settlePendingDecision(allowNavigation) {
    if (!pendingDecision) {
      confirmVisible.value = false
      return false
    }

    const resolve = pendingDecision
    pendingDecision = null
    confirmVisible.value = false
    resolve(Boolean(allowNavigation))
    return true
  }

  function requestNavigationDecision() {
    if (pendingDecision) {
      settlePendingDecision(false)
    }

    confirmVisible.value = true

    return new Promise((resolve) => {
      pendingDecision = resolve
    })
  }

  function continueEditing() {
    settlePendingDecision(false)
  }

  function discardAndNavigate() {
    settlePendingDecision(true)
  }

  function handleBeforeUnload(event) {
    if (!hasUnsavedChanges()) {
      return
    }

    event.preventDefault()
    event.returnValue = ''
  }

  onBeforeRouteLeave(() => {
    if (!hasUnsavedChanges()) {
      return true
    }

    return requestNavigationDecision()
  })

  onMounted(() => {
    window.addEventListener(
      'beforeunload',
      handleBeforeUnload
    )
  })

  onBeforeUnmount(() => {
    window.removeEventListener(
      'beforeunload',
      handleBeforeUnload
    )

    settlePendingDecision(false)
  })

  return {
    confirmVisible,
    continueEditing,
    discardAndNavigate,
  }
}
