import {
  ADMIN_ROLES,
} from '../roles'

import {
  NAV_KEYS,
} from '@/navigation/navigation.config'

const adminMeta = {
  requiresAuth: true,
  roles: ADMIN_ROLES,
  workspaceRoles: ADMIN_ROLES,
}

function navigationMeta(
  navKey,
  breadcrumbKey
) {
  return {
    ...adminMeta,
    navKey,
    breadcrumbKey,
  }
}

export const adminRoutes = [
  {
    path: '/admin/users',
    name: 'admin-users',

    component: () =>
      import(
        '@/views/admin/UsersView.vue'
      ),

    meta: navigationMeta(
      NAV_KEYS.ADMIN_USERS,
      'admin-users'
    ),
  },

  {
    path: '/admin/roles',
    name: 'admin-roles',

    component: () =>
      import(
        '@/views/admin/RolesPermissionsView.vue'
      ),

    meta: navigationMeta(
      NAV_KEYS.ADMIN_ROLES,
      'admin-roles'
    ),
  },

  {
    path: '/admin/faculties',
    name: 'admin-faculties',

    component: () =>
      import(
        '@/views/admin/FacultiesView.vue'
      ),

    meta: navigationMeta(
      NAV_KEYS.ADMIN_FACULTIES,
      'admin-faculties'
    ),
  },

  {
    path: '/admin/groups',
    name: 'admin-groups',

    component: () =>
      import(
        '@/views/admin/GroupsView.vue'
      ),

    meta: navigationMeta(
      NAV_KEYS.ADMIN_GROUPS,
      'admin-groups'
    ),
  },

  {
    path: '/admin/subjects',
    name: 'admin-subjects',

    component: () =>
      import(
        '@/views/admin/SubjectsAdminView.vue'
      ),

    meta: navigationMeta(
      NAV_KEYS.ADMIN_SUBJECTS,
      'admin-subjects'
    ),
  },

  {
    path: '/admin/faculty-subjects',
    name: 'admin-faculty-subjects',

    component: () =>
      import(
        '@/views/admin/FacultySubjectsView.vue'
      ),

    meta: navigationMeta(
      NAV_KEYS.ADMIN_FACULTY_SUBJECTS,
      'admin-faculty-subjects'
    ),
  },

  {
    path: '/admin/teacher-subjects',
    name: 'admin-teacher-subjects',

    component: () =>
      import(
        '@/views/admin/TeacherSubjectsView.vue'
      ),

    meta: navigationMeta(
      NAV_KEYS.ADMIN_TEACHER_SUBJECTS,
      'admin-teacher-subjects'
    ),
  },

  {
    path: '/admin/teaching',
    name: 'admin-teaching',

    component: () =>
      import(
        '@/views/admin/TeachingTemplatesView.vue'
      ),

    meta: navigationMeta(
      NAV_KEYS.ADMIN_TEACHING,
      'admin-teaching'
    ),
  },
]
