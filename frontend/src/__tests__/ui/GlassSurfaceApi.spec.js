import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import {
  SURFACE_PRESETS,
  SHINE_PRESETS,
  SHINE_INTENSITIES,
  glassClasses,
  isGlassSurface,
  isShinePreset,
  isShineIntensity,
} from '@/config/glassPresets'
import UiCard from '@/components/ui/UiCard.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiDialog from '@/components/ui/UiDialog.vue'
import UiSelect from '@/components/ui/UiSelect.vue'

const stubs = { Button: true, Card: true, Dialog: true, Select: true }
const mountUi = (component, props = {}) => mount(component, {
  props,
  global: { stubs },
})

describe('Glass Surface API', () => {
  it('accepts only explicitly supported material, sweep and intensity', () => {
    expect(SURFACE_PRESETS).toEqual(['default', 'glass', 'frosted'])
    expect(SHINE_PRESETS).toEqual(['none', 'sweep'])
    expect(SHINE_INTENSITIES).toEqual(['subtle', 'normal', 'strong'])
    expect(isGlassSurface('glass')).toBe(true)
    expect(isGlassSurface('custom')).toBe(false)
    expect(isShinePreset('sweep')).toBe(true)
    expect(isShinePreset('pointer-follow')).toBe(false)
    expect(isShineIntensity('strong')).toBe(true)
    expect(isShineIntensity('unknown')).toBe(false)
  })

  it('does not opt into glass or animate when surface is default', () => {
    expect(glassClasses('default', 'sweep', 'strong')).toEqual([])
    expect(glassClasses('glass', 'none', 'normal')).toEqual([
      'st-glass-surface', 'st-glass-surface--glass',
    ])
    expect(glassClasses('frosted', 'sweep', 'subtle')).toEqual([
      'st-glass-surface', 'st-glass-surface--frosted',
      'st-glass-shine--sweep', 'st-glass-shine--subtle',
    ])
    expect(glassClasses('glass', 'invalid', 'normal')).not.toContain('st-glass-shine--sweep')
  })

  it('keeps cards and buttons unchanged unless opted in, then updates reactively', async () => {
    const card = mountUi(UiCard, { title: 'Предмет' })
    const button = mountUi(UiButton, { label: 'Сохранить', motion: 'press' })
    expect(card.classes()).not.toContain('st-glass-surface')
    expect(button.classes()).not.toContain('st-glass-surface')

    await card.setProps({ surface: 'glass', shine: 'sweep', shineIntensity: 'strong' })
    await button.setProps({ surface: 'frosted', shine: 'sweep', shineIntensity: 'subtle' })
    expect(card.classes()).toContain('st-glass-surface--glass')
    expect(card.classes()).toContain('st-glass-shine--strong')
    expect(button.classes()).toContain('st-glass-surface--frosted')
    expect(button.classes()).toContain('st-glass-shine--subtle')
    expect(button.classes()).toContain('st-motion-button--press')
    card.unmount()
    button.unmount()
  })

  it('supports button links, disables shine on disabled interaction, and preserves URL', () => {
    const link = mountUi(UiButton, {
      href: '/courses', surface: 'glass', shine: 'sweep', label: 'Курсы',
    })
    expect(link.element.tagName).toBe('A')
    expect(link.attributes('href')).toBe('/courses')
    expect(link.classes()).toContain('st-glass-shine--sweep')
    link.unmount()

    const disabledLink = mountUi(UiButton, {
      href: '/courses', disabled: true, surface: 'glass', shine: 'sweep',
    })
    expect(disabledLink.attributes('aria-disabled')).toBe('true')
    expect(disabledLink.attributes('tabindex')).toBe('-1')
    disabledLink.unmount()
  })

  it('supports glass material without sweep on dialogs and select triggers', () => {
    const dialog = mountUi(UiDialog, { surface: 'frosted' })
    const select = mountUi(UiSelect, { surface: 'glass' })
    expect(dialog.classes()).toContain('st-glass-surface--frosted')
    expect(select.findComponent({ name: 'Select' }).classes()).toContain('st-glass-surface--glass')
    expect(dialog.classes()).not.toContain('st-glass-shine--sweep')
    expect(select.findComponent({ name: 'Select' }).classes()).not.toContain('st-glass-shine--sweep')
    dialog.unmount()
    select.unmount()
  })
})
