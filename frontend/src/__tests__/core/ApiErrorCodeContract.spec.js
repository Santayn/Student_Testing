import {
  describe,
  expect,
  it,
} from 'vitest'

import {
  API_ERROR_CODES,
  apiErrorCodeMessage,
} from '@/api'

import {
  presentApiError,
} from '@/utils/apiErrorPresentation'

function responseError(
  status,
  data = undefined
) {
  return {
    isAxiosError: true,
    config: {},
    response: {
      status,
      data,
    },
  }
}

describe('machine-readable API error-code contract', () => {
  it('keeps stable frontend error-code constants', () => {
    expect(
      API_ERROR_CODES
        .GROUP_HAS_DEPENDENCIES
    ).toBe(
      'GROUP_HAS_DEPENDENCIES'
    )

    expect(
      API_ERROR_CODES
        .VALIDATION_ERROR
    ).toBe(
      'VALIDATION_ERROR'
    )
  })

  it('resolves a message by exact machine-readable code', () => {
    expect(
      apiErrorCodeMessage(
        'GROUP_HAS_DEPENDENCIES',
        {
          GROUP_HAS_DEPENDENCIES:
            'Группа используется.',
        }
      )
    ).toBe(
      'Группа используется.'
    )
  })

  it('prefers a code-specific message over generic status semantics', () => {
    const result =
      presentApiError(
        responseError(
          409,
          {
            code:
              'GROUP_HAS_DEPENDENCIES',
            message:
              'Generic conflict',
          }
        ),
        {
          context: 'delete',
          codeMessages: {
            GROUP_HAS_DEPENDENCIES:
              'Группа используется связанными данными.',
          },
          conflictMessage:
            'Конфликт данных.',
        }
      )

    expect(result.message).toBe(
      'Группа используется связанными данными.'
    )
  })

  it('falls back to status-specific behavior when backend sends no code', () => {
    const result =
      presentApiError(
        responseError(
          409,
          {
            message:
              'Generic conflict',
          }
        ),
        {
          context: 'delete',
          conflictMessage:
            'Конфликт данных.',
        }
      )

    expect(result.message).toBe(
      'Конфликт данных.'
    )
  })

  it('falls back to backend/fallback message for an unknown code', () => {
    const result =
      presentApiError(
        responseError(
          422,
          {
            code:
              'UNKNOWN_DOMAIN_CODE',
            message:
              'Невозможно выполнить операцию.',
          }
        ),
        {
          context: 'form',
          codeMessages: {
            VALIDATION_ERROR:
              'Проверьте данные.',
          },
        }
      )

    expect(result.message).toBe(
      'Невозможно выполнить операцию.'
    )
  })
})
