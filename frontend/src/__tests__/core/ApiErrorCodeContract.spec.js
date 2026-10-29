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
        .CONFLICT
    ).toBe(
      'conflict'
    )

    expect(
      API_ERROR_CODES
        .VALIDATION_FAILED
    ).toBe(
      'validation_failed'
    )
  })

  it('resolves a message by exact machine-readable code', () => {
    expect(
      apiErrorCodeMessage(
        API_ERROR_CODES.CONFLICT,
        {
          [API_ERROR_CODES.CONFLICT]:
            'Обнаружен конфликт данных.',
        }
      )
    ).toBe(
      'Обнаружен конфликт данных.'
    )
  })

  it('prefers a code-specific message over generic status semantics', () => {
    const result =
      presentApiError(
        responseError(
          409,
          {
            code:
              API_ERROR_CODES.CONFLICT,
            message:
              'Generic conflict',
          }
        ),
        {
          context: 'delete',
          codeMessages: {
            [API_ERROR_CODES.CONFLICT]:
              'Операция конфликтует с данными.',
          },
          conflictMessage:
            'Конфликт данных.',
        }
      )

    expect(result.message).toBe(
      'Операция конфликтует с данными.'
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
            [API_ERROR_CODES.VALIDATION_FAILED]:
              'Проверьте данные.',
          },
        }
      )

    expect(result.message).toBe(
      'Невозможно выполнить операцию.'
    )
  })
})
