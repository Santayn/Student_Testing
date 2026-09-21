<script setup>
import {
  computed,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
} from 'vue'
import { useRoute } from 'vue-router'

import {
  getActiveNavigationKey,
  getWorkspaceNavigation,
} from '@/navigation'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const authStore = useAuthStore()

const mobileOpen = ref(false)

const sections = computed(() => {
  return getWorkspaceNavigation(
    authStore.workspaceRole
  )
})

const activeKey = computed(() => {
  return getActiveNavigationKey(route)
})

const visible = computed(() => {
  return sections.value.some(
    (section) => section.items.length > 0
  )
})

function isActive(item) {
  return item.key === activeKey.value
}

function openMobile() {
  mobileOpen.value = true
}

function closeMobile() {
  mobileOpen.value = false
}

function toggleMobile() {
  mobileOpen.value = !mobileOpen.value
}

function handleKeydown(event) {
  if (event.key === 'Escape') {
    closeMobile()
  }
}

watch(
  () => route.fullPath,
  () => {
    closeMobile()
  }
)

watch(mobileOpen, (open) => {
  if (typeof document === 'undefined') {
    return
  }

  document.body.classList.toggle(
    'sidebar-mobile-open',
    open
  )
})

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown)

  if (typeof document !== 'undefined') {
    document.body.classList.remove(
      'sidebar-mobile-open'
    )
  }
})
</script>

<template>
  <template v-if="visible">
    <div class="sidebar-mobile-bar">
      <button
        class="sidebar-mobile-toggle"
        type="button"
        :aria-expanded="mobileOpen"
        aria-controls="app-sidebar"
        @click="toggleMobile"
      >
        <span class="sidebar-mobile-toggle__icon">
          ☰
        </span>

        <span>
          Разделы
        </span>
      </button>
    </div>

    <div
      v-if="mobileOpen"
      class="sidebar-overlay"
      aria-hidden="true"
      @click="closeMobile"
    />

    <aside
      id="app-sidebar"
      class="app-sidebar"
      :class="{
        'app-sidebar--open': mobileOpen,
      }"
      aria-label="Боковая навигация"
    >
      <div class="app-sidebar__mobile-header">
        <strong>
          Разделы
        </strong>

        <button
          class="app-sidebar__close"
          type="button"
          aria-label="Закрыть меню"
          @click="closeMobile"
        >
          ×
        </button>
      </div>

      <nav class="app-sidebar__nav">
        <section
          v-for="section in sections"
          :key="section.key"
          class="app-sidebar__section"
          :class="{
            'app-sidebar__section--actions':
              section.kind === 'actions',
          }"
        >
          <h2 class="app-sidebar__title">
            {{ section.label }}
          </h2>

          <RouterLink
            v-for="item in section.items"
            :key="item.key"
            class="app-sidebar__link"
            :class="{
              'app-sidebar__link--active':
                isActive(item),
              'app-sidebar__link--action':
                section.kind === 'actions',
            }"
            :to="item.route"
            :aria-current="
              isActive(item)
                ? 'page'
                : undefined
            "
          >
            <i
              v-if="item.icon"
              class="app-sidebar__icon"
              :class="item.icon"
              aria-hidden="true"
            />

            <span>
              {{ item.label }}
            </span>
          </RouterLink>
        </section>
      </nav>
    </aside>
  </template>
</template>

<style scoped>
.app-sidebar {
  width: 250px;
  min-width: 250px;

  align-self: flex-start;

  position: sticky;
  top: 80px;

  max-height: calc(100vh - 96px);

  overflow-y: auto;

  background:
    var(--st-shell-bg);

  border: 1px solid
    var(--st-shell-border);

  border-radius: 12px;
}

.app-sidebar__nav {
  padding: 10px;
}

.app-sidebar__section + .app-sidebar__section {
  margin-top: 14px;
  padding-top: 14px;

  border-top: 1px solid
    var(--st-shell-border);
}

.app-sidebar__title {
  margin: 0 8px 7px;

  color:
    var(--st-shell-muted);

  font-size: 12px;
  font-weight: 700;
  line-height: 1.3;

  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.app-sidebar__link {
  min-height: 40px;

  padding: 9px 10px;

  display: flex;
  align-items: center;
  gap: 9px;

  color:
    var(--st-shell-text);

  border-radius: 8px;

  font-size: 14px;
  text-decoration: none;

  transition:
    background-color 0.15s ease,
    color 0.15s ease;
}

.app-sidebar__icon {
  width: 18px;

  flex: 0 0 18px;

  color:
    var(--st-shell-muted);

  text-align: center;

  transition: color 0.15s ease;
}

.app-sidebar__link:hover,
.app-sidebar__link:focus-visible {
  background:
    var(--st-shell-hover);
}

.app-sidebar__link:focus-visible {
  outline: 2px solid
    var(--st-primary);
  outline-offset: 2px;

  box-shadow:
    var(--st-focus-shadow);
}

.app-sidebar__link--active {
  color:
    var(--st-shell-text);

  background:
    var(--st-shell-active);

  font-weight: 600;
}

.app-sidebar__link--active .app-sidebar__icon {
  color:
    var(--st-primary);
}

.app-sidebar__section--actions .app-sidebar__link {
  border: 1px solid
    var(--st-shell-border);
}

.app-sidebar__section--actions .app-sidebar__link--action {
  background:
    var(--st-shell-hover);
}

.app-sidebar__section--actions
  .app-sidebar__link--action:hover,
.app-sidebar__section--actions
  .app-sidebar__link--action:focus-visible {
  background:
    var(--st-shell-active);
}

.app-sidebar__mobile-header,
.sidebar-mobile-bar,
.sidebar-overlay {
  display: none;
}

/*
 * Mobile / tablet drawer.
 */
@media (max-width: 960px) {
  .sidebar-mobile-bar {
    display: block;

    width: 100%;
  }

  .sidebar-mobile-toggle {
    width: 100%;
    min-height: 44px;

    padding: 8px 14px;

    display: flex;
    align-items: center;
    gap: 9px;

    color:
      var(--st-shell-text);

    background:
      var(--st-shell-bg);

    border: 0;
    border-bottom: 1px solid
      var(--st-shell-border);

    font: inherit;
    font-size: 14px;
    font-weight: 600;

    cursor: pointer;
  }

  .sidebar-mobile-toggle__icon {
    font-size: 18px;
    line-height: 1;
  }

  .sidebar-overlay {
    position: fixed;
    inset: 0;
    z-index: 190;

    display: block;

    background: var(--st-overlay-backdrop);
  }

  .app-sidebar {
    width: min(86vw, 310px);
    min-width: 0;
    height: 100dvh;
    max-height: 100dvh;

    position: fixed;
    top: 0;
    left: 0;
    z-index: 200;

    overflow-y: auto;

    border: 0;
    border-right: 1px solid
      var(--st-shell-border);

    border-radius: 0;

    transform: translateX(-100%);

    transition: transform 0.22s ease;
  }

  .app-sidebar--open {
    transform: translateX(0);
  }

  .app-sidebar__mobile-header {
    min-height: 58px;

    padding: 0 14px;

    position: sticky;
    top: 0;
    z-index: 2;

    display: flex;
    align-items: center;
    justify-content: space-between;

    background:
      var(--st-shell-bg);

    border-bottom: 1px solid
      var(--st-shell-border);
  }

  .app-sidebar__close {
    width: 40px;
    height: 40px;

    display: inline-flex;
    align-items: center;
    justify-content: center;

    color:
      var(--st-shell-text);

    background: transparent;

    border: 0;
    border-radius: 8px;

    font: inherit;
    font-size: 28px;
    line-height: 1;

    cursor: pointer;
  }

  .app-sidebar__close:hover {
    background:
      var(--st-shell-hover);
  }

  .app-sidebar__nav {
    padding: 12px;
  }

  .app-sidebar__link {
    min-height: 44px;

    padding: 10px 12px;

    font-size: 15px;
  }
}
</style>

<style>
body.sidebar-mobile-open {
  overflow: hidden;
}
</style>
