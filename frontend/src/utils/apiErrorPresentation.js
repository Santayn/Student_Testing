import {
  apiErrorCodeMessage,
  normalizeApiError,
} from '@/api'

const CONTEXT_CHANNEL = Object.freeze({
  read: 'inline',
  form: 'form',
  delete: 'dialog',
  action: 'toast',
  'route-resource': 'inline',
})

function hasFieldErrors(fieldErrors) {
  return Object.values(fieldErrors ?? {})
    .some(
      (messages) =>
        Array.isArray(messages) &&
        messages.length > 0
    )
}

function overrideMessage(
  normalized,
  {
    codeMessages = {},
    badRequestMessage = '',
    forbiddenMessage = '',
    notFoundMessage = '',
    conflictMessage = '',
    unprocessableMessage = '',
    rateLimitedMessage = '',
  }
) {
  const codeMessage =
    apiErrorCodeMessage(
      normalized.code,
      codeMessages
    )

  if (codeMessage) {
    return codeMessage
  }

  switch (normalized.status) {
    case 400:
      return badRequestMessage || normalized.message
    case 403:
      return forbiddenMessage || normalized.message
    case 404:
      return notFoundMessage || normalized.message
    case 409:
      return conflictMessage || normalized.message
    case 422:
      return unprocessableMessage || normalized.message
    case 429: {
      const base =
        rateLimitedMessage ||
        normalized.message ||
        'Слишком много запросов.'

      if (
        normalized.retryAfterSeconds !== null &&
        normalized.retryAfterSeconds !== undefined
      ) {
        return `${base} Повторите через ${normalized.retryAfterSeconds} сек.`
      }

      return base
    }
    default:
      return normalized.message
  }
}

export function presentApiError(
  error,
  {
    context = 'action',
    fallback = 'Не удалось выполнить запрос',
    codeMessages = {},
    badRequestMessage = '',
    forbiddenMessage = '',
    notFoundMessage = '',
    conflictMessage = '',
    unprocessableMessage = '',
    rateLimitedMessage = '',
  } = {}
) {
  const normalized =
    normalizeApiError(
      error,
      fallback
    )

  const fieldErrors =
    normalized.fieldErrors ?? {}

  let channel =
    CONTEXT_CHANNEL[context] ??
    'inline'

  if (
    context === 'form' &&
    hasFieldErrors(fieldErrors)
  ) {
    channel = 'field'
  }

  return {
    ...normalized,
    channel,
    fieldErrors,
    message: overrideMessage(
      normalized,
      {
        codeMessages,
        badRequestMessage,
        forbiddenMessage,
        notFoundMessage,
        conflictMessage,
        unprocessableMessage,
        rateLimitedMessage,
      }
    ),
  }
}

export function apiFieldError(
  presentation,
  ...fieldNames
) {
  for (const fieldName of fieldNames) {
    const messages =
      presentation?.fieldErrors?.[
        fieldName
      ]

    if (
      Array.isArray(messages) &&
      messages.length
    ) {
      return messages[0]
    }
  }

  return ''
}
