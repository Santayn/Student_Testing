import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'

import {
  MOTION_PRESETS,
  STANDARD_MOTION_PRESETS,
  isMotionPresetFor,
  motionClassFor,
  standardMotionFor,
} from '@/config/motionPresets'
import UiButton from '@/components/ui/UiButton.vue'
import UiCard from '@/components/ui/UiCard.vue'
import UiAlert from '@/components/ui/UiAlert.vue'
import UiDialog from '@/components/ui/UiDialog.vue'
import UiSelect from '@/components/ui/UiSelect.vue'
import UiField from '@/components/ui/UiField.vue'
import UiInput from '@/components/ui/UiInput.vue'
import UiTextarea from '@/components/ui/UiTextarea.vue'

const stubs = {
  Button: true,
  Card: true,
  Message: true,
  Dialog: true,
  Select: true,
  InputText: true,
  Textarea: true,
}

function render(component, props = {}, slots = {}) {
  return mount(component, { props, slots, global: { stubs } })
}

describe('Standard Motion API defaults', () => {
  it('uses valid, centrally declared presets only for foundation components', () => {
    expect(Object.keys(STANDARD_MOTION_PRESETS).sort()).toEqual([
      'alert', 'button', 'card', 'dialog', 'field', 'select',
    ])
    for (const [target, preset] of Object.entries(STANDARD_MOTION_PRESETS)) {
      expect(MOTION_PRESETS[target]).toContain(preset)
      expect(isMotionPresetFor(target, preset)).toBe(true)
      expect(standardMotionFor(target)).toBe(preset)
      expect(motionClassFor(target, preset)).toBe(`st-motion-${target}--${preset}`)
    }
    expect(standardMotionFor('unknown')).toBe('default')
    expect(standardMotionFor('table')).toBe('default')
    expect(motionClassFor('button', 'default')).toBe('')
  })

  it('gives button and RouterLink-mode controls press feedback by default', async () => {
    const button = render(UiButton, { label: 'Сохранить' })
    expect(button.classes()).toContain('st-motion-button--press')
    await button.setProps({ motion: 'none' })
    expect(button.classes()).toContain('st-motion-button--none')
    await button.setProps({ motion: 'default' })
    expect(button.classes().some((name) => name.startsWith('st-motion-button--'))).toBe(false)
    await button.setProps({ motion: 'hover' })
    expect(button.classes()).toContain('st-motion-button--hover')
    button.unmount()

    const link = render(UiButton, { href: '/resources', label: 'Перейти' })
    expect(link.element.tagName).toBe('A')
    expect(link.classes()).toContain('st-motion-button--press')
    link.unmount()

    const disabled = render(UiButton, { href: '/resources', disabled: true })
    expect(disabled.attributes('aria-disabled')).toBe('true')
    expect(disabled.classes()).toContain('st-motion-button--press')
    disabled.unmount()
  })

  it('keeps cards stationary by default and does not opt into glass', () => {
    const card = render(UiCard, { title: 'Предмет' })
    expect(card.classes()).toContain('st-motion-card--focused')
    expect(card.classes()).not.toContain('st-motion-card--hover')
    expect(card.classes().some((name) => name.startsWith('st-glass-'))).toBe(false)
    card.unmount()
  })

  it('animates alert entry and dialog surface while preserving dialog props', async () => {
    const alert = render(UiAlert, { message: 'Готово' })
    expect(alert.classes()).toContain('st-motion-alert--fade')
    alert.unmount()

    const dialog = render(UiDialog, { title: 'Изменение', modelValue: false })
    expect(dialog.classes()).toContain('st-motion-dialog--scale-fade')
    expect(dialog.findComponent({ name: 'Dialog' }).props('modal')).toBe(true)
    await dialog.setProps({ motion: 'none' })
    expect(dialog.classes()).toContain('st-motion-dialog--none')
    dialog.unmount()
  })

  it('uses focus-only defaults for Select and text fields without shifting input DOM', () => {
    const select = render(UiSelect)
    expect(select.findComponent({ name: 'Select' }).classes()).toContain('st-motion-select--focus')
    select.unmount()

    const field = render(UiField, { id: 'subject', label: 'Предмет', error: 'Заполните поле' }, {
      default: '<input id="subject" />',
    })
    expect(field.classes()).toContain('st-motion-field--focused')
    expect(field.find('#subject-error').text()).toBe('Заполните поле')
    field.unmount()

    for (const component of [UiInput, UiTextarea]) {
      const control = render(component, { id: 'title', label: 'Название' })
      expect(control.findComponent(UiField).props('motion')).toBe('focused')
      expect(control.findComponent(UiField).classes()).toContain('st-motion-field--focused')
      control.unmount()
    }
  })
})
