export const publicRoutes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: {
      guestOnly: true,
    },
  },

  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: {
      guestOnly: true,
    },
  },

  {
    path: '/auth-required',
    name: 'auth-required',
    component: () => import('@/views/auth/RequireAuthView.vue'),
    meta: {
      public: true,
    },
  },

  {
    path: '/403',
    name: 'forbidden',
    component: () => import('@/views/ForbiddenView.vue'),
    meta: {
      requiresAuth: true,
    },
  },
]
