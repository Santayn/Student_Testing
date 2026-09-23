import { defineStore } from 'pinia'

import {
  authApi,
  getApiErrorMessage,
} from '@/api'

import {
  getAuthErrorMessage,
} from '@/utils/authErrorMessage'

import {
  resolveWorkspaceRole,
  workspaceRolesFromUserRoles,
} from '@/utils/workspaceRole'

import {
  invalidateLearningContextCache,
} from '@/utils/learningContextCache'

const TOKEN_EXPIRY_MARGIN_MS = 30_000
const AUTH_SESSION_STALE_CODE =
  'AUTH_SESSION_STALE'

let refreshOperation = null

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

function staleSessionError() {
  const error = new Error(
    'Операция относится к завершённой сессии'
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

function sessionExpiredError(
  message = 'Сессия истекла'
) {
  const error = new Error(message)

  error.authSessionInvalid = true

  return error
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
      activeWorkspaceRole: null,

      /*
       * Runtime-only generation of the current authentication session.
       * It is intentionally not persisted.
       *
       * Any explicit session replacement/clear increments the epoch.
       * Async auth work may commit its result only while the epoch that
       * started it is still current.
       */
      sessionEpoch: 0,

      initializing: false,
      loggingIn: false,
      registering: false,
      refreshing: false,
      syncingIdentity: false,
      loggingOut: false,
      changingPassword: false,

      initialized: false,

      loginError: null,
      registerError: null,
      passwordError: null,
      sessionRestoreError: null,
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

      workspaceRoles() {
        return workspaceRolesFromUserRoles(
          this.roles
        )
      },

      workspaceRole() {
        return resolveWorkspaceRole(
          this.roles,
          this.activeWorkspaceRole
        )
      },

      hasMultipleWorkspaceRoles() {
        return this.workspaceRoles.length > 1
      },

      isAdminMode() {
        return this.workspaceRole === 'ADMIN'
      },

      isTeacherMode() {
        return this.workspaceRole === 'TEACHER'
      },

      isStudentMode() {
        return this.workspaceRole === 'STUDENT'
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

      /*
       * Compatibility getter for existing generic UI.
       * New auth screens should prefer operation-specific flags.
       */
      loading() {
        return (
          this.initializing ||
          this.loggingIn ||
          this.registering ||
          this.syncingIdentity ||
          this.loggingOut ||
          this.changingPassword
        )
      },

      /*
       * Compatibility getter. Auth views use their own operation error,
       * so an error from registration cannot leak onto Login and vice versa.
       */
      error() {
        return (
          this.loginError ||
          this.registerError ||
          this.passwordError ||
          this.sessionRestoreError ||
          null
        )
      },
    },

    actions: {
      advanceSessionEpoch() {
        this.sessionEpoch += 1
        invalidateLearningContextCache(this)

        return this.sessionEpoch
      },

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
        const previousSecurityContext = JSON.stringify([
          this.userId,
          this.personId,
          this.activeWorkspaceRole,
          this.roles,
          this.permissions,
        ])

        this.user =
          user || null

        this.syncWorkspaceRole()

        const nextSecurityContext = JSON.stringify([
          this.userId,
          this.personId,
          this.activeWorkspaceRole,
          this.roles,
          this.permissions,
        ])

        if (previousSecurityContext !== nextSecurityContext) {
          invalidateLearningContextCache(this)
        }
      },

      syncWorkspaceRole() {
        this.activeWorkspaceRole =
          resolveWorkspaceRole(
            this.roles,
            this.activeWorkspaceRole
          )

        return this.activeWorkspaceRole
      },

      setWorkspaceRole(role) {
        const available =
          this.workspaceRoles

        if (!available.includes(role)) {
          throw new Error(
            `Роль ${role} недоступна текущему пользователю`
          )
        }

        if (this.activeWorkspaceRole !== role) {
          invalidateLearningContextCache(this)
          this.activeWorkspaceRole = role
        }

        return role
      },

      assertSessionEpoch(
        expectedEpoch
      ) {
        if (
          this.sessionEpoch !==
          expectedEpoch
        ) {
          throw staleSessionError()
        }
      },

      resetSessionData() {
        this.tokenType = 'Bearer'

        this.accessToken = null
        this.accessTokenExpiresAtUtc =
          null

        this.refreshToken = null
        this.refreshTokenExpiresAtUtc =
          null

        this.lifetimeKind = null
        this.user = null
        this.activeWorkspaceRole = null

        this.refreshing = false
        this.syncingIdentity = false
        this.loggingIn = false
        this.registering = false
        this.changingPassword = false
      },

      clearSession() {
        this.advanceSessionEpoch()
        this.resetSessionData()
      },

      async login(
        login,
        password,
        lifetimeKind = undefined
      ) {
        this.loginError = null

        /*
         * A new login is a new security context even if another account
         * was previously authenticated in this tab.
         */
        this.clearSession()
        this.loggingIn = true

        const operationEpoch =
          this.sessionEpoch

        try {
          const response =
            await authApi.login({
              login,
              password,
              lifetimeKind,
            })

          this.assertSessionEpoch(
            operationEpoch
          )

          this.setSessionTokens(
            response.data
          )

          await this.loadCurrentUser({
            expectedEpoch:
              operationEpoch,
          })

          this.assertSessionEpoch(
            operationEpoch
          )

          return {
            user: this.user,
            accessToken:
              this.accessToken,
          }
        } catch (error) {
          if (
            this.sessionEpoch ===
              operationEpoch &&
            !isStaleSessionError(
              error
            )
          ) {
            this.loginError =
              getAuthErrorMessage(
                error,
                'login'
              )

            this.clearSession()
            this.loggingIn = false
          }

          throw error
        } finally {
          if (
            this.sessionEpoch ===
            operationEpoch
          ) {
            this.loggingIn = false
          }
        }
      },

      async register(data) {
        this.registerError = null

        this.clearSession()
        this.registering = true

        const operationEpoch =
          this.sessionEpoch

        try {
          const response =
            await authApi.register(
              data
            )

          this.assertSessionEpoch(
            operationEpoch
          )

          this.setSessionTokens(
            response.data
          )

          await this.loadCurrentUser({
            expectedEpoch:
              operationEpoch,
          })

          this.assertSessionEpoch(
            operationEpoch
          )

          return {
            user: this.user,
            accessToken:
              this.accessToken,
          }
        } catch (error) {
          if (
            this.sessionEpoch ===
              operationEpoch &&
            !isStaleSessionError(
              error
            )
          ) {
            this.registerError =
              getAuthErrorMessage(
                error,
                'register'
              )

            this.clearSession()
            this.registering = false
          }

          throw error
        } finally {
          if (
            this.sessionEpoch ===
            operationEpoch
          ) {
            this.registering = false
          }
        }
      },

      async loadCurrentUser({
        expectedEpoch =
          this.sessionEpoch,
      } = {}) {
        this.assertSessionEpoch(
          expectedEpoch
        )

        if (!this.accessToken) {
          this.user = null
          return null
        }

        const response =
          await authApi.me()

        this.assertSessionEpoch(
          expectedEpoch
        )

        this.setUser(
          response.data ?? null
        )

        return this.user
      },

      async refreshSession() {
        const operationEpoch =
          this.sessionEpoch

        if (
          refreshOperation?.epoch ===
          operationEpoch
        ) {
          return refreshOperation.promise
        }

        if (!this.refreshToken) {
          throw sessionExpiredError(
            'Refresh token отсутствует'
          )
        }

        if (
          this.isRefreshTokenExpired
        ) {
          throw sessionExpiredError()
        }

        /*
         * Refresh token rotates. Capture the exact token and epoch that
         * own this request. A later login/logout creates another epoch,
         * so this response can no longer mutate the store.
         */
        const currentRefreshToken =
          this.refreshToken

        this.refreshing = true

        const networkPromise =
          authApi
            .refresh(
              currentRefreshToken
            )
            .then(
              (response) =>
                response.data
            )

        const operation = {
          epoch: operationEpoch,
          networkPromise,
          promise: null,
        }

        operation.promise =
          (async () => {
            try {
              const data =
                await networkPromise

              this.assertSessionEpoch(
                operationEpoch
              )

              this.setSessionTokens(
                data
              )

              return this.accessToken
            } finally {
              if (
                refreshOperation ===
                operation
              ) {
                refreshOperation = null
              }

              if (
                this.sessionEpoch ===
                operationEpoch
              ) {
                this.refreshing = false
              }
            }
          })()

        refreshOperation = operation

        return operation.promise
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
          const error =
            sessionExpiredError()

          this.clearSession()

          throw error
        }

        return this.refreshSession()
      },

      async refreshIdentity() {
        const operationEpoch =
          this.sessionEpoch

        // A manual identity refresh must also pick up external changes to
        // teaching/group assignments even when user/roles remain identical.
        invalidateLearningContextCache(this)

        this.syncingIdentity = true
        this.sessionRestoreError = null

        try {
          await this.refreshSession()

          this.assertSessionEpoch(
            operationEpoch
          )

          return await this.loadCurrentUser({
            expectedEpoch:
              operationEpoch,
          })
        } catch (error) {
          if (
            this.sessionEpoch ===
              operationEpoch &&
            !isStaleSessionError(
              error
            )
          ) {
            this.clearSession()
          }

          throw error
        } finally {
          if (
            this.sessionEpoch ===
            operationEpoch
          ) {
            this.syncingIdentity = false
          }
        }
      },

      async init() {
        if (this.initialized) {
          return
        }

        this.initialized = true
        this.initializing = true
        this.sessionRestoreError = null

        if (
          !this.accessToken &&
          !this.refreshToken
        ) {
          this.user = null
          this.initializing = false
          return
        }

        const operationEpoch =
          this.sessionEpoch

        try {
          if (
            !this.accessToken ||
            this.isAccessTokenExpired
          ) {
            await this.refreshSession()
          }

          this.assertSessionEpoch(
            operationEpoch
          )

          await this.loadCurrentUser({
            expectedEpoch:
              operationEpoch,
          })
        } catch (error) {
          if (
            this.sessionEpoch ===
            operationEpoch
          ) {
            this.clearSession()

            /*
             * Expired/revoked credentials are a normal reason to show
             * Login. Network/server failures remain visible as restore
             * errors. We intentionally keep the existing fail-closed
             * policy and clear persisted credentials on bootstrap failure.
             */
            if (
              error.response?.status !== 400 &&
              error.response?.status !== 401 &&
              !error.authSessionInvalid
            ) {
              this.sessionRestoreError =
                getApiErrorMessage(
                  error,
                  'Не удалось восстановить сессию'
                )
            }
          }
        } finally {
          this.initializing = false
        }
      },

      async changePassword(
        currentPassword,
        newPassword
      ) {
        this.changingPassword = true
        this.passwordError = null

        const operationEpoch =
          this.sessionEpoch

        try {
          await this.ensureAccessToken()

          this.assertSessionEpoch(
            operationEpoch
          )

          await authApi.changePassword({
            currentPassword,
            newPassword,
          })

          this.assertSessionEpoch(
            operationEpoch
          )

          /*
           * Backend revokes every refresh token after password change.
           * Do not keep a locally "valid-looking" but server-revoked
           * refresh token. The caller must send the user to Login.
           */
          this.clearSession()
          this.passwordError = null

          return {
            requiresReauthentication:
              true,
          }
        } catch (error) {
          if (
            this.sessionEpoch ===
              operationEpoch &&
            !isStaleSessionError(
              error
            )
          ) {
            this.passwordError =
              getAuthErrorMessage(
                error,
                'password'
              )
          }

          throw error
        } finally {
          this.changingPassword = false
        }
      },

      updateUser(data) {
        if (!this.user) {
          this.user = {
            ...data,
          }

          this.syncWorkspaceRole()
          return
        }

        this.user = {
          ...this.user,
          ...data,
        }

        this.syncWorkspaceRole()
      },

      async logout() {
        if (this.loggingOut) {
          return
        }

        this.loggingOut = true

        const operationEpoch =
          this.sessionEpoch

        const captured = {
          tokenType:
            this.tokenType ||
            'Bearer',
          accessToken:
            this.accessToken,
          accessTokenExpiresAtUtc:
            this.accessTokenExpiresAtUtc,
          refreshToken:
            this.refreshToken,
          refreshTokenExpiresAtUtc:
            this.refreshTokenExpiresAtUtc,
        }

        const pendingRefresh =
          refreshOperation?.epoch ===
          operationEpoch
            ? refreshOperation.networkPromise
            : null

        /*
         * Local logout is immediate. This also invalidates every request
         * and refresh that belongs to the old session epoch.
         */
        this.clearSession()
        this.clearError()

        try {
          if (!captured.refreshToken) {
            return
          }

          let accessToken =
            captured.accessToken
          let refreshToken =
            captured.refreshToken
          let tokenType =
            captured.tokenType

          if (pendingRefresh) {
            try {
              const rotated =
                await pendingRefresh

              accessToken =
                rotated?.accessToken ??
                null
              refreshToken =
                rotated?.refreshToken ??
                null
              tokenType =
                rotated?.tokenType ||
                'Bearer'
            } catch {
              accessToken = null
              refreshToken = null
            }
          } else if (
            !accessToken ||
            isExpired(
              captured.accessTokenExpiresAtUtc
            )
          ) {
            if (
              !isExpired(
                captured.refreshTokenExpiresAtUtc
              )
            ) {
              try {
                const response =
                  await authApi.refresh(
                    captured.refreshToken
                  )

                accessToken =
                  response.data?.accessToken ??
                  null
                refreshToken =
                  response.data?.refreshToken ??
                  null
                tokenType =
                  response.data?.tokenType ||
                  'Bearer'
              } catch {
                accessToken = null
                refreshToken = null
              }
            }
          }

          if (
            accessToken &&
            refreshToken
          ) {
            await authApi.revoke(
              refreshToken,
              accessToken,
              tokenType
            )
          }
        } catch {
          /*
           * Backend revoke is best-effort. Local session has already been
           * invalidated and must never be restored because revoke failed.
           */
        } finally {
          this.loggingOut = false
        }
      },

      clearError(scope = null) {
        if (!scope || scope === 'login') {
          this.loginError = null
        }

        if (!scope || scope === 'register') {
          this.registerError = null
        }

        if (!scope || scope === 'password') {
          this.passwordError = null
        }

        if (!scope || scope === 'session') {
          this.sessionRestoreError = null
        }
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
        'activeWorkspaceRole',
      ],
    },
  }
)
