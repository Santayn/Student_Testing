import {
  readFileSync,
} from 'node:fs'
import {
  dirname,
  resolve,
} from 'node:path'
import {
  fileURLToPath,
} from 'node:url'
import {
  describe,
  expect,
  it,
} from 'vitest'

const testsDir =
  dirname(fileURLToPath(import.meta.url))
const srcDir =
  resolve(testsDir, '..')
const frontendDir =
  resolve(srcDir, '..')

function sourceFromFrontend(relativePath) {
  return readFileSync(
    resolve(frontendDir, relativePath),
    'utf8'
  )
}

function sourceFromSrc(relativePath) {
  return readFileSync(
    resolve(srcDir, relativePath),
    'utf8'
  )
}

describe('accessibility and metadata contracts', () => {
  it('uses Russian document language and a non-template base title', () => {
    const html =
      sourceFromFrontend('index.html')

    expect(html).toContain('<html lang="ru">')
    expect(html).toContain('<title>Student Testing</title>')
    expect(html).not.toContain('Vite App')
  })

  it('keeps only App as the production page main landmark', () => {
    const app = sourceFromSrc('App.vue')

    expect(app).toContain('<main')
    expect(app).toContain('tabindex="-1"')

    for (const shell of [
      'components/subjects/SubjectsPageShell.vue',
      'components/lectures/LecturesPageShell.vue',
      'components/results/ResultsPageShell.vue',
      'components/tests/TestsPageShell.vue',
    ]) {
      const source = sourceFromSrc(shell)

      expect(source).not.toContain('<main')
      expect(source).toContain('<section')
    }
  })

  it('tracks the popup state in UiActionMenu accessibility attributes', () => {
    const source =
      sourceFromSrc('components/ui/UiActionMenu.vue')

    expect(source).toContain("const menuOpen = ref(false)")
    expect(source).toContain(':aria-expanded="menuOpen ? \'true\' : \'false\'"')
    expect(source).toContain('@show="onShow"')
    expect(source).toContain('@hide="onHide"')
  })
})
