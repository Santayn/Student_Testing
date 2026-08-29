import {
  createRouter,
  createWebHistory,
} from 'vue-router'

import { publicRoutes } from './routes/public'
import { studentRoutes } from './routes/student'
import { teacherRoutes } from './routes/teacher'
import { adminRoutes } from './routes/admin'

import { authGuard } from './guards/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),

  routes: [
    ...publicRoutes,
    ...studentRoutes,
    ...teacherRoutes,
    ...adminRoutes,

    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
    },
  ],

  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }

    return {
      top: 0,
    }
  },
})

router.beforeEach(authGuard)

export default router
