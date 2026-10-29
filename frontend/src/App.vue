<script setup>
import {
  computed,
  nextTick,
  ref,
  watch,
} from 'vue'
import { useRoute } from 'vue-router'

import AppHeader from '@/components/layout/AppHeader.vue'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import AppNavigationRail from '@/components/layout/AppNavigationRail.vue'
import AppBottomNavigation from '@/components/layout/AppBottomNavigation.vue'
import { useLayoutStore } from '@/stores/layout'
import { useNavigationUiStore } from '@/stores/navigationUi'
import AppBreadcrumb from '@/components/layout/AppBreadcrumb.vue'
import AppFooter from '@/components/layout/AppFooter.vue'

import {
  provideBreadcrumbContext,
} from '@/navigation'
import {
  getDocumentTitle,
} from '@/router/pageMetadata'
import {
  focusRouteContent,
} from '@/utils/focusRouteContent'

const route = useRoute()
const layoutStore = useLayoutStore()
const navigationUi = useNavigationUiStore()
const navigationMode = computed(() => layoutStore.mode || 'wide')
const immersiveNavigation = computed(() =>
  ['test', 'teacher-lecture-presentation'].includes(String(route.name))
)
const appMain = ref(null)

provideBreadcrumbContext()

const showSidebar = computed(() => {
  return Boolean(route.meta.navKey)
})
const wideNavigationShown = computed(() =>
  showSidebar.value && !immersiveNavigation.value &&
  navigationMode.value === 'wide' && navigationUi.sidebarVisible
)
const mediumNavigationShown = computed(() =>
  showSidebar.value && !immersiveNavigation.value &&
  navigationMode.value === 'medium' && navigationUi.railVisible
)
const revealNavigation = computed(() =>
  showSidebar.value && !immersiveNavigation.value && (
    (navigationMode.value === 'wide' && !navigationUi.sidebarVisible) ||
    (navigationMode.value === 'medium' && !navigationUi.railVisible)
  )
)
const revealButton = ref(null)

async function focusReopenControl() {
  await nextTick()
  revealButton.value?.focus()
}

async function reopenNavigation() {
  const rail = navigationMode.value === 'medium'
  if (rail) navigationUi.showRail()
  else navigationUi.showSidebar()

  await nextTick()
  const selector = rail
    ? '.app-navigation-rail__collapse'
    : '.app-sidebar__collapse'
  document.querySelector(selector)?.focus()
}

const routeContentKey = computed(() => {
  return String(
    route.name ??
    route.path ??
    'route-content'
  )
})

watch(
  () => route.name,
  (routeName) => {
    document.title =
      getDocumentTitle(routeName)
  },
  { immediate: true }
)

watch(
  () => route.fullPath,
  async (currentPath, previousPath) => {
    if (
      !previousPath ||
      currentPath === previousPath
    ) {
      return
    }

    await nextTick()

    focusRouteContent(
      appMain.value
    )
  },
  { flush: 'post' }
)
</script>

<template>
  <div class="app-shell">
    <AppHeader />

    <div
      class="app-body"
      :class="{
        'app-body--with-sidebar': wideNavigationShown || mediumNavigationShown,
        'app-body--compact-navigation': showSidebar && navigationMode === 'compact' && !immersiveNavigation,
      }"
    >
      <Transition name="app-navigation-surface">
        <AppSidebar
          v-if="wideNavigationShown"
          @hide="focusReopenControl"
        />
      </Transition>
      <Transition name="app-navigation-surface">
        <AppNavigationRail
          v-if="mediumNavigationShown"
          @hide="focusReopenControl"
        />
      </Transition>

      <main
        ref="appMain"
        class="app-main"
        tabindex="-1"
      >
        <div
          v-if="revealNavigation"
          class="app-navigation-reveal-toolbar"
        >
          <button
            ref="revealButton"
            class="app-navigation-reveal"
            type="button"
            :aria-label="navigationMode === 'wide' ? 'Показать боковую панель' : 'Показать панель навигации'"
            :title="navigationMode === 'wide' ? 'Показать боковую панель' : 'Показать панель навигации'"
            :aria-controls="navigationMode === 'wide' ? 'workspace-sidebar' : 'workspace-rail'"
            :aria-expanded="false"
            @click="reopenNavigation"
          >
            <i class="pi pi-angle-double-right" aria-hidden="true" />
          </button>
        </div>

        <AppBreadcrumb />

        <div
          :key="routeContentKey"
          class="app-route-content"
        >
          <RouterView />
        </div>
      </main>
    </div>

    <AppFooter />
    <AppBottomNavigation
      v-if="showSidebar && navigationMode === 'compact' && !immersiveNavigation"
    />
  </div>
</template>

<style>
html,
body,
#app {
  min-height: 100%;
}

body {
  margin: 0;

  background:
    var(--st-page-bg);

  color:
    var(--st-text);
}

#app {
  min-height: 100vh;
}

*,
*::before,
*::after {
  box-sizing: border-box;
}

.app-shell {
  min-height: 100vh;

  display: flex;
  flex-direction: column;

  color: var(--st-text);
  background: var(--st-page-bg);
}

.app-body {
  position: relative;
  width: min(100%, 1180px);

  margin: 0 auto;
  padding: 20px;

  flex: 1;

  display: block;
}

.app-body--with-sidebar {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

.app-main {
  min-width: 0;
  width: 100%;
  flex: 1;

  color: var(--st-text);
  background: transparent;
}

.app-route-content {
  animation:
    app-route-content-reveal
    var(--st-motion-normal)
    var(--st-ease-standard)
    both;
}

@keyframes app-route-content-reveal {
  from {
    opacity: 0;
    transform: translateY(4px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.app-body--compact-navigation { padding-bottom: calc(78px + env(safe-area-inset-bottom)); }

/* The reopen button lives outside the removable navigation surface.
 * No icon-only rail or sidebar column is reserved when navigation is hidden. */
.app-navigation-reveal-toolbar {
  position: sticky;
  top: 80px;
  z-index: 30;
  width: max-content;
  margin-bottom: 12px;
}

.app-navigation-reveal {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  color: var(--st-shell-text);
  background: var(--st-shell-bg);
  border: 1px solid var(--st-shell-border);
  border-radius: var(--st-radius-control);
  box-shadow: var(--st-shadow-card);
  cursor: pointer;
  transition: background-color var(--st-motion-fast) var(--st-ease-standard);
}
.app-navigation-reveal:hover { background: var(--st-shell-hover); }
.app-navigation-reveal:focus-visible {
  outline: 2px solid var(--st-primary);
  outline-offset: 2px;
}

.app-navigation-surface-enter-active,
.app-navigation-surface-leave-active {
  transition:
    opacity var(--st-motion-normal) var(--st-ease-standard),
    transform var(--st-motion-normal) var(--st-ease-standard);
}
.app-navigation-surface-enter-from,
.app-navigation-surface-leave-to {
  opacity: 0;
  transform: translateX(-10px);
}
/* Override the sticky positioning of the two navigation surfaces only
 * during their leave transition, so main content can fill the freed width. */
.app-body > .app-navigation-surface-leave-active {
  position: absolute !important;
  pointer-events: none;
  z-index: 20;
}

.app-main:focus {
  outline: none;
}

@media (prefers-reduced-motion: reduce) {
  .app-route-content {
    animation: none;
  }
  .app-navigation-reveal,
  .app-navigation-surface-enter-active,
  .app-navigation-surface-leave-active {
    transition: none;
  }
}

@media (max-width: 960px) {
  .app-body,
  .app-body--with-sidebar {
    width: 100%;

    padding: 0;

    display: block;
  }

  .app-body--with-sidebar {
    display: flex;
    align-items: flex-start;
    gap: 12px;
  }

  .app-main {
    padding: 16px;
  }
}

@media (max-width: 640px) {
  .app-body--with-sidebar { display: block; }
}
</style>
