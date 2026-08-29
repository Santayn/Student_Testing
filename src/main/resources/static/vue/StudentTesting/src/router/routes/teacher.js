const teacherRoles = ['TEACHER', 'ADMIN']

export const teacherRoutes = [
  {
    path: '/teacher/questions',
    name: 'teacher-questions',
    component: () => import('@/views/teacher/QuestionsView.vue'),
    meta: {
      requiresAuth: true,
      roles: teacherRoles,
    },
  },

  {
    path: '/teacher/tests/create',
    name: 'teacher-test-create',
    component: () => import('@/views/teacher/TestEditorView.vue'),
    meta: {
      requiresAuth: true,
      roles: teacherRoles,
    },
  },

  {
    path: '/teacher/lectures',
    name: 'teacher-lectures',
    component: () => import('@/views/teacher/LectureManagementView.vue'),
    meta: {
      requiresAuth: true,
      roles: teacherRoles,
    },
  },

  {
    path: '/teacher/topics',
    name: 'teacher-topics',
    component: () => import('@/views/teacher/TopicLibraryView.vue'),
    meta: {
      requiresAuth: true,
      roles: teacherRoles,
    },
  },

  {
    path: '/teacher/courses',
    name: 'teacher-courses',
    component: () => import('@/views/teacher/CourseTemplatesView.vue'),
    meta: {
      requiresAuth: true,
      roles: teacherRoles,
    },
  },

  {
    path: '/teacher/workload',
    name: 'teacher-workload',
    component: () => import('@/views/teacher/TeacherWorkloadView.vue'),
    meta: {
      requiresAuth: true,
      roles: teacherRoles,
    },
  },
]
