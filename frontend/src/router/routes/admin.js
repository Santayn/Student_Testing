const adminRoles = ['ADMIN']

const adminMeta = {
  requiresAuth: true,
  roles: adminRoles,
  sidebar: true,
}

export const adminRoutes = [
  {
    path: '/admin/users',
    name: 'admin-users',
    component: () => import('@/views/admin/UsersView.vue'),
    meta: adminMeta,
  },

  {
    path: '/admin/faculties',
    name: 'admin-faculties',
    component: () => import('@/views/admin/FacultiesView.vue'),
    meta: adminMeta,
  },

  {
    path: '/admin/groups',
    name: 'admin-groups',
    component: () => import('@/views/admin/GroupsView.vue'),
    meta: adminMeta,
  },

  {
    path: '/admin/subjects',
    name: 'admin-subjects',
    component: () => import('@/views/admin/SubjectsAdminView.vue'),
    meta: adminMeta,
  },

  {
    path: '/admin/faculty-subjects',
    name: 'admin-faculty-subjects',
    component: () => import('@/views/admin/FacultySubjectsView.vue'),
    meta: adminMeta,
  },

  {
    path: '/admin/teacher-subjects',
    name: 'admin-teacher-subjects',
    component: () => import('@/views/admin/TeacherSubjectsView.vue'),
    meta: adminMeta,
  },

  {
    path: '/admin/teaching',
    name: 'admin-teaching',
    component: () => import('@/views/admin/TeachingTemplatesView.vue'),
    meta: adminMeta,
  },
]
