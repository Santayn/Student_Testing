import { defineComponent, h } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import UiCheckbox from '@/components/ui/UiCheckbox.vue'
import UiRadio from '@/components/ui/UiRadio.vue'

const CheckboxContractStub = defineComponent({
  name: 'Checkbox',
  props: {
    inputId: { type: String, default: '' },
    modelValue: { type: null, default: false },
    value: { type: null, default: undefined },
    binary: { type: Boolean, default: false },
    trueValue: { type: null, default: true },
    falseValue: { type: null, default: false },
    disabled: { type: Boolean, default: false },
    readonly: { type: Boolean, default: false },
    required: { type: Boolean, default: false },
    invalid: { type: Boolean, default: false },
    indeterminate: { type: Boolean, default: false },
    pt: { type: Object, default: undefined },
  },
  emits: [
    'update:modelValue',
    'update:indeterminate',
    'change',
    'focus',
    'blur',
  ],
  setup(props) {
    return () => h('input', {
      id: props.inputId,
      type: 'checkbox',
    })
  },
})

const RadioContractStub = defineComponent({
  name: 'RadioButton',
  props: {
    inputId: { type: String, default: '' },
    modelValue: { type: null, default: null },
    value: { type: null, default: undefined },
    name: { type: String, default: '' },
    disabled: { type: Boolean, default: false },
    readonly: { type: Boolean, default: false },
    invalid: { type: Boolean, default: false },
    pt: { type: Object, default: undefined },
  },
  emits: [
    'update:modelValue',
    'change',
    'focus',
    'blur',
  ],
  setup(props) {
    return () => h('input', {
      id: props.inputId,
      type: 'radio',
      name: props.name,
    })
  },
})

function mountChoice(component, props) {
  return mount(component, {
    props,
    global: {
      stubs: {
        Checkbox: CheckboxContractStub,
        RadioButton: RadioContractStub,
      },
    },
  })
}

describe('Ui choice control contracts', () => {
  it('uses explicit binary checkbox mode by default', async () => {
    const wrapper = mountChoice(UiCheckbox, {
      modelValue: false,
      label: 'Уведомления',
      description: 'Описание',
    })

    const control = wrapper.findComponent(CheckboxContractStub)

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
    const wrapper = mountChoice(UiCheckbox, {
      modelValue: [],
      mode: 'multiple',
      value: 17,
      label: 'Предмет',
    })

    const control = wrapper.findComponent(CheckboxContractStub)

    expect(control.props('binary')).toBe(false)
    expect(control.props('value')).toBe(17)

    control.vm.$emit('update:modelValue', [17])
    await wrapper.vm.$nextTick()

    expect(wrapper.emitted('update:modelValue')).toEqual([[[17]]])
  })

  it('keeps the entire radio row associated with one native input and forwards one change event', async () => {
    const wrapper = mountChoice(UiRadio, {
      modelValue: 'student',
      value: 'teacher',
      name: 'role',
      label: 'Преподаватель',
      description: 'Описание роли',
    })

    const control = wrapper.findComponent(RadioContractStub)
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
    const checkbox = mountChoice(UiCheckbox, {
      modelValue: false,
      label: 'Состояние',
      readonly: true,
      invalid: true,
      indeterminate: true,
    })
    const radio = mountChoice(UiRadio, {
      modelValue: null,
      value: 'x',
      name: 'state',
      label: 'Состояние',
      readonly: true,
      invalid: true,
    })

    const checkboxControl = checkbox.findComponent(CheckboxContractStub)
    const radioControl = radio.findComponent(RadioContractStub)

    expect(checkboxControl.props('readonly')).toBe(true)
    expect(checkboxControl.props('invalid')).toBe(true)
    expect(checkboxControl.props('indeterminate')).toBe(true)
    expect(checkbox.classes()).toContain('st-ui-choice--readonly')
    expect(checkbox.classes()).toContain('st-ui-choice--invalid')

    expect(radioControl.props('readonly')).toBe(true)
    expect(radioControl.props('invalid')).toBe(true)
    expect(radio.classes()).toContain('st-ui-choice--readonly')
    expect(radio.classes()).toContain('st-ui-choice--invalid')
  })
})
