import { beforeEach, describe, expect, it, vi } from 'vitest'

import http from '../api/http'
import { teachingApi } from '../api/teaching.api'

vi.mock('../api/http', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}))

describe('teaching API contract', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads subject load types with backend-supported filters', async () => {
    http.get.mockResolvedValue({ data: [] })

    const params = {
      subjectMembershipId: 17,
      teachingLoadTypeId: 3,
    }

    await teachingApi.getSubjectLoadTypes(params)

    expect(http.get).toHaveBeenCalledWith(
      '/teaching/subject-load-types',
      { params }
    )
  })
})
