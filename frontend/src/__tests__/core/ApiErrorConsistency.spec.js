import {
  describe,
  expect,
  it,
} from 'vitest'

import {
  apiErrorCode,
  apiErrorStatus,
  apiRetryAfterSeconds,
  isApiBadRequest,
  isApiConflict,
  isApiForbidden,
  isApiNetworkError,
  isApiNotFound,
  isApiRateLimited,
  isApiTimeout,
  isApiUnauthorized,
  isApiUnprocessable,
  normalizeApiError,
} from '@/api'

function responseError(
  status,
  data = undefined,
  headers = undefined
) {
  return {
    isAxiosError: true,
    config: {},
    response: {
      status,
      data,
      headers,
    },
  }
}

describe('API error consistency helpers', () => {
  it('extracts only a numeric HTTP response status', () => {
    expect(
      apiErrorStatus(
        responseError(409)
      )
    ).toBe(409)

    expect(
      apiErrorStatus({
        response: {
          status: '404',
        },
      })
    ).toBe(404)

    expect(
      apiErrorStatus(
        new Error('boom')
      )
    ).toBeNull()
  })

  it('classifies supported 4xx statuses without parsing backend text', () => {
    expect(
      isApiBadRequest(
        responseError(400)
      )
    ).toBe(true)

    expect(
      isApiUnauthorized(
        responseError(401)
      )
    ).toBe(true)

    expect(
      isApiForbidden(
        responseError(403)
      )
    ).toBe(true)

    expect(
      isApiNotFound(
        responseError(404)
      )
    ).toBe(true)

    expect(
      isApiConflict(
        responseError(409)
      )
    ).toBe(true)

    expect(
      isApiUnprocessable(
        responseError(422)
      )
    ).toBe(true)

    expect(
      isApiRateLimited(
        responseError(429)
      )
    ).toBe(true)

    expect(
      isApiConflict(
        responseError(400)
      )
    ).toBe(false)
  })

  it('extracts a machine-readable backend error code when provided', () => {
    expect(
      apiErrorCode(
        responseError(
          409,
          {
            code:
              'GROUP_HAS_DEPENDENCIES',
          }
        )
      )
    ).toBe(
      'GROUP_HAS_DEPENDENCIES'
    )

    expect(
      apiErrorCode(
        responseError(
          422,
          {
            errorCode:
              'ATTEMPTS_EXHAUSTED',
          }
        )
      )
    ).toBe(
      'ATTEMPTS_EXHAUSTED'
    )

    expect(
      apiErrorCode(
        responseError(
          400,
          {
            message: 'Ошибка',
          }
        )
      )
    ).toBeNull()
  })

  it('normalizes field validation details without losing the general message', () => {
    const normalized =
      normalizeApiError(
        responseError(
          422,
          {
            code:
              'VALIDATION_ERROR',
            message:
              'Проверьте данные',
            details: [
              {
                field: 'name',
                issue:
                  'Обязательное поле',
              },
              {
                field: 'name',
                issue:
                  'Слишком короткое значение',
              },
              {
                field: 'code',
                issue:
                  'Код уже используется',
              },
            ],
          }
        )
      )

    expect(
      normalized.status
    ).toBe(422)

    expect(
      normalized.code
    ).toBe(
      'VALIDATION_ERROR'
    )

    expect(
      normalized.fieldErrors
    ).toEqual({
      name: [
        'Обязательное поле',
        'Слишком короткое значение',
      ],
      code: [
        'Код уже используется',
      ],
    })

    expect(
      normalized.message
    ).toContain(
      'Проверьте данные'
    )
  })

  it('normalizes Retry-After expressed as seconds', () => {
    const error =
      responseError(
        429,
        {
          message:
            'Слишком много запросов',
        },
        {
          'retry-after': '7',
        }
      )

    expect(
      apiRetryAfterSeconds(
        error
      )
    ).toBe(7)

    expect(
      normalizeApiError(
        error
      ).retryAfterSeconds
    ).toBe(7)
  })

  it('distinguishes timeout and network failures from HTTP responses', () => {
    const timeout = {
      isAxiosError: true,
      config: {},
      code: 'ECONNABORTED',
    }

    const network = {
      isAxiosError: true,
      config: {},
      message: 'Network Error',
    }

    expect(
      isApiTimeout(timeout)
    ).toBe(true)

    expect(
      isApiNetworkError(timeout)
    ).toBe(false)

    expect(
      isApiNetworkError(network)
    ).toBe(true)

    const normalized =
      normalizeApiError(network)

    expect(
      normalized.status
    ).toBeNull()

    expect(
      normalized.isNetworkError
    ).toBe(true)

    expect(
      normalized.isTimeout
    ).toBe(false)
  })

  it('does not classify ordinary JavaScript errors as network failures', () => {
    const error =
      new TypeError(
        'submitAttempt is not a function'
      )

    const normalized =
      normalizeApiError(error)

    expect(
      normalized.isNetworkError
    ).toBe(false)

    expect(
      normalized.isTimeout
    ).toBe(false)

    expect(
      normalized.message
    ).toContain(
      'submitAttempt is not a function'
    )
  })
})
