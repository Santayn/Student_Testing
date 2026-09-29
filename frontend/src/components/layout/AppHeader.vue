<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Select from 'primevue/select'

import { publicRegistrationEnabled } from '@/config/features'
import { hasWorkspaceAccess } from '@/utils/accountAccess'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import { WORKSPACE_ROLE_LABELS } from '@/utils/workspaceRole'

const route = useRoute()
const router = useRouter()

const authStore = useAuthStore()
const themeStore = useThemeStore()

const mobileMenuOpen = ref(false)

const accountReady = computed(() => {
  return hasWorkspaceAccess(
    authStore
  )
})

const authenticatedHomeRoute = computed(() => {
  return accountReady.value
    ? { name: 'home' }
    : { name: 'account-pending' }
})

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
  return (
    WORKSPACE_ROLE_LABELS[
      authStore.workspaceRole
    ] ?? ''
  )
})

const workspaceRoleOptions = computed(() => {
  return authStore.workspaceRoles.map(
    (role) => ({
      value: role,
      label:
        WORKSPACE_ROLE_LABELS[role] ??
        role,
    })
  )
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

async function changeWorkspaceRole(nextRole) {

  if (
    !nextRole ||
    nextRole === authStore.workspaceRole
  ) {
    return
  }

  authStore.setWorkspaceRole(nextRole)
  closeMobileMenu()

  if (route.name !== 'home') {
    await router.push({
      name: 'home',
    })
  }
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
              ? authenticatedHomeRoute
              : { name: 'login' }
          "
          @click="closeMobileMenu"
        >
          Student Testing
        </RouterLink>

        <button
          class="mobile-menu-button"
          type="button"
          :aria-label="
            authStore.isAuthenticated
              ? 'Открыть меню аккаунта'
              : 'Открыть меню'
          "
          aria-controls="mobile-account-menu"
          :aria-expanded="mobileMenuOpen"
          @click="toggleMobileMenu"
        >
          <i
            class="mobile-menu-button__icon pi"
            :class="
              mobileMenuOpen
                ? 'pi-times'
                : authStore.isAuthenticated
                  ? 'pi-user'
                  : 'pi-bars'
            "
            aria-hidden="true"
          />
        </button>
      </div>

      <div
        id="mobile-account-menu"
        class="app-header__content st-scrollbar st-scrollbar--shell"
        :class="{
          'app-header__content--open': mobileMenuOpen,
        }"
      >
        <div class="app-header__actions">
          <label
            v-if="
              authStore.isAuthenticated &&
              accountReady &&
              authStore.hasMultipleWorkspaceRoles
            "
            class="workspace-role-switcher"
          >
            <span class="workspace-role-switcher__label">
              Режим
            </span>

            <Select
              class="workspace-role-switcher__select"
              :model-value="authStore.workspaceRole"
              :options="workspaceRoleOptions"
              option-label="label"
              option-value="value"
              aria-label="Режим работы"
              @update:model-value="changeWorkspaceRole"
            />
          </label>

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
            <div
              class="user-badge"
              aria-label="Текущий пользователь"
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
            </div>

            <button
              class="app-header__button"
              type="button"
              :disabled="authStore.loggingOut"
              @click="logout"
            >
              Выйти
            </button>
          </template>

          <template v-else>
            <RouterLink
              class="app-header__login-link"
              :to="{ name: 'login' }"
            >
              Войти
            </RouterLink>

            <RouterLink
              v-if="publicRegistrationEnabled"
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
    var(--st-shell-text);

  background:
    var(--st-shell-bg);

  border-bottom: 1px solid
    var(--st-shell-border);
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

  font-size: var(--st-font-lg);
  font-weight: var(--st-font-weight-bold);
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

.app-header__actions {
  margin-left: auto;

  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.workspace-role-switcher {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.workspace-role-switcher__label {
  color: var(--st-shell-muted);

  font-size: var(--st-font-xs);
  font-weight: var(--st-font-weight-semibold);
}

.workspace-role-switcher__select {
  width: 164px;
  max-width: 164px;
  min-height: 38px;
}

.workspace-role-switcher :deep(.workspace-role-switcher__select.p-select) {
  color: var(--st-shell-text);
  background: var(--st-shell-hover);

  border: 1px solid var(--st-shell-border);
  border-radius: var(--st-radius-control);

  box-shadow: none;

  font-size: var(--st-font-sm);
  font-weight: var(--st-font-weight-medium);

  transition:
    background-color 0.15s ease,
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.workspace-role-switcher :deep(.workspace-role-switcher__select.p-select:hover) {
  background: var(--st-shell-active);
  border-color: color-mix(
    in srgb,
    var(--st-shell-muted) 58%,
    var(--st-shell-border)
  );
}

.workspace-role-switcher :deep(.workspace-role-switcher__select.p-select.p-focus) {
  background: var(--st-shell-active);
  border-color: var(--st-primary);
  box-shadow: var(--st-focus-shadow);
}

.workspace-role-switcher :deep(.workspace-role-switcher__select .p-select-label) {
  padding: 8px 8px 8px 11px;

  color: var(--st-shell-text);

  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workspace-role-switcher :deep(.workspace-role-switcher__select .p-select-dropdown) {
  width: 34px;

  color: var(--st-shell-muted);
}

.workspace-role-switcher :deep(.workspace-role-switcher__select:hover .p-select-dropdown),
.workspace-role-switcher :deep(.workspace-role-switcher__select.p-focus .p-select-dropdown) {
  color: var(--st-shell-text);
}

.app-header__login-link {
  margin-left: 2px;
  padding: 8px 10px;

  color:
    var(--st-shell-muted);

  border-radius: var(--st-radius-control);

  text-decoration: none;
  white-space: nowrap;

  transition:
    background-color 0.15s ease,
    color 0.15s ease;
}

.app-header__login-link:hover,
.app-header__login-link:focus-visible {
  color:
    var(--st-shell-text);

  background:
    var(--st-shell-hover);
}

.theme-toggle {
  width: 38px;
  height: 38px;

  padding: 0;

  display: inline-grid;
  place-items: center;

  color:
    var(--st-shell-text);

  background:
    var(--st-shell-hover);

  border: 1px solid
    var(--st-shell-border);

  border-radius: var(--st-radius-md);

  cursor: pointer;

  transition:
    background-color 0.15s ease,
    transform 0.15s ease;
}

.theme-toggle:hover {
  background:
    var(--st-shell-active);
}

.theme-toggle:active {
  transform: scale(0.96);
}

.theme-toggle__icon {
  font-size: var(--st-font-xl);
  line-height: 1;
}

.user-badge {
  min-width: 0;
  max-width: 220px;

  padding: 6px 10px;

  display: grid;
  gap: 1px;

  color: inherit;

  border-radius: var(--st-radius-control);
}

.user-badge__name,
.user-badge__role {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-badge__name {
  font-size: var(--st-font-md);
  font-weight: var(--st-font-weight-semibold);
}

.user-badge__role {
  color:
    var(--st-shell-muted);

  font-size: var(--st-font-xs);
}

.app-header__button {
  min-height: 36px;

  padding: 7px 12px;

  display: inline-flex;
  align-items: center;
  justify-content: center;

  color:
    var(--st-shell-text);

  background:
    var(--st-shell-hover);

  border: 1px solid
    var(--st-shell-border);

  border-radius: var(--st-radius-control);

  font: inherit;
  font-size: var(--st-font-md);
  text-decoration: none;

  cursor: pointer;

  transition:
    background-color 0.15s ease,
    border-color 0.15s ease;
}

.app-header__button:hover:not(:disabled) {
  background:
    var(--st-shell-active);
}

.app-header__button:disabled {
  opacity: 0.55;
  cursor: default;
}

.app-header__button--primary {
  background:
    var(--st-primary);

  border-color:
    var(--st-primary);
}

.app-header__button--primary:hover {
  background:
    var(--st-primary-hover);
}

.mobile-menu-button {
  display: none;

  width: 44px;
  height: 44px;

  padding: 0;

  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 5px;

  color: inherit;

  background: transparent;

  border: 0;
  border-radius: var(--st-radius-control);

  cursor: pointer;
}

.mobile-menu-button:hover {
  background:
    var(--st-shell-hover);
}

.mobile-menu-button__icon {
  font-size: 18px;
  line-height: 1;
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
    max-height: min(620px, calc(100dvh - 58px));

    padding: 4px 0 14px;

    overflow-y: auto;
    overscroll-behavior: contain;

    opacity: 1;
  }

  /*
   * На мобильном Header раскрывает только глобальные
   * настройки аккаунта. Навигация остаётся в Sidebar.
   */
  .app-header__actions {
    margin-left: 0;

    align-items: stretch;
    flex-direction: column;
    gap: 8px;

    padding-top: 10px;

    border-top: 1px solid
      var(--st-shell-border);
  }

  .workspace-role-switcher {
    width: 100%;

    display: grid;
    grid-template-columns: auto minmax(0, 1fr);
  }

  .workspace-role-switcher__select {
    width: 100%;
    max-width: none;
  }

  .theme-toggle {
    width: 100%;
    min-height: 44px;

    display: flex;
    align-items: center;
    justify-content: center;
  }

  .theme-toggle::after {
    content: 'Переключить тему';

    margin-left: 8px;

    font-size: var(--st-font-md);
    font-weight: var(--st-font-weight-semibold);
  }

  .user-badge {
    max-width: none;

    padding: 9px 12px;

    background:
      var(--st-shell-hover);
  }

  .app-header__login-link,
  .app-header__button {
    width: 100%;
    min-height: 44px;
  }

  .app-header__login-link {
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

@media (max-width: 420px) {
  .app-header__brand {
    max-width: calc(100vw - 90px);

    overflow: hidden;
    text-overflow: ellipsis;

    font-size: var(--st-font-lg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .app-header__content,
  .app-header__login-link,
  .theme-toggle,
  .app-header__button {
    transition: none;
  }

  .theme-toggle:active {
    transform: none;
  }
}
</style>
