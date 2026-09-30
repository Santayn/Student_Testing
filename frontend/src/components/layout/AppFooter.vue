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
        aria-label="Служебные ссылки"
      >
        <RouterLink
          class="app-footer__link"
          :to="{ name: 'about' }"
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
    var(--st-shell-muted);

  background:
    var(--st-shell-bg);

  border-top: 1px solid
    var(--st-shell-border);
}

.app-footer__inner {
  width: min(100%, 1180px);

  min-height: 40px;

  margin: 0 auto;
  padding: 7px 20px;

  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.app-footer__copyright {
  color: color-mix(
    in srgb,
    var(--st-shell-muted) 86%,
    transparent
  );

  font-size: var(--st-font-sm);
  font-weight: var(--st-font-weight-regular);
  white-space: nowrap;
}

.app-footer__nav {
  display: flex;
  align-items: center;
  gap: 14px;
}

.app-footer__link {
  padding: 4px 6px;

  color: inherit;

  border-radius: var(--st-radius-control);

  font-size: var(--st-font-sm);
  font-weight: var(--st-font-weight-medium);
  text-decoration: none;

  transition:
    color var(--st-motion-fast) var(--st-ease-standard),
    background-color var(--st-motion-fast) var(--st-ease-standard);
}

.app-footer__link:hover,
.app-footer__link.router-link-active {
  color:
    var(--st-shell-text);

  background:
    var(--st-shell-hover);
}

.app-footer__link:focus-visible {
  outline: 2px solid
    var(--st-primary);
  outline-offset: 3px;
  border-radius: var(--st-radius-control);
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

@media (prefers-reduced-motion: reduce) {
  .app-footer__link {
    transition: none;
  }
}
</style>
