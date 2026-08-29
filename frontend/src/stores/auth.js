import { defineStore } from 'pinia'
import {
  authApi,
  usersApi,
  getApiErrorMessage,
} from '@/api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: null,
    user: null,

    loading: false,
    initialized: false,
    error: null,
  }),

  getters: {
    /**
     * Сессия считается активной только если есть
     * и access token, и текущий пользователь.
     */
    isAuthenticated: (state) => {
      return Boolean(
        state.accessToken &&
        state.user
      )
    },

    hasUser: (state) => state.user !== null,

    userId: (state) => state.user?.id ?? null,

    loginName: (state) => state.user?.login ?? '',

    email: (state) => state.user?.email ?? '',

    firstName: (state) => state.user?.firstName ?? '',

    lastName: (state) => state.user?.lastName ?? '',

    middleName: (state) => state.user?.middleName ?? '',

    fullName() {
      return [
        this.lastName,
        this.firstName,
        this.middleName,
      ]
        .filter(Boolean)
        .join(' ')
    },

    roles: (state) => {
      if (!Array.isArray(state.user?.roles)) {
        return []
      }

      return state.user.roles
    },

    hasRole() {
      return (role) => {
        return this.roles.some((userRole) => {
          if (typeof userRole === 'string') {
            return userRole === role
          }

          return (
            userRole?.name === role ||
            userRole?.code === role ||
            userRole?.authority === role
          )
        })
      }
    },

    hasAnyRole() {
      return (...roles) => {
        return roles.some(
          (role) => this.hasRole(role)
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
  },

  actions: {
    setAccessToken(token) {
      this.accessToken = token || null
    },

    setUser(user) {
      this.user = user || null
      this.error = null
    },

    /**
     * Авторизация.
     *
     * Поддерживает несколько распространённых имён поля токена,
     * чтобы не привязывать frontend к одному DTO раньше времени.
     */
    async login(login, password) {
      this.loading = true
      this.error = null

      try {
        const response = await authApi.login(
          login,
          password
        )

        const data = response.data ?? {}

        const token =
          data.accessToken ??
          data.access_token ??
          data.token ??
          null

        if (!token) {
          throw new Error(
            'Backend не вернул access token'
          )
        }

        this.accessToken = token

        /**
         * Если backend сразу вернул пользователя —
         * используем его.
         *
         * Иначе загружаем /users/me.
         */
        if (data.user) {
          this.user = data.user
        } else {
          await this.loadCurrentUser()
        }

        return {
          accessToken: this.accessToken,
          user: this.user,
        }
      } catch (error) {
        /**
         * Не оставляем частично созданную сессию,
         * если login завершился ошибкой.
         */
        this.accessToken = null
        this.user = null

        this.error = getApiErrorMessage(
          error,
          'Не удалось войти в систему'
        )

        throw error
      } finally {
        this.loading = false
      }
    },

    /**
     * Регистрация.
     *
     * По умолчанию только создаёт пользователя.
     * Автоматический login после регистрации не предполагается,
     * потому что поведение backend может отличаться.
     */
    async register(data) {
      this.loading = true
      this.error = null

      try {
        const response = await authApi.register(data)

        return response.data
      } catch (error) {
        this.error = getApiErrorMessage(
          error,
          'Не удалось зарегистрироваться'
        )

        throw error
      } finally {
        this.loading = false
      }
    },

    /**
     * Получить актуального пользователя с backend.
     */
    async loadCurrentUser() {
      if (!this.accessToken) {
        this.user = null
        return null
      }

      try {
        const response = await usersApi.getMe()

        this.user = response.data

        return this.user
      } catch (error) {
        /**
         * 401 означает, что сохранённая сессия
         * больше недействительна.
         */
        if (error.response?.status === 401) {
          this.clearSession()
        }

        throw error
      }
    },

    /**
     * Восстановление сохранённой Pinia-сессии
     * при старте приложения.
     */
    async init() {
      if (this.initialized) {
        return
      }

      this.initialized = true
      this.error = null

      if (!this.accessToken) {
        this.user = null
        return
      }

      this.loading = true

      try {
        await this.loadCurrentUser()
      } catch (error) {
        if (error.response?.status !== 401) {
          this.error = getApiErrorMessage(
            error,
            'Не удалось восстановить сессию'
          )
        }
      } finally {
        this.loading = false
      }
    },

    /**
     * Локально обновить уже загруженного пользователя.
     */
    updateUser(data) {
      if (!this.user) {
        this.user = { ...data }
        return
      }

      this.user = {
        ...this.user,
        ...data,
      }
    },

    /**
     * Очистка данных сессии.
     */
    clearSession() {
      this.accessToken = null
      this.user = null
      this.error = null
    },

    /**
     * Logout.
     *
     * В текущем API-слое server-side logout endpoint пока
     * не определён, поэтому очищаем локальную JWT-сессию.
     */
    logout() {
      this.clearSession()
    },

    clearError() {
      this.error = null
    },
  },

  /**
   * Сохраняем только данные сессии.
   *
   * loading / error / initialized после F5
   * должны создаваться заново.
   */
  persist: {
    pick: [
      'accessToken',
      'user',
    ],
  },
})
