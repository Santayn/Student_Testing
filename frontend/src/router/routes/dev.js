export const devRoutes = [
  {
    path: '/overlay-components-preview',
    name: 'overlay-components-preview',

    component: () =>
      import(
        '@/views/dev/OverlayComponentsPreviewView.vue'
      ),

    meta: {
      public: true,
    },
  },
  {
    path: '/ui-components-preview',
    name: 'ui-components-preview',

    component: () =>
      import(
        '@/views/dev/UiComponentsPreviewView.vue'
      ),

    meta: {
      public: true,
    },
  },
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
