const adminRoles = ['ADMIN']

export const adminRoutes = [
  {
    path: '/admin/users',
    name: 'admin-users',
    component: () => import('@/views/admin/UsersView.vue'),
    meta: {
      requiresAuth: true,
      roles: adminRoles,
    },
  },

  {
    path: '/admin/faculties',
    name: 'admin-faculties',
    component: () => import('@/views/admin/FacultiesView.vue'),
    meta: {
      requiresAuth: true,
      roles: adminRoles,
    },
  },

  {
    path: '/admin/groups',
    name: 'admin-groups',
    component: () => import('@/views/admin/GroupsView.vue'),
    meta: {
      requiresAuth: true,
      roles: adminRoles,
    },
  },

  {
    path: '/admin/subjects',
    name: 'admin-subjects',
    component: () => import('@/views/admin/SubjectsAdminView.vue'),
    meta: {
      requiresAuth: true,
      roles: adminRoles,
    },
  },

  {
    path: '/admin/faculty-subjects',
    name: 'admin-faculty-subjects',
    component: () => import('@/views/admin/FacultySubjectsView.vue'),
    meta: {
      requiresAuth: true,
      roles: adminRoles,
    },
  },

  {
    path: '/admin/teacher-subjects',
    name: 'admin-teacher-subjects',
    component: () => import('@/views/admin/TeacherSubjectsView.vue'),
    meta: {
      requiresAuth: true,
      roles: adminRoles,
    },
  },

  {
    path: '/admin/teaching',
    name: 'admin-teaching',
    component: () => import('@/views/admin/TeachingTemplatesView.vue'),
    meta: {
      requiresAuth: true,
      roles: adminRoles,
    },
  },
]
