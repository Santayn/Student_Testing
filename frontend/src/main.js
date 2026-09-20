import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'

import StudentTestingPreset from '@/theme/studentTestingPreset'

import App from './App.vue'
import '@/assets/tailwind.css'
import '@/assets/theme.css'
import '@/theme/tokens.css'
import '@/theme/foundation.css'
import 'primeicons/primeicons.css'

import router from './router'

import {
  configureHttpAuth,
} from '@/api'

import {
  useAuthStore,
} from '@/stores/auth'

import {
  useThemeStore,
} from '@/stores/theme'

async function bootstrap() {
  const app = createApp(App)

  const pinia = createPinia()

  pinia.use(
    piniaPluginPersistedstate
  )

  app.use(pinia)

  app.use(PrimeVue, {
    ripple: true,

    theme: {
      preset:
        StudentTestingPreset,

      options: {
        darkModeSelector:
          "html[data-theme='dark']",

        cssLayer: {
          name: 'primevue',
          order:
            'theme, primevue, utilities',
        },
      },
    },
  })

  app.use(ToastService)

  const themeStore =
    useThemeStore()

  themeStore.init()

  const authStore =
    useAuthStore()

  let routerReady = false

  configureHttpAuth({
    getAccessToken:
      () =>
        authStore.accessToken,

    ensureAccessToken:
      () =>
        authStore.ensureAccessToken(),

    refreshSession:
      () =>
        authStore.refreshSession(),

    onSessionInvalid:
      () => {
        authStore.clearSession()

        /*
         * Во время первоначального init()
         * Router ещё не запущен.
         * После app.use(router) guard сам отправит
         * пользователя на login.
         */
        if (!routerReady) {
          return
        }

        const currentRoute =
          router.currentRoute.value

        if (
          currentRoute.meta
            .requiresAuth
        ) {
          router.replace({
            name: 'login',

            query: {
              redirect:
                currentRoute.fullPath,
            },
          })
        }
      },
  })

  /*
   * Восстанавливаем persisted session:
   *
   * access жив -> /auth/me
   * access истёк -> /auth/refresh -> /auth/me
   */
  await authStore.init()

  app.use(router)

  await router.isReady()

  routerReady = true

  app.mount('#app')
}

bootstrap().catch((error) => {
  console.error(
    'Не удалось запустить приложение:',
    error
  )
})
