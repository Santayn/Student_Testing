export const devRoutes = [
  {
    path: '/ui-showcase',
    name: 'ui-showcase',

    component: () =>
      import(
        '@/views/dev/UiShowcaseView.vue'
      ),

    meta: {
      public: true,
    },
  },
]
