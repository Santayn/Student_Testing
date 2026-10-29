<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useNavigationUiStore } from '@/stores/navigationUi'
import { getActiveNavigationKey, getWorkspaceNavigationNodes } from '@/navigation'
import { NAVIGATION_GROUP_ICONS } from '@/navigation/presentationNavigation'
import { getRailFlyoutLayout } from '@/navigation/railFlyout'
import NavigationRailFlyout from './NavigationRailFlyout.vue'

const route = useRoute()
const auth = useAuthStore()
const navigationUi = useNavigationUiStore()
const emit = defineEmits(['hide'])
const rail = ref(null)
const flyout = ref(null)
const openGroupKey = ref(null)
const position = ref(null)
let activeTrigger = null

const nodes = computed(() => getWorkspaceNavigationNodes(auth.workspaceRole))
const activeKey = computed(() => getActiveNavigationKey(route))
const openGroup = computed(() => nodes.value.find(
  (node) => node.type === 'group' && node.key === openGroupKey.value
) ?? null)

function groupIsActive(group) {
  return group.items.some((item) => item.key === activeKey.value)
}

function updatePosition() {
  if (!activeTrigger || !openGroup.value) return
  position.value = getRailFlyoutLayout(activeTrigger.getBoundingClientRect(), {
    viewportWidth: window.innerWidth,
    viewportHeight: window.innerHeight,
    itemCount: openGroup.value.items.length,
  })
}

function closeFlyout({ restoreFocus = false } = {}) {
  const trigger = activeTrigger
  openGroupKey.value = null
  position.value = null
  activeTrigger = null
  if (restoreFocus && trigger?.isConnected) trigger.focus()
}

async function toggleGroup(group, event) {
  if (openGroupKey.value === group.key) {
    closeFlyout()
    return
  }
  activeTrigger = event.currentTarget
  openGroupKey.value = group.key
  updatePosition()
  if (event.detail === 0) {
    await nextTick()
    flyout.value?.focusFirstLink()
  }
}

function hideRail() {
  closeFlyout()
  navigationUi.hideRail()
  emit('hide')
}

function handleOutsidePointer(event) {
  if (rail.value?.contains(event.target) || flyout.value?.containsTarget(event.target)) return
  closeFlyout()
}
function handleFocusOutside(event) {
  if (rail.value?.contains(event.target) || flyout.value?.containsTarget(event.target)) return
  closeFlyout()
}
function handleKeydown(event) {
  if (event.key !== 'Escape') return
  event.preventDefault()
  closeFlyout({ restoreFocus: true })
}
function handleScroll(event) {
  // Scrolling the flyout is independent; a page/rail scroll closes stale popovers.
  if (!flyout.value?.containsTarget(event.target)) closeFlyout()
}
function connectListeners() {
  document.addEventListener('pointerdown', handleOutsidePointer, true)
  document.addEventListener('focusin', handleFocusOutside)
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('resize', updatePosition)
  window.addEventListener('scroll', handleScroll, true)
}
function disconnectListeners() {
  document.removeEventListener('pointerdown', handleOutsidePointer, true)
  document.removeEventListener('focusin', handleFocusOutside)
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('resize', updatePosition)
  window.removeEventListener('scroll', handleScroll, true)
}
watch(openGroupKey, (key) => {
  disconnectListeners()
  if (key) connectListeners()
})
watch(() => route.fullPath, () => closeFlyout())
watch(() => auth.workspaceRole, () => closeFlyout())
onBeforeUnmount(disconnectListeners)
</script>

<template>
  <aside id="workspace-rail" ref="rail" class="app-navigation-rail" aria-label="Навигационная панель">
    <div class="app-navigation-rail__toolbar">
      <button
        class="app-navigation-rail__collapse"
        type="button"
        aria-label="Скрыть панель навигации"
        title="Скрыть панель навигации"
        aria-controls="workspace-rail"
        :aria-expanded="true"
        @click="hideRail"
      >
        <i class="pi pi-angle-double-left" aria-hidden="true" />
      </button>
    </div>
    <nav class="app-navigation-rail__items st-scrollbar st-scrollbar--shell" aria-label="Основные разделы">
      <template v-for="node in nodes" :key="node.key">
        <button
          v-if="node.type === 'group'"
          type="button"
          class="app-navigation-rail__item"
          :class="{
            'app-navigation-rail__item--active': groupIsActive(node),
            'app-navigation-rail__item--open': openGroupKey === node.key,
          }"
          :aria-label="node.label"
          :title="node.label"
          :aria-expanded="openGroupKey === node.key"
          :aria-controls="openGroupKey === node.key ? 'navigation-rail-flyout' : undefined"
          @click="toggleGroup(node, $event)"
        >
          <i :class="NAVIGATION_GROUP_ICONS[node.key] || 'pi pi-folder'" aria-hidden="true" />
          <span>{{ node.label }}</span>
        </button>
        <RouterLink
          v-else
          class="app-navigation-rail__item"
          :class="{ 'app-navigation-rail__item--active': activeKey === node.key }"
          :to="node.route"
          :title="node.label"
          :aria-current="activeKey === node.key ? 'page' : undefined"
          @click="closeFlyout()"
        >
          <i :class="node.icon" aria-hidden="true" />
          <span>{{ node.label }}</span>
        </RouterLink>
      </template>
    </nav>
    <NavigationRailFlyout
      ref="flyout"
      :section="openGroup"
      :active-key="activeKey"
      :position="position || {}"
      @navigate="closeFlyout()"
    />
  </aside>
</template>

<style scoped>
.app-navigation-rail {
  width: 76px;
  min-width: 76px;
  position: sticky;
  top: 76px;
  height: calc(100dvh - 96px);
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--st-shell-border);
  background: var(--st-shell-bg);
  border-radius: var(--st-radius-card);
}
.app-navigation-rail__toolbar {
  position: sticky;
  top: 0;
  z-index: 1;
  flex: 0 0 auto;
  padding: 6px 5px;
  display: flex;
  justify-content: center;
  background: var(--st-shell-bg);
  border-bottom: 1px solid var(--st-shell-border);
}
.app-navigation-rail__collapse {
  width: 42px;
  height: 42px;
  border: 0;
  border-radius: var(--st-radius-control);
  color: var(--st-shell-text);
  background: transparent;
  cursor: pointer;
}
.app-navigation-rail__collapse:hover,
.app-navigation-rail__collapse:focus-visible { background: var(--st-shell-hover); }
.app-navigation-rail__collapse:focus-visible {
  outline: 2px solid var(--st-primary);
  outline-offset: -2px;
}
.app-navigation-rail__items {
  min-height: 0;
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 8px 5px;
  overflow-y: auto;
  overflow-x: hidden;
  overscroll-behavior: contain;
}
.app-navigation-rail__item {
  width: 100%;
  min-height: 58px;
  padding: 7px 3px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 5px;
  font: inherit;
  color: var(--st-shell-text);
  font-size: 10px;
  line-height: 1.2;
  text-align: center;
  text-decoration: none;
  border: 0;
  border-radius: 10px;
  background: transparent;
  cursor: pointer;
  overflow-wrap: anywhere;
  transition: background-color var(--st-motion-normal) var(--st-ease-standard);
}
.app-navigation-rail__item > i { font-size: 20px; color: var(--st-shell-muted); }
.app-navigation-rail__item--active { background: var(--st-shell-active); font-weight: var(--st-font-weight-semibold); }
.app-navigation-rail__item--active > i { color: var(--st-primary); }
.app-navigation-rail__item--open { outline: 1px solid var(--st-primary); outline-offset: -1px; }
.app-navigation-rail__item:hover, .app-navigation-rail__item:focus-visible { background: var(--st-shell-hover); }
.app-navigation-rail__item:focus-visible { outline: 2px solid var(--st-primary); outline-offset: -2px; }
@media (prefers-reduced-motion: reduce) { .app-navigation-rail__item { transition: none; } }
</style>
