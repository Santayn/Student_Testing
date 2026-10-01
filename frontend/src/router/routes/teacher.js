const teacherMeta = (permissions) => ({
  requiresAuth: true,
  permissions,
  sidebar: true,
})

export const teacherRoutes = [
  {
    path: '/teacher/questions',
    name: 'teacher-questions',

    component: () =>
      import(
        '@/views/teacher/QuestionsView.vue'
      ),

    meta: teacherMeta(['questions.manage']),
  },

  {
    path: '/teacher/tests',
    name: 'teacher-tests',

    component: () =>
      import(
        '@/views/teacher/TestManagementView.vue'
      ),

    meta: teacherMeta(['tests.manage']),
  },

  {
    path: '/teacher/tests/create',
    name: 'teacher-test-create',

    component: () =>
      import(
        '@/views/teacher/TestEditorView.vue'
      ),

    meta: teacherMeta(['tests.manage']),
  },

  {
    path: '/teacher/lectures',
    name: 'teacher-lectures',

    component: () =>
      import(
        '@/views/teacher/LectureManagementView.vue'
      ),

    meta: teacherMeta(['courses.manage']),
  },

  {
    path: '/teacher/topics',
    name: 'teacher-topics',

    component: () =>
      import(
        '@/views/teacher/TopicLibraryView.vue'
      ),

    meta: teacherMeta(['questions.manage']),
  },

  {
    path: '/teacher/courses',
    name: 'teacher-courses',

    component: () =>
      import(
        '@/views/teacher/CourseTemplatesView.vue'
      ),

    meta: teacherMeta(['courses.manage']),
  },

  {
    path: '/teacher/workload',
    name: 'teacher-workload',

    component: () =>
      import(
        '@/views/teacher/TeacherWorkloadView.vue'
      ),

    meta: teacherMeta(['teaching.manage']),
  },
]
