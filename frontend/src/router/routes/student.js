import {
  WORKSPACE_ROLES,
  LEARNING_ROLES,
  STUDENT_LEARNING_ROLES,
  TEST_TAKER_ROLES,
} from '../roles'

import {
  NAV_KEYS,
} from '@/navigation/navigation.config'

const authenticatedMeta = {
  requiresAuth: true,
  roles: WORKSPACE_ROLES,
}

const learningMeta = {
  requiresAuth: true,
  roles: LEARNING_ROLES,
}

const studentLearningMeta = {
  requiresAuth: true,
  roles: STUDENT_LEARNING_ROLES,
  workspaceRoles: STUDENT_LEARNING_ROLES,
}

const testTakingMeta = {
  requiresAuth: true,
  roles: TEST_TAKER_ROLES,
  workspaceRoles: TEST_TAKER_ROLES,
}

function navigationMeta(
  baseMeta,
  navKey,
  breadcrumbKey
) {
  return {
    ...baseMeta,
    navKey,
    breadcrumbKey,
  }
}

export const studentRoutes = [
  {
    path: '/',
    name: 'home',

    component: () =>
      import(
        '@/views/HomeView.vue'
      ),

    meta: navigationMeta(
      authenticatedMeta,
      NAV_KEYS.HOME,
      'home'
    ),
  },

  {
    path: '/profile',
    name: 'profile',

    component: () =>
      import(
        '@/views/ProfileView.vue'
      ),

    meta: navigationMeta(
      authenticatedMeta,
      NAV_KEYS.PROFILE,
      'profile'
    ),
  },

  {
    path: '/subjects',
    name: 'subjects',

    component: () =>
      import(
        '@/views/subjects/SubjectsView.vue'
      ),

    meta: navigationMeta(
      learningMeta,
      NAV_KEYS.SUBJECTS,
      'subjects'
    ),
  },

  {
    path: '/subjects/:subjectId',
    name: 'subject-details',

    component: () =>
      import(
        '@/views/subjects/SubjectDetailsView.vue'
      ),

    meta: navigationMeta(
      learningMeta,
      NAV_KEYS.SUBJECTS,
      'subject-details'
    ),
  },

  {
    path: '/subjects/:subjectId/lectures',
    name: 'subject-lectures',

    component: () =>
      import(
        '@/views/lectures/SubjectLecturesView.vue'
      ),

    meta: navigationMeta(
      studentLearningMeta,
      NAV_KEYS.SUBJECTS,
      'subject-lectures'
    ),
  },

  {
    path: '/lectures/:lectureId',
    name: 'lecture-details',

    component: () =>
      import(
        '@/views/lectures/LectureDetailsView.vue'
      ),

    meta: navigationMeta(
      studentLearningMeta,
      NAV_KEYS.SUBJECTS,
      'lecture-details'
    ),
  },

  {
    path: '/tests/:testId',
    name: 'test',

    component: () =>
      import(
        '@/views/tests/TestView.vue'
      ),

    meta: navigationMeta(
      testTakingMeta,
      NAV_KEYS.SUBJECTS,
      'test'
    ),
  },

  {
    path: '/results',
    name: 'results',

    component: () =>
      import(
        '@/views/results/ResultsView.vue'
      ),

    meta: navigationMeta(
      learningMeta,
      NAV_KEYS.RESULTS,
      'results'
    ),
  },
]
