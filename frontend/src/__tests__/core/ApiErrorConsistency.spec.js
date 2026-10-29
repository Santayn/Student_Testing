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
  isApiCanceled,
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
              'conflict',
          }
        )
      )
    ).toBe(
      'conflict'
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
          400,
          {
            code:
              'validation_failed',
            message:
              'Validation error',
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
    ).toBe(400)

    expect(
      normalized.code
    ).toBe(
      'validation_failed'
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
      'Проверьте введённые данные'
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

  it('does not classify canceled requests as network errors', () => {
    const canceled = {
      isAxiosError: true,
      config: {},
      code: 'ERR_CANCELED',
    }

    expect(isApiCanceled(canceled)).toBe(true)
    expect(isApiNetworkError(canceled)).toBe(false)
    expect(normalizeApiError(canceled)).toMatchObject({
      isCanceled: true,
      isNetworkError: false,
      message: 'Запрос отменён',
    })
  })

  it('localizes standard backend errors and retains traceId', () => {
    const forbidden = normalizeApiError(responseError(403, {
      code: 'forbidden',
      message: 'Not enough permissions.',
      details: [],
      traceId: 'test-trace-403',
    }))

    expect(forbidden).toMatchObject({
      status: 403,
      code: 'forbidden',
      traceId: 'test-trace-403',
      message: 'Недостаточно прав для выполнения операции.',
    })

    const serverFailure = normalizeApiError(responseError(500, {
      code: 'internal_error',
      message: 'Unexpected server error',
      traceId: 'test-trace-500',
    }))

    expect(serverFailure.message).toBe(
      'На сервере произошла ошибка. Попробуйте позже.'
    )
    expect(serverFailure.traceId).toBe('test-trace-500')
  })

  it('distinguishes AI and backup service failures, preserving upload limits', () => {
    expect(normalizeApiError(responseError(503, {
      code: 'ai_unavailable',
      message: 'Provider unavailable',
    })).message).toContain('Сервис ИИ')

    expect(normalizeApiError(responseError(503, {
      code: 'database_backup_failed',
      message: 'Backup failed',
    })).message).toContain('резервного копирования')

    expect(normalizeApiError(responseError(413, {
      code: 'payload_too_large',
      message: 'Размер загружаемых файлов превышает допустимый лимит: до 50 МБ на файл.',
    })).message).toContain('50 МБ')
  })

  it('protects the UI from proxy HTML responses', () => {
    const proxyError = responseError(502, '<html><body>Bad Gateway</body></html>', {
      'content-type': 'text/html; charset=UTF-8',
    })

    expect(normalizeApiError(proxyError).message).toBe(
      'Сервер временно недоступен. Попробуйте позже.'
    )
  })

  it('normalizes 400 validation_failed details into field errors', () => {
    const result = normalizeApiError(responseError(400, {
      code: 'validation_failed',
      message: 'Validation error',
      traceId: 'validation-trace',
      details: [{ field: 'title', issue: 'Название обязательно' }],
    }))

    expect(result.message).toBe('Проверьте введённые данные: title: Название обязательно')
    expect(result.fieldErrors).toEqual({ title: ['Название обязательно'] })
    expect(result.traceId).toBe('validation-trace')
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
