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

const themeButtonLabel = computed(() => {
  return themeStore.isDark
    ? 'Включить светлую тему'
    : 'Включить тёмную тему'
})

const themeButtonIcon = computed(() => {
  return themeStore.isDark
    ? '☀'
    : '☾'
})

function toggleMobileMenu() {
  mobileMenuOpen.value = !mobileMenuOpen.value
}

function closeMobileMenu() {
  mobileMenuOpen.value = false
}

function toggleTheme() {
  themeStore.toggleTheme()
}

async function logout() {
  closeMobileMenu()

  await authStore.logout()

  await router.replace({
    name: 'login',
  })
}

function handleKeydown(event) {
  if (event.key === 'Escape') {
    closeMobileMenu()
  }
}

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
          <button
            class="theme-toggle"
            type="button"
            :aria-label="themeButtonLabel"
            :title="themeButtonLabel"
            @click="toggleTheme"
          >
            <span
              class="theme-toggle__icon"
              aria-hidden="true"
            >
              {{ themeButtonIcon }}
            </span>
          </button>

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
              class="app-header__nav-link app-header__login-link"
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
    var(--header-text);

  background:
    var(--header-bg);

  border-bottom: 1px solid
    var(--header-border);
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
  margin-left: auto;

  gap: 10px;
  flex-shrink: 0;
}

.app-header__nav-link {
  padding: 8px 10px;

  color:
    var(--header-muted-text);

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
    var(--header-text);

  background:
    var(--header-hover);
}

.app-header__login-link {
  margin-left: 2px;
}

.theme-toggle {
  width: 38px;
  height: 38px;

  padding: 0;

  display: inline-grid;
  place-items: center;

  color:
    var(--header-text);

  background:
    var(--header-hover);

  border: 1px solid
    var(--header-button-border);

  border-radius: 9px;

  cursor: pointer;

  transition:
    background-color 0.15s ease,
    transform 0.15s ease;
}

.theme-toggle:hover {
  background:
    var(--header-button-hover);
}

.theme-toggle:active {
  transform: scale(0.96);
}

.theme-toggle__icon {
  font-size: 20px;
  line-height: 1;
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
    var(--header-hover);
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
    var(--header-muted-text);

  font-size: 12px;
}

.app-header__button {
  min-height: 36px;

  padding: 7px 12px;

  display: inline-flex;
  align-items: center;
  justify-content: center;

  color:
    var(--header-text);

  background:
    var(--header-hover);

  border: 1px solid
    var(--header-button-border);

  border-radius: 8px;

  font: inherit;
  font-size: 14px;
  text-decoration: none;

  cursor: pointer;

  transition:
    background-color 0.15s ease,
    border-color 0.15s ease;
}

.app-header__button:hover:not(:disabled) {
  background:
    var(--header-button-hover);
}

.app-header__button:disabled {
  opacity: 0.55;
  cursor: default;
}

.app-header__button--primary {
  background:
    var(--brand);

  border-color:
    var(--brand);
}

.app-header__button--primary:hover {
  background:
    var(--brand-hover);
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
    var(--header-hover);
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
 * Mobile / tablet.
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

  /*
   * На мобильном правый desktop-блок превращается
   * в отдельный нижний блок меню.
   */
  .app-header__actions {
    margin-left: 0;

    align-items: stretch;
    flex-direction: column;
    gap: 8px;

    padding-top: 10px;

    border-top: 1px solid
      var(--header-border);
  }

  .theme-toggle {
    width: 100%;
    height: 40px;

    display: flex;
    align-items: center;
    justify-content: center;
  }

  .theme-toggle::after {
    content: 'Переключить тему';

    margin-left: 8px;

    font-size: 14px;
    font-weight: 600;
  }

  .user-badge {
    max-width: none;

    padding: 9px 12px;

    background:
      var(--header-hover);
  }

  .app-header__button {
    width: 100%;
    min-height: 40px;
  }
}

@media (max-width: 420px) {
  .app-header__brand {
    max-width: calc(100vw - 90px);

    overflow: hidden;
    text-overflow: ellipsis;

    font-size: 16px;
  }
}
</style>
