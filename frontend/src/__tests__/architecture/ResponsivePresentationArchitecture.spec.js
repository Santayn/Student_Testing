import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick, ref } from 'vue'
import {
  CONTAINER_BREAKPOINTS,
  resolvePresentationMode,
} from '@/config/presentationModes'
import { useLayoutStore } from '@/stores/layout'
import { usePresentationSpace } from '@/composables/usePresentationSpace'
import {
  createPresentationRegistry,
  getPresentationKeys,
  resolvePresentationLoader,
} from '@/config/presentationRegistry'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'

describe('responsive presentation foundation', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('resolves viewport boundaries without inferring device types', () => {
    expect(resolvePresentationMode(0)).toBe(null)
    expect(resolvePresentationMode(Number.NaN)).toBe(null)
    expect(resolvePresentationMode(640)).toBe('compact')
    expect(resolvePresentationMode(641)).toBe('medium')
    expect(resolvePresentationMode(960)).toBe('medium')
    expect(resolvePresentationMode(961)).toBe('wide')
    expect(resolvePresentationMode(560, CONTAINER_BREAKPOINTS)).toBe('compact')
    expect(resolvePresentationMode(881, CONTAINER_BREAKPOINTS)).toBe('wide')
  })

  it('keeps the global layout store reactive, idempotent and non-persistent', () => {
    const store = useLayoutStore()
    const originalWidth = window.innerWidth
    const originalHeight = window.innerHeight

    try {
      Object.defineProperty(window, 'innerWidth', {
        configurable: true,
        value: 500,
        writable: true,
      })
      Object.defineProperty(window, 'innerHeight', {
        configurable: true,
        value: 850,
        writable: true,
      })
      const add = vi.spyOn(window, 'addEventListener')
      store.init()
      store.init()
      expect(store.initialized).toBe(true)
      expect(store.mode).toBe('compact')
      expect(store.isLandscape).toBe(false)
      expect(add.mock.calls.filter(([event]) => event === 'resize')).toHaveLength(1)

      window.innerWidth = 1024
      window.innerHeight = 700
      window.dispatchEvent(new Event('resize'))
      expect(store.mode).toBe('wide')
      expect(store.isLandscape).toBe(true)
    } finally {
      store.dispose()
      Object.defineProperty(window, 'innerWidth', {
        configurable: true,
        value: originalWidth,
        writable: true,
      })
      Object.defineProperty(window, 'innerHeight', {
        configurable: true,
        value: originalHeight,
        writable: true,
      })
    }
  })

  it('selects matching lazy view and falls back without executing loaders', () => {
    const compact = vi.fn(() => Promise.resolve({ default: {} }))
    const shared = vi.fn(() => Promise.resolve({ default: {} }))
    const wide = vi.fn(() => Promise.resolve({ default: {} }))
    const registry = createPresentationRegistry({
      '/src/views/teacher/presentations/questions.compact.vue': compact,
      '/src/views/teacher/presentations/questions.wide.vue': wide,
      '/src/views/admin/presentations/groups.shared.vue': shared,
      '/unrelated/file.vue': vi.fn(),
    })

    expect(resolvePresentationLoader(registry, 'teacher/questions', 'compact')).toBe(compact)
    expect(resolvePresentationLoader(registry, 'teacher/questions', 'medium')).toBe(compact)
    expect(resolvePresentationLoader(registry, 'teacher/questions', 'wide')).toBe(wide)
    expect(resolvePresentationLoader(registry, 'admin/groups', 'medium')).toBe(shared)
    expect(resolvePresentationLoader(registry, 'missing', 'compact')).toBe(null)
    expect(compact).not.toHaveBeenCalled()
    expect(shared).not.toHaveBeenCalled()
  })

  it('discovers the three dev preview chunks lazily', () => {
    expect(getPresentationKeys()).toContain('dev/layout-workspace')
  })

  it('measures a local container and disconnects observer on unmount', async () => {
    const callbacks = []
    const disconnect = vi.fn()
    const observe = vi.fn()
    const NativeObserver = globalThis.ResizeObserver
    globalThis.ResizeObserver = class {
      constructor(callback) { callbacks.push(callback) }
      observe = observe
      disconnect = disconnect
    }

    const Component = defineComponent({
      setup() {
        const container = ref(null)
        const { mode } = usePresentationSpace(container)
        return () => h('div', { ref: container }, mode.value ?? 'unmeasured')
      },
    })

    let wrapper
    try {
      wrapper = mount(Component, { attachTo: document.body })
      await nextTick()
      expect(observe).toHaveBeenCalledOnce()
      callbacks[0]([{ target: wrapper.element, contentRect: { width: 500 } }])
      await nextTick()
      expect(wrapper.text()).toBe('compact')
      callbacks[0]([{ target: wrapper.element, contentRect: { width: 900 } }])
      await nextTick()
      expect(wrapper.text()).toBe('wide')
    } finally {
      wrapper?.unmount()
      expect(disconnect).toHaveBeenCalled()
      globalThis.ResizeObserver = NativeObserver
    }
  })
})
