import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

const THEMES = ['system', 'light', 'dark']

export const useThemeStore = defineStore(
  'theme',
  () => {
    const theme = ref('system')
    const resolvedTheme = ref('light')
    const initialized = ref(false)

    let mediaQuery = null

    const isDark = computed(() => {
      return resolvedTheme.value === 'dark'
    })

    function getSystemTheme() {
      if (typeof window === 'undefined') {
        return 'light'
      }

      return window.matchMedia(
        '(prefers-color-scheme: dark)'
      ).matches
        ? 'dark'
        : 'light'
    }

    function resolveTheme() {
      return theme.value === 'system'
        ? getSystemTheme()
        : theme.value
    }

    function applyTheme() {
      const resolved = resolveTheme()
      resolvedTheme.value = resolved

      if (typeof document === 'undefined') {
        return
      }

      document.documentElement.dataset.theme = resolved
      document.documentElement.style.colorScheme = resolved
    }

    function setTheme(value) {
      if (!THEMES.includes(value)) {
        throw new Error(`Неизвестная тема: ${value}`)
      }

      theme.value = value
      applyTheme()
    }

    function toggleTheme() {
      setTheme(
        resolvedTheme.value === 'dark'
          ? 'light'
          : 'dark'
      )
    }

    function handleSystemThemeChange() {
      if (theme.value === 'system') {
        applyTheme()
      }
    }

    function init() {
      if (!THEMES.includes(theme.value)) {
        theme.value = 'system'
      }

      applyTheme()

      if (
        typeof window !== 'undefined' &&
        !mediaQuery
      ) {
        mediaQuery = window.matchMedia(
          '(prefers-color-scheme: dark)'
        )

        mediaQuery.addEventListener(
          'change',
          handleSystemThemeChange
        )
      }

      initialized.value = true
    }

    return {
      theme,
      resolvedTheme,
      initialized,
      isDark,
      setTheme,
      toggleTheme,
      init,
    }
  },
  {
    persist: {
      pick: ['theme'],
    },
  }
)
