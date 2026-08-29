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

export function getApiErrorMessage(
  error,
  fallback = 'Не удалось выполнить запрос'
) {
  if (!error) return fallback

  if (error.code === 'ECONNABORTED') {
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
  const isAxiosError =
    error?.isAxiosError === true ||
    Boolean(error?.config)

  if (isAxiosError && !error.response) {
    return 'Не удалось связаться с сервером'
  }

  if (!error.response) {
    return (
      error?.message ||
      fallback
    )
  }

  const payload = error.response.data

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
