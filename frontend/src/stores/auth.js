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

      refreshToken: null,
      refreshTokenExpiresAtUtc: null,

      lifetimeKind: null,

      user: null,

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
          this.refreshToken &&
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

        this.refreshToken =
          data?.refreshToken ||
          null

        this.refreshTokenExpiresAtUtc =
          data?.refreshTokenExpiresAtUtc ||
          null

        this.lifetimeKind =
          data?.lifetimeKind ??
          null

        if (
          !this.accessToken ||
          !this.refreshToken
        ) {
          throw new Error(
            'Backend не вернул полную пару access/refresh tokens'
          )
        }
      },

      setUser(user) {
        this.user =
          user || null

        this.error = null
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

          await this.loadCurrentUser()

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

          await this.loadCurrentUser()

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

      async refreshSession() {
        if (refreshPromise) {
          return refreshPromise
        }

        if (!this.refreshToken) {
          this.clearSession()

          throw new Error(
            'Refresh token отсутствует'
          )
        }

        if (
          this.isRefreshTokenExpired
        ) {
          this.clearSession()

          throw new Error(
            'Сессия истекла'
          )
        }

        /*
         * Refresh token вращается.
         * Сохраняем значение, которое было актуально
         * на момент старта refresh-запроса.
         */
        const currentRefreshToken =
          this.refreshToken

        this.refreshing = true

        refreshPromise = (async () => {
          try {
            const response =
              await authApi.refresh(
                currentRefreshToken
              )

            /*
             * Backend возвращает НОВУЮ пару.
             * Заменяем и access, и refresh token.
             */
            this.setSessionTokens(
              response.data
            )

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
          return null
        }

        if (
          !this.isAccessTokenExpired
        ) {
          return this.accessToken
        }

        if (!this.canRefresh) {
          this.clearSession()

          throw new Error(
            'Сессия истекла'
          )
        }

        return this.refreshSession()
      },

      async init() {
        if (this.initialized) {
          return
        }

        this.initialized = true
        this.error = null

        if (
          !this.accessToken &&
          !this.refreshToken
        ) {
          this.user = null
          return
        }

        this.loading = true

        try {
          if (
            !this.accessToken ||
            this.isAccessTokenExpired
          ) {
            await this.refreshSession()
          }

          await this.loadCurrentUser()
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

        this.refreshToken = null
        this.refreshTokenExpiresAtUtc =
          null

        this.lifetimeKind = null
        this.user = null
      },

      async logout() {
        const hasRefreshToken =
          Boolean(this.refreshToken)

        try {
          if (hasRefreshToken) {
            /*
             * revoke требует действующий Bearer.
             *
             * Если access token уже истёк,
             * сначала refreshSession() получит новую
             * пару, а revoke отправит уже НОВЫЙ
             * refresh token.
             */
            if (
              !this.accessToken ||
              this.isAccessTokenExpired
            ) {
              if (this.canRefresh) {
                await this.refreshSession()
              }
            }

            if (
              this.accessToken &&
              this.refreshToken
            ) {
              await authApi.revoke(
                this.refreshToken
              )
            }
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
      pick: [
        'tokenType',

        'accessToken',
        'accessTokenExpiresAtUtc',

        'refreshToken',
        'refreshTokenExpiresAtUtc',

        'lifetimeKind',

        'user',
      ],
    },
  }
)
