<script setup>
const currentYear = new Date().getFullYear()
</script>

<template>
  <footer class="app-footer">
    <div class="app-footer__inner">
      <span class="app-footer__copyright">
        © {{ currentYear }} Student Testing
      </span>

      <nav
        class="app-footer__nav"
        aria-label="Навигация в подвале"
      >
        <RouterLink
          class="app-footer__link"
          :to="{ name: 'home' }"
        >
          Главная
        </RouterLink>

        <RouterLink
          class="app-footer__link"
          to="/about"
        >
          О системе
        </RouterLink>
      </nav>
    </div>
  </footer>
</template>

<style scoped>
.app-footer {
  width: 100%;

  margin-top: auto;

  color:
    var(--text-secondary, #6b7280);

  background:
    var(--surface, #ffffff);

  border-top: 1px solid
    var(--border, #e5e7eb);
}

.app-footer__inner {
  width: min(100%, 1180px);

  min-height: 42px;

  margin: 0 auto;
  padding: 8px 20px;

  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.app-footer__copyright {
  font-size: 13px;
  white-space: nowrap;
}

.app-footer__nav {
  display: flex;
  align-items: center;
  gap: 14px;
}

.app-footer__link {
  color: inherit;

  font-size: 13px;
  text-decoration: none;

  transition: color 0.15s ease;
}

.app-footer__link:hover,
.app-footer__link.router-link-active {
  color:
    var(--text, #111827);
}

@media (max-width: 520px) {
  .app-footer__inner {
    min-height: auto;

    padding-top: 10px;
    padding-bottom: 10px;

    flex-direction: column;
    gap: 6px;
  }
}
</style>
