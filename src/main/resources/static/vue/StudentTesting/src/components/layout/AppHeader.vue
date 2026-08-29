<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'

const route = useRoute()
const router = useRouter()

const authStore = useAuthStore()
const themeStore = useThemeStore()

const mobileMenuOpen = ref(false)

const userLabel = computed(() => {
  if (authStore.fullName) {
    return authStore.fullName
  }

  if (authStore.loginName) {
    return authStore.loginName
  }

  if (authStore.email) {
    return authStore.email
  }

  return 'Пользователь'
})

const roleLabel = computed(() => {
  if (authStore.isAdmin) {
    return 'Администратор'
  }

  if (authStore.isTeacher) {
    return 'Преподаватель'
  }

  if (authStore.isStudent) {
    return 'Студент'
  }

  return ''
})

function toggleMobileMenu() {
  mobileMenuOpen.value = !mobileMenuOpen.value
}

function closeMobileMenu() {
  mobileMenuOpen.value = false
}

function changeTheme(event) {
  themeStore.setTheme(event.target.value)
}

async function logout() {
  closeMobileMenu()

  authStore.logout()

  await router.replace({
    name: 'login',
  })
}

function handleKeydown(event) {
  if (event.key === 'Escape') {
    closeMobileMenu()
  }
}

/**
 * После любого перехода закрываем мобильное меню.
 */
watch(
  () => route.fullPath,
  () => {
    closeMobileMenu()
  }
)

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <header class="app-header">
    <div class="app-header__inner">
      <div class="app-header__top">
        <RouterLink
          class="app-header__brand"
          :to="
            authStore.isAuthenticated
              ? { name: 'home' }
              : { name: 'login' }
          "
          @click="closeMobileMenu"
        >
          Student Testing
        </RouterLink>

        <button
          class="mobile-menu-button"
          type="button"
          aria-label="Открыть меню"
          aria-controls="mobile-navigation"
          :aria-expanded="mobileMenuOpen"
          @click="toggleMobileMenu"
        >
          <span
            class="mobile-menu-button__line"
            :class="{
              'mobile-menu-button__line--top-open':
                mobileMenuOpen,
            }"
          />

          <span
            class="mobile-menu-button__line"
            :class="{
              'mobile-menu-button__line--middle-open':
                mobileMenuOpen,
            }"
          />

          <span
            class="mobile-menu-button__line"
            :class="{
              'mobile-menu-button__line--bottom-open':
                mobileMenuOpen,
            }"
          />
        </button>
      </div>

      <div
        id="mobile-navigation"
        class="app-header__content"
        :class="{
          'app-header__content--open': mobileMenuOpen,
        }"
      >
        <nav
          v-if="authStore.isAuthenticated"
          class="app-header__nav"
          aria-label="Основная навигация"
        >
          <RouterLink
            class="app-header__nav-link"
            :to="{ name: 'home' }"
          >
            Главная
          </RouterLink>

          <RouterLink
            class="app-header__nav-link"
            :to="{ name: 'profile' }"
          >
            Профиль
          </RouterLink>

          <RouterLink
            v-if="authStore.isStudent || authStore.isTeacher"
            class="app-header__nav-link"
            :to="{ name: 'subjects' }"
          >
            Предметы
          </RouterLink>

          <RouterLink
            v-if="authStore.isStudent || authStore.isTeacher"
            class="app-header__nav-link"
            :to="{ name: 'results' }"
          >
            Результаты
          </RouterLink>
        </nav>

        <div class="app-header__actions">
          <label class="theme-control">
            <span class="theme-control__label">
              Тема оформления
            </span>

            <select
              class="theme-control__select"
              :value="themeStore.theme"
              aria-label="Тема оформления"
              @change="changeTheme"
            >
              <option value="system">
                Системная
              </option>

              <option value="light">
                Светлая
              </option>

              <option value="dark">
                Тёмная
              </option>
            </select>
          </label>

          <template v-if="authStore.isAuthenticated">
            <RouterLink
              class="user-badge"
              :to="{ name: 'profile' }"
              title="Открыть профиль"
            >
              <span class="user-badge__name">
                {{ userLabel }}
              </span>

              <span
                v-if="roleLabel"
                class="user-badge__role"
              >
                {{ roleLabel }}
              </span>
            </RouterLink>

            <button
              class="app-header__button"
              type="button"
              :disabled="authStore.loading"
              @click="logout"
            >
              Выйти
            </button>
          </template>

          <template v-else>
            <RouterLink
              class="app-header__nav-link"
              :to="{ name: 'login' }"
            >
              Войти
            </RouterLink>

            <RouterLink
              class="app-header__button app-header__button--primary"
              :to="{ name: 'register' }"
            >
              Регистрация
            </RouterLink>
          </template>
        </div>
      </div>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 100;

  width: 100%;

  color:
    var(--header-text, #ffffff);

  background:
    var(--header-bg, #172033);

  border-bottom: 1px solid
    var(--header-border, rgb(255 255 255 / 10%));
}

.app-header__inner {
  width: min(100%, 1180px);
  min-height: 64px;

  margin: 0 auto;
  padding: 0 20px;

  display: flex;
  align-items: center;
  gap: 28px;
}

.app-header__top {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.app-header__brand {
  color: inherit;

  font-size: 17px;
  font-weight: 700;
  text-decoration: none;
  white-space: nowrap;
}

.app-header__content {
  min-width: 0;
  flex: 1;

  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.app-header__nav,
.app-header__actions {
  display: flex;
  align-items: center;
}

.app-header__nav {
  gap: 6px;
}

.app-header__actions {
  gap: 10px;
  flex-shrink: 0;
}

.app-header__nav-link {
  padding: 8px 10px;

  color:
    var(--header-muted-text, #dbe4f0);

  border-radius: 8px;

  text-decoration: none;
  white-space: nowrap;

  transition:
    background-color 0.15s ease,
    color 0.15s ease;
}

.app-header__nav-link:hover,
.app-header__nav-link.router-link-active {
  color:
    var(--header-text, #ffffff);

  background:
    var(--header-hover, rgb(255 255 255 / 10%));
}

.user-badge {
  min-width: 0;
  max-width: 220px;

  padding: 6px 10px;

  display: grid;
  gap: 1px;

  color: inherit;

  border-radius: 8px;

  text-decoration: none;

  transition: background-color 0.15s ease;
}

.user-badge:hover {
  background:
    var(--header-hover, rgb(255 255 255 / 10%));
}

.user-badge__name,
.user-badge__role {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-badge__name {
  font-size: 14px;
  font-weight: 600;
}

.user-badge__role {
  color:
    var(--header-muted-text, #cbd5e1);

  font-size: 12px;
}

.theme-control {
  display: flex;
  align-items: center;
  gap: 7px;
}

.theme-control__label {
  color:
    var(--header-muted-text, #cbd5e1);

  font-size: 13px;
}

.theme-control__select {
  min-height: 36px;

  padding: 0 30px 0 10px;

  color:
    var(--text, #1f2937);

  background:
    var(--surface, #ffffff);

  border: 1px solid
    var(--border, #dbe3ec);

  border-radius: 8px;

  font: inherit;
  font-size: 13px;
}

.app-header__button {
  min-height: 36px;

  padding: 7px 12px;

  display: inline-flex;
  align-items: center;
  justify-content: center;

  color:
    var(--header-text, #ffffff);

  background:
    var(--header-hover, rgb(255 255 255 / 10%));

  border: 1px solid
    var(--header-button-border, rgb(255 255 255 / 18%));

  border-radius: 8px;

  font: inherit;
  font-size: 14px;
  text-decoration: none;

  cursor: pointer;
}

.app-header__button:disabled {
  opacity: 0.55;
  cursor: default;
}

.app-header__button--primary {
  background:
    var(--brand, #2563eb);

  border-color:
    var(--brand, #2563eb);
}

.mobile-menu-button {
  display: none;

  width: 42px;
  height: 42px;

  padding: 0;

  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 5px;

  color: inherit;

  background: transparent;

  border: 0;
  border-radius: 8px;

  cursor: pointer;
}

.mobile-menu-button:hover {
  background:
    var(--header-hover, rgb(255 255 255 / 10%));
}

.mobile-menu-button__line {
  width: 22px;
  height: 2px;

  display: block;

  background: currentColor;

  border-radius: 999px;

  transition:
    transform 0.2s ease,
    opacity 0.2s ease;
}

.mobile-menu-button__line--top-open {
  transform: translateY(7px) rotate(45deg);
}

.mobile-menu-button__line--middle-open {
  opacity: 0;
}

.mobile-menu-button__line--bottom-open {
  transform: translateY(-7px) rotate(-45deg);
}

/*
 * Tablet/mobile mode.
 */
@media (max-width: 820px) {
  .app-header__inner {
    min-height: 58px;

    padding: 0 14px;

    display: block;
  }

  .app-header__top {
    min-height: 58px;

    justify-content: space-between;
  }

  .mobile-menu-button {
    display: flex;
  }

  .app-header__content {
    max-height: 0;

    overflow: hidden;

    display: grid;
    gap: 12px;

    opacity: 0;

    transition:
      max-height 0.25s ease,
      opacity 0.2s ease,
      padding 0.25s ease;
  }

  .app-header__content--open {
    max-height: 620px;

    padding: 4px 0 14px;

    opacity: 1;
  }

  .app-header__nav {
    align-items: stretch;
    flex-direction: column;
    gap: 4px;
  }

  .app-header__nav-link {
    width: 100%;

    padding: 10px 12px;
  }

  .app-header__actions {
    align-items: stretch;
    flex-direction: column;
    gap: 8px;

    padding-top: 10px;

    border-top: 1px solid
      var(--header-border, rgb(255 255 255 / 10%));
  }

  .theme-control {
    width: 100%;

    justify-content: space-between;
  }

  .theme-control__label {
    display: block;
  }

  .theme-control__select {
    min-width: 150px;
  }

  .user-badge {
    max-width: none;

    padding: 9px 12px;

    background:
      var(--header-hover, rgb(255 255 255 / 7%));
  }

  .app-header__button {
    width: 100%;

    min-height: 40px;
  }
}

/*
 * Small phones.
 */
@media (max-width: 420px) {
  .app-header__brand {
    max-width: calc(100vw - 90px);

    overflow: hidden;
    text-overflow: ellipsis;

    font-size: 16px;
  }

  .theme-control {
    align-items: stretch;
    flex-direction: column;
    gap: 6px;
  }

  .theme-control__select {
    width: 100%;
  }
}
</style>
