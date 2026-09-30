// @vitest-environment node

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import {
  describe,
  expect,
  it,
} from 'vitest'

const app = readFileSync(
  resolve(process.cwd(), 'src/App.vue'),
  'utf8'
)

describe('motion stage 4 route content reveal', () => {
  it('animates only routed content inside the stable app shell', () => {
    expect(app).toContain('class="app-route-content"')
    expect(app).toContain('<AppHeader />')
    expect(app).toContain('<AppSidebar v-if="showSidebar" />')
    expect(app).toContain('<AppFooter />')
    expect(app).toContain('<RouterView />')
  })

  it('keys reveal by logical route rather than fullPath', () => {
    expect(app).toContain('route.name ??')
    expect(app).not.toContain(':key="route.fullPath"')
  })

  it('uses a subtle normal-duration enter reveal', () => {
    expect(app).toContain('@keyframes app-route-content-reveal')
    expect(app).toContain('transform: translateY(4px)')
    expect(app).toContain('var(--st-motion-normal)')
    expect(app).toContain('var(--st-ease-standard)')
  })

  it('disables route reveal for reduced motion', () => {
    expect(app).toMatch(
      /@media \(prefers-reduced-motion: reduce\)[\s\S]*?\.app-route-content\s*\{[\s\S]*?animation:\s*none/
    )
  })

  it('does not add leave, horizontal slide or artificial delay behavior', () => {
    expect(app).not.toContain('mode="out-in"')
    expect(app).not.toContain('setTimeout(')
    expect(app).not.toContain('translateX(')
  })
})
