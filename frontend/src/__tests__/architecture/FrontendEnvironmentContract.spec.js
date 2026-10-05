// @vitest-environment node

import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import { describe, expect, it } from 'vitest'

describe('frontend environment contract', () => {
  const projectRoot = resolve(process.cwd(), '..')
  const rootEnvExample = resolve(projectRoot, '.env.example')
  const frontendEnvExample = resolve(process.cwd(), '.env.example')
  const backendEnvExample = resolve(projectRoot, 'backend', '.env.example')

  it('uses the root environment template as the single project configuration reference', () => {
    const envExample = readFileSync(rootEnvExample, 'utf8')

    expect(existsSync(frontendEnvExample)).toBe(false)
    expect(existsSync(backendEnvExample)).toBe(false)
    expect(envExample).toContain('BACKEND_PORT')
    expect(envExample).toContain('VITE_API_BASE_URL')
    expect(envExample).toContain('APP_PUBLIC_REGISTRATION_ENABLED')
    expect(envExample).not.toContain('VITE_PUBLIC_REGISTRATION_ENABLED')
    expect(envExample).not.toContain('VITE_API_ROOT_URL')
  })

  it('loads Vite env and the development proxy from the project root', () => {
    const viteConfig = readFileSync(
      resolve(process.cwd(), 'vite.config.js'),
      'utf8'
    )

    expect(viteConfig).toContain('envDir: projectRoot')
    expect(viteConfig).toContain("'BACKEND_'")
    expect(viteConfig).toContain('configEnv.BACKEND_PORT')
    expect(viteConfig).toContain("'import.meta.env.APP_PUBLIC_REGISTRATION_ENABLED'")
  })
})
