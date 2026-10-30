import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'

import { isMotionPresetFor, motionClassFor } from '@/config/motionPresets'
import UiTable from '@/components/ui/UiTable.vue'
import UiField from '@/components/ui/UiField.vue'
import UiInput from '@/components/ui/UiInput.vue'
import UiTextarea from '@/components/ui/UiTextarea.vue'
import UiLoadingState from '@/components/ui/UiLoadingState.vue'
import UiToastHost from '@/components/ui/UiToastHost.vue'
import UiDisclosure from '@/components/ui/UiDisclosure.vue'

const stubs = {
  DataTable: true,
  Column: true,
  InputText: true,
  Textarea: true,
  Toast: true,
}

function render(component, props = {}, slots = {}) {
  return mount(component, { props, slots, global: { stubs } })
}

describe('Complex UI Motion API', () => {
  it('whitelists only motion supported by the target component', () => {
    for (const [target, accepted, rejected] of [
      ['table', 'row-highlight', 'press'],
      ['field', 'error-reveal', 'scale-fade'],
      ['loading', 'soft-pulse', 'focused'],
      ['toast', 'fade-slide', 'lift'],
      ['disclosure', 'expand', 'hover'],
    ]) {
      expect(isMotionPresetFor(target, accepted)).toBe(true)
      expect(isMotionPresetFor(target, rejected)).toBe(false)
      expect(motionClassFor(target, accepted)).toBe(`st-motion-${target}--${accepted}`)
      expect(motionClassFor(target, 'default')).toBe('')
    }
  })

  it('keeps sorting and rows under PrimeVue ownership and only styles its scroll shell', async () => {
    const wrapper = render(UiTable, { columns: [{ key: 'name', label: 'Имя' }], rows: [{ id: 1, name: 'A' }] })
    expect(wrapper.classes()).toContain('st-ui-table-scroll')
    expect(wrapper.classes()).not.toContain('st-motion-table--row-highlight')
    await wrapper.setProps({ motion: 'row-highlight' })
    expect(wrapper.classes()).toContain('st-motion-table--row-highlight')
    expect(wrapper.findComponent({ name: 'DataTable' }).exists()).toBe(true)
    wrapper.unmount()
  })

  it('animates field feedback while preserving validation semantics', async () => {
    const field = render(UiField, { id: 'x', label: 'Имя', error: 'Обязательно', motion: 'error-reveal' }, {
      default: '<input id="x" />',
    })
    expect(field.classes()).toContain('st-motion-field--error-reveal')
    expect(field.find('#x-error').text()).toBe('Обязательно')
    await field.setProps({ error: '' })
    expect(field.find('#x-error').exists()).toBe(false)
    field.unmount()

    for (const component of [UiInput, UiTextarea]) {
      const control = render(component, { id: 'sample', motion: 'focused' })
      expect(control.findComponent(UiField).props('motion')).toBe('focused')
      control.unmount()
    }
  })

  it('keeps loading status and toast host contract unchanged', () => {
    const loading = render(UiLoadingState, { motion: 'soft-pulse' })
    expect(loading.classes()).toContain('st-motion-loading--soft-pulse')
    expect(loading.attributes('role')).toBe('status')
    expect(loading.attributes('aria-busy')).toBe('true')
    loading.unmount()
    const toast = render(UiToastHost, { motion: 'fade-slide', position: 'bottom-right' })
    expect(toast.classes()).toContain('st-motion-toast--fade-slide')
    expect(toast.findComponent({ name: 'Toast' }).props('position')).toBe('bottom-right')
    toast.unmount()
  })

  it('uses native buttons and keeps disclosure contents mounted while collapsed', async () => {
    const wrapper = render(UiDisclosure, { title: 'Дополнительно' }, {
      default: '<input data-test="draft" value="сохранено" />',
    })
    expect(wrapper.find('button').attributes('aria-expanded')).toBe('false')
    const content = wrapper.find('.st-ui-disclosure__content')
    expect(content.exists()).toBe(true)
    const input = wrapper.find('[data-test="draft"]')
    const inputNode = input.element
    await wrapper.find('button').trigger('click')
    await nextTick()
    expect(wrapper.find('button').attributes('aria-expanded')).toBe('true')
    await wrapper.find('button').trigger('click')
    await nextTick()
    expect(wrapper.find('[data-test="draft"]').element).toBe(inputNode)
    wrapper.unmount()
  })

  it('does not expand a disabled disclosure', async () => {
    const wrapper = render(UiDisclosure, { title: 'Недоступно', disabled: true })
    await wrapper.find('button').trigger('click')
    expect(wrapper.find('button').attributes('aria-expanded')).toBe('false')
    wrapper.unmount()
  })
})
