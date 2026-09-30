// @vitest-environment node

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import {
  describe,
  expect,
  it,
} from 'vitest'

function source(path) {
  return readFileSync(resolve(process.cwd(), path), 'utf8')
}

describe('header and footer polish contract', () => {
  const header = source('src/components/layout/AppHeader.vue')
  const footer = source('src/components/layout/AppFooter.vue')

  it('keeps profile as an account action in the Header', () => {
    expect(header).toContain(':to="{ name: \'profile\' }"')
    expect(header).toContain('aria-label="Открыть профиль"')
  })

  it('uses PrimeIcons for theme control', () => {
    expect(header).toContain("'pi-sun'")
    expect(header).toContain("'pi-moon'")
    expect(header).not.toContain("'☀'")
    expect(header).not.toContain("'☾'")
  })

  it('keeps the Footer minimal', () => {
    expect(footer).toContain('О системе')
    expect(footer).not.toContain('Помощь')
    expect(footer).not.toContain('Политика конфиденциальности')
  })
})
