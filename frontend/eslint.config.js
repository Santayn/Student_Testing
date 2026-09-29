import js from '@eslint/js'
import { defineConfig } from 'eslint/config'
import pluginVue from 'eslint-plugin-vue'
import globals from 'globals'

const unusedVarsRule = [
  'error',
  {
    argsIgnorePattern: '^_',
    caughtErrorsIgnorePattern: '^_',
    varsIgnorePattern: '^_',
  },
]

export default defineConfig([
  {
    ignores: [
      'dist/**',
      'coverage/**',
      'node_modules/**',
    ],
  },

  {
    files: ['src/**/*.{js,vue}'],
    extends: [
      js.configs.recommended,
      pluginVue.configs['flat/essential'],
    ],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: {
        ...globals.browser,
      },
    },
    rules: {
      'no-unused-vars': unusedVarsRule,
      'vue/no-mutating-props': [
        'error',
        {
          shallowOnly: true,
        },
      ],
    },
  },

  {
    files: ['src/__tests__/**/*.js'],
    languageOptions: {
      globals: {
        ...globals.browser,
        ...globals.node,
      },
    },
    rules: {
      'vue/multi-word-component-names': 'off',
    },
  },

  {
    files: [
      'scripts/**/*.js',
      '*.config.js',
      'eslint.config.js',
    ],
    extends: [
      js.configs.recommended,
    ],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: {
        ...globals.node,
      },
    },
    rules: {
      'no-unused-vars': unusedVarsRule,
    },
  },
])
