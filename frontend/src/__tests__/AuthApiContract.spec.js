import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

const clients = vi.hoisted(() => ({
  httpPost: vi.fn(),
  httpGet: vi.fn(),
  authPost: vi.fn(),
}))

vi.mock('@/api/http', () => ({
  default: {
    post: clients.httpPost,
    get: clients.httpGet,
  },
  authHttp: {
    post: clients.authPost,
  },
}))

import {
  authApi,
} from '@/api/auth.api'

describe('auth API safety contract', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('does not response-refresh and retry a rejected current password', async () => {
    clients.httpPost.mockResolvedValueOnce({
      status: 204,
    })

    await authApi.changePassword({
      currentPassword: 'old-password',
      newPassword: 'new-password',
    })

    expect(clients.httpPost)
      .toHaveBeenCalledWith(
        '/auth/change-password',
        {
          currentPassword:
            'old-password',
          newPassword:
            'new-password',
        },
        {
          skipAuthRefresh: true,
        }
      )
  })

  it('revokes with credentials captured from the session being closed', async () => {
    clients.authPost.mockResolvedValueOnce({
      status: 204,
    })

    await authApi.revoke(
      'old-refresh',
      'old-access',
      'Bearer'
    )

    expect(clients.authPost)
      .toHaveBeenCalledWith(
        '/auth/revoke',
        {
          refreshToken:
            'old-refresh',
        },
        {
          headers: {
            Authorization:
              'Bearer old-access',
          },
        }
      )
  })
})
