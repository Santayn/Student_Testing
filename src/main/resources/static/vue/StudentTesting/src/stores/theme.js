import { defineStore } from 'pinia'

export const useThemeStore = defineStore('theme', {
  state: () => ({
    // light | dark | system
    theme: 'system',

    // Фактически применённая тема
    resolvedTheme: 'light',

    initialized: false,

    mediaQuery: null,
  }),

  getters: {
    isDark: (state) => state.resolvedTheme === 'dark',

    isLight: (state) => state.resolvedTheme === 'light',

    isSystem: (state) => state.theme === 'system',
  },

  actions: {
    /**
     * Установить тему.
     *
     * @param {'light'|'dark'|'system'} theme
     */
    setTheme(theme) {
      const allowedThemes = [
        'light',
        'dark',
        'system',
      ]

      if (!allowedThemes.includes(theme)) {
        console.warn(`Неизвестная тема: ${theme}`)
        return
      }

      this.theme = theme

      this.resolveTheme()
      this.applyTheme()
    },

    /**
     * Определяет реальную тему.
     *
     * system -> светлая/тёмная тема ОС.
     */
    resolveTheme() {
      if (this.theme === 'system') {
        const prefersDark = window.matchMedia(
          '(prefers-color-scheme: dark)'
        ).matches

        this.resolvedTheme = prefersDark
          ? 'dark'
          : 'light'

        return
      }

      this.resolvedTheme = this.theme
    },

    /**
     * Применяет тему к HTML.
     */
    applyTheme() {
      const html = document.documentElement

      html.setAttribute(
        'data-theme',
        this.resolvedTheme
      )

      html.style.colorScheme = this.resolvedTheme
    },

    /**
     * Переключение light <-> dark.
     *
     * Если сейчас system, переключаемся
     * на противоположную от текущей системной.
     */
    toggleTheme() {
      if (this.resolvedTheme === 'dark') {
        this.setTheme('light')
      } else {
        this.setTheme('dark')
      }
    },

    /**
     * Обработчик изменения системной темы.
     */
    handleSystemThemeChange(event) {
      if (this.theme !== 'system') {
        return
      }

      this.resolvedTheme = event.matches
        ? 'dark'
        : 'light'

      this.applyTheme()
    },

    /**
     * Инициализация ThemeStore.
     *
     * Вызывать один раз при запуске приложения.
     */
    init() {
      if (this.initialized) {
        return
      }

      this.mediaQuery = window.matchMedia(
        '(prefers-color-scheme: dark)'
      )

      this.resolveTheme()
      this.applyTheme()

      this.mediaQuery.addEventListener(
        'change',
        this.handleSystemThemeChange
      )

      this.initialized = true
    },
  },

  persist: {
    pick: ['theme'],
  },
})