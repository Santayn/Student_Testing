<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getActiveNavigationKey, getWorkspaceNavigationNodes } from '@/navigation'
import { useAuthStore } from '@/stores/auth'
import { useNavigationGroups } from '@/composables/navigation/useNavigationGroups'
import NavigationDisclosureGroup from './NavigationDisclosureGroup.vue'
import NavigationRootLink from './NavigationRootLink.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
})
const emit = defineEmits(['close'])
const route = useRoute()
const authStore = useAuthStore()
const role = computed(() => authStore.workspaceRole)
const nodes = computed(() => getWorkspaceNavigationNodes(role.value))
const sections = computed(() => nodes.value.filter((node) => node.type === 'group'))
const activeKey = computed(() => getActiveNavigationKey(route))
const { expanded, toggleGroup } = useNavigationGroups({
  role, sections, activeKey, surface: 'more',
})

const sheet = ref(null)
const closeButton = ref(null)
let restoreFocus = null

function close() { emit('close') }
function handleKeydown(event) {
  if (!props.open) return
  if (event.key === 'Escape') {
    event.preventDefault()
    close()
    return
  }
  if (event.key !== 'Tab') return
  const focusable = [...(sheet.value?.querySelectorAll('button, a[href]') ?? [])]
    .filter((element) => element.getClientRects().length > 0 || element === closeButton.value)
  if (!focusable.length) return
  const first = focusable[0]
  const last = focusable[focusable.length - 1]
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first.focus()
  }
}

watch(() => props.open, async (open) => {
  if (open) {
    restoreFocus = document.activeElement
    document.addEventListener('keydown', handleKeydown)
    document.body.classList.add('navigation-sheet-open')
    document.documentElement.classList.add('navigation-sheet-open')
    await nextTick()
    closeButton.value?.focus()
  } else {
    document.removeEventListener('keydown', handleKeydown)
    document.body.classList.remove('navigation-sheet-open')
    document.documentElement.classList.remove('navigation-sheet-open')
    await nextTick()
    restoreFocus?.focus?.()
    restoreFocus = null
  }
})
watch(() => route.fullPath, close)
onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleKeydown)
  document.body.classList.remove('navigation-sheet-open')
  document.documentElement.classList.remove('navigation-sheet-open')
})
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="navigation-sheet-backdrop" @click.self="close">
      <section
        ref="sheet"
        class="navigation-sheet"
        role="dialog"
        aria-modal="true"
        aria-label="Все разделы приложения"
      >
        <div class="navigation-sheet__heading">
          <strong>Все разделы</strong>
          <button ref="closeButton" type="button" aria-label="Закрыть разделы" @click="close">
            <i class="pi pi-times" aria-hidden="true" />
          </button>
        </div>
        <nav class="navigation-sheet__body st-scrollbar st-scrollbar--shell" aria-label="Навигация по разделам">
          <template v-for="node in nodes" :key="node.key">
            <NavigationRootLink
              v-if="node.type === 'link'"
              :item="node"
              :active-key="activeKey"
              @navigate="close"
            />
            <NavigationDisclosureGroup
              v-else
              :section="node"
              :expanded="expanded.includes(node.key)"
              :active-key="activeKey"
              id-prefix="more-section"
              @toggle="toggleGroup"
              @navigate="close"
            />
          </template>
        </nav>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.navigation-sheet-backdrop { position: fixed; inset: 0; z-index: 500; display: flex; align-items: flex-end; justify-content: center; background: var(--st-overlay-backdrop); }
/* Keep the dialog geometry fixed while groups expand. Scrolling belongs
 * to the navigation list, not the sheet/backdrop or the underlying page. */
.navigation-sheet { width: min(100%, 520px); height: min(82dvh, 760px); min-height: 0; display: flex; flex-direction: column; overflow: hidden; padding-bottom: env(safe-area-inset-bottom); border-radius: 20px 20px 0 0; background: var(--st-shell-bg); color: var(--st-shell-text); box-shadow: 0 -12px 38px rgba(0,0,0,.18); animation: navigation-sheet-enter var(--st-motion-slow) var(--st-ease-standard); }
.navigation-sheet__heading { flex: 0 0 auto; background: var(--st-shell-bg); padding: 14px 18px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--st-shell-border); }
.navigation-sheet__heading button { width: 42px; height: 42px; background: transparent; border: 0; border-radius: 8px; color: inherit; cursor: pointer; }
.navigation-sheet__heading button:hover, .navigation-sheet__heading button:focus-visible { background: var(--st-shell-hover); }
.navigation-sheet__heading button:focus-visible { outline: 2px solid var(--st-primary); }
.navigation-sheet__body { flex: 1 1 auto; min-height: 0; padding: 12px 16px; overflow-x: hidden; overflow-y: auto; overscroll-behavior: contain; scrollbar-gutter: stable; }
@keyframes navigation-sheet-enter { from { transform: translateY(20px); opacity: .65; } to { transform: translateY(0); opacity: 1; } }
@media (prefers-reduced-motion: reduce) { .navigation-sheet { animation: none; } }
</style>
<style>
html.navigation-sheet-open,
body.navigation-sheet-open {
  overflow: hidden;
}
</style>
