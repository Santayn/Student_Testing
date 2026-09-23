import {
  describe,
  expect,
  it,
} from 'vitest'

import {
  getAuthErrorMessage,
} from '@/utils/authErrorMessage'

function responseError(status, message = 'backend message') {
  return {
    isAxiosError: true,
    config: {},
    response: {
      status,
      data: {
        message,
      },
    },
  }
}

describe('auth-specific error messages', () => {
  it('does not expose backend credential text on login', () => {
    expect(
      getAuthErrorMessage(
        responseError(401, 'Invalid login or password.'),
        'login'
      )
    ).toBe('Неверный логин или пароль.')
  })

  it('maps wrong current password to a local user-facing message', () => {
    expect(
      getAuthErrorMessage(
        responseError(401, 'The current password is incorrect.'),
        'password'
      )
    ).toBe('Текущий пароль указан неверно.')
  })

  it('maps registration conflict without exposing backend wording', () => {
    expect(
      getAuthErrorMessage(
        responseError(409, 'A user with this login already exists.'),
        'register'
      )
    ).toBe('Пользователь с таким логином уже существует.')
  })

  it('distinguishes timeout and network failures', () => {
    expect(
      getAuthErrorMessage(
        { code: 'ECONNABORTED' },
        'login'
      )
    ).toContain('слишком долго')

    expect(
      getAuthErrorMessage(
        {
          isAxiosError: true,
          config: {},
        },
        'login'
      )
    ).toContain('Не удалось связаться с сервером')
  })
})
