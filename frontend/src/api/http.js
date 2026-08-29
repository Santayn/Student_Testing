import axios from 'axios'

const clientConfig = {
  baseURL: '/api/v1',
  timeout: 15000,

  headers: {
    Accept: 'application/json',
  },
}

/*
 * authHttp не содержит auth interceptors.
 *
 * Через него выполняются:
 * login / register / refresh.
 *
 * Это важно, чтобы refresh-запрос сам не попал
 * в обработчик 401 и не создал бесконечный цикл.
 */
export const authHttp =
  axios.create(clientConfig)

const http =
  axios.create(clientConfig)

let accessTokenProvider =
  () => null

let ensureAccessTokenHandler =
  null

let refreshSessionHandler =
  null

let sessionInvalidHandler =
  null

export function configureHttpAuth({
  getAccessToken,
  ensureAccessToken,
  refreshSession,
  onSessionInvalid,
}) {
  if (
    typeof getAccessToken !==
    'function'
  ) {
    throw new TypeError(
      'getAccessToken должен быть функцией'
    )
  }

  accessTokenProvider =
    getAccessToken

  ensureAccessTokenHandler =
    typeof ensureAccessToken ===
    'function'
      ? ensureAccessToken
      : null

  refreshSessionHandler =
    typeof refreshSession ===
    'function'
      ? refreshSession
      : null

  sessionInvalidHandler =
    typeof onSessionInvalid ===
    'function'
      ? onSessionInvalid
      : null
}

/*
 * Оставлено для совместимости со старым main.js.
 * Новая версия приложения использует configureHttpAuth().
 */
export function setAccessTokenProvider(
  provider
) {
  if (
    typeof provider !==
    'function'
  ) {
    throw new TypeError(
      'Access token provider должен быть функцией'
    )
  }

  accessTokenProvider =
    provider
}

http.interceptors.request.use(
  async (config) => {
    /*
     * Если access token скоро истечёт,
     * AuthStore обновит его ДО отправки запроса.
     */
    if (
      accessTokenProvider?.() &&
      ensureAccessTokenHandler
    ) {
      try {
        await ensureAccessTokenHandler()
      } catch (error) {
        sessionInvalidHandler?.()
        return Promise.reject(error)
      }
    }

    const token =
      accessTokenProvider?.()

    if (token) {
      config.headers =
        config.headers ?? {}

      config.headers.Authorization =
        `Bearer ${token}`
    }

    return config
  },
  (error) =>
    Promise.reject(error)
)

http.interceptors.response.use(
  (response) => response,

  async (error) => {
    const status =
      error.response?.status

    const config =
      error.config

    if (
      status !== 401 ||
      !config ||
      config._authRetry ||
      config.skipAuthRefresh === true
    ) {
      return Promise.reject(
        error
      )
    }

    if (
      !refreshSessionHandler
    ) {
      return Promise.reject(
        error
      )
    }

    config._authRetry = true

    try {
      /*
       * Если другой параллельный запрос уже успел
       * обновить access token, второй refresh
       * не нужен — просто повторяем запрос.
       */
      const currentToken =
        accessTokenProvider?.()

      const failedAuthorization =
        config.headers?.Authorization

      const currentAuthorization =
        currentToken
          ? `Bearer ${currentToken}`
          : null

      if (
        !currentToken ||
        !failedAuthorization ||
        failedAuthorization ===
          currentAuthorization
      ) {
        await refreshSessionHandler()
      }

      const freshToken =
        accessTokenProvider?.()

      if (!freshToken) {
        throw new Error(
          'Не удалось восстановить сессию'
        )
      }

      config.headers =
        config.headers ?? {}

      config.headers.Authorization =
        `Bearer ${freshToken}`

      return http(config)
    } catch (refreshError) {
      sessionInvalidHandler?.()

      return Promise.reject(
        refreshError
      )
    }
  }
)

export default http
