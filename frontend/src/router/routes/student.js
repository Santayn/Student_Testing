export const studentRoutes = [
  {
    path: '/',
    name: 'home',
    component: () =>
      import(
        '@/views/HomeView.vue'
      ),
    meta: {
      requiresAuth: true,
    },
  },

  {
    path: '/profile',
    name: 'profile',
    component: () =>
      import(
        '@/views/ProfileView.vue'
      ),
    meta: {
      requiresAuth: true,
    },
  },

  {
    path: '/subjects',
    name: 'subjects',
    component: () =>
      import(
        '@/views/subjects/SubjectsView.vue'
      ),
    meta: {
      requiresAuth: true,
    },
  },

  {
    path: '/subjects/:subjectId',
    name: 'subject-details',
    component: () =>
      import(
        '@/views/subjects/SubjectDetailsView.vue'
      ),
    meta: {
      requiresAuth: true,
    },
  },

  {
    path: '/subjects/:subjectId/lectures',
    name: 'subject-lectures',
    component: () =>
      import(
        '@/views/lectures/SubjectLecturesView.vue'
      ),
    meta: {
      requiresAuth: true,
    },
  },

  {
    path: '/lectures/:lectureId',
    name: 'lecture-details',
    component: () =>
      import(
        '@/views/lectures/LectureDetailsView.vue'
      ),
    meta: {
      requiresAuth: true,
    },
  },

  {
    path: '/tests/:testId',
    name: 'test',
    component: () =>
      import(
        '@/views/tests/TestView.vue'
      ),
    meta: {
      requiresAuth: true,
    },
  },

  {
    path: '/results',
    name: 'results',
    component: () =>
      import(
        '@/views/results/ResultsView.vue'
      ),
    meta: {
      requiresAuth: true,
    },
  },
]
