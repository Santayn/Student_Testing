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

import AdminLoadTypeManager from '@/components/admin/AdminLoadTypeManager.vue'

const UiDialogStub = defineComponent({
  name: 'UiDialog',
  props: {
    modelValue: { type: Boolean, default: false },
    title: { type: String, default: '' },
  },
  emits: ['update:modelValue'],
  setup(props, { slots }) {
    return () => props.modelValue
      ? h('section', [
          h('h2', props.title),
          slots.default?.(),
        ])
      : null
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

const UiTextareaStub = defineComponent({
  name: 'UiTextarea',
  props: {
    modelValue: { type: String, default: '' },
    label: { type: String, default: '' },
  },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    return () => h('textarea', {
      'aria-label': props.label,
      value: props.modelValue,
      onInput: (event) => emit('update:modelValue', event.target.value),
    })
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
    description: { type: String, default: '' },
  },
  setup(props, { slots }) {
    return () => h('div', [
      props.message,
      props.description,
      slots.default?.(),
    ])
  },
})

const UiUnsavedChangesConfirmStub = defineComponent({
  name: 'UiUnsavedChangesConfirm',
  props: {
    modelValue: { type: Boolean, default: false },
  },
  emits: [
    'update:modelValue',
    'continue',
    'discard',
  ],
  setup(props, { emit }) {
    return () => props.modelValue
      ? h('div', [
          h('button', {
            onClick: () => emit('continue'),
          }, 'Продолжить'),
          h('button', {
            onClick: () => emit('discard'),
          }, 'Отбросить'),
        ])
      : null
  },
})

const global = {
  stubs: {
    UiDialog: UiDialogStub,
    UiInput: UiInputStub,
    UiTextarea: UiTextareaStub,
    UiButton: UiButtonStub,
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

describe('AdminLoadTypeManager', () => {
  it('edits the parent-owned form and forwards manager actions', async () => {
    const form = reactive({
      id: null,
      name: '',
      description: '',
    })
    const loadType = {
      id: 2,
      name: 'Практика',
      description: 'Практические занятия',
    }
    const wrapper = mount(AdminLoadTypeManager, {
      props: {
        modelValue: true,
        title: 'Типы нагрузки',
        editorTitle: 'Новый тип нагрузки',
        form,
        searchQuery: '',
        filteredLoadTypes: [loadType],
        dirty: true,
      },
      global,
    })

    await wrapper.find('input[aria-label="Поиск"]').setValue('лек')
    await wrapper.find('input[aria-label="Название"]').setValue('Лекции')
    await wrapper.find('textarea[aria-label="Описание"]').setValue('Описание')
    await button(wrapper, 'Новый тип').trigger('click')
    await wrapper.find('.load-type-item').trigger('click')
    await button(wrapper, 'Отменить изменения').trigger('click')
    await button(wrapper, 'Создать тип').trigger('click')

    expect(form).toMatchObject({
      name: 'Лекции',
      description: 'Описание',
    })
    expect(wrapper.emitted('update:searchQuery')).toEqual([['лек']])
    expect(wrapper.emitted('start-new')).toHaveLength(1)
    expect(wrapper.emitted('edit')).toEqual([[loadType]])
    expect(wrapper.emitted('cancel')).toHaveLength(1)
    expect(wrapper.emitted('save')).toHaveLength(1)
  })

  it('forwards dialog close and unsaved-change decisions', async () => {
    const wrapper = mount(AdminLoadTypeManager, {
      props: {
        modelValue: true,
        closeConfirmVisible: true,
        form: reactive({
          id: null,
          name: '',
          description: '',
        }),
        filteredLoadTypes: [],
      },
      global,
    })

    wrapper.findComponent(UiDialogStub).vm.$emit('update:modelValue', false)
    await button(wrapper, 'Продолжить').trigger('click')
    await button(wrapper, 'Отбросить').trigger('click')

    expect(wrapper.emitted('update:modelValue')).toEqual([[false]])
    expect(wrapper.emitted('continue-editing')).toHaveLength(1)
    expect(wrapper.emitted('discard')).toHaveLength(1)
  })
})
