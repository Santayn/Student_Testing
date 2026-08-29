const teacherMeta = {
  requiresAuth: true,
  roles: [
    'TEACHER',
    'ADMIN',
  ],
  sidebar: true,
}

export const teacherRoutes = [
  {
    path: '/teacher/questions',
    name: 'teacher-questions',
    component: () =>
      import(
        '@/views/teacher/QuestionsView.vue'
      ),
    meta: teacherMeta,
  },

  {
    path: '/teacher/tests/create',
    name: 'teacher-test-create',
    component: () =>
      import(
        '@/views/teacher/TestEditorView.vue'
      ),
    meta: teacherMeta,
  },

  {
    path: '/teacher/lectures',
    name: 'teacher-lectures',
    component: () =>
      import(
        '@/views/teacher/LectureManagementView.vue'
      ),
    meta: teacherMeta,
  },

  {
    path: '/teacher/topics',
    name: 'teacher-topics',
    component: () =>
      import(
        '@/views/teacher/TopicLibraryView.vue'
      ),
    meta: teacherMeta,
  },

  {
    path: '/teacher/courses',
    name: 'teacher-courses',
    component: () =>
      import(
        '@/views/teacher/CourseTemplatesView.vue'
      ),
    meta: teacherMeta,
  },

  {
    path: '/teacher/workload',
    name: 'teacher-workload',
    component: () =>
      import(
        '@/views/teacher/TeacherWorkloadView.vue'
      ),
    meta: teacherMeta,
  },
]
