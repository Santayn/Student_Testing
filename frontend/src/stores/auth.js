import { defineStore } from 'pinia'

import {
  authApi,
  getApiErrorMessage,
} from '@/api'

const TOKEN_EXPIRY_MARGIN_MS = 30_000

let refreshPromise = null

function expirationTime(value) {
  if (!value) {
    return 0
  }

  const time = new Date(value).getTime()

  return Number.isFinite(time)
    ? time
    : 0
}

function isExpired(
  expiresAtUtc,
  marginMs = 0
) {
  const expiresAt =
    expirationTime(expiresAtUtc)

  if (!expiresAt) {
    return true
  }

  return (
    Date.now() + marginMs >=
    expiresAt
  )
}

export const useAuthStore = defineStore(
  'auth',
  {
    state: () => ({
      tokenType: 'Bearer',

      accessToken: null,
      accessTokenExpiresAtUtc: null,

      refreshTokenExpiresAtUtc: null,
      csrfToken: null,

      lifetimeKind: null,

      user: null,
      publicRegistrationEnabled: null,

      loading: false,
      refreshing: false,
      initialized: false,
      error: null,
    }),

    getters: {
      isAuthenticated: (state) => {
        return Boolean(
          state.accessToken &&
          state.user
        )
      },

      hasUser: (state) => {
        return state.user !== null
      },

      accountState: (state) => {
        return (
          state.user?.accountState ??
          null
        )
      },

      isAccountReady() {
        return this.accountState === 'ACTIVE'
      },

      userId: (state) => {
        return (
          state.user?.userId ??
          state.user?.id ??
          null
        )
      },

      personId: (state) => {
        return (
          state.user?.personId ??
          state.user?.person?.id ??
          null
        )
      },

      loginName: (state) => {
        return (
          state.user?.login ??
          ''
        )
      },

      email: (state) => {
        return (
          state.user?.email ??
          state.user?.person?.email ??
          ''
        )
      },

      firstName: (state) => {
        return (
          state.user?.firstName ??
          state.user?.person?.firstName ??
          ''
        )
      },

      lastName: (state) => {
        return (
          state.user?.lastName ??
          state.user?.person?.lastName ??
          ''
        )
      },

      middleName: (state) => {
        return (
          state.user?.middleName ??
          state.user?.person?.middleName ??
          ''
        )
      },

      fullName() {
        if (this.user?.fullName) {
          return this.user.fullName
        }

        return [
          this.lastName,
          this.firstName,
          this.middleName,
        ]
          .filter(Boolean)
          .join(' ')
          .trim()
      },

      roles: (state) => {
        return Array.isArray(
          state.user?.roles
        )
          ? state.user.roles
          : []
      },

      permissions: (state) => {
        return Array.isArray(
          state.user?.permissions
        )
          ? state.user.permissions
          : []
      },

      hasRole() {
        return (role) => {
          return this.roles.some(
            (userRole) => {
              if (
                typeof userRole ===
                'string'
              ) {
                return (
                  userRole === role
                )
              }

              return (
                userRole?.name === role ||
                userRole?.code === role ||
                userRole?.authority === role
              )
            }
          )
        }
      },

      hasAnyRole() {
        return (...roles) => {
          return roles.some(
            (role) =>
              this.hasRole(role)
          )
        }
      },

      hasPermission() {
        return (permission) => {
          return this.permissions.includes(
            permission
          )
        }
      },

      hasAnyPermission() {
        return (...permissions) => {
          return permissions.some(
            (permission) =>
              this.hasPermission(
                permission
              )
          )
        }
      },

      isAdmin() {
        return this.hasRole('ADMIN')
      },

      isTeacher() {
        return this.hasRole('TEACHER')
      },

      isStudent() {
        return this.hasRole('STUDENT')
      },

      isAccessTokenExpired: (
        state
      ) => {
        return isExpired(
          state.accessTokenExpiresAtUtc,
          TOKEN_EXPIRY_MARGIN_MS
        )
      },

      isRefreshTokenExpired: (
        state
      ) => {
        return isExpired(
          state.refreshTokenExpiresAtUtc
        )
      },

      canRefresh() {
        return Boolean(
          this.refreshTokenExpiresAtUtc &&
          !this.isRefreshTokenExpired
        )
      },
    },

    actions: {
      setSessionTokens(data) {
        this.tokenType =
          data?.tokenType ||
          'Bearer'

        this.accessToken =
          data?.accessToken ||
          null

        this.accessTokenExpiresAtUtc =
          data?.accessTokenExpiresAtUtc ||
          null

        this.refreshTokenExpiresAtUtc =
          data?.refreshTokenExpiresAtUtc ||
          null

        this.lifetimeKind =
          data?.lifetimeKind ??
          null

        if (data?.user) {
          this.setUser(data.user)
        }

        if (!this.accessToken) {
          throw new Error(
            'Backend не вернул access token'
          )
        }
      },

      setUser(user) {
        this.user =
          user || null

        this.error = null
      },

      async loadPublicAuthConfig() {
        const response =
          await authApi.config()

        this.publicRegistrationEnabled =
          Boolean(
            response.data
              ?.publicRegistrationEnabled
          )

        return this.publicRegistrationEnabled
      },

      async login(
        login,
        password,
        lifetimeKind = undefined
      ) {
        this.loading = true
        this.error = null

        try {
          const response =
            await authApi.login({
              login,
              password,
              lifetimeKind,
            })

          this.setSessionTokens(
            response.data
          )

          if (!this.user) {
            await this.loadCurrentUser()
          }

          return {
            user: this.user,
            accessToken:
              this.accessToken,
          }
        } catch (error) {
          this.clearSession()

          this.error =
            getApiErrorMessage(
              error,
              'Не удалось войти в систему'
            )

          throw error
        } finally {
          this.loading = false
        }
      },

      async register(data) {
        this.loading = true
        this.error = null

        try {
          const response =
            await authApi.register(
              data
            )

          this.setSessionTokens(
            response.data
          )

          if (!this.user) {
            await this.loadCurrentUser()
          }

          return {
            user: this.user,
            accessToken:
              this.accessToken,
          }
        } catch (error) {
          this.clearSession()

          this.error =
            getApiErrorMessage(
              error,
              'Не удалось зарегистрироваться'
            )

          throw error
        } finally {
          this.loading = false
        }
      },

      async loadCurrentUser() {
        if (!this.accessToken) {
          this.user = null
          return null
        }

        const response =
          await authApi.me()

        this.user =
          response.data ?? null

        return this.user
      },

      async ensureCsrfToken(
        forceRefresh = false
      ) {
        if (
          this.csrfToken &&
          !forceRefresh
        ) {
          return this.csrfToken
        }

        const response =
          await authApi.csrf()

        const csrfToken =
          response.data?.csrfToken ??
          null

        if (!csrfToken) {
          throw new Error(
            'Backend не вернул CSRF token'
          )
        }

        this.csrfToken = csrfToken
        return csrfToken
      },

      async refreshSession() {
        if (refreshPromise) {
          return refreshPromise
        }

        /*
         * HttpOnly cookie является источником истины для refresh-session.
         * JS может потерять persisted expiry metadata, хотя cookie всё ещё
         * существует (очистка storage, новая вкладка, миграция клиента).
         * Поэтому решение о валидности сессии принимает backend, а не store.
         */
        this.refreshing = true

        refreshPromise = (async () => {
          try {
            let csrfToken =
              await this.ensureCsrfToken()

            let response

            try {
              response =
                await authApi.refresh(
                  csrfToken
                )
            } catch (error) {
              /*
               * CSRF cookie может быть очищен браузером
               * независимо от in-memory token. Получаем
               * свежую double-submit пару и повторяем
               * refresh ровно один раз.
               */
              if (
                error.response?.status !== 403
              ) {
                throw error
              }

              csrfToken =
                await this.ensureCsrfToken(
                  true
                )

              response =
                await authApi.refresh(
                  csrfToken
                )
            }

            /*
             * Refresh token вращается внутри
             * HttpOnly cookie и никогда не попадает
             * в JavaScript. В store сохраняется только
             * новый access token и метаданные сессии.
             */
            this.setSessionTokens(
              response.data
            )

            if (!this.user) {
              await this.loadCurrentUser()
            }

            return this.accessToken
          } catch (error) {
            this.clearSession()
            throw error
          } finally {
            this.refreshing = false
            refreshPromise = null
          }
        })()

        return refreshPromise
      },

      async ensureAccessToken() {
        if (!this.accessToken) {
          return this.refreshSession()
        }

        if (
          !this.isAccessTokenExpired
        ) {
          return this.accessToken
        }

        return this.refreshSession()
      },

      async init() {
        if (this.initialized) {
          return
        }

        this.initialized = true
        this.error = null

        this.loading = true

        try {
          if (
            !this.accessToken ||
            this.isAccessTokenExpired
          ) {
            /*
             * Даже без persisted metadata один раз пробуем cookie-session.
             * Отсутствующая/истёкшая cookie штатно вернёт 401 и приведёт
             * к чистому гостевому состоянию.
             */
            await this.refreshSession()
          }

          if (!this.user) {
            await this.loadCurrentUser()
          }
        } catch (error) {
          this.clearSession()

          /*
           * Истёкшая/отозванная сессия —
           * нормальная причина показать login,
           * поэтому не держим её как UI error.
           *
           * Сетевую проблему сохраняем.
           */
          if (
            error.response?.status !== 400 &&
            error.response?.status !== 401
          ) {
            this.error =
              getApiErrorMessage(
                error,
                'Не удалось восстановить сессию'
              )
          }
        } finally {
          this.loading = false
        }
      },

      async changePassword(
        currentPassword,
        newPassword
      ) {
        this.loading = true
        this.error = null

        try {
          await this.ensureAccessToken()

          await authApi.changePassword({
            currentPassword,
            newPassword,
          })

          /*
           * Backend повышает securityVersion и очищает
           * refresh-cookie, поэтому текущая сессия после
           * смены пароля должна завершиться и локально.
           */
          this.clearSession()
        } catch (error) {
          this.error =
            getApiErrorMessage(
              error,
              'Не удалось изменить пароль'
            )

          throw error
        } finally {
          this.loading = false
        }
      },

      updateUser(data) {
        if (!this.user) {
          this.user = {
            ...data,
          }

          return
        }

        this.user = {
          ...this.user,
          ...data,
        }
      },

      clearSession() {
        this.tokenType = 'Bearer'

        this.accessToken = null
        this.accessTokenExpiresAtUtc =
          null

        this.refreshTokenExpiresAtUtc =
          null
        this.csrfToken = null

        this.lifetimeKind = null
        this.user = null
      },

      async logout() {
        try {
          /*
           * HttpOnly refresh-cookie является источником истины.
           * Даже полностью пустой JS store не доказывает, что
           * серверной refresh-сессии нет (например, storage был
           * очищен вручную или приложение открыто в новой вкладке).
           * Поэтому logout всегда пытается восстановить access token
           * через cookie и затем отозвать серверную сессию.
           */
          if (
            !this.accessToken ||
            this.isAccessTokenExpired
          ) {
            await this.refreshSession()
          }

          if (
            this.accessToken &&
            !this.isAccessTokenExpired
          ) {
            const csrfToken =
              await this.ensureCsrfToken(
                true
              )

            await authApi.revoke(
              csrfToken
            )
          }
        } catch {
          /*
           * Даже если backend/revoke недоступен,
           * локальный logout всё равно выполняется.
           */
        } finally {
          this.clearSession()
          this.error = null
        }
      },

      clearError() {
        this.error = null
      },
    },

    persist: {
      /*
       * Access token остаётся только в памяти.
       * Refresh token хранится только в HttpOnly cookie.
       * Persisted state содержит лишь несекретные метаданные.
       * Наличие refresh-session определяется только backend cookie,
       * поэтому эти поля не используются как запрет на refresh.
       */
      pick: [
        'tokenType',
        'refreshTokenExpiresAtUtc',
        'lifetimeKind',
        'user',
      ],
    },
  }
)
