import { defineComponent, h, nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'

import {
  MOTION_PRESETS,
  isMotionPresetFor,
  motionClassFor,
  motionTransitionName,
} from '@/config/motionPresets'
import { useMotionPreferences } from '@/composables/motion/useMotionPreferences'
import UiButton from '@/components/ui/UiButton.vue'
import UiCard from '@/components/ui/UiCard.vue'
import UiAlert from '@/components/ui/UiAlert.vue'
import UiDialog from '@/components/ui/UiDialog.vue'
import UiSelect from '@/components/ui/UiSelect.vue'
import UiMotion from '@/components/ui/UiMotion.vue'

const componentStubs = {
  Button: true,
  Card: true,
  Message: true,
  Dialog: true,
  Select: true,
}

function mountUi(component, props = {}, options = {}) {
  return mount(component, {
    props,
    global: { stubs: componentStubs },
    ...options,
  })
}

describe('Motion API registry', () => {
  it('only accepts explicitly whitelisted per-component effects', () => {
    expect(isMotionPresetFor('button', 'press')).toBe(true)
    expect(isMotionPresetFor('card', 'press')).toBe(false)
    expect(isMotionPresetFor('dialog', 'scale-fade')).toBe(true)
    expect(isMotionPresetFor('dialog', 'lift')).toBe(false)
    expect(isMotionPresetFor('unknown', 'press')).toBe(false)
    expect(MOTION_PRESETS.transition).toContain('expand')
    for (const surface of ['button', 'card', 'select']) {
      expect(isMotionPresetFor(surface, 'hover')).toBe(true)
      expect(isMotionPresetFor(surface, 'focused')).toBe(true)
    }
    expect(isMotionPresetFor('dialog', 'hover')).toBe(false)
    expect(isMotionPresetFor('alert', 'focused')).toBe(false)
    expect(isMotionPresetFor('select', 'focus')).toBe(true) // backward compatible
  })

  it('does not generate CSS classes from invalid or default values', () => {
    expect(motionClassFor('button', 'default')).toBe('')
    expect(motionClassFor('button', 'press')).toBe('st-motion-button--press')
    expect(motionClassFor('button', 'none')).toBe('st-motion-button--none')
    expect(motionClassFor('button', 'focused')).toBe('st-motion-button--focused')
    expect(motionClassFor('card', 'hover')).toBe('st-motion-card--hover')
    expect(motionClassFor('button', 'some-css-class')).toBe('')
    expect(motionTransitionName('fade-slide')).toBe('st-motion-fade-slide')
    expect(motionTransitionName('none')).toBe('')
    expect(motionTransitionName('unexpected')).toBe('')
  })
})

describe('Ui* motion prop integration', () => {
  it('preserves existing default behavior and exposes an opt-in button class', async () => {
    const wrapper = mountUi(UiButton, { label: 'Сохранить' })
    expect(wrapper.classes()).not.toContain('st-motion-button--press')
    await wrapper.setProps({ motion: 'press' })
    expect(wrapper.classes()).toContain('st-motion-button--press')
    await wrapper.setProps({ motion: 'focused' })
    expect(wrapper.classes()).toContain('st-motion-button--focused')
    await wrapper.setProps({ motion: 'none', disabled: true })
    expect(wrapper.classes()).toContain('st-motion-button--none')
    wrapper.unmount()

    const link = mountUi(UiButton, { href: '/materials', motion: 'press' })
    expect(link.element.tagName).toBe('A')
    expect(link.classes()).toContain('st-motion-button--press')
    link.unmount()
  })

  it('sets motion classes on card, alert and dialog without replacing PrimeVue components', () => {
    const card = mountUi(UiCard, { motion: 'lift' })
    const alert = mountUi(UiAlert, { motion: 'fade-slide', message: 'Готово' })
    const dialog = mountUi(UiDialog, { motion: 'scale-fade', modelValue: false })

    expect(card.classes()).toContain('st-motion-card--lift')
    expect(alert.classes()).toContain('st-motion-alert--fade-slide')
    expect(dialog.classes()).toContain('st-motion-dialog--scale-fade')

    card.unmount()
    alert.unmount()
    dialog.unmount()
  })

  it('keeps the select popup anchor stable with focus, hover and focused presets', async () => {
    const wrapper = mountUi(UiSelect, { motion: 'focus' })
    expect(wrapper.findComponent({ name: 'Select' }).classes()).toContain('st-motion-select--focus')
    await wrapper.setProps({ motion: 'hover' })
    expect(wrapper.findComponent({ name: 'Select' }).classes()).toContain('st-motion-select--hover')
    await wrapper.setProps({ motion: 'focused' })
    expect(wrapper.findComponent({ name: 'Select' }).classes()).toContain('st-motion-select--focused')
    wrapper.unmount()
  })
})

describe('UiMotion visibility', () => {
  it('switches conditional content without replacing the surrounding view', async () => {
    const wrapper = mount(UiMotion, {
      props: { show: true, preset: 'fade-slide' },
      slots: { default: '<span>Уведомление</span>' },
      global: { stubs: { Transition: true } },
    })
    expect(wrapper.find('.st-ui-motion span').text()).toBe('Уведомление')
    await wrapper.setProps({ show: false })
    expect(wrapper.find('.st-ui-motion').exists()).toBe(false)
    await wrapper.setProps({ show: true, preset: 'expand' })
    expect(wrapper.find('.st-ui-motion--expand .st-ui-motion__inner').exists()).toBe(true)
    wrapper.unmount()
  })
})

describe('useMotionPreferences', () => {
  const originalMatchMedia = window.matchMedia

  afterEach(() => {
    Object.defineProperty(window, 'matchMedia', { configurable: true, value: originalMatchMedia })
  })

  it('updates on OS preference changes and removes its listener on unmount', async () => {
    let listener
    const addEventListener = vi.fn((_name, callback) => { listener = callback })
    const removeEventListener = vi.fn()
    Object.defineProperty(window, 'matchMedia', {
      configurable: true,
      value: vi.fn(() => ({ matches: true, addEventListener, removeEventListener })),
    })

    const Harness = defineComponent({
      setup() { return useMotionPreferences() },
      render() { return h('div', String(this.prefersReducedMotion)) },
    })
    const wrapper = mount(Harness)
    expect(wrapper.text()).toBe('true')
    expect(addEventListener).toHaveBeenCalledWith('change', expect.any(Function))
    listener({ matches: false })
    await nextTick()
    expect(wrapper.text()).toBe('false')
    wrapper.unmount()
    expect(removeEventListener).toHaveBeenCalledWith('change', listener)
  })
})
