import {
  describe,
  expect,
  it,
} from 'vitest'
import {
  defineComponent,
  h,
  nextTick,
  reactive,
} from 'vue'
import { mount } from '@vue/test-utils'
import LectureEditorDrawer from '@/components/teacher/LectureEditorDrawer.vue'

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
  setup(props, { emit, slots }) {
    return () => h('button', {
      disabled: props.disabled || props.loading,
      onClick: (event) => emit('click', event),
    }, props.label || slots.default?.())
  },
})

const UiInputStub = defineComponent({
  name: 'UiInput',
  props: {
    modelValue: { type: String, default: '' },
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
  props: { modelValue: { type: String, default: '' } },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    return () => h('textarea', {
      value: props.modelValue,
      onInput: (event) => emit('update:modelValue', event.target.value),
    })
  },
})

const UiCheckboxStub = defineComponent({
  name: 'UiCheckbox',
  props: {
    modelValue: { type: [Boolean, Array], default: false },
    mode: { type: String, default: '' },
    value: { type: [String, Number], default: null },
    label: { type: String, default: '' },
  },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    return () => h('label', [
      h('input', {
        type: 'checkbox',
        'data-test': props.value === null ? 'visibility' : `test-${props.value}`,
        checked: props.mode === 'multiple'
          ? props.modelValue.includes(props.value)
          : props.modelValue,
        onChange: (event) => {
          if (props.mode !== 'multiple') {
            emit('update:modelValue', event.target.checked)
            return
          }
          const current = props.modelValue
          emit('update:modelValue', event.target.checked
            ? [...current, props.value]
            : current.filter((id) => id !== props.value))
        },
      }),
      props.label,
    ])
  },
})

const UiFileInputStub = defineComponent({
  name: 'UiFileInput',
  emits: ['files-change'],
  setup() {
    return () => h('input', { type: 'file' })
  },
})

const UiAlertStub = defineComponent({
  name: 'UiAlert',
  props: { message: { type: String, default: '' } },
  emits: ['close'],
  setup(props) {
    return () => h('div', props.message)
  },
})

const UiEmptyStateStub = defineComponent({
  name: 'UiEmptyState',
  props: { description: { type: String, default: '' } },
  setup(props) {
    return () => h('p', props.description)
  },
})

function setupDrawer({ form: initialForm = {}, ...props } = {}) {
  const form = reactive({
    id: null,
    title: '',
    description: '',
    publicVisible: true,
    testIds: [],
    ...initialForm,
  })
  const wrapper = mount(LectureEditorDrawer, {
    props: {
      open: true,
      title: 'Новая лекция',
      form,
      availableTests: [{ id: 72, title: 'Промежуточный тест' }],
      ...props,
    },
    global: {
      stubs: {
        UiDrawer: UiDrawerStub,
        UiButton: UiButtonStub,
        UiInput: UiInputStub,
        UiTextarea: UiTextareaStub,
        UiCheckbox: UiCheckboxStub,
        UiFileInput: UiFileInputStub,
        UiAlert: UiAlertStub,
        UiEmptyState: UiEmptyStateStub,
      },
    },
  })
  return { form, wrapper }
}

function button(wrapper, label) {
  return wrapper.findAll('button').find((entry) => entry.text().includes(label))
}

describe('LectureEditorDrawer', () => {
  it('edits the parent-owned reactive form without replacing its object', async () => {
    const { wrapper, form } = setupDrawer()
    await wrapper.find('input[aria-label="Название"]').setValue('Лекция о SQL')
    await wrapper.find('textarea').setValue('Описание лекции')
    await wrapper.find('input[data-test="visibility"]').setValue(false)
    await wrapper.find('input[data-test="test-72"]').setValue(true)

    expect(form.title).toBe('Лекция о SQL')
    expect(form.description).toBe('Описание лекции')
    expect(form.publicVisible).toBe(false)
    expect(form.testIds).toEqual([72])
    expect(wrapper.text()).toContain('1 выбрано')
  })

  it('forwards save, close and original drawer dismissal to its owner', async () => {
    const { wrapper } = setupDrawer()
    await button(wrapper, 'Создать лекцию').trigger('click')
    await button(wrapper, 'Закрыть').trigger('click')
    wrapper.findComponent(UiDrawerStub).vm.$emit('update:modelValue', false)
    await nextTick()

    expect(wrapper.emitted('save')).toHaveLength(1)
    expect(wrapper.emitted('close')).toHaveLength(1)
    expect(wrapper.emitted('update:open')).toEqual([[false]])
  })

  it('forwards file and material actions and retains the partial-create id', async () => {
    const material = { id: 9, fileName: 'already-uploaded.pdf' }
    const pending = { name: 'to-upload.pdf' }
    const { wrapper, form } = setupDrawer({
      form: { id: 100 },
      materials: [material],
      pendingFiles: [pending],
    })
    expect(wrapper.text()).toContain('Лекция #100')
    expect(button(wrapper, 'Сохранить лекцию').exists()).toBe(true)

    wrapper.findComponent(UiFileInputStub).vm.$emit('files-change', [pending])
    await button(wrapper, 'Убрать').trigger('click')
    await button(wrapper, 'Скачать').trigger('click')
    await button(wrapper, 'Удалить').trigger('click')

    expect(form.id).toBe(100)
    expect(wrapper.emitted('files-change')).toEqual([[[pending]]])
    expect(wrapper.emitted('remove-pending-file')).toEqual([[0]])
    expect(wrapper.emitted('download-material')).toEqual([[material]])
    expect(wrapper.emitted('request-delete-material')).toEqual([[material]])
  })

  it('disables save/close and pending-file removal during saving', async () => {
    const { wrapper } = setupDrawer({
      saving: true,
      pendingFiles: [{ name: 'queued.pdf' }],
    })
    expect(button(wrapper, 'Закрыть').attributes('disabled')).toBeDefined()
    expect(button(wrapper, 'Создать лекцию').attributes('disabled')).toBeDefined()
    expect(button(wrapper, 'Убрать').attributes('disabled')).toBeDefined()
  })

  it('emits error dismissal without mutating the parent message', async () => {
    const { wrapper } = setupDrawer({ formError: 'Не удалось сохранить' })
    expect(wrapper.text()).toContain('Не удалось сохранить')
    wrapper.findComponent(UiAlertStub).vm.$emit('close')
    await nextTick()
    expect(wrapper.emitted('dismiss-error')).toHaveLength(1)
    expect(wrapper.props('formError')).toBe('Не удалось сохранить')
  })
})
