import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import UiAlert from '@/components/ui/UiAlert.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiEmptyState from '@/components/ui/UiEmptyState.vue'
import UiFileInput from '@/components/ui/UiFileInput.vue'
import UiTag from '@/components/ui/UiTag.vue'

describe('Ui palette component contracts', () => {
  it('adds a stable variant class to UiButton', () => {
    const wrapper = mount(UiButton, {
      props: {
        variant: 'primary',
        label: 'Сохранить',
      },
      global: {
        stubs: {
          RouterLink: {
            template: '<a><slot /></a>',
          },
        },
      },
    })

    expect(wrapper.classes()).toContain(
      'st-ui-button--primary'
    )
  })

  it('adds a stable semantic class to UiAlert', () => {
    const wrapper = mount(UiAlert, {
      props: {
        variant: 'danger',
        message: 'Ошибка',
      },
    })

    expect(wrapper.classes()).toContain(
      'st-ui-alert--danger'
    )
  })

  it('adds a stable semantic class to UiTag', () => {
    const wrapper = mount(UiTag, {
      props: {
        variant: 'success',
        value: 'Активен',
      },
    })

    expect(wrapper.classes()).toContain(
      'st-ui-tag--success'
    )
  })

  it('keeps UiEmptyState compact state on a stable class', () => {
    const wrapper = mount(UiEmptyState, {
      props: {
        title: 'Нет данных',
        compact: true,
      },
    })

    expect(wrapper.classes()).toContain('st-ui-empty')
    expect(wrapper.classes()).toContain('st-ui-empty--compact')
  })

  it('keeps UiFileInput validation state on a stable class', () => {
    const wrapper = mount(UiFileInput, {
      props: {
        label: 'Файл',
        error: 'Неверный формат',
      },
    })

    const input = wrapper.get('input[type="file"]')
    expect(input.classes()).toContain('st-ui-file-input')
    expect(input.classes()).toContain('st-ui-file-input--invalid')
    expect(input.attributes('aria-invalid')).toBe('true')
  })
})
