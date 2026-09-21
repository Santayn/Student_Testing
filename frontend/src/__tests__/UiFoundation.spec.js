import {
  beforeEach,
  describe,
  expect,
  it,
} from 'vitest'
import {
  createPinia,
  setActivePinia,
} from 'pinia'

import StudentTestingPreset from '@/theme/studentTestingPreset'
import {
  devRoutes,
} from '@/router/routes/dev'
import {
  useThemeStore,
} from '@/stores/theme'

describe('UI foundation', () => {
  beforeEach(() => {
    setActivePinia(
      createPinia()
    )

    document.documentElement.removeAttribute(
      'data-theme'
    )
    document.documentElement.classList.remove(
      'app-dark'
    )
  })

  it('exposes the custom PrimeVue preset', () => {
    expect(StudentTestingPreset).toBeTruthy()
  })

  it('keeps dev preview routes public and isolated', () => {
    expect(devRoutes).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          path: '/ui-showcase',
          name: 'ui-showcase',
          meta: {
            public: true,
          },
        }),
        expect.objectContaining({
          path: '/primevue-preview',
          name: 'primevue-preview',
          meta: {
            public: true,
          },
        }),
      ])
    )
  })

  it('synchronizes the DOM selector used by PrimeVue dark mode', () => {
    const themeStore =
      useThemeStore()

    themeStore.setTheme('dark')

    expect(
      document.documentElement.dataset.theme
    ).toBe('dark')
    expect(
      document.documentElement.classList.contains(
        'app-dark'
      )
    ).toBe(true)
    expect(
      document.documentElement.style.colorScheme
    ).toBe('dark')

    themeStore.setTheme('light')

    expect(
      document.documentElement.dataset.theme
    ).toBe('light')
    expect(
      document.documentElement.classList.contains(
        'app-dark'
      )
    ).toBe(false)
    expect(
      document.documentElement.style.colorScheme
    ).toBe('light')
  })
})
