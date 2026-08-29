<script setup>
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

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
    <div class="error-card">
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
        <RouterLink
          class="error-button error-button--primary"
          :to="primaryRoute"
        >
          {{ primaryLabel }}
        </RouterLink>

        <button
          class="error-button"
          type="button"
          @click="$router.back()"
        >
          Назад
        </button>
      </div>
    </div>
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
    var(--text, #111827);

  background:
    var(--surface, #ffffff);

  border: 1px solid
    var(--border, #e5e7eb);

  border-radius: 16px;

  box-shadow:
    var(--shadow, 0 12px 30px rgb(0 0 0 / 8%));
}

.error-code {
  font-size: clamp(64px, 14vw, 110px);
  font-weight: 800;
  line-height: 0.9;

  color:
    var(--brand, #2563eb);

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
    var(--text-secondary, #6b7280);

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

.error-button {
  min-height: 42px;

  padding: 9px 16px;

  display: inline-flex;
  align-items: center;
  justify-content: center;

  color:
    var(--text, #111827);

  background:
    var(--surface-secondary, #f3f4f6);

  border: 1px solid
    var(--border, #e5e7eb);

  border-radius: 9px;

  font: inherit;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;

  cursor: pointer;
}

.error-button:hover {
  filter: brightness(0.98);
}

.error-button--primary {
  color: #ffffff;

  background:
    var(--brand, #2563eb);

  border-color:
    var(--brand, #2563eb);
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

  .error-button {
    width: 100%;
  }
}
</style>
