import { useAuthStore } from '@/stores/auth'

export async function authGuard(to) {
  const authStore = useAuthStore()

  if (!authStore.initialized) {
    await authStore.init()
  }

  if (
    to.meta.requiresAuth &&
    !authStore.isAuthenticated
  ) {
    return {
      name: 'auth-required',
      query: {
        redirect: to.fullPath,
      },
    }
  }

  if (
    to.meta.guestOnly &&
    authStore.isAuthenticated
  ) {
    return {
      name: 'home',
    }
  }

  if (
    to.meta.roles?.length &&
    !authStore.hasAnyRole(...to.meta.roles)
  ) {
    return {
      name: 'forbidden',
      query: {
        from: to.fullPath,
      },
    }
  }

  return true
}
