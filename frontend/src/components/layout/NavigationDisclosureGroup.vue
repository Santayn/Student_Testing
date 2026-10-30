<script setup>
import { computed } from 'vue'
import { NAVIGATION_GROUP_ICONS } from '@/navigation/presentationNavigation'
import { navigationMotionClass, navigationTransitionName } from '@/config/motionPresets'

const props = defineProps({
  section: { type: Object, required: true },
  expanded: { type: Boolean, default: false },
  activeKey: { type: String, default: null },
  idPrefix: { type: String, required: true },
})
const emit = defineEmits(['toggle', 'navigate'])

const panelId = computed(() => `${props.idPrefix}-${props.section.key}`)
const activeGroup = computed(() => props.section.items.some(
  (item) => item.key === props.activeKey
))

function toggle() { emit('toggle', props.section.key) }
</script>

<template>
  <section class="navigation-group" :class="{ 'navigation-group--active': activeGroup }">
    <button
      class="navigation-group__trigger"
      :class="navigationMotionClass('item')"
      type="button"
      :aria-expanded="expanded"
      :aria-controls="panelId"
      @click="toggle"
    >
      <i :class="NAVIGATION_GROUP_ICONS[section.key] || 'pi pi-folder'" aria-hidden="true" />
      <span class="navigation-group__label">{{ section.label }}</span>
      <i class="pi pi-chevron-down navigation-group__chevron" :class="{ 'navigation-group__chevron--open': expanded }" aria-hidden="true" />
    </button>
    <Transition :name="navigationTransitionName('disclosure')">
      <div v-if="expanded" :id="panelId" class="navigation-group__items">
        <div class="navigation-group__items-inner">
          <div class="navigation-group__links">
            <RouterLink
              v-for="item in section.items"
              :key="item.key"
              class="app-sidebar__link"
              :class="{ [navigationMotionClass('item')]: true, 'app-sidebar__link--active': item.key === activeKey }"
              :to="item.route"
              :aria-current="item.key === activeKey ? 'page' : undefined"
              @click="emit('navigate')"
            >
              <i v-if="item.icon" class="app-sidebar__icon" :class="item.icon" aria-hidden="true" />
              <span>{{ item.label }}</span>
            </RouterLink>
          </div>
        </div>
      </div>
    </Transition>
  </section>
</template>

<style scoped>
.navigation-group { margin-bottom: 4px; }
.navigation-group__trigger {
  width: 100%; min-height: 44px; padding: 9px 10px; display: flex;
  align-items: center; gap: 10px; text-align: left;
  font: inherit; font-size: 13px; font-weight: var(--st-font-weight-semibold);
  color: var(--st-shell-text); background: transparent; border: 0;
  border-radius: 9px; cursor: pointer;
}
.navigation-group__trigger:hover, .navigation-group__trigger:focus-visible { background: var(--st-shell-hover); }
.navigation-group__trigger:focus-visible { outline: 2px solid var(--st-primary); outline-offset: -2px; }
.navigation-group__trigger > .pi:first-child { width: 20px; text-align: center; color: var(--st-shell-muted); }
.navigation-group__label { min-width: 0; flex: 1; }
.navigation-group__chevron { color: var(--st-shell-muted); font-size: 11px; }
.navigation-group__chevron--open { transform: rotate(180deg); }
/* Grid tracks interpolate between the real content height and zero. This
 * avoids an arbitrary max-height and the resulting uneven animation speed. */
.navigation-group__items { display: grid; overflow: hidden; }
.navigation-group__items-inner { min-height: 0; overflow: hidden; }
.navigation-group__links { padding: 1px 0 6px 14px; }
:deep(.app-sidebar__link) {
  min-height: 40px; padding: 9px 10px; display: flex; align-items: center; gap: 10px;
  color: var(--st-shell-text); border-radius: 8px; text-decoration: none; font-size: 13px;
}
:deep(.app-sidebar__icon) { width: 18px; flex: 0 0 18px; text-align: center; color: var(--st-shell-muted); }
:deep(.app-sidebar__link:hover), :deep(.app-sidebar__link:focus-visible) { background: var(--st-shell-hover); }
:deep(.app-sidebar__link:focus-visible) { outline: 2px solid var(--st-primary); outline-offset: -2px; }
:deep(.app-sidebar__link--active) { background: var(--st-shell-active); font-weight: var(--st-font-weight-bold); }
:deep(.app-sidebar__link--active .app-sidebar__icon) { color: var(--st-primary); }
</style>
