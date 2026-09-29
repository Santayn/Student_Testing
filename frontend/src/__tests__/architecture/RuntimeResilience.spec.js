// @vitest-environment node

import {
  readFileSync,
} from 'node:fs'
import {
  resolve,
} from 'node:path'

import {
  describe,
  expect,
  it,
} from 'vitest'

const source = (...segments) => readFileSync(
  resolve(process.cwd(), 'src', ...segments),
  'utf8'
)

describe('runtime resilience architecture', () => {
  it('uses a plain bootstrap fallback for unexpected startup failures', () => {
    const main = source('main.js')
    const fallback = source('utils', 'bootstrapFailure.js')

    expect(main).toContain("import AppRoot from './AppRoot.vue'")
    expect(main).toContain('renderBootstrapFailure(error)')
    expect(fallback).toContain('root.replaceChildren(panel)')
    expect(fallback).not.toContain('innerHTML')
    expect(fallback).not.toContain('error.message')
    expect(fallback).not.toContain('error.stack')
  })

  it('places the Vue runtime boundary above App.vue', () => {
    const root = source('AppRoot.vue')
    const boundary = source(
      'components',
      'runtime',
      'AppRuntimeErrorBoundary.vue'
    )

    expect(root).toContain('<AppRuntimeErrorBoundary>')
    expect(root).toContain('<App />')
    expect(boundary).toContain('onErrorCaptured')
    expect(boundary).toContain('return false')
    expect(boundary).toContain('window.location.reload()')
  })

  it('does not introduce a fatal global unhandled-rejection handler', () => {
    const production = [
      source('main.js'),
      source('AppRoot.vue'),
      source('components', 'runtime', 'AppRuntimeErrorBoundary.vue'),
      source('utils', 'bootstrapFailure.js'),
    ].join('\n')

    expect(production).not.toContain('unhandledrejection')
    expect(production).not.toContain('app.config.errorHandler')
  })
})
