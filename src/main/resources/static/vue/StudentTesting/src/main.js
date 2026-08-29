import { createApp } from 'vue'
import { createPinia } from 'pinia'

import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'

import App from './App.vue'
import router from './router'

import { useThemeStore } from '@/stores/theme'

import './assets/main.css'

const app = createApp(App)

const pinia = createPinia()

pinia.use(piniaPluginPersistedstate)

app.use(pinia)
app.use(router)

// ThemeStore можно использовать только после установки Pinia
const themeStore = useThemeStore()

themeStore.init()

app.mount('#app')