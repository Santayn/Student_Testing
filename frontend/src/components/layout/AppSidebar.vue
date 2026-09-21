<script setup>
import {
  computed,
  nextTick,
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
const mobileToggleRef = ref(null)
const sidebarRef = ref(null)
const closeButtonRef = ref(null)

let mobileMediaQuery = null

const sections = computed(() => {
  return getWorkspaceNavigation(
    authStore.workspaceRole
  )
})

const activeKey = computed(() => {
  return getActiveNavigationKey(route)
})

const activeItemLabel = computed(() => {
  for (const section of sections.value) {
    const item = section.items.find(
      (candidate) =>
        candidate.key === activeKey.value
    )

    if (item) {
      return item.label
    }
  }

  return ''
})

const visible = computed(() => {
  return sections.value.some(
    (section) => section.items.length > 0
  )
})

function isActive(item) {
  return item.key === activeKey.value
}

function focusActiveItem() {
  const sidebar = sidebarRef.value

  if (!sidebar) {
    return
  }

  const activeLink = sidebar.querySelector(
    '.app-sidebar__link--active'
  )

  activeLink?.scrollIntoView?.({
    block: 'nearest',
  })
}

async function openMobile() {
  if (mobileOpen.value) {
    return
  }

  mobileOpen.value = true

  await nextTick()

  focusActiveItem()
  closeButtonRef.value?.focus()
}

async function closeMobile(
  restoreFocus = false
) {
  const wasOpen = mobileOpen.value

  mobileOpen.value = false

  if (restoreFocus && wasOpen) {
    await nextTick()
    mobileToggleRef.value?.focus()
  }
}

function toggleMobile() {
  if (mobileOpen.value) {
    closeMobile(true)
    return
  }

  openMobile()
}

function drawerFocusableElements() {
  const sidebar = sidebarRef.value

  if (!sidebar) {
    return []
  }

  return [
    ...sidebar.querySelectorAll(
      '.app-sidebar__close, .app-sidebar__link'
    ),
  ].filter(
    (element) =>
      !element.hasAttribute('disabled') &&
      element.getAttribute('tabindex') !== '-1'
  )
}

function keepFocusInsideDrawer(event) {
  if (
    event.key !== 'Tab' ||
    !mobileOpen.value
  ) {
    return
  }

  const focusable = drawerFocusableElements()

  if (!focusable.length) {
    return
  }

  const first = focusable[0]
  const last = focusable[focusable.length - 1]
  const active = document.activeElement

  if (
    event.shiftKey &&
    active === first
  ) {
    event.preventDefault()
    last.focus()
    return
  }

  if (
    !event.shiftKey &&
    active === last
  ) {
    event.preventDefault()
    first.focus()
  }
}

function handleKeydown(event) {
  if (
    event.key === 'Escape' &&
    mobileOpen.value
  ) {
    event.preventDefault()
    closeMobile(true)
    return
  }

  keepFocusInsideDrawer(event)
}

function handleViewportChange(event) {
  if (!event.matches) {
    closeMobile(false)
  }
}

watch(
  () => route.fullPath,
  async () => {
    await closeMobile(false)
    await nextTick()
    focusActiveItem()
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

onMounted(async () => {
  window.addEventListener('keydown', handleKeydown)

  mobileMediaQuery = window.matchMedia?.(
    '(max-width: 960px)'
  ) ?? null

  if (mobileMediaQuery?.addEventListener) {
    mobileMediaQuery.addEventListener(
      'change',
      handleViewportChange
    )
  } else {
    mobileMediaQuery?.addListener?.(
      handleViewportChange
    )
  }

  await nextTick()
  focusActiveItem()
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown)

  if (mobileMediaQuery?.removeEventListener) {
    mobileMediaQuery.removeEventListener(
      'change',
      handleViewportChange
    )
  } else {
    mobileMediaQuery?.removeListener?.(
      handleViewportChange
    )
  }

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
        ref="mobileToggleRef"
        class="sidebar-mobile-toggle"
        type="button"
        :aria-expanded="mobileOpen"
        aria-controls="app-sidebar"
        aria-label="Открыть навигацию по разделам"
        @click="toggleMobile"
      >
        <span
          class="sidebar-mobile-toggle__icon"
          aria-hidden="true"
        >
          ☰
        </span>

        <span class="sidebar-mobile-toggle__label">
          Разделы
        </span>

        <span
          v-if="activeItemLabel"
          class="sidebar-mobile-toggle__current"
        >
          {{ activeItemLabel }}
        </span>
      </button>
    </div>

    <div
      v-if="mobileOpen"
      class="sidebar-overlay"
      aria-hidden="true"
      @click="closeMobile(true)"
    />

    <aside
      id="app-sidebar"
      ref="sidebarRef"
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
          ref="closeButtonRef"
          class="app-sidebar__close"
          type="button"
          aria-label="Закрыть меню"
          @click="closeMobile(true)"
        >
          ×
        </button>
      </div>

      <nav class="app-sidebar__nav">
        <section
          v-for="section in sections"
          :key="section.key"
          class="app-sidebar__section"
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
            }"
            :to="item.route"
            :aria-current="
              isActive(item)
                ? 'page'
                : undefined
            "
            @click="closeMobile(true)"
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
  top: 84px;

  max-height: calc(100dvh - 104px);

  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
  scrollbar-width: thin;
  scrollbar-color:
    var(--st-shell-border)
    transparent;

  background:
    var(--st-shell-bg);

  border: 1px solid
    var(--st-shell-border);

  border-radius: 12px;
}

.app-sidebar::-webkit-scrollbar {
  width: 8px;
}

.app-sidebar::-webkit-scrollbar-thumb {
  background:
    var(--st-shell-border);

  border: 2px solid transparent;
  border-radius: 999px;
  background-clip: padding-box;
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

.app-sidebar__link:focus-visible,
.sidebar-mobile-toggle:focus-visible,
.app-sidebar__close:focus-visible {
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
    width: 100%;

    position: sticky;
    top: 64px;
    z-index: 90;

    display: block;
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
    flex: 0 0 auto;

    font-size: 18px;
    line-height: 1;
  }

  .sidebar-mobile-toggle__label {
    flex: 0 0 auto;
  }

  .sidebar-mobile-toggle__current {
    min-width: 0;
    margin-left: auto;

    overflow: hidden;

    color:
      var(--st-shell-muted);

    font-size: 13px;
    font-weight: 500;

    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .sidebar-overlay {
    position: fixed;
    inset: 0;
    z-index: 190;

    display: block;

    background:
      var(--st-overlay-backdrop);

    touch-action: none;
  }

  .app-sidebar {
    width: min(88vw, 320px);
    min-width: 0;
    height: 100dvh;
    max-height: 100dvh;

    position: fixed;
    top: 0;
    left: 0;
    z-index: 200;

    overflow-y: auto;
    overscroll-behavior: contain;
    -webkit-overflow-scrolling: touch;

    visibility: hidden;
    pointer-events: none;

    border: 0;
    border-right: 1px solid
      var(--st-shell-border);

    border-radius: 0;

    transform: translateX(-100%);

    transition:
      transform 0.22s ease,
      visibility 0s linear 0.22s;
  }

  .app-sidebar--open {
    visibility: visible;
    pointer-events: auto;

    transform: translateX(0);

    transition-delay: 0s;
  }

  .app-sidebar__mobile-header {
    min-height: calc(
      58px + env(safe-area-inset-top)
    );

    padding:
      env(safe-area-inset-top)
      14px
      0;

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
    padding:
      12px
      12px
      calc(12px + env(safe-area-inset-bottom));
  }

  .app-sidebar__link {
    min-height: 44px;

    padding: 10px 12px;

    font-size: 15px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .app-sidebar {
    transition: none;
  }
}
</style>

<style>
@media (max-width: 960px) {
  body.sidebar-mobile-open {
    overflow: hidden;
    touch-action: none;
  }
}
</style>
