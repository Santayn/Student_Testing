import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'
import {
  defineComponent,
  h,
  ref,
} from 'vue'
import {
  flushPromises,
  mount,
} from '@vue/test-utils'

const state = vi.hoisted(() => ({
  route: {
    query: {},
  },
  teacher: {},
  api: {
    lecturesAll: vi.fn(),
    lectureTests: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    setTests: vi.fn(),
    getMaterials: vi.fn(),
    uploadMaterials: vi.fn(),
    testsAll: vi.fn(),
  },
}))

vi.mock('vue-router', () => ({
  useRoute: () => state.route,
}))

vi.mock('@/composables/useTeacherSubjects', async () => {
  const vue = await import('vue')

  return {
    useTeacherSubjects: () => state.teacher,
  }
})

vi.mock('@/api', () => ({
  getApiErrorMessage: (_error, fallback) => fallback,
  lecturesApi: {
    getAll: state.api.lecturesAll,
    getTests: state.api.lectureTests,
    create: state.api.create,
    update: state.api.update,
    setTests: state.api.setTests,
    getMaterials: state.api.getMaterials,
    uploadMaterials: state.api.uploadMaterials,
  },
  testsApi: {
    getAll: state.api.testsAll,
  },
}))

import LectureManagementView from '@/views/teacher/LectureManagementView.vue'

const Passthrough = defineComponent({
  setup(_, { slots }) {
    return () => h('section', [
      slots.default?.(),
      slots.filters?.(),
      slots.actions?.(),
      slots.footer?.(),
    ])
  },
})

const UiButtonStub = defineComponent({
  name: 'UiButton',
  inheritAttrs: false,
  props: {
    label: { type: String, default: '' },
    disabled: { type: Boolean, default: false },
    loading: { type: Boolean, default: false },
  },
  emits: ['click'],
  setup(props, { attrs, slots, emit }) {
    return () => h(
      'button',
      {
        ...attrs,
        disabled: props.disabled || props.loading,
        onClick: (event) => emit('click', event),
      },
      props.label || slots.default?.()
    )
  },
})

const UiInputStub = defineComponent({
  name: 'UiInput',
  inheritAttrs: false,
  props: {
    modelValue: { type: String, default: '' },
    label: { type: String, default: '' },
  },
  emits: ['update:modelValue'],
  setup(props, { attrs, emit }) {
    return () => h('label', [
      h('span', props.label),
      h('input', {
        ...attrs,
        value: props.modelValue,
        onInput: (event) => emit('update:modelValue', event.target.value),
      }),
    ])
  },
})

const UiTextareaStub = defineComponent({
  name: 'UiTextarea',
  props: {
    modelValue: { type: String, default: '' },
  },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    return () => h('textarea', {
      value: props.modelValue,
      onInput: (event) => emit('update:modelValue', event.target.value),
    })
  },
})

const UiDrawerStub = defineComponent({
  name: 'UiDrawer',
  props: {
    modelValue: { type: Boolean, default: false },
    title: { type: String, default: '' },
  },
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

const UiFileInputStub = defineComponent({
  name: 'UiFileInput',
  emits: ['files-change'],
  setup() {
    return () => h('input', { type: 'file' })
  },
})

const UiAlertStub = defineComponent({
  name: 'UiAlert',
  props: {
    message: { type: String, default: '' },
  },
  setup(props) {
    return () => h('div', props.message)
  },
})

function buttonByText(wrapper, text) {
  return wrapper
    .findAll('button')
    .find((button) => button.text().includes(text))
}

function mountView() {
  return mount(LectureManagementView, {
    global: {
      stubs: {
        TeacherPageShell: Passthrough,
        UiAlert: UiAlertStub,
        UiButton: UiButtonStub,
        UiCard: Passthrough,
        UiCheckbox: Passthrough,
        UiDialog: false,
        UiDrawer: UiDrawerStub,
        UiEmptyState: Passthrough,
        UiFileInput: UiFileInputStub,
        UiFilterBar: Passthrough,
        UiInput: UiInputStub,
        UiSelect: Passthrough,
        UiTextarea: UiTextareaStub,
        UiUnsavedChangesConfirm: false,
      },
    },
  })
}

describe('lecture partial-create retry', () => {
  beforeEach(() => {
    state.route.query = {}

    state.teacher = {
      loadingSubjects: ref(false),
      selectedMembershipId: ref(10),
      selectedSubjectId: ref(20),
      selectedSubject: ref({ id: 20, name: 'Базы данных' }),
      selectedMembership: ref({ id: 10, subjectId: 20 }),
      membershipOptions: ref([{ value: 10, label: 'Базы данных' }]),
      ensureSelectedMembershipActive: vi.fn().mockResolvedValue(undefined),
      loadTeacherSubjects: vi.fn().mockResolvedValue(undefined),
    }

    state.api.lecturesAll.mockReset()
    state.api.lecturesAll.mockResolvedValue({ data: [] })
    state.api.lectureTests.mockReset()
    state.api.lectureTests.mockResolvedValue({ data: [] })
    state.api.testsAll.mockReset()
    state.api.testsAll.mockResolvedValue({ data: [] })
    state.api.getMaterials.mockReset()
    state.api.getMaterials.mockResolvedValue({ data: [] })
    state.api.uploadMaterials.mockReset()
    state.api.uploadMaterials.mockResolvedValue({ data: [] })
    state.api.update.mockReset()
    state.api.update.mockImplementation((id, payload) => Promise.resolve({
      data: { id, ...payload },
    }))
    state.api.create.mockReset()
    state.api.create.mockImplementation((payload) => Promise.resolve({
      data: { id: 100, ...payload },
    }))
    state.api.setTests.mockReset()
    state.api.setTests
      .mockRejectedValueOnce(new Error('linked tests unavailable'))
      .mockResolvedValue({ data: [] })
  })

  it('does not POST a second lecture when dependent saving fails and Save is retried', async () => {
    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Добавить лекцию').trigger('click')
    await wrapper.find('input').setValue('Лекция 1')

    await buttonByText(wrapper, 'Создать лекцию').trigger('click')
    await flushPromises()

    expect(state.api.create).toHaveBeenCalledTimes(1)
    expect(state.api.update).not.toHaveBeenCalled()
    expect(state.api.setTests).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('Лекция #100')
    expect(wrapper.text()).toContain('Лекция уже создана')

    await buttonByText(wrapper, 'Сохранить лекцию').trigger('click')
    await flushPromises()

    expect(state.api.create).toHaveBeenCalledTimes(1)
    expect(state.api.update).not.toHaveBeenCalled()
    expect(state.api.setTests).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('Лекция создана.')
  })

  it('retries a failed material upload without creating the lecture again', async () => {
    state.api.setTests.mockReset()
    state.api.setTests.mockResolvedValue({ data: [] })
    state.api.uploadMaterials
      .mockRejectedValueOnce(new Error('storage unavailable'))
      .mockResolvedValue({ data: [] })

    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Добавить лекцию').trigger('click')
    await wrapper.find('input:not([type="file"])').setValue('Лекция с файлом')
    wrapper.findComponent(UiFileInputStub).vm.$emit('files-change', [
      { name: 'notes.pdf' },
    ])
    await flushPromises()

    await buttonByText(wrapper, 'Создать лекцию').trigger('click')
    await flushPromises()

    expect(state.api.create).toHaveBeenCalledTimes(1)
    expect(state.api.uploadMaterials).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('Лекция уже создана')
    expect(wrapper.text()).toContain('notes.pdf')

    await buttonByText(wrapper, 'Сохранить лекцию').trigger('click')
    await flushPromises()

    expect(state.api.create).toHaveBeenCalledTimes(1)
    expect(state.api.update).not.toHaveBeenCalled()
    expect(state.api.uploadMaterials).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('Лекция создана.')
  })

})
