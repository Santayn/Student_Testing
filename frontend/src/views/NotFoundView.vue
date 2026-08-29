<script setup>
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

import {
  UiButton,
  UiCard,
} from '@/components/ui'

const authStore = useAuthStore()

const primaryRoute = computed(() => {
  return authStore.isAuthenticated
    ? { name: 'home' }
    : { name: 'login' }
})

const primaryLabel = computed(() => {
  return authStore.isAuthenticated
    ? 'На главную'
    : 'Войти'
})
</script>

<template>
  <section class="error-page">
    <UiCard class="error-card">
      <div class="error-code">
        404
      </div>

      <h1 class="error-title">
        Страница не найдена
      </h1>

      <p class="error-description">
        Возможно, адрес введён неверно,
        страница была удалена или перемещена.
      </p>

      <div class="error-actions">
        <UiButton
          variant="primary"
          :to="primaryRoute"
        >
          {{ primaryLabel }}
        </UiButton>

        <UiButton
          type="button"
          @click="$router.back()"
        >
          Назад
        </UiButton>
      </div>
    </UiCard>
  </section>
</template>

<style scoped>
.error-page {
  min-height: calc(100vh - 160px);

  display: grid;
  place-items: center;

  padding: 32px 20px;
}

.error-card {
  width: min(100%, 560px);

  padding: 36px;

  display: grid;
  justify-items: center;
  gap: 18px;

  text-align: center;

  color:
    var(--text);

  background:
    var(--surface);

  border: 1px solid
    var(--border);

  border-radius: 16px;

  box-shadow:
    var(--shadow-elevated);
}

.error-card :deep(.ui-card__content) {
  display: grid;
  justify-items: center;
  gap: 18px;

  text-align: center;
}

.error-code {
  font-size: clamp(64px, 14vw, 110px);
  font-weight: 800;
  line-height: 0.9;

  color:
    var(--brand);

  letter-spacing: -0.05em;
}

.error-title {
  margin: 0;

  font-size: clamp(24px, 5vw, 34px);
  line-height: 1.15;
}

.error-description {
  max-width: 430px;

  margin: 0;

  color:
    var(--text-secondary);

  font-size: 15px;
  line-height: 1.6;
}

.error-actions {
  margin-top: 4px;

  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 10px;
}


@media (max-width: 520px) {
  .error-page {
    min-height: calc(100vh - 140px);
    padding: 20px 14px;
  }

  .error-card {
    padding: 28px 20px;
  }

  .error-actions {
    width: 100%;
  }

  .error-actions :deep(.ui-button) {
    width: 100%;
  }
}
</style>
