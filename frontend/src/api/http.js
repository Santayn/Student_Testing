import axios from 'axios'

const TOKEN_KEY = 'student-testing.access-token'

/**
 * Основной backend API: /api/v1/...
 */
export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15000,
  headers: {
    Accept: 'application/json',
  },
})

/**
 * Некоторые endpoint'ы существующего backend находятся под /api/...
 * без /v1, например импорт вопросов и загрузка материалов.
 */
export const rootHttp = axios.create({
  baseURL: import.meta.env.VITE_API_ROOT_URL || '/api',
  timeout: 15000,
  headers: {
    Accept: 'application/json',
  },
})

let accessTokenProvider = () => localStorage.getItem(TOKEN_KEY)

/**
 * Позволяет позже подключить AuthStore, не импортируя Pinia прямо в http.js.
 *
 * Пример после создания authStore:
 * setAccessTokenProvider(() => authStore.accessToken)
 */
export function setAccessTokenProvider(provider) {
  if (typeof provider !== 'function') {
    throw new TypeError('Access token provider должен быть функцией')
  }

  accessTokenProvider = provider
}

function applyAuth(config) {
  const token = accessTokenProvider?.()

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
}

function responseSuccess(response) {
  return response
}

function responseError(error) {
  return Promise.reject(error)
}

function installInterceptors(instance) {
  instance.interceptors.request.use(
    applyAuth,
    error => Promise.reject(error),
  )

  instance.interceptors.response.use(
    responseSuccess,
    responseError,
  )
}

installInterceptors(http)
installInterceptors(rootHttp)

export default http
