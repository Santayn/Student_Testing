import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import Checkbox from 'primevue/checkbox'
import RadioButton from 'primevue/radiobutton'

import UiCheckbox from '@/components/ui/UiCheckbox.vue'
import UiRadio from '@/components/ui/UiRadio.vue'

describe('Ui choice control contracts', () => {
  it('uses explicit binary checkbox mode by default', async () => {
    const wrapper = mount(UiCheckbox, {
      props: {
        modelValue: false,
        label: 'Уведомления',
        description: 'Описание',
      },
    })

    const control = wrapper.findComponent(Checkbox)

    expect(wrapper.element.tagName).toBe('LABEL')
    expect(wrapper.classes()).toContain('st-ui-checkbox')
    expect(wrapper.classes()).toContain('ui-checkbox')
    expect(control.props('binary')).toBe(true)
    expect(wrapper.attributes('for')).toBe(control.props('inputId'))

    control.vm.$emit('update:modelValue', true)
    await wrapper.vm.$nextTick()

    expect(wrapper.emitted('update:modelValue')).toEqual([[true]])
  })

  it('uses explicit multiple checkbox mode without inferring it from modelValue', async () => {
    const wrapper = mount(UiCheckbox, {
      props: {
        modelValue: [],
        mode: 'multiple',
        value: 17,
        label: 'Предмет',
      },
    })

    const control = wrapper.findComponent(Checkbox)

    expect(control.props('binary')).toBe(false)
    expect(control.props('value')).toBe(17)

    control.vm.$emit('update:modelValue', [17])
    await wrapper.vm.$nextTick()

    expect(wrapper.emitted('update:modelValue')).toEqual([[[17]]])
  })

  it('keeps the entire radio row associated with one native input and forwards one change event', async () => {
    const wrapper = mount(UiRadio, {
      props: {
        modelValue: 'student',
        value: 'teacher',
        name: 'role',
        label: 'Преподаватель',
        description: 'Описание роли',
      },
    })

    const control = wrapper.findComponent(RadioButton)
    const event = new Event('change')

    expect(wrapper.element.tagName).toBe('LABEL')
    expect(wrapper.classes()).toContain('st-ui-radio')
    expect(wrapper.classes()).toContain('ui-radio')
    expect(wrapper.attributes('for')).toBe(control.props('inputId'))
    expect(control.props('name')).toBe('role')

    control.vm.$emit('update:modelValue', 'teacher')
    control.vm.$emit('change', event)
    await wrapper.vm.$nextTick()

    expect(wrapper.emitted('update:modelValue')).toEqual([['teacher']])
    expect(wrapper.emitted('change')).toHaveLength(1)
    expect(wrapper.emitted('change')[0][0]).toBe(event)
  })

  it('forwards readonly, invalid and checkbox indeterminate states to PrimeVue', () => {
    const checkbox = mount(UiCheckbox, {
      props: {
        modelValue: false,
        label: 'Состояние',
        readonly: true,
        invalid: true,
        indeterminate: true,
      },
    })
    const radio = mount(UiRadio, {
      props: {
        modelValue: null,
        value: 'x',
        name: 'state',
        label: 'Состояние',
        readonly: true,
        invalid: true,
      },
    })

    expect(checkbox.findComponent(Checkbox).props('readonly')).toBe(true)
    expect(checkbox.findComponent(Checkbox).props('invalid')).toBe(true)
    expect(checkbox.findComponent(Checkbox).props('indeterminate')).toBe(true)
    expect(checkbox.classes()).toContain('st-ui-choice--readonly')
    expect(checkbox.classes()).toContain('st-ui-choice--invalid')

    expect(radio.findComponent(RadioButton).props('readonly')).toBe(true)
    expect(radio.findComponent(RadioButton).props('invalid')).toBe(true)
    expect(radio.classes()).toContain('st-ui-choice--readonly')
    expect(radio.classes()).toContain('st-ui-choice--invalid')
  })
})
