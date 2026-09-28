import {
  defineComponent,
  h,
} from 'vue'
import { mount } from '@vue/test-utils'
import {
  describe,
  expect,
  it,
} from 'vitest'

import AdminGroupMembersDrawer from '@/components/admin/AdminGroupMembersDrawer.vue'

const UiDrawerStub = defineComponent({
  name: 'UiDrawer',
  props: {
    modelValue: { type: Boolean, default: false },
    title: { type: String, default: '' },
  },
  emits: ['update:modelValue', 'after-hide'],
  setup(props, { slots }) {
    return () => props.modelValue
      ? h('aside', [h('h2', props.title), slots.default?.()])
      : null
  },
})

const UiDialogStub = defineComponent({
  name: 'UiDialog',
  props: {
    modelValue: { type: Boolean, default: false },
    title: { type: String, default: '' },
  },
  emits: ['update:modelValue'],
  setup(props, { slots }) {
    return () => props.modelValue
      ? h('section', [h('h3', props.title), slots.default?.(), slots.footer?.()])
      : null
  },
})

const UiFilterBarStub = defineComponent({
  name: 'UiFilterBar',
  props: {
    modelValue: { type: String, default: '' },
  },
  emits: ['update:modelValue', 'reset'],
  setup(props) {
    return () => h('div', { 'data-search': props.modelValue })
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

const SimpleStub = defineComponent({
  props: {
    message: { type: String, default: '' },
  },
  emits: ['close'],
  setup(props, { slots }) {
    return () => h('div', [props.message, slots.default?.()])
  },
})

const global = {
  stubs: {
    UiDrawer: UiDrawerStub,
    UiDialog: UiDialogStub,
    UiFilterBar: UiFilterBarStub,
    UiButton: UiButtonStub,
    UiAlert: SimpleStub,
    UiEmptyState: SimpleStub,
  },
}

function personById(id) {
  return {
    id,
    firstName: id === 1 ? 'Анна' : 'Борис',
    lastName: id === 1 ? 'Иванова' : 'Петров',
    email: `user${id}@example.com`,
  }
}

function personName(person) {
  return `${person?.lastName ?? ''} ${person?.firstName ?? ''}`.trim()
}

function personContact(person) {
  return person?.email ?? ''
}

function baseProps(overrides = {}) {
  return {
    modelValue: true,
    title: 'Состав группы KB-01',
    notice: { type: 'info', message: '' },
    search: '',
    resultText: 'В группе: 1; доступно: 1',
    currentMemberships: [
      { id: 11, personId: 1, notes: null },
    ],
    filteredCurrentMemberships: [
      { id: 11, personId: 1, notes: null },
    ],
    availableStudents: [personById(2)],
    filteredAvailableStudents: [personById(2)],
    group: { id: 7, name: 'KB-01' },
    personById,
    personName,
    personContact,
    pausedStudentMembership: () => null,
    availableStudentActionLabel: () => 'Добавить',
    ...overrides,
  }
}

function button(wrapper, label) {
  return wrapper.findAll('button').find(
    (entry) => entry.text().includes(label)
  )
}

describe('AdminGroupMembersDrawer', () => {
  it('forwards search, add, remove and drawer lifecycle actions', async () => {
    const wrapper = mount(AdminGroupMembersDrawer, {
      props: baseProps(),
      global,
    })

    wrapper.findComponent(UiFilterBarStub).vm.$emit('update:modelValue', 'иван')
    wrapper.findComponent(UiFilterBarStub).vm.$emit('reset')
    await button(wrapper, 'Добавить').trigger('click')
    await button(wrapper, 'Убрать').trigger('click')
    wrapper.findComponent(UiDrawerStub).vm.$emit('update:modelValue', false)
    wrapper.findComponent(UiDrawerStub).vm.$emit('after-hide')

    expect(wrapper.emitted('update:search')).toEqual([['иван']])
    expect(wrapper.emitted('reset-search')).toHaveLength(1)
    expect(wrapper.emitted('add-student')[0][0].id).toBe(2)
    expect(wrapper.emitted('request-remove')[0][0].id).toBe(11)
    expect(wrapper.emitted('update:modelValue')).toEqual([[false]])
    expect(wrapper.emitted('after-hide')).toHaveLength(1)
  })

  it('keeps removal confirmation in the extracted component and emits confirmation actions', async () => {
    const wrapper = mount(AdminGroupMembersDrawer, {
      props: baseProps({
        removeConfirmVisible: true,
        removeTarget: {
          membership: { id: 11, personId: 1 },
          person: personById(1),
        },
      }),
      global,
    })

    expect(wrapper.text()).toContain('Историческая запись назначения сохранится в системе.')

    await button(wrapper, 'Убрать из группы').trigger('click')
    await button(wrapper, 'Отмена').trigger('click')
    wrapper.findComponent(UiDialogStub).vm.$emit('update:modelValue', false)

    expect(wrapper.emitted('confirm-remove')).toHaveLength(1)
    expect(wrapper.emitted('close-remove')).toHaveLength(2)
  })
})
