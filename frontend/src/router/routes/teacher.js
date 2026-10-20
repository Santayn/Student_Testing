import {
  TEACHER_ROLES,
} from '../roles'

import {
  NAV_KEYS,
} from '@/navigation/navigation.config'

const teacherMeta = {
  requiresAuth: true,
  roles: TEACHER_ROLES,
  workspaceRoles: TEACHER_ROLES,
}

function navigationMeta(
  navKey,
  breadcrumbKey
) {
  return {
    ...teacherMeta,
    navKey,
    breadcrumbKey,
  }
}

export const teacherRoutes = [
  {
    path: '/teacher/questions',
    name: 'teacher-questions',

    component: () =>
      import(
        '@/views/teacher/QuestionsView.vue'
      ),

    meta: navigationMeta(
      NAV_KEYS.TEACHER_QUESTIONS,
      'teacher-questions'
    ),
  },

  {
    path: '/teacher/tests/create',
    name: 'teacher-test-create',

    component: () =>
      import(
        '@/views/teacher/TestEditorView.vue'
      ),

    meta: navigationMeta(
      NAV_KEYS.TEACHER_QUESTIONS,
      'teacher-test-create'
    ),
  },

  {
    path: '/teacher/lectures',
    name: 'teacher-lectures',

    component: () =>
      import(
        '@/views/teacher/LectureManagementView.vue'
      ),

    meta: navigationMeta(
      NAV_KEYS.TEACHER_LECTURES,
      'teacher-lectures'
    ),
  },

  {
    path: '/teacher/lectures/:lectureId/presentation',
    name: 'teacher-lecture-presentation',

    component: () =>
      import(
        '@/views/teacher/LecturePresentationView.vue'
      ),

    meta: navigationMeta(
      NAV_KEYS.TEACHER_LECTURES,
      'teacher-lecture-presentation'
    ),
  },

  {
    path: '/teacher/topics',
    name: 'teacher-topics',

    component: () =>
      import(
        '@/views/teacher/TopicLibraryView.vue'
      ),

    meta: navigationMeta(
      NAV_KEYS.TEACHER_TOPICS,
      'teacher-topics'
    ),
  },

  {
    path: '/teacher/courses',
    name: 'teacher-courses',

    component: () =>
      import(
        '@/views/teacher/CourseTemplatesView.vue'
      ),

    meta: navigationMeta(
      NAV_KEYS.TEACHER_COURSES,
      'teacher-courses'
    ),
  },

  {
    path: '/teacher/review',
    name: 'teacher-review',

    component: () =>
      import(
        '@/views/results/ReviewQueueView.vue'
      ),

    meta: navigationMeta(
      NAV_KEYS.TEACHER_REVIEW,
      'teacher-review'
    ),
  },

  {
    path: '/teacher/workload',
    name: 'teacher-workload',

    component: () =>
      import(
        '@/views/teacher/TeacherWorkloadView.vue'
      ),

    meta: navigationMeta(
      NAV_KEYS.TEACHER_WORKLOAD,
      'teacher-workload'
    ),
  },
]
