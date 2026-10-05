// @vitest-environment jsdom

import {
  defineComponent,
  nextTick,
  ref,
} from 'vue'
import {
  afterEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'
import {
  mount,
} from '@vue/test-utils'

import AppRuntimeErrorBoundary from '@/components/runtime/AppRuntimeErrorBoundary.vue'

const ThrowingChild = defineComponent({
  props: {
    explode: Boolean,
  },

  setup(props) {
    return () => {
      if (props.explode) {
        throw new Error('technical-secret-message')
      }

      return 'Рабочий интерфейс'
    }
  },
})

afterEach(() => {
  vi.restoreAllMocks()
})

describe('AppRuntimeErrorBoundary', () => {
  it('replaces a crashed descendant with a safe reload fallback', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    const explode = ref(false)
    const reload = vi.fn()

    const Harness = defineComponent({
      components: {
        AppRuntimeErrorBoundary,
        ThrowingChild,
      },
      setup: () => ({ explode, reload }),
      template: `
        <AppRuntimeErrorBoundary :reload="reload">
          <ThrowingChild :explode="explode" />
        </AppRuntimeErrorBoundary>
      `,
    })

    const wrapper = mount(Harness, {
      attachTo: document.body,
    })

    expect(wrapper.text()).toContain('Рабочий интерфейс')

    explode.value = true
    await nextTick()
    await nextTick()

    expect(wrapper.text()).toContain(
      'Произошла непредвиденная ошибка'
    )
    expect(wrapper.text()).not.toContain(
      'technical-secret-message'
    )

    const heading = wrapper.get('#runtime-error-title')
    expect(document.activeElement).toBe(heading.element)

    await wrapper.get('button').trigger('click')
    expect(reload).toHaveBeenCalledTimes(1)

    wrapper.unmount()
  })
})
