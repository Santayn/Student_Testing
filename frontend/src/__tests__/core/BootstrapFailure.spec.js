// @vitest-environment jsdom

import {
  afterEach,
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import {
  renderBootstrapFailure,
} from '@/utils/bootstrapFailure'

describe('bootstrap fatal fallback', () => {
  let consoleError

  beforeEach(() => {
    document.body.innerHTML = '<div id="app"></div>'
    document.title = 'Student Testing'
    consoleError = vi
      .spyOn(console, 'error')
      .mockImplementation(() => {})
  })

  afterEach(() => {
    consoleError.mockRestore()
  })

  it('renders a safe accessible fallback without exposing technical error text', () => {
    const secret = 'backend-secret-stack-message'

    const rendered = renderBootstrapFailure(
      new Error(secret),
      { reload: vi.fn() }
    )

    const root = document.getElementById('app')
    const alert = root.querySelector('[role="alert"]')
    const heading = root.querySelector('h1')

    expect(rendered).toBe(true)
    expect(alert).not.toBeNull()
    expect(heading.textContent).toBe(
      'Не удалось запустить приложение'
    )
    expect(root.textContent).not.toContain(secret)
    expect(document.title).toBe(
      'Ошибка запуска — Student Testing'
    )
    expect(document.activeElement).toBe(heading)
    expect(consoleError).toHaveBeenCalled()
  })

  it('runs the injected reload action', () => {
    const reload = vi.fn()

    renderBootstrapFailure(
      new Error('boom'),
      { reload }
    )

    document
      .querySelector('button')
      .click()

    expect(reload).toHaveBeenCalledTimes(1)
  })

  it('fails safely when the app root is unavailable', () => {
    document.body.replaceChildren()

    expect(
      renderBootstrapFailure(
        new Error('boom'),
        { reload: vi.fn() }
      )
    ).toBe(false)
  })
})
