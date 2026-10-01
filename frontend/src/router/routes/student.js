const authenticatedMeta = {
  requiresAuth: true,
}

const subjectWorkspaceMeta = {
  requiresAuth: true,
  permissions: [
    'lectures.read',
    'courses.manage',
    'teaching.manage',
    'academic.manage',
  ],
}

const studentLearningMeta = {
  requiresAuth: true,
  roles: ['STUDENT'],
}

const testTakingMeta = {
  requiresAuth: true,
  roles: ['STUDENT'],
}

const resultsMeta = {
  requiresAuth: true,
  permissions: [
    'lectures.read',
    'tests.manage',
  ],
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

    meta: subjectWorkspaceMeta,
  },

  {
    path: '/subjects/:subjectId',
    name: 'subject-details',

    component: () =>
      import(
        '@/views/subjects/SubjectDetailsView.vue'
      ),

    meta: subjectWorkspaceMeta,
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

    meta: resultsMeta,
  },
]
