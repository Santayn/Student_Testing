import {
  defineComponent,
  h,
  nextTick,
} from 'vue'
import { mount } from '@vue/test-utils'
import {
  describe,
  expect,
  it,
} from 'vitest'

import AdminRolePermissionsDrawer from '@/components/admin/AdminRolePermissionsDrawer.vue'

const UiDrawerStub = defineComponent({
  name: 'UiDrawer',
  props: {
    modelValue: { type: Boolean, default: false },
    title: { type: String, default: '' },
  },
  emits: ['update:modelValue'],
  setup(props, { slots }) {
    return () => props.modelValue
      ? h('aside', [
          h('h2', props.title),
          slots.default?.(),
          slots.footer?.(),
        ])
      : null
  },
})

const UiButtonStub = defineComponent({
  name: 'UiButton',
  props: {
    label: { type: String, default: '' },
    disabled: { type: Boolean, default: false },
    loading: { type: Boolean, default: false },
  },
  emits: ['click'],
  setup(props, { emit }) {
    return () => h('button', {
      disabled: props.disabled || props.loading,
      onClick: (event) => emit('click', event),
    }, props.label)
  },
})

const UiFilterBarStub = defineComponent({
  name: 'UiFilterBar',
  props: {
    modelValue: { type: String, default: '' },
    searchPlaceholder: { type: String, default: '' },
  },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    return () => h('input', {
      'aria-label': props.searchPlaceholder,
      value: props.modelValue,
      onInput: (event) => emit('update:modelValue', event.target.value),
    })
  },
})

const UiCheckboxStub = defineComponent({
  name: 'UiCheckbox',
  props: {
    modelValue: { type: Array, default: () => [] },
    value: { type: Number, required: true },
    label: { type: String, default: '' },
    disabled: { type: Boolean, default: false },
  },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    return () => h('label', [
      h('input', {
        type: 'checkbox',
        'aria-label': props.label,
        disabled: props.disabled,
        checked: props.modelValue.includes(props.value),
        onChange: (event) => {
          const next = event.target.checked
            ? [...props.modelValue, props.value]
            : props.modelValue.filter((value) => value !== props.value)
          emit('update:modelValue', next)
        },
      }),
      props.label,
    ])
  },
})

const SimpleStub = defineComponent({
  props: {
    message: { type: String, default: '' },
    description: { type: String, default: '' },
  },
  setup(props) {
    return () => h('div', `${props.message}${props.description}`)
  },
})

const UiUnsavedChangesConfirmStub = defineComponent({
  name: 'UiUnsavedChangesConfirm',
  props: {
    modelValue: { type: Boolean, default: false },
  },
  emits: ['update:modelValue', 'discard', 'continue'],
  setup(props, { emit }) {
    return () => props.modelValue
      ? h('div', [
          h('button', { onClick: () => emit('continue') }, 'Продолжить'),
          h('button', { onClick: () => emit('discard') }, 'Отбросить'),
        ])
      : null
  },
})

const global = {
  stubs: {
    UiDrawer: UiDrawerStub,
    UiButton: UiButtonStub,
    UiFilterBar: UiFilterBarStub,
    UiCheckbox: UiCheckboxStub,
    UiAlert: SimpleStub,
    UiEmptyState: SimpleStub,
    UiUnsavedChangesConfirm: UiUnsavedChangesConfirmStub,
  },
}

function button(wrapper, label) {
  return wrapper.findAll('button').find(
    (entry) => entry.text().includes(label)
  )
}

describe('AdminRolePermissionsDrawer', () => {
  it('forwards search, permission changes and save/close actions', async () => {
    const wrapper = mount(AdminRolePermissionsDrawer, {
      props: {
        modelValue: true,
        role: { id: 1, name: 'Администратор' },
        permissions: [
          { id: 10, name: 'users.read', description: 'Чтение' },
          { id: 20, name: 'users.write', description: 'Изменение' },
        ],
        filteredPermissions: [
          { id: 10, name: 'users.read', description: 'Чтение' },
          { id: 20, name: 'users.write', description: 'Изменение' },
        ],
        permissionIds: [10],
        dirty: true,
      },
      global,
    })

    expect(wrapper.text()).toContain('Права роли «Администратор»')

    await wrapper.find('input[aria-label="Найти permission"]').setValue('write')
    await wrapper.find('input[aria-label="users.write"]').setValue(true)
    await button(wrapper, 'Сохранить права').trigger('click')
    await button(wrapper, 'Закрыть').trigger('click')

    expect(wrapper.emitted('update:search')).toEqual([['write']])
    expect(wrapper.emitted('update:permissionIds')).toEqual([[[10, 20]]])
    expect(wrapper.emitted('save')).toHaveLength(1)
    expect(wrapper.emitted('request-close')).toHaveLength(1)

    wrapper.findComponent(UiDrawerStub).vm.$emit('update:modelValue', false)
    await nextTick()
    expect(wrapper.emitted('update:modelValue')).toEqual([[false]])
  })

  it('shows unsaved confirmation and forwards the decision', async () => {
    const wrapper = mount(AdminRolePermissionsDrawer, {
      props: {
        modelValue: true,
        confirmCloseVisible: true,
        permissions: [],
        filteredPermissions: [],
        permissionIds: [],
      },
      global,
    })

    await button(wrapper, 'Продолжить').trigger('click')
    await button(wrapper, 'Отбросить').trigger('click')

    expect(wrapper.emitted('continue')).toHaveLength(1)
    expect(wrapper.emitted('discard')).toHaveLength(1)
  })
})
