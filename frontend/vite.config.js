import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig(({ mode }) => {
  const projectRoot = fileURLToPath(new URL('../', import.meta.url))
  const configEnv = loadEnv(
    mode,
    projectRoot,
    [
      'BACKEND_',
      'APP_PUBLIC_REGISTRATION_ENABLED',
    ]
  )

  const backendPort =
    configEnv.BACKEND_PORT ||
    process.env.BACKEND_PORT ||
    '8080'

  const publicRegistrationEnabled =
    configEnv.APP_PUBLIC_REGISTRATION_ENABLED ||
    process.env.APP_PUBLIC_REGISTRATION_ENABLED ||
    'false'

  return {
    // The root .env/.env.example pair is the canonical configuration source.
    envDir: projectRoot,

    // Expose only this specific safe APP_* feature flag to browser code.
    // Other APP_* values (including secrets) remain server-side only.
    define: {
      'import.meta.env.APP_PUBLIC_REGISTRATION_ENABLED': JSON.stringify(
        publicRegistrationEnabled
      ),
    },

    plugins: [
      vue(),
      tailwindcss(),
    ],

    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },

    server: {
      proxy: {
        '/api': {
          target: `http://127.0.0.1:${backendPort}`,
          changeOrigin: true,
        },
      },
    },
  }
})
