import {
  WORKSPACE_ROLES,
  LEARNING_ROLES,
  STUDENT_LEARNING_ROLES,
  TEST_TAKER_ROLES,
} from '../roles'

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

export const studentRoutes = [
  {
    path: '/',
    name: 'home',

    component: () =>
      import(
        '@/views/HomeView.vue'
      ),

    meta: authenticatedMeta,
  },

  {
    path: '/profile',
    name: 'profile',

    component: () =>
      import(
        '@/views/ProfileView.vue'
      ),

    meta: authenticatedMeta,
  },

  {
    path: '/subjects',
    name: 'subjects',

    component: () =>
      import(
        '@/views/subjects/SubjectsView.vue'
      ),

    meta: learningMeta,
  },

  {
    path: '/subjects/:subjectId',
    name: 'subject-details',

    component: () =>
      import(
        '@/views/subjects/SubjectDetailsView.vue'
      ),

    meta: learningMeta,
  },

  {
    path: '/subjects/:subjectId/lectures',
    name: 'subject-lectures',

    component: () =>
      import(
        '@/views/lectures/SubjectLecturesView.vue'
      ),

    meta: studentLearningMeta,
  },

  {
    path: '/lectures/:lectureId',
    name: 'lecture-details',

    component: () =>
      import(
        '@/views/lectures/LectureDetailsView.vue'
      ),

    meta: studentLearningMeta,
  },

  {
    path: '/tests/:testId',
    name: 'test',

    component: () =>
      import(
        '@/views/tests/TestView.vue'
      ),

    meta: testTakingMeta,
  },

  {
    path: '/results',
    name: 'results',

    component: () =>
      import(
        '@/views/results/ResultsView.vue'
      ),

    meta: learningMeta,
  },
]
