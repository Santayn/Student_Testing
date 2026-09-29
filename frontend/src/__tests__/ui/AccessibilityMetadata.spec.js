import {
  describe,
  expect,
  it,
} from 'vitest'
import { mount, RouterLinkStub } from '@vue/test-utils'

import UiButton from '@/components/ui/UiButton.vue'
import {
  APP_NAME,
  getDocumentTitle,
} from '@/router/pageMetadata'
import {
  focusRouteContent,
} from '@/utils/focusRouteContent'

function mountUiButton(props) {
  return mount(UiButton, {
    props,
    global: {
      stubs: {
        RouterLink: RouterLinkStub,
      },
    },
  })
}

describe('accessibility and metadata foundation', () => {
  it('builds route-aware document titles with a safe fallback', () => {
    expect(
      getDocumentTitle('profile')
    ).toBe('Профиль — Student Testing')

    expect(
      getDocumentTitle('teacher-test-create')
    ).toBe('Создание теста — Student Testing')

    expect(
      getDocumentTitle('unknown-route')
    ).toBe(APP_NAME)
  })

  it('focuses the first page heading after route navigation', () => {
    const main = document.createElement('main')
    main.tabIndex = -1

    const heading = document.createElement('h1')
    heading.textContent = 'Новая страница'
    main.append(heading)
    document.body.append(main)

    const target =
      focusRouteContent(main)

    expect(target).toBe(heading)
    expect(heading.getAttribute('tabindex')).toBe('-1')
    expect(document.activeElement).toBe(heading)

    main.remove()
  })

  it('falls back to the main landmark when the page has no h1', () => {
    const main = document.createElement('main')
    main.tabIndex = -1
    document.body.append(main)

    const target =
      focusRouteContent(main)

    expect(target).toBe(main)
    expect(document.activeElement).toBe(main)

    main.remove()
  })

  it('removes disabled internal-link buttons from navigation and tab order', () => {
    const wrapper = mountUiButton({
      to: { name: 'home' },
      disabled: true,
      label: 'Главная',
    })

    const disabledLink =
      wrapper.get('[role="link"]')

    expect(disabledLink.attributes('aria-disabled')).toBe('true')
    expect(disabledLink.attributes('tabindex')).toBe('-1')
    expect(wrapper.find('a').exists()).toBe(false)
  })

  it('removes disabled external-link buttons from navigation and tab order', () => {
    const wrapper = mountUiButton({
      href: 'https://example.test',
      disabled: true,
      label: 'Внешняя ссылка',
    })

    const disabledLink =
      wrapper.get('[role="link"]')

    expect(disabledLink.attributes('aria-disabled')).toBe('true')
    expect(disabledLink.attributes('tabindex')).toBe('-1')
    expect(wrapper.find('a').exists()).toBe(false)
  })
})
