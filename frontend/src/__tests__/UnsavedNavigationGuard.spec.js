import {
  afterEach,
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import {
  mount,
} from '@vue/test-utils'

import {
  defineComponent,
  ref,
} from 'vue'

const routerState = vi.hoisted(() => ({
  leaveHandler: null,
}))

vi.mock('vue-router', () => ({
  onBeforeRouteLeave: (handler) => {
    routerState.leaveHandler = handler
  },
}))

import {
  useUnsavedNavigationGuard,
} from '@/composables/useUnsavedNavigationGuard'

const Harness = defineComponent({
  name: 'UnsavedNavigationGuardHarness',
  setup() {
    const dirty = ref(false)
    const navigationGuard =
      useUnsavedNavigationGuard(dirty)

    return {
      dirty,
      ...navigationGuard,
    }
  },
  template: '<div />',
})

const mountedWrappers = []

function mountHarness() {
  const wrapper = mount(Harness)
  mountedWrappers.push(wrapper)
  return wrapper
}

describe('useUnsavedNavigationGuard', () => {
  beforeEach(() => {
    routerState.leaveHandler = null
  })

  afterEach(() => {
    while (mountedWrappers.length) {
      mountedWrappers.pop().unmount()
    }
  })

  it('allows route navigation immediately when the editor is clean', () => {
    mountHarness()

    expect(routerState.leaveHandler).toBeTypeOf('function')
    expect(routerState.leaveHandler()).toBe(true)
  })

  it('keeps the original route transition pending until the user decides', async () => {
    const wrapper = mountHarness()
    wrapper.vm.dirty = true

    const decision = routerState.leaveHandler()

    expect(decision).toBeInstanceOf(Promise)
    expect(wrapper.vm.confirmVisible).toBe(true)

    wrapper.vm.continueEditing()

    await expect(decision).resolves.toBe(false)
    expect(wrapper.vm.confirmVisible).toBe(false)
  })

  it('allows the same pending transition when changes are discarded', async () => {
    const wrapper = mountHarness()
    wrapper.vm.dirty = true

    const decision = routerState.leaveHandler()
    wrapper.vm.discardAndNavigate()

    await expect(decision).resolves.toBe(true)
    expect(wrapper.vm.confirmVisible).toBe(false)
  })

  it('uses beforeunload only while unsaved changes exist', () => {
    const wrapper = mountHarness()

    const cleanEvent = new Event(
      'beforeunload',
      { cancelable: true }
    )
    window.dispatchEvent(cleanEvent)
    expect(cleanEvent.defaultPrevented).toBe(false)

    wrapper.vm.dirty = true

    const dirtyEvent = new Event(
      'beforeunload',
      { cancelable: true }
    )
    window.dispatchEvent(dirtyEvent)
    expect(dirtyEvent.defaultPrevented).toBe(true)
  })

  it('cancels a pending transition when the guarded view unmounts', async () => {
    const wrapper = mountHarness()
    wrapper.vm.dirty = true

    const decision = routerState.leaveHandler()
    wrapper.unmount()

    await expect(decision).resolves.toBe(false)
  })
})
