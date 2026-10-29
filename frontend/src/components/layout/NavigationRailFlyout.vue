<script setup>
import { ref } from 'vue'

defineOptions({ name: 'NavigationRailFlyout' })

defineProps({
  section: { type: Object, default: null },
  activeKey: { type: String, default: null },
  position: { type: Object, default: () => ({}) },
})

const emit = defineEmits(['navigate'])
const panel = ref(null)

defineExpose({
  containsTarget: (target) => Boolean(panel.value?.contains(target)),
  focusFirstLink: () => panel.value?.querySelector('a[href]')?.focus(),
})
</script>

<template>
  <Teleport to="body">
    <Transition name="rail-flyout">
      <nav
        v-if="section"
        id="navigation-rail-flyout"
        ref="panel"
        class="navigation-rail-flyout st-scrollbar st-scrollbar--shell"
        :style="position"
        :aria-label="section.label"
      >
        <div class="navigation-rail-flyout__heading">{{ section.label }}</div>
        <div class="navigation-rail-flyout__links">
          <RouterLink
            v-for="item in section.items"
            :key="item.key"
            :to="item.route"
            class="navigation-rail-flyout__link"
            :class="{ 'navigation-rail-flyout__link--active': item.key === activeKey }"
            :aria-current="item.key === activeKey ? 'page' : undefined"
            @click="emit('navigate')"
          >
            <i :class="item.icon" aria-hidden="true" />
            <span>{{ item.label }}</span>
          </RouterLink>
        </div>
      </nav>
    </Transition>
  </Teleport>
</template>

<style scoped>
.navigation-rail-flyout {
  position: fixed;
  z-index: 350;
  min-width: 0;
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 14px 16px 16px;
  border: 1px solid var(--st-shell-border);
  border-radius: var(--st-radius-card);
  background: var(--st-shell-elevated);
  color: var(--st-shell-text);
  box-shadow: var(--st-shadow-elevated);
}
.navigation-rail-flyout__heading {
  padding: 2px 0 12px;
  font-size: var(--st-font-sm);
  font-weight: var(--st-font-weight-bold);
}
.navigation-rail-flyout__links {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 156px), 1fr));
  gap: 9px;
}
.navigation-rail-flyout__link {
  min-width: 0;
  min-height: 48px;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 10px;
  border: 1px solid var(--st-shell-border);
  border-radius: var(--st-radius-control);
  color: var(--st-shell-text);
  background: var(--st-shell-bg);
  text-decoration: none;
  font-size: var(--st-font-sm);
  line-height: var(--st-line-normal);
  overflow-wrap: anywhere;
  transition: background-color var(--st-motion-fast) var(--st-ease-standard);
}
.navigation-rail-flyout__link i { flex: none; color: var(--st-shell-muted); }
.navigation-rail-flyout__link:hover,
.navigation-rail-flyout__link:focus-visible { background: var(--st-shell-hover); }
.navigation-rail-flyout__link:focus-visible { outline: 2px solid var(--st-primary); outline-offset: 2px; }
.navigation-rail-flyout__link--active { background: var(--st-shell-active); border-color: var(--st-primary); font-weight: var(--st-font-weight-semibold); }
.navigation-rail-flyout__link--active i { color: var(--st-primary); }
.rail-flyout-enter-active { transition: opacity var(--st-motion-normal) var(--st-ease-standard), transform var(--st-motion-normal) var(--st-ease-standard); }
.rail-flyout-leave-active { pointer-events: none; transition: opacity var(--st-motion-fast) var(--st-ease-standard), transform var(--st-motion-fast) var(--st-ease-standard); }
.rail-flyout-enter-from, .rail-flyout-leave-to { opacity: 0; transform: translateX(-8px); }
@media (prefers-reduced-motion: reduce) {
  .rail-flyout-enter-active, .rail-flyout-leave-active, .navigation-rail-flyout__link { transition: none; }
}
</style>
