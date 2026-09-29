import {
  describe,
  expect,
  it,
} from 'vitest'
import {
  defineComponent,
  h,
  nextTick,
} from 'vue'
import { mount } from '@vue/test-utils'

import LectureMaterialsManager from '@/components/teacher/LectureMaterialsManager.vue'

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

const UiFileInputStub = defineComponent({
  name: 'UiFileInput',
  emits: ['files-change'],
  setup() {
    return () => h('input', { type: 'file' })
  },
})

const UiEmptyStateStub = defineComponent({
  name: 'UiEmptyState',
  props: { description: { type: String, default: '' } },
  setup(props) {
    return () => h('p', props.description)
  },
})

function mountManager(props = {}) {
  return mount(LectureMaterialsManager, {
    props: {
      lectureId: 100,
      materials: [],
      pendingFiles: [],
      ...props,
    },
    global: {
      stubs: {
        UiButton: UiButtonStub,
        UiFileInput: UiFileInputStub,
        UiEmptyState: UiEmptyStateStub,
      },
    },
  })
}

function button(wrapper, label) {
  return wrapper.findAll('button').find((entry) => entry.text() === label)
}

describe('LectureMaterialsManager', () => {
  it('shows queued and uploaded files and forwards material actions', async () => {
    const pending = { name: 'queued.pdf' }
    const material = { id: 9, fileName: 'uploaded.pdf' }
    const wrapper = mountManager({
      pendingFiles: [pending],
      materials: [material],
    })

    expect(wrapper.text()).toContain('1 загружено')
    expect(wrapper.text()).toContain('queued.pdf')
    expect(wrapper.text()).toContain('uploaded.pdf')

    wrapper.findComponent(UiFileInputStub).vm.$emit('files-change', [pending])
    await button(wrapper, 'Убрать').trigger('click')
    await button(wrapper, 'Скачать').trigger('click')
    await button(wrapper, 'Удалить').trigger('click')
    await nextTick()

    expect(wrapper.emitted('files-change')).toEqual([[[pending]]])
    expect(wrapper.emitted('remove-pending-file')).toEqual([[0]])
    expect(wrapper.emitted('download-material')).toEqual([[material]])
    expect(wrapper.emitted('request-delete-material')).toEqual([[material]])
  })

  it('keeps pending files visible before the lecture has a server id', () => {
    const wrapper = mountManager({
      lectureId: null,
      pendingFiles: [{ name: 'queued.pdf' }],
      materials: [{ id: 1, fileName: 'should-not-render.pdf' }],
    })

    expect(wrapper.text()).toContain('Будут загружены после создания')
    expect(wrapper.text()).toContain('queued.pdf')
    expect(wrapper.text()).not.toContain('should-not-render.pdf')
  })

  it('disables pending-file removal while the lecture is saving', () => {
    const wrapper = mountManager({
      saving: true,
      pendingFiles: [{ name: 'queued.pdf' }],
    })

    expect(button(wrapper, 'Убрать').attributes('disabled')).toBeDefined()
  })

  it('shows loading and empty states only for persisted lectures', async () => {
    const loading = mountManager({ loadingMaterials: true })
    expect(loading.text()).toContain('Загрузка материалов...')

    await loading.setProps({ loadingMaterials: false })
    expect(loading.text()).toContain('Загруженных материалов пока нет.')
  })
})
