<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const redirect = computed(() => {
  return typeof route.query.redirect === 'string'
    ? route.query.redirect
    : '/'
})

const loginRoute = computed(() => ({
  name: 'login',
  query: {
    redirect: redirect.value,
  },
}))
</script>

<template>
  <div class="auth-required">
    <div class="auth-required__icon">
      !
    </div>

    <h1>
      Требуется авторизация
    </h1>

    <p>
      Для просмотра этой страницы необходимо войти
      в свою учётную запись.
    </p>

    <div class="auth-required__actions">
      <RouterLink
        class="auth-required__button auth-required__button--primary"
        :to="loginRoute"
      >
        Войти
      </RouterLink>

      <RouterLink
        class="auth-required__button"
        :to="{ name: 'register' }"
      >
        Регистрация
      </RouterLink>
    </div>
  </div>
</template>

<style scoped>
.auth-required {
  display: grid;
  justify-items: center;
  gap: 16px;

  text-align: center;
}

.auth-required__icon {
  width: 64px;
  height: 64px;

  display: grid;
  place-items: center;

  color:
    var(--warning);

  background:
    var(--warning-soft);

  border: 1px solid
    var(--warning-border);

  border-radius: 50%;

  font-size: 32px;
  font-weight: 800;
}

.auth-required h1,
.auth-required p {
  margin: 0;
}

.auth-required h1 {
  color:
    var(--text);

  font-size: 27px;
}

.auth-required p {
  max-width: 360px;

  color:
    var(--text-secondary);

  font-size: 14px;
  line-height: 1.6;
}

.auth-required__actions {
  width: 100%;

  display: flex;
  justify-content: center;
  gap: 10px;
}

.auth-required__button {
  min-height: 42px;

  padding: 9px 16px;

  display: inline-flex;
  align-items: center;
  justify-content: center;

  color:
    var(--text);

  background:
    var(--surface-secondary);

  border: 1px solid
    var(--border);

  border-radius: 9px;

  font-size: 14px;
  font-weight: 700;
  text-decoration: none;
}

.auth-required__button--primary {
  color: var(--text-on-brand);

  background:
    var(--brand);

  border-color:
    var(--brand);
}

@media (max-width: 420px) {
  .auth-required__actions {
    flex-direction: column;
  }

  .auth-required__button {
    width: 100%;
  }
}
</style>
