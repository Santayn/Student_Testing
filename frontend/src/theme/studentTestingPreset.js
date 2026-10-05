import {
  definePreset,
} from '@primeuix/themes'
import Aura from '@primeuix/themes/aura'

/*
 * Academic Navy
 *
 * PrimeVue owns interactive component states while the application shell
 * keeps its own navy/slate tokens in tokens.css.  The primary scale is
 * intentionally restrained: one cold blue accent, no second brand color.
 */
const primary = {
  50: '#eff6ff',
  100: '#dbeafe',
  200: '#bfdbfe',
  300: '#93c5fd',
  400: '#60a5fa',
  500: '#3b82f6',
  600: '#2563eb',
  700: '#1d4ed8',
  800: '#1e40af',
  900: '#1e3a8a',
  950: '#172554',
}

const lightSurface = {
  0: '#ffffff',
  50: '#f8fafc',
  100: '#f1f5f9',
  200: '#e2e8f0',
  300: '#cbd5e1',
  400: '#94a3b8',
  500: '#64748b',
  600: '#475569',
  700: '#334155',
  800: '#1e293b',
  900: '#0f172a',
  950: '#020617',
}

const darkSurface = {
  0: '#ffffff',
  50: '#f8fafc',
  100: '#f1f5f9',
  200: '#e2e8f0',
  300: '#cbd5e1',
  400: '#94a3b8',
  500: '#64748b',
  600: '#475569',
  700: '#334155',
  800: '#1e293b',
  900: '#111827',
  950: '#0b1120',
}

const StudentTestingPreset =
  definePreset(Aura, {
    semantic: {
      primary,

      focusRing: {
        width: '2px',
        style: 'solid',
        color: '{primary.color}',
        offset: '2px',
      },

      colorScheme: {
        light: {
          surface: lightSurface,
        },

        dark: {
          surface: darkSurface,
        },
      },
    },
  })

export default StudentTestingPreset
