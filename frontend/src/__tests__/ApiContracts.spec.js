import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import http from '@/api/http'
import { lecturesApi } from '@/api/lectures.api'
import { membershipsApi } from '@/api/memberships.api'
import { teachingApi } from '@/api/teaching.api'
import { testsApi } from '@/api/tests.api'

vi.mock('@/api/http', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('frontend API contracts', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads subject load types through the supported endpoint', async () => {
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

  it('adds a load type to a concrete subject membership', async () => {
    http.post.mockResolvedValue({ data: {} })

    const payload = {
      teachingLoadTypeId: 3,
      hours: 36,
    }

    await teachingApi.addLoadTypeToSubjectMembership(
      17,
      payload
    )

    expect(http.post).toHaveBeenCalledWith(
      '/teaching/subject-memberships/17/load-types',
      payload
    )
  })

  it('uploads lecture materials with the backend multipart field name', async () => {
    http.post.mockResolvedValue({ data: [] })

    const first = new File(['a'], 'a.txt', {
      type: 'text/plain',
    })
    const second = new File(['b'], 'b.txt', {
      type: 'text/plain',
    })

    await lecturesApi.uploadMaterials(9, [
      first,
      second,
    ])

    expect(http.post).toHaveBeenCalledTimes(1)

    const [url, body] = http.post.mock.calls[0]

    expect(url).toBe('/lectures/9/materials')
    expect(body).toBeInstanceOf(FormData)
    expect(body.getAll('files')).toEqual([
      first,
      second,
    ])
    expect(body.has('file')).toBe(false)
  })

  it('updates a full subject membership through the supported endpoint', async () => {
    http.put.mockResolvedValue({ data: {} })

    const payload = {
      status: 1,
      notes: 'Возобновлено',
    }

    await membershipsApi.updateSubjectMembership(44, payload)

    expect(http.put).toHaveBeenCalledWith(
      '/memberships/subjects/memberships/44',
      payload
    )
  })

  it('deletes a test through the supported endpoint', async () => {
    http.delete.mockResolvedValue({})

    await testsApi.delete(101)

    expect(http.delete).toHaveBeenCalledWith(
      '/tests/101'
    )
  })
})
