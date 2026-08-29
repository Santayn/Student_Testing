export const publicRoutes = [
  {
    path: '/auth',
    component: () => import('@/layouts/AuthLayout.vue'),

    children: [
      {
        path: 'login',
        name: 'login',
        component: () => import('@/views/auth/LoginView.vue'),
        meta: {
          guestOnly: true,
        },
      },

      {
        path: 'register',
        name: 'register',
        component: () => import('@/views/auth/RegisterView.vue'),
        meta: {
          guestOnly: true,
        },
      },

      {
        path: 'required',
        name: 'auth-required',
        component: () => import('@/views/auth/RequireAuthView.vue'),
        meta: {
          public: true,
        },
      },
    ],
  },

  {
    path: '/login',
    redirect: {
      name: 'login',
    },
  },

  {
    path: '/register',
    redirect: {
      name: 'register',
    },
  },

  {
    path: '/auth-required',
    redirect: (to) => ({
      name: 'auth-required',
      query: to.query,
    }),
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
