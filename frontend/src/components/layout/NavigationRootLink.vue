<script setup>
import { navigationMotionClass } from '@/config/motionPresets'

defineOptions({ name: 'NavigationRootLink' })

defineProps({
  item: { type: Object, required: true },
  activeKey: { type: String, default: null },
})
const emit = defineEmits(['navigate'])
</script>

<template>
  <RouterLink
    class="navigation-root-link"
    :class="{
      'navigation-root-link--active': item.key === activeKey,
      [navigationMotionClass('item')]: true,
    }"
    :to="item.route"
    :aria-current="item.key === activeKey ? 'page' : undefined"
    @click="emit('navigate')"
  >
    <i v-if="item.icon" class="navigation-root-link__icon" :class="item.icon" aria-hidden="true" />
    <span class="navigation-root-link__label">{{ item.label }}</span>
  </RouterLink>
</template>

<style scoped>
.navigation-root-link {
  min-height: 44px;
  margin-bottom: 4px;
  padding: 9px 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-radius: 9px;
  text-decoration: none;
  font-size: 13px;
  font-weight: var(--st-font-weight-semibold);
  color: var(--st-shell-text);
}
.navigation-root-link__icon {
  width: 20px;
  text-align: center;
  color: var(--st-shell-muted);
}
.navigation-root-link__label { min-width: 0; flex: 1; }
.navigation-root-link:hover, .navigation-root-link:focus-visible { background: var(--st-shell-hover); }
.navigation-root-link:focus-visible { outline: 2px solid var(--st-primary); outline-offset: -2px; }
.navigation-root-link--active { background: var(--st-shell-active); }
.navigation-root-link--active .navigation-root-link__icon { color: var(--st-primary); }
@media (prefers-reduced-motion: reduce) {
  .navigation-root-link { transition: none; }
}
</style>
