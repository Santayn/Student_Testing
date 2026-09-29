<script setup>
import {
  nextTick,
  onErrorCaptured,
  ref,
} from 'vue'

const props = defineProps({
  reload: {
    type: Function,
    default: () => window.location.reload(),
  },
})

const failed = ref(false)
const fallbackTitle = ref(null)

function reloadApplication() {
  props.reload()
}

onErrorCaptured((error, instance, info) => {
  console.error(
    'Непредвиденная ошибка интерфейса:',
    error,
    info,
    instance
  )

  failed.value = true

  nextTick(() => {
    fallbackTitle.value?.focus()
  })

  // Ошибка уже преобразована в безопасный root fallback.
  return false
})
</script>

<template>
  <section
    v-if="failed"
    class="runtime-error"
    role="alert"
    aria-labelledby="runtime-error-title"
  >
    <div class="runtime-error__panel">
      <h1
        id="runtime-error-title"
        ref="fallbackTitle"
        class="runtime-error__title"
        tabindex="-1"
      >
        Произошла непредвиденная ошибка
      </h1>

      <p class="runtime-error__message">
        Приложение не может безопасно продолжить работу.
        Перезагрузите страницу.
      </p>

      <button
        type="button"
        class="runtime-error__reload"
        @click="reloadApplication"
      >
        Перезагрузить
      </button>
    </div>
  </section>

  <slot v-else />
</template>

<style scoped>
.runtime-error {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  box-sizing: border-box;
  background: var(--st-page-bg);
  color: var(--st-text);
}

.runtime-error__panel {
  width: min(560px, 100%);
  padding: 28px;
  border: 1px solid var(--st-border);
  border-radius: 16px;
  background: var(--st-surface);
  box-shadow: var(--st-shadow-elevated);
}

.runtime-error__title {
  margin: 0 0 12px;
  font-size: clamp(1.5rem, 4vw, 2rem);
  line-height: 1.2;
}

.runtime-error__message {
  margin: 0 0 20px;
  line-height: 1.6;
}

.runtime-error__reload {
  min-height: 44px;
  padding: 10px 18px;
  border: 1px solid var(--st-border);
  border-radius: 10px;
  background: var(--st-primary);
  color: var(--st-on-primary, #fff);
  cursor: pointer;
  font: inherit;
}

.runtime-error__reload:focus-visible {
  outline: none;
  box-shadow: var(--st-focus-shadow);
}
</style>
