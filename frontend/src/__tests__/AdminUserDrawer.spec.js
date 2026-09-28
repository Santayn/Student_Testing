import {
  defineComponent,
  h,
  nextTick,
  reactive,
} from 'vue'
import { mount } from '@vue/test-utils'
import {
  describe,
  expect,
  it,
} from 'vitest'

import AdminUserDrawer from '@/components/admin/AdminUserDrawer.vue'

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

const UiCardStub = defineComponent({
  name: 'UiCard',
  props: { title: { type: String, default: '' } },
  setup(props, { slots }) {
    return () => h('section', [
      h('h3', props.title),
      slots.default?.(),
    ])
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

const UiInputStub = defineComponent({
  name: 'UiInput',
  props: {
    modelValue: { type: [String, Number], default: '' },
    label: { type: String, default: '' },
  },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    return () => h('input', {
      'aria-label': props.label,
      value: props.modelValue,
      onInput: (event) => emit('update:modelValue', event.target.value),
    })
  },
})

const UiSelectStub = defineComponent({
  name: 'UiSelect',
  props: {
    modelValue: { type: [String, Number], default: null },
    label: { type: String, default: '' },
    options: { type: Array, default: () => [] },
  },
  emits: ['update:modelValue'],
  setup(props) {
    return () => h('div', {
      'data-select-label': props.label,
    })
  },
})

const UiCheckboxStub = defineComponent({
  name: 'UiCheckbox',
  props: {
    modelValue: { type: [Boolean, Array], default: false },
    label: { type: String, default: '' },
    value: { type: [String, Number, Boolean], default: null },
  },
  emits: ['update:modelValue'],
  setup(props) {
    return () => h('div', {
      'data-checkbox-label': props.label,
    })
  },
})

const SimpleStub = defineComponent({
  props: { message: { type: String, default: '' } },
  setup(props, { slots }) {
    return () => h('div', [props.message, slots.default?.()])
  },
})

const global = {
  stubs: {
    UiDrawer: UiDrawerStub,
    UiCard: UiCardStub,
    UiButton: UiButtonStub,
    UiInput: UiInputStub,
    UiSelect: UiSelectStub,
    UiCheckbox: UiCheckboxStub,
    UiAlert: SimpleStub,
    UiEmptyState: SimpleStub,
    UiTag: SimpleStub,
  },
}

function button(wrapper, label) {
  return wrapper.findAll('button').find(
    (entry) => entry.text().includes(label)
  )
}

function baseProps(overrides = {}) {
  return {
    modelValue: true,
    userForm: reactive({
      id: 10,
      login: 'teacher',
      active: true,
      personId: 5,
      roleIds: [2],
      newPerson: {
        firstName: '',
        lastName: '',
        dateOfBirth: '',
        email: '',
        phone: '',
      },
    }),
    roles: [
      { id: 2, name: 'TEACHER', description: 'Преподаватель' },
      { id: 3, name: 'STUDENT', description: 'Студент' },
    ],
    selectedUserName: 'Иванов Иван',
    selectedPerson: {
      id: 5,
      firstName: 'Иван',
      lastName: 'Иванов',
      email: 'ivan@example.com',
      phone: '+70000000000',
    },
    selectedPersonName: 'Иванов Иван',
    availablePersonOptions: [
      { label: 'Иванов Иван', value: 5 },
      { label: 'Петров Пётр', value: 6 },
    ],
    selectedRolePermissionNames: ['TEST_EDIT'],
    selectedUserPermissionNames: ['TEST_EDIT'],
    personEditDraft: reactive({
      firstName: 'Иван',
      lastName: 'Иванов',
      dateOfBirth: '1990-01-01',
      email: 'ivan@example.com',
      phone: '+70000000000',
    }),
    ...overrides,
  }
}

describe('AdminUserDrawer', () => {
  it('edits parent-owned account state and forwards drawer actions', async () => {
    const props = baseProps()
    const wrapper = mount(AdminUserDrawer, { props, global })

    const checkboxes = wrapper.findAllComponents(UiCheckboxStub)
    const active = checkboxes.find(
      (entry) => entry.props('label') === 'Учётная запись активна'
    )
    const studentRole = checkboxes.find(
      (entry) => entry.props('label') === 'STUDENT'
    )

    active.vm.$emit('update:modelValue', false)
    studentRole.vm.$emit('update:modelValue', [2, 3])

    const personSelect = wrapper.findComponent(UiSelectStub)
    personSelect.vm.$emit('update:modelValue', 6)

    await button(wrapper, 'Сохранить изменения').trigger('click')
    await button(wrapper, 'Отмена').trigger('click')
    wrapper.findComponent(UiDrawerStub).vm.$emit('update:modelValue', false)
    await nextTick()

    expect(props.userForm.active).toBe(false)
    expect(props.userForm.roleIds).toEqual([2, 3])
    expect(wrapper.emitted('request-person-selection-change')).toEqual([[6]])
    expect(wrapper.emitted('save')).toHaveLength(1)
    expect(wrapper.emitted('request-close')).toHaveLength(1)
    expect(wrapper.emitted('update:modelValue')).toEqual([[false]])
  })

  it('keeps Person creation UI inside the drawer but delegates persistence', async () => {
    const props = baseProps({
      personCreatorVisible: true,
      selectedPerson: null,
      selectedPersonName: '—',
    })
    const wrapper = mount(AdminUserDrawer, { props, global })

    await wrapper.find('input[aria-label="Фамилия"]').setValue('Петров')
    await wrapper.find('input[aria-label="Имя"]').setValue('Пётр')
    await wrapper.find('input[aria-label="Email"]').setValue('petr@example.com')
    await button(wrapper, 'Создать профиль').trigger('click')
    await button(wrapper, 'Отмена создания').trigger('click')

    expect(props.userForm.newPerson).toMatchObject({
      lastName: 'Петров',
      firstName: 'Пётр',
      email: 'petr@example.com',
    })
    expect(wrapper.emitted('create-person')).toHaveLength(1)
    expect(wrapper.emitted('cancel-person-creator')).toHaveLength(1)
  })

  it('edits the parent-owned Person draft and delegates its save flow', async () => {
    const props = baseProps({
      personEditorVisible: true,
      personEditorDirty: true,
    })
    const wrapper = mount(AdminUserDrawer, { props, global })

    await wrapper.find('input[aria-label="Фамилия"]').setValue('Сидоров')
    await button(wrapper, 'Сохранить профиль').trigger('click')
    await button(wrapper, 'Отмена редактирования').trigger('click')

    expect(props.personEditDraft.lastName).toBe('Сидоров')
    expect(wrapper.emitted('update-person')).toHaveLength(1)
    expect(wrapper.emitted('request-cancel-person-editor')).toHaveLength(1)
  })
})
