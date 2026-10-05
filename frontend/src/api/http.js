import axios from 'axios'

import {
  API_TIMEOUTS,
} from './timeouts'

import {
  invalidateAllLearningContextCaches,
} from '@/utils/learningContextCache'

const clientConfig = {
  baseURL:
    import.meta.env
      .VITE_API_BASE_URL ||
    '/api/v1',
  timeout: API_TIMEOUTS.standard,

  headers: {
    Accept: 'application/json',
  },
}

const AUTH_SESSION_STALE_CODE =
  'AUTH_SESSION_STALE'

function staleSessionError() {
  const error = new Error(
    'Запрос относится к завершённой сессии'
  )

  error.code = AUTH_SESSION_STALE_CODE
  error.authSessionStale = true

  return error
}

function isStaleSessionError(error) {
  return (
    error?.authSessionStale === true ||
    error?.code === AUTH_SESSION_STALE_CODE
  )
}

/*
 * authHttp не содержит auth interceptors.
 *
 * Через него выполняются:
 * login / register / refresh / best-effort revoke.
 *
 * Это важно, чтобы refresh-запрос сам не попал
 * в обработчик 401 и не создал бесконечный цикл.
 */
export const authHttp =
  axios.create(clientConfig)

const rawHttp =
  axios.create(clientConfig)

let accessTokenProvider =
  () => null

let sessionEpochProvider =
  () => 0

let ensureAccessTokenHandler =
  null

let refreshSessionHandler =
  null

let sessionInvalidHandler =
  null

export function configureHttpAuth({
  getAccessToken,
  getSessionEpoch,
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

  sessionEpochProvider =
    typeof getSessionEpoch ===
    'function'
      ? getSessionEpoch
      : () => 0

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

function requestEpoch(config) {
  return config?._authSessionEpoch
}

function currentEpoch() {
  return sessionEpochProvider?.() ?? 0
}

function epochChanged(config) {
  return (
    requestEpoch(config) !==
    currentEpoch()
  )
}

function shouldInvalidateSession(
  error,
  config
) {
  if (isStaleSessionError(error)) {
    return false
  }

  if (error?.authSessionInvalid) {
    return true
  }

  return !epochChanged(config)
}

function captureSessionEpoch(config = {}) {
  return {
    ...config,
    _authSessionEpoch:
      config._authSessionEpoch ??
      currentEpoch(),
  }
}

/*
 * Axios request interceptors start from a Promise chain when at least one
 * interceptor is async. That means an epoch captured only inside the
 * interceptor can already belong to a newer login/logout cycle.
 *
 * Capture the epoch synchronously at the public http.* call boundary, then
 * let the interceptor validate that immutable request context before/after
 * any awaited preflight refresh.
 */
function http(config) {
  return rawHttp(
    captureSessionEpoch(config)
  )
}

http.request = (config) =>
  rawHttp.request(
    captureSessionEpoch(config)
  )

for (const method of [
  'get',
  'delete',
  'head',
  'options',
]) {
  http[method] = (url, config) =>
    rawHttp[method](
      url,
      captureSessionEpoch(config)
    )
}

for (const method of [
  'post',
  'put',
  'patch',
]) {
  http[method] = (url, data, config) =>
    rawHttp[method](
      url,
      data,
      captureSessionEpoch(config)
    )
}

http.defaults = rawHttp.defaults
http.interceptors = rawHttp.interceptors
http.getUri = rawHttp.getUri.bind(rawHttp)

rawHttp.interceptors.request.use(
  async (config) => {
    const epoch =
      config._authSessionEpoch ??
      currentEpoch()

    config._authSessionEpoch = epoch

    /*
     * A retried request keeps the epoch of its original dispatch.
     * If another login/logout already replaced that session, abort before
     * ensureAccessToken() can touch credentials of the newer account.
     */
    if (epochChanged(config)) {
      return Promise.reject(
        staleSessionError()
      )
    }

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
        if (
          shouldInvalidateSession(
            error,
            config
          )
        ) {
          sessionInvalidHandler?.()
        }

        if (
          epochChanged(config) &&
          !error?.authSessionInvalid
        ) {
          return Promise.reject(
            staleSessionError()
          )
        }

        return Promise.reject(error)
      }
    }

    /*
     * Logout or a new Login may have happened while preflight refresh was
     * in flight. Never send an old request using credentials from the new
     * security context.
     */
    if (epochChanged(config)) {
      return Promise.reject(
        staleSessionError()
      )
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

rawHttp.interceptors.response.use(
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
      epochChanged(config)
    ) {
      return Promise.reject(
        staleSessionError()
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

      if (epochChanged(config)) {
        throw staleSessionError()
      }

      const freshToken =
        accessTokenProvider?.()

      if (!freshToken) {
        const sessionError =
          new Error(
            'Не удалось восстановить сессию'
          )

        sessionError.authSessionInvalid =
          true

        throw sessionError
      }

      config.headers =
        config.headers ?? {}

      config.headers.Authorization =
        `Bearer ${freshToken}`

      return http(config)
    } catch (refreshError) {
      if (
        shouldInvalidateSession(
          refreshError,
          config
        )
      ) {
        sessionInvalidHandler?.()
      }

      return Promise.reject(
        refreshError
      )
    }
  }
)

// Only successful domain writes invalidate shared read snapshots. The
// mutating request itself is never cached; failed writes keep the cache.
rawHttp.interceptors.response.use((response) => {
  const method = response.config?.method?.toLowerCase()
  const url = response.config?.url ?? ''

  if (
    ['post', 'put', 'patch', 'delete'].includes(method) &&
    /^\/(?:subjects|groups|faculties|memberships\/(?:groups|subjects)|teaching\/(?:assignments|enrollments|subject-memberships|lecture-assignments))(?:\/|$)/.test(url) &&
    !epochChanged(response.config)
  ) {
    invalidateAllLearningContextCaches()
  }

  return response
})

export default http
