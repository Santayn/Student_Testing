<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { getActiveNavigationKey, getWorkspaceNavigationNodes } from '@/navigation'
import { useAuthStore } from '@/stores/auth'
import { useNavigationUiStore } from '@/stores/navigationUi'
import { useNavigationGroups } from '@/composables/navigation/useNavigationGroups'
import NavigationDisclosureGroup from './NavigationDisclosureGroup.vue'
import NavigationRootLink from './NavigationRootLink.vue'

const route = useRoute()
const authStore = useAuthStore()
const ui = useNavigationUiStore()
const emit = defineEmits(['hide'])
const role = computed(() => authStore.workspaceRole)
const nodes = computed(() => getWorkspaceNavigationNodes(role.value))
const sections = computed(() => nodes.value.filter((node) => node.type === 'group'))
const activeKey = computed(() => getActiveNavigationKey(route))
const { expanded, toggleGroup } = useNavigationGroups({
  role, sections, activeKey, surface: 'sidebar',
})

function hideSidebar() {
  ui.hideSidebar()
  emit('hide')
}
</script>

<template>
  <aside
    id="workspace-sidebar"
    class="app-sidebar"
    aria-label="Навигация по рабочему пространству"
  >
    <div class="app-sidebar__toolbar">
      <span class="app-sidebar__heading">Разделы</span>
      <button
        class="app-sidebar__collapse"
        type="button"
        aria-label="Скрыть боковую панель"
        title="Скрыть боковую панель"
        :aria-expanded="true"
        aria-controls="workspace-sidebar"
        @click="hideSidebar"
      >
        <i class="pi pi-angle-double-left" aria-hidden="true" />
      </button>
    </div>
    <nav
      class="app-sidebar__nav st-scrollbar st-scrollbar--shell st-scrollbar--sidebar"
      aria-label="Разделы приложения"
    >
      <template v-for="node in nodes" :key="node.key">
        <NavigationRootLink
          v-if="node.type === 'link'"
          :item="node"
          :active-key="activeKey"
        />
        <NavigationDisclosureGroup
          v-else
          :section="node"
          :active-key="activeKey"
          :expanded="expanded.includes(node.key)"
          id-prefix="sidebar-section"
          @toggle="toggleGroup"
        />
      </template>
    </nav>
  </aside>
</template>

<style scoped>
.app-sidebar {
  width: var(--st-sidebar-width);
  min-width: var(--st-sidebar-width);
  position: sticky;
  top: 84px;
  align-self: flex-start;
  display: flex;
  flex-direction: column;
  /* Reserve the full available viewport space so expanding a group cannot
   * stretch the page. Only __nav scrolls; the toolbar stays visible. */
  height: calc(100dvh - 104px);
  min-height: 0;
  overflow: hidden;
  color: var(--st-shell-text);
  background: var(--st-shell-bg);
  border: 1px solid var(--st-shell-border);
  border-radius: 12px;
  transition:
    border-color var(--st-motion-fast) var(--st-ease-standard);
}

/* Keep the collapse control visible while the navigation links scroll. */
.app-sidebar__toolbar {
  position: sticky;
  top: 0;
  z-index: 1;
  flex: 0 0 auto;
  min-height: 49px;
  padding: 5px 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--st-shell-bg);
  border-bottom: 1px solid var(--st-shell-border);
}

.app-sidebar__heading {
  padding-left: 7px;
  font-weight: var(--st-font-weight-semibold);
  font-size: 13px;
}

.app-sidebar__collapse {
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 8px;
  color: var(--st-shell-text);
  background: transparent;
  cursor: pointer;
}

.app-sidebar__collapse:hover,
.app-sidebar__collapse:focus-visible {
  background: var(--st-shell-hover);
}

.app-sidebar__collapse:focus-visible {
  outline: 2px solid var(--st-primary);
  outline-offset: 2px;
}

.app-sidebar__nav {
  min-height: 0;
  flex: 1 1 auto;
  padding: 8px;
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
}

@media (prefers-reduced-motion: reduce) {
  .app-sidebar { transition: none; }
}
</style>
