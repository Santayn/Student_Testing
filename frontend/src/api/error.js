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

  return Number.isInteger(status)
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

export function isApiNetworkError(error) {
  const isAxiosError =
    error?.isAxiosError === true ||
    Boolean(error?.config)

  return Boolean(
    isAxiosError &&
    !error?.response &&
    !isApiTimeout(error)
  )
}

export function getApiErrorMessage(
  error,
  fallback = 'Не удалось выполнить запрос'
) {
  if (!error) return fallback

  if (isApiTimeout(error)) {
    return 'Сервер слишком долго отвечает'
  }

  /*
   * Важно отличать Axios network error от обычной
   * JavaScript-ошибки клиента.
   *
   * Раньше TypeError вроде:
   *   learningApi.submitAttempt is not a function
   * попадал сюда же и показывался как:
   *   «Не удалось связаться с сервером».
   */
  if (isApiNetworkError(error)) {
    return 'Не удалось связаться с сервером'
  }

  if (!error.response) {
    return (
      error?.message ||
      fallback
    )
  }

  const payload =
    responsePayload(error)

  if (
    typeof payload === 'string' &&
    payload.trim()
  ) {
    return payload
  }

  const details =
    validationDetails(payload)

  const baseMessage =
    payload?.message ||
    payload?.detail ||
    payload?.title ||
    payload?.error_description ||
    (
      typeof payload?.error ===
      'string'
        ? payload.error
        : ''
    ) ||
    fallback

  return details
    ? `${baseMessage}: ${details}`
    : baseMessage
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
  }
}
