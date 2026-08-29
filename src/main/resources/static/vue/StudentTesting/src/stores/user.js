import { defineStore } from 'pinia'
import { usersApi, getApiErrorMessage } from '@/api'

export const useUserStore = defineStore('user', {
  state: () => ({
    user: null,
    loading: false,
    error: null,
  }),

  getters: {
    hasUser: (state) => state.user !== null,

    id: (state) => state.user?.id ?? null,

    login: (state) => state.user?.login ?? '',

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
      if (!state.user?.roles) {
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
        return roles.some((role) => this.hasRole(role))
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
    setUser(user) {
      this.user = user
      this.error = null
    },

    async loadCurrentUser() {
      this.loading = true
      this.error = null

      try {
        const response = await usersApi.getMe()

        this.user = response.data

        return this.user
      } catch (error) {
        this.error = getApiErrorMessage(
          error,
          'Не удалось загрузить пользователя'
        )

        throw error
      } finally {
        this.loading = false
      }
    },

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

    clearUser() {
      this.user = null
      this.error = null
      this.loading = false
    },
  },

  persist: {
    pick: ['user'],
  },
})
