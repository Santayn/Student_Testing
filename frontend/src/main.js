import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'

import App from './App.vue'
import router from './router'

import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'

import { setAccessTokenProvider } from '@/api'

async function bootstrap() {
  const app = createApp(App)

  const pinia = createPinia()
  pinia.use(piniaPluginPersistedstate)

  app.use(pinia)

  /*
   * Тема применяется до монтирования приложения,
   * чтобы уменьшить мигание светлой/тёмной темы.
   */
  const themeStore = useThemeStore()
  themeStore.init()

  /*
   * AuthStore становится единственным источником JWT.
   */
  const authStore = useAuthStore()

  setAccessTokenProvider(
    () => authStore.accessToken
  )

  /*
   * Проверяем сохранённую сессию до первого отображения страниц.
   */
  await authStore.init()

  app.use(router)

  await router.isReady()

  app.mount('#app')
}

bootstrap().catch((error) => {
  console.error(
    'Не удалось запустить приложение:',
    error
  )
})
