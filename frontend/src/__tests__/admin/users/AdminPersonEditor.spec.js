import {
  defineComponent,
  h,
  reactive,
} from 'vue'
import { mount } from '@vue/test-utils'
import {
  describe,
  expect,
  it,
} from 'vitest'

import AdminPersonEditor from '@/components/admin/AdminPersonEditor.vue'

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

const SimpleStub = defineComponent({
  props: { message: { type: String, default: '' } },
  setup(props) {
    return () => h('div', props.message)
  },
})

function button(wrapper, label) {
  return wrapper.findAll('button').find(
    (entry) => entry.text().includes(label)
  )
}

function props(overrides = {}) {
  return {
    userForm: reactive({
      newPerson: {
        firstName: '',
        lastName: '',
        dateOfBirth: '',
        email: '',
        phone: '',
      },
    }),
    selectedPerson: {
      id: 5,
      firstName: 'Иван',
      lastName: 'Иванов',
      email: 'ivan@example.com',
      phone: '+70000000000',
    },
    selectedPersonName: 'Иванов Иван',
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

const global = {
  stubs: {
    UiButton: UiButtonStub,
    UiInput: UiInputStub,
    UiAlert: SimpleStub,
  },
}

describe('AdminPersonEditor', () => {
  it('edits parent-owned create draft and delegates persistence', async () => {
    const input = props({ personCreatorVisible: true })
    const wrapper = mount(AdminPersonEditor, {
      props: input,
      global,
    })

    await wrapper.find('input[aria-label="Фамилия"]').setValue('Петров')
    await wrapper.find('input[aria-label="Email"]').setValue('petr@example.com')
    await button(wrapper, 'Создать профиль').trigger('click')

    expect(input.userForm.newPerson.lastName).toBe('Петров')
    expect(input.userForm.newPerson.email).toBe('petr@example.com')
    expect(wrapper.emitted('create-person')).toHaveLength(1)
  })

  it('edits parent-owned Person draft and delegates save/cancel', async () => {
    const input = props({
      personEditorVisible: true,
      personEditorDirty: true,
    })
    const wrapper = mount(AdminPersonEditor, {
      props: input,
      global,
    })

    await wrapper.find('input[aria-label="Имя"]').setValue('Иван II')
    await button(wrapper, 'Сохранить профиль').trigger('click')
    await button(wrapper, 'Отмена редактирования').trigger('click')

    expect(input.personEditDraft.firstName).toBe('Иван II')
    expect(wrapper.emitted('update-person')).toHaveLength(1)
    expect(wrapper.emitted('request-cancel-person-editor')).toHaveLength(1)
  })
})
