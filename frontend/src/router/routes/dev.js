export const devRoutes = [
  {
    path: '/primevue-preview',
    name: 'primevue-preview',

    component: () =>
      import(
        '@/views/dev/PrimeVuePreviewView.vue'
      ),

    meta: {
      public: true,
    },
  },
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
