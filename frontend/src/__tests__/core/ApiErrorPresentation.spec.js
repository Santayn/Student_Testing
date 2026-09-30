import {
  describe,
  expect,
  it,
} from 'vitest'

import {
  apiFieldError,
  presentApiError,
} from '@/utils/apiErrorPresentation'

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

describe('API error presentation contract', () => {
  it('routes structured form validation to field errors', () => {
    const result = presentApiError(
      responseError(
        422,
        {
          message: 'Проверьте данные',
          details: [
            {
              field: 'name',
              issue: 'Обязательное поле',
            },
          ],
        }
      ),
      {
        context: 'form',
        fallback: 'Не удалось сохранить',
      }
    )

    expect(result.channel).toBe('field')
    expect(
      apiFieldError(
        result,
        'name'
      )
    ).toBe('Обязательное поле')
  })

  it('keeps non-field form failures inside the form', () => {
    const result = presentApiError(
      responseError(
        409,
        {
          message:
            'Конфликт данных',
        }
      ),
      {
        context: 'form',
        fallback:
          'Не удалось сохранить',
      }
    )

    expect(result.channel).toBe('form')
    expect(result.message).toBe(
      'Конфликт данных'
    )
  })

  it('keeps delete failures inside the confirmation dialog', () => {
    const result = presentApiError(
      responseError(403),
      {
        context: 'delete',
        fallback:
          'Не удалось удалить',
        forbiddenMessage:
          'Недостаточно прав.',
      }
    )

    expect(result.channel).toBe(
      'dialog'
    )
    expect(result.message).toBe(
      'Недостаточно прав.'
    )
  })

  it('keeps read failures persistent and inline', () => {
    const result = presentApiError(
      responseError(404),
      {
        context: 'route-resource',
        fallback:
          'Не удалось загрузить',
        notFoundMessage:
          'Ресурс не найден.',
      }
    )

    expect(result.channel).toBe(
      'inline'
    )
    expect(result.message).toBe(
      'Ресурс не найден.'
    )
  })

  it('adds Retry-After guidance to rate-limited actions', () => {
    const result = presentApiError(
      responseError(
        429,
        {
          message:
            'Слишком много запросов',
        },
        {
          'retry-after': '5',
        }
      ),
      {
        context: 'action',
      }
    )

    expect(result.channel).toBe(
      'toast'
    )
    expect(result.message).toContain(
      '5 сек.'
    )
  })

  it('does not turn 401 into a presentation-side logout action', () => {
    const result = presentApiError(
      responseError(
        401,
        {
          message:
            'Unauthorized',
        }
      ),
      {
        context: 'read',
      }
    )

    expect(result.status).toBe(401)
    expect(result.channel).toBe(
      'inline'
    )
  })
})
