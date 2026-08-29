<template>
  <section class="auth-layout">
    <div class="auth-layout__panel">
      <RouterLink
        class="auth-layout__brand"
        :to="{ name: 'login' }"
      >
        Student Testing
      </RouterLink>

      <RouterView />
    </div>
  </section>
</template>

<style scoped>
.auth-layout {
  min-height: calc(100vh - 140px);

  display: grid;
  place-items: center;

  padding: 32px 20px;
}

.auth-layout__panel {
  width: min(100%, 440px);

  padding: 32px;

  background:
    var(--surface, #ffffff);

  border: 1px solid
    var(--border, #e5e7eb);

  border-radius: 16px;

  box-shadow:
    var(--shadow, 0 12px 30px rgb(0 0 0 / 8%));
}

.auth-layout__brand {
  margin-bottom: 24px;

  display: inline-block;

  color:
    var(--text, #111827);

  font-size: 19px;
  font-weight: 800;
  text-decoration: none;
}

@media (max-width: 520px) {
  .auth-layout {
    padding: 18px 12px;
  }

  .auth-layout__panel {
    padding: 24px 18px;
  }
}
</style>
