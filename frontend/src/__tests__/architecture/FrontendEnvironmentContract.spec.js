// @vitest-environment node

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import { describe, expect, it } from 'vitest'

describe('frontend environment contract', () => {
  it('does not advertise configuration that production source does not consume', () => {
    const envExample = readFileSync(
      resolve(process.cwd(), '.env.example'),
      'utf8'
    )

    expect(envExample).not.toContain('VITE_API_ROOT_URL')
  })
})
