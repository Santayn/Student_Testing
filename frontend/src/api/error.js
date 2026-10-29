import { API_ERROR_CODES } from './errorCodes'

function validationDetails(payload) {
  if (!Array.isArray(payload?.details)) {
    return ''
  }

  return payload.details
    .map((detail) => {
      if (!detail || typeof detail !== 'object') {
        return ''
      }

      const field =
        detail.field
          ? String(detail.field)
          : ''

      const issue =
        detail.issue
          ? String(detail.issue)
          : ''

      if (field && issue) {
        return `${field}: ${issue}`
      }

      return issue || field
    })
    .filter(Boolean)
    .join('; ')
}

function normalizedFieldErrors(payload) {
  if (!Array.isArray(payload?.details)) {
    return {}
  }

  const result = {}

  for (const detail of payload.details) {
    if (
      !detail ||
      typeof detail !== 'object'
    ) {
      continue
    }

    const field =
      detail.field
        ? String(detail.field).trim()
        : ''

    const issue =
      detail.issue
        ? String(detail.issue).trim()
        : ''

    if (!field || !issue) {
      continue
    }

    if (!result[field]) {
      result[field] = []
    }

    result[field].push(issue)
  }

  return result
}

function responsePayload(error) {
  return error?.response?.data
}

function responseHeader(
  error,
  headerName
) {
  const headers =
    error?.response?.headers

  if (!headers) {
    return null
  }

  if (
    typeof headers.get ===
    'function'
  ) {
    return (
      headers.get(headerName) ??
      headers.get(
        headerName.toLowerCase()
      )
    )
  }

  const expected =
    headerName.toLowerCase()

  const entry =
    Object.entries(headers)
      .find(
        ([key]) =>
          String(key)
            .toLowerCase() ===
          expected
      )

  return entry?.[1] ?? null
}

export function apiErrorStatus(error) {
  const status =
    Number(
      error?.response?.status
    )

  return Number.isInteger(status) && status >= 100 && status <= 599
    ? status
    : null
}

export function apiErrorCode(error) {
  const payload =
    responsePayload(error)

  if (
    !payload ||
    typeof payload !== 'object'
  ) {
    return null
  }

  const value =
    payload.code ??
    payload.errorCode ??
    null

  if (
    value === null ||
    value === undefined ||
    value === ''
  ) {
    return null
  }

  return String(value)
}

export function apiRetryAfterSeconds(
  error
) {
  const value =
    responseHeader(
      error,
      'Retry-After'
    )

  if (
    value === null ||
    value === undefined ||
    value === ''
  ) {
    return null
  }

  const seconds = Number(value)

  if (
    Number.isFinite(seconds) &&
    seconds >= 0
  ) {
    return Math.ceil(seconds)
  }

  const retryAt =
    new Date(value).getTime()

  if (!Number.isFinite(retryAt)) {
    return null
  }

  return Math.max(
    0,
    Math.ceil(
      (retryAt - Date.now()) /
      1000
    )
  )
}

export function isApiBadRequest(error) {
  return apiErrorStatus(error) === 400
}

export function isApiUnauthorized(error) {
  return apiErrorStatus(error) === 401
}

export function isApiForbidden(error) {
  return apiErrorStatus(error) === 403
}

export function isApiNotFound(error) {
  return apiErrorStatus(error) === 404
}

export function isApiConflict(error) {
  return apiErrorStatus(error) === 409
}

export function isApiUnprocessable(error) {
  return apiErrorStatus(error) === 422
}

export function isApiRateLimited(error) {
  return apiErrorStatus(error) === 429
}

export function isApiTimeout(error) {
  return (
    error?.code ===
      'ECONNABORTED' ||
    error?.code ===
      'ETIMEDOUT'
  )
}

export function isApiCanceled(error) {
  return error?.code === 'ERR_CANCELED' || error?.__CANCEL__ === true
}

export function isApiNetworkError(error) {
  const isAxiosError =
    error?.isAxiosError === true ||
    Boolean(error?.config)

  return Boolean(
    isAxiosError &&
    !error?.response &&
    !isApiTimeout(error) &&
    !isApiCanceled(error)
  )
}

// These messages come from ApiExceptionHandler and carry no user-facing
// meaning beyond their machine-readable code. Localize them without hiding
// domain-specific details sent by other endpoints.
const STANDARD_MESSAGES = Object.freeze({
  [API_ERROR_CODES.VALIDATION_FAILED]: 'Проверьте введённые данные',
  [API_ERROR_CODES.UNAUTHORIZED]: 'Необходима авторизация. Войдите в систему.',
  [API_ERROR_CODES.FORBIDDEN]: 'Недостаточно прав для выполнения операции.',
  [API_ERROR_CODES.NOT_FOUND]: 'Запрошенные данные не найдены.',
  [API_ERROR_CODES.CONFLICT]: 'Операция конфликтует с существующими данными.',
  [API_ERROR_CODES.INTERNAL_ERROR]: 'На сервере произошла ошибка. Попробуйте позже.',
  [API_ERROR_CODES.AI_UNAVAILABLE]: 'Сервис ИИ временно недоступен. Попробуйте позже.',
  [API_ERROR_CODES.DATABASE_BACKUP_FAILED]: 'Не удалось выполнить операцию резервного копирования. Попробуйте позже.',
})

function isHtmlResponse(error, payload) {
  const contentType = String(responseHeader(error, 'Content-Type') ?? '')
    .toLowerCase()

  if (contentType.includes('text/html')) {
    return true
  }

  return typeof payload === 'string' &&
    /<\s*(?:!doctype\s+html|html|head|body|title|h[1-6])\b/i.test(payload)
}

function readableBackendMessage(payload) {
  const message =
    payload?.message ||
    payload?.detail ||
    payload?.title ||
    payload?.error_description ||
    (typeof payload?.error === 'string' ? payload.error : '')

  return typeof message === 'string' ? message.trim() : ''
}

function resolveBackendMessage(error, fallback) {
  const payload = responsePayload(error)
  const code = apiErrorCode(error)
  const status = apiErrorStatus(error)

  if (isHtmlResponse(error, payload)) {
    return status >= 500
      ? 'Сервер временно недоступен. Попробуйте позже.'
      : fallback
  }

  if (typeof payload === 'string') {
    return payload.trim() || fallback
  }

  const message = readableBackendMessage(payload)

  // An internal error should never expose its raw technical description.
  if (code === API_ERROR_CODES.INTERNAL_ERROR) {
    return STANDARD_MESSAGES[code]
  }

  // Preserve meaningful server-specific messages (including upload limits).
  if (code === API_ERROR_CODES.PAYLOAD_TOO_LARGE) {
    return message || 'Размер загружаемых файлов превышает допустимый лимит.'
  }

  if (code === API_ERROR_CODES.BAD_REQUEST) {
    if (/^Malformed request body$/i.test(message)) {
      return 'Некорректный формат запроса.'
    }
    if (/^Missing request parameter:/i.test(message)) {
      return 'Не указан обязательный параметр запроса.'
    }
    if (/^Invalid value for parameter:/i.test(message)) {
      return 'Некорректное значение параметра запроса.'
    }
    return message || 'Проверьте параметры запроса.'
  }

  // The exception handler's default conflict message does not indicate
  // which relation (if any) is responsible. Never infer dependencies from 409.
  if (code === API_ERROR_CODES.CONFLICT) {
    return !message || message === 'The request conflicts with existing data.'
      ? STANDARD_MESSAGES[code]
      : message
  }

  // The backend emits English technical messages for these codes. The
  // concrete page can still override the text with presentApiError().
  if (code && STANDARD_MESSAGES[code]) {
    return STANDARD_MESSAGES[code]
  }

  if (status >= 500 && (!message || /^Unexpected server error$/i.test(message))) {
    return 'Сервер временно недоступен. Попробуйте позже.'
  }

  return message || fallback
}

export function getApiErrorMessage(
  error,
  fallback = 'Не удалось выполнить запрос'
) {
  if (!error) return fallback

  if (isApiCanceled(error)) {
    return 'Запрос отменён'
  }

  if (isApiTimeout(error)) {
    return 'Сервер слишком долго отвечает'
  }

  // Transport failures and ordinary client-side errors are distinct.
  if (isApiNetworkError(error)) {
    return 'Не удалось связаться с сервером'
  }

  if (!error.response) {
    return error?.message || fallback
  }

  const message = resolveBackendMessage(error, fallback)
  const details = validationDetails(responsePayload(error))

  return details ? `${message}: ${details}` : message
}

export function normalizeApiError(
  error,
  fallback = 'Не удалось выполнить запрос'
) {
  const payload =
    responsePayload(error)

  return {
    status:
      apiErrorStatus(error),

    code:
      apiErrorCode(error),

    traceId:
      typeof payload?.traceId === 'string' && payload.traceId.trim()
        ? payload.traceId.trim()
        : null,

    message:
      getApiErrorMessage(
        error,
        fallback
      ),

    fieldErrors:
      normalizedFieldErrors(
        payload
      ),

    retryAfterSeconds:
      apiRetryAfterSeconds(
        error
      ),

    isTimeout:
      isApiTimeout(error),

    isNetworkError:
      isApiNetworkError(error),

    isCanceled:
      isApiCanceled(error),
  }
}
