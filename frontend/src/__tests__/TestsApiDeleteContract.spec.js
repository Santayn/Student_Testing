import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import http from '../api/http'
import { testsApi } from '../api/tests.api'

vi.mock('../api/http', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('tests API delete contract', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('deletes a test through the backend-supported endpoint', async () => {
    http.delete.mockResolvedValue({})

    await testsApi.delete(101)

    expect(http.delete).toHaveBeenCalledWith(
      '/tests/101'
    )
  })
})
