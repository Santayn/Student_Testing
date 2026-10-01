const permissionMeta = (permissions) => ({
  requiresAuth: true,
  permissions,
  sidebar: true,
})

const strictAdminMeta = {
  requiresAuth: true,
  roles: ['ADMIN'],
  sidebar: true,
}

export const adminRoutes = [
  {
    path: '/admin/users',
    name: 'admin-users',

    component: () =>
      import(
        '@/views/admin/UsersView.vue'
      ),

    meta: permissionMeta(['users.read']),
  },

  {
    path: '/admin/faculties',
    name: 'admin-faculties',

    component: () =>
      import(
        '@/views/admin/FacultiesView.vue'
      ),

    meta: permissionMeta(['academic.manage']),
  },

  {
    path: '/admin/groups',
    name: 'admin-groups',

    component: () =>
      import(
        '@/views/admin/GroupsView.vue'
      ),

    meta: permissionMeta(['academic.manage']),
  },

  {
    path: '/admin/subjects',
    name: 'admin-subjects',

    component: () =>
      import(
        '@/views/admin/SubjectsAdminView.vue'
      ),

    meta: permissionMeta(['academic.manage']),
  },

  {
    path: '/admin/faculty-subjects',
    name: 'admin-faculty-subjects',

    component: () =>
      import(
        '@/views/admin/FacultySubjectsView.vue'
      ),

    meta: permissionMeta(['academic.manage']),
  },

  {
    path: '/admin/teacher-subjects',
    name: 'admin-teacher-subjects',

    component: () =>
      import(
        '@/views/admin/TeacherSubjectsView.vue'
      ),

    meta: permissionMeta(['academic.manage']),
  },

  {
    path: '/admin/teaching',
    name: 'admin-teaching',

    component: () =>
      import(
        '@/views/admin/TeachingTemplatesView.vue'
      ),

    meta: permissionMeta(['academic.manage']),
  },

  {
    path: '/admin/database-backups',
    name: 'admin-database-backups',

    component: () =>
      import(
        '@/views/admin/DatabaseBackupsView.vue'
      ),

    meta: strictAdminMeta,
  },
]
