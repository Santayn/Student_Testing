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

import AdminTeachingAssignmentDrawer from '@/components/admin/AdminTeachingAssignmentDrawer.vue'

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
    modelValue: { type: [String, Number], default: '' },
    label: { type: String, default: '' },
  },
  emits: ['update:modelValue', 'change'],
  setup(props) {
    return () => h('div', { 'data-select-label': props.label })
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

const SimpleStub = defineComponent({
  props: {
    message: { type: String, default: '' },
    label: { type: String, default: '' },
  },
  setup(props, { slots }) {
    return () => h('div', [props.message, props.label, slots.default?.()])
  },
})

const global = {
  stubs: {
    UiDrawer: UiDrawerStub,
    UiCard: UiCardStub,
    UiButton: UiButtonStub,
    UiInput: UiInputStub,
    UiSelect: UiSelectStub,
    UiTextarea: UiTextareaStub,
    UiCheckbox: SimpleStub,
    UiAlert: SimpleStub,
    UiEmptyState: SimpleStub,
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
    title: 'Новое назначение нагрузки',
    form: reactive({
      id: null,
      subjectId: '',
      subjectMembershipId: '',
      groupId: '',
      groupIds: [],
      loadTypeId: '',
      courseVersionId: '',
      semester: '1',
      studyCourse: '1',
      academicYear: '2026',
      hoursPerWeek: '',
      status: '1',
      notes: '',
    }),
    loadTypes: [{ id: 1, name: 'Лекции' }],
    subjectOptions: [{ value: '5', label: 'Базы данных' }],
    teacherOptions: [{ value: '11', label: 'Иванов' }],
    loadTypeOptions: [{ value: '1', label: 'Лекции' }],
    courseVersionOptions: [{ value: '', label: 'Без версии курса' }],
    groupOptions: [{ value: '21', label: 'КБ-21' }],
    filteredGroups: [{ id: 21, name: 'КБ-21', code: '21' }],
    groupSearchQuery: '',
    ...overrides,
  }
}

describe('AdminTeachingAssignmentDrawer', () => {
  it('edits parent-owned fields and forwards save/close/search actions', async () => {
    const props = baseProps()
    const wrapper = mount(AdminTeachingAssignmentDrawer, { props, global })

    await wrapper.find('input[aria-label="Часы в неделю"]').setValue('3.5')
    await wrapper.find('textarea[aria-label="Примечание"]').setValue('Практика')
    await wrapper.find('input[aria-label="Поиск группы"]').setValue('22')
    await button(wrapper, 'Создать нагрузку').trigger('click')
    await button(wrapper, 'Отмена').trigger('click')

    expect(props.form.hoursPerWeek).toBe('3.5')
    expect(props.form.notes).toBe('Практика')
    expect(wrapper.emitted('update:group-search-query')).toEqual([['22']])
    expect(wrapper.emitted('save')).toHaveLength(1)
    expect(wrapper.emitted('request-close')).toHaveLength(1)
  })

  it('forwards subject and teacher change events and drawer visibility', () => {
    const props = baseProps()
    const wrapper = mount(AdminTeachingAssignmentDrawer, { props, global })
    const selects = wrapper.findAllComponents(UiSelectStub)

    selects.find((entry) => entry.props('label') === 'Предмет').vm.$emit('change')
    selects.find((entry) => entry.props('label') === 'Преподаватель').vm.$emit('change')
    wrapper.findComponent(UiDrawerStub).vm.$emit('update:modelValue', false)

    expect(wrapper.emitted('subject-change')).toHaveLength(1)
    expect(wrapper.emitted('teacher-change')).toHaveLength(1)
    expect(wrapper.emitted('update:modelValue')).toEqual([[false]])
  })

  it('shows the edit save label and disables actions while saving', () => {
    const wrapper = mount(AdminTeachingAssignmentDrawer, {
      props: baseProps({
        isCreate: false,
        saving: true,
      }),
      global,
    })

    expect(button(wrapper, 'Сохранить изменения').attributes('disabled')).toBeDefined()
    expect(button(wrapper, 'Отмена').attributes('disabled')).toBeDefined()
  })
})
