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
  })

  it('exposes the custom PrimeVue preset', () => {
    expect(StudentTestingPreset).toBeTruthy()
  })

  it('keeps the showcase route isolated in dev routes', () => {
    expect(devRoutes).toHaveLength(1)
    expect(devRoutes[0]).toMatchObject({
      path: '/ui-showcase',
      name: 'ui-showcase',
      meta: {
        public: true,
      },
    })
  })

  it('synchronizes the DOM selector used by PrimeVue dark mode', () => {
    const themeStore =
      useThemeStore()

    themeStore.setTheme('dark')

    expect(
      document.documentElement.dataset.theme
    ).toBe('dark')

    themeStore.setTheme('light')

    expect(
      document.documentElement.dataset.theme
    ).toBe('light')
  })
})
