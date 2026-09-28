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

import CourseTemplateEditorDrawer from '@/components/teacher/CourseTemplateEditorDrawer.vue'
import CourseVersionEditorDrawer from '@/components/teacher/CourseVersionEditorDrawer.vue'

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
    disabled: { type: Boolean, default: false },
    loading: { type: Boolean, default: false },
  },
  emits: ['click'],
  setup(props, { emit, slots }) {
    return () => h('button', {
      disabled: props.disabled || props.loading,
      onClick: (event) => emit('click', event),
    }, slots.default?.())
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

const UiCheckboxStub = defineComponent({
  name: 'UiCheckbox',
  props: {
    modelValue: { type: Boolean, default: false },
    label: { type: String, default: '' },
  },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    return () => h('label', [
      h('input', {
        type: 'checkbox',
        'aria-label': props.label,
        checked: props.modelValue,
        onChange: (event) => emit('update:modelValue', event.target.checked),
      }),
      props.label,
    ])
  },
})

const UiAlertStub = defineComponent({
  name: 'UiAlert',
  props: { message: { type: String, default: '' } },
  setup(props) {
    return () => h('div', props.message)
  },
})

const global = {
  stubs: {
    UiDrawer: UiDrawerStub,
    UiButton: UiButtonStub,
    UiInput: UiInputStub,
    UiTextarea: UiTextareaStub,
    UiCheckbox: UiCheckboxStub,
    UiAlert: UiAlertStub,
  },
}

function button(wrapper, text) {
  return wrapper.findAll('button').find((entry) => entry.text().includes(text))
}

describe('course template editor drawers', () => {
  it('edits the parent-owned template form and forwards drawer actions', async () => {
    const form = reactive({
      id: null,
      name: '',
      publicVisible: true,
    })
    const wrapper = mount(CourseTemplateEditorDrawer, {
      props: {
        open: true,
        title: 'Новый шаблон курса',
        form,
        subjectLabel: 'Базы данных',
      },
      global,
    })

    await wrapper.find('input[aria-label="Название шаблона"]').setValue('Основной поток')
    await wrapper.find('input[aria-label="Публиковать шаблон"]').setValue(false)
    await button(wrapper, 'Сохранить').trigger('click')
    await button(wrapper, 'Отмена').trigger('click')
    wrapper.findComponent(UiDrawerStub).vm.$emit('update:modelValue', false)
    await nextTick()

    expect(form).toMatchObject({
      name: 'Основной поток',
      publicVisible: false,
    })
    expect(wrapper.text()).toContain('Базы данных')
    expect(wrapper.emitted('save')).toHaveLength(1)
    expect(wrapper.emitted('close')).toHaveLength(1)
    expect(wrapper.emitted('update:open')).toEqual([[false]])
  })

  it('edits version fields while preserving create-only publication control', async () => {
    const form = reactive({
      id: null,
      versionNumber: 2,
      title: 'Версия 2',
      description: '',
      changeNotes: '',
      published: false,
    })
    const wrapper = mount(CourseVersionEditorDrawer, {
      props: {
        open: true,
        title: 'Новая версия курса',
        form,
        isCreate: true,
        templateLabel: 'Основной поток',
      },
      global,
    })

    await wrapper.find('input[aria-label="Номер версии"]').setValue('4')
    await wrapper.find('input[aria-label="Название версии"]').setValue('Версия 4')
    await wrapper.find('textarea[aria-label="Описание"]').setValue('Описание')
    await wrapper.find('textarea[aria-label="Что изменилось"]').setValue('Новые темы')
    await wrapper.find('input[aria-label="Опубликовать сразу после создания"]').setValue(true)
    await button(wrapper, 'Сохранить версию').trigger('click')

    expect(form).toMatchObject({
      versionNumber: '4',
      title: 'Версия 4',
      description: 'Описание',
      changeNotes: 'Новые темы',
      published: true,
    })
    expect(wrapper.text()).toContain('Основной поток')
    expect(wrapper.emitted('save')).toHaveLength(1)
  })

  it('shows edit publication guidance and disables actions while saving', () => {
    const form = reactive({
      id: 9,
      versionNumber: 1,
      title: 'Первая',
      description: '',
      changeNotes: '',
      published: true,
    })
    const wrapper = mount(CourseVersionEditorDrawer, {
      props: {
        open: true,
        title: 'Редактирование версии',
        form,
        isCreate: false,
        saving: true,
      },
      global,
    })

    expect(wrapper.text()).toContain('Статус публикации существующей версии')
    expect(wrapper.find('input[type="checkbox"]').exists()).toBe(false)
    expect(button(wrapper, 'Отмена').attributes('disabled')).toBeDefined()
    expect(button(wrapper, 'Сохранить версию').attributes('disabled')).toBeDefined()
  })
})
