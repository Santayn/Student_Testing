export function getApiErrorMessage(error, fallback = 'Не удалось выполнить запрос') {
  if (!error) {
    return fallback
  }

  if (error.code === 'ECONNABORTED') {
    return 'Сервер слишком долго отвечает'
  }

  if (!error.response) {
    return 'Не удалось связаться с сервером'
  }

  const payload = error.response.data

  if (typeof payload === 'string' && payload.trim()) {
    return payload
  }

  if (payload?.message) {
    return payload.message
  }

  if (payload?.detail) {
    return payload.detail
  }

  if (payload?.title) {
    return payload.title
  }

  if (payload?.error_description) {
    return payload.error_description
  }

  if (typeof payload?.error === 'string') {
    return payload.error
  }

  if (payload?.errors && typeof payload.errors === 'object') {
    const firstKey = Object.keys(payload.errors)[0]
    const firstError = firstKey ? payload.errors[firstKey] : null

    if (Array.isArray(firstError) && firstError.length) {
      return firstError[0]
    }

    if (typeof firstError === 'string') {
      return firstError
    }
  }

  return fallback
}
