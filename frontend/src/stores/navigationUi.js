import { ref } from 'vue'
import { defineStore } from 'pinia'

const SIDEBAR_VISIBLE_KEY = 'st:navigation:sidebar-visible'
const RAIL_VISIBLE_KEY = 'st:navigation:rail-visible'
const LEGACY_COLLAPSED_KEY = 'st:navigation:sidebar-collapsed'

function readBooleanPreference(key, fallback) {
  try {
    const stored = localStorage.getItem(key)
    if (stored === 'true') return true
    if (stored === 'false') return false
  } catch {
    // Storage can be unavailable without blocking navigation.
  }
  return fallback
}

function savePreference(key, value) {
  try {
    localStorage.setItem(key, String(value))
  } catch {
    // Preferences are optional; UI interaction must still work.
  }
}

export const useNavigationUiStore = defineStore('navigation-ui', () => {
  const sidebarVisible = ref(true)
  const railVisible = ref(true)
  let initialized = false

  function init() {
    if (initialized) return
    initialized = true

    // A user who had the old icon-only mode enabled starts with the new
    // fully hidden mode instead of unexpectedly reopening the large sidebar.
    const legacyVisible = !readBooleanPreference(LEGACY_COLLAPSED_KEY, false)
    sidebarVisible.value = readBooleanPreference(SIDEBAR_VISIBLE_KEY, legacyVisible)
    railVisible.value = readBooleanPreference(RAIL_VISIBLE_KEY, true)
  }

  function setSidebarVisible(visible) {
    sidebarVisible.value = Boolean(visible)
    savePreference(SIDEBAR_VISIBLE_KEY, sidebarVisible.value)
  }

  function setRailVisible(visible) {
    railVisible.value = Boolean(visible)
    savePreference(RAIL_VISIBLE_KEY, railVisible.value)
  }

  function hideSidebar() { setSidebarVisible(false) }
  function showSidebar() { setSidebarVisible(true) }
  function toggleSidebar() { setSidebarVisible(!sidebarVisible.value) }
  function hideRail() { setRailVisible(false) }
  function showRail() { setRailVisible(true) }
  function toggleRail() { setRailVisible(!railVisible.value) }

  return {
    sidebarVisible,
    railVisible,
    init,
    setSidebarVisible,
    setRailVisible,
    hideSidebar,
    showSidebar,
    toggleSidebar,
    hideRail,
    showRail,
    toggleRail,
  }
})
