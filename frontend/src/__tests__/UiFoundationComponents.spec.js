import { describe, expect, it } from 'vitest'
import * as ui from '@/components/ui'

const expectedComponents = [
  'UiActionMenu',
  'UiAlert',
  'UiButton',
  'UiCard',
  'UiCheckbox',
  'UiDialog',
  'UiField',
  'UiInput',
  'UiRadio',
  'UiSearchInput',
  'UiSelect',
  'UiTable',
  'UiTag',
  'UiTextarea',
  'UiToastHost',
  'UiToolbar',
]

describe('UI foundation public surface', () => {
  it('exports the shared PrimeVue-backed controls through one project entrypoint', () => {
    for (const name of expectedComponents) {
      expect(ui[name], `${name} must be exported`).toBeTruthy()
    }
    expect(typeof ui.useUiToast).toBe('function')
  })
})
