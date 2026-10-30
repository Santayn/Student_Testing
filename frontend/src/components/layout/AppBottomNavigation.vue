<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getActiveNavigationKey } from '@/navigation'
import { getPrimaryNavigation, activePrimaryKey, MORE_DESTINATION_KEY } from '@/navigation/presentationNavigation'
import { useScrollNavigation } from '@/composables/navigation/useScrollNavigation'
import NavigationMoreSheet from './NavigationMoreSheet.vue'
import { navigationMotionClass } from '@/config/motionPresets'

const route = useRoute()
const auth = useAuthStore()
const tabs = computed(() => getPrimaryNavigation(auth.workspaceRole))
const activeKey = computed(() => activePrimaryKey(getActiveNavigationKey(route), tabs.value))
const moreOpen = ref(false)
const { visible } = useScrollNavigation(route)
watch(() => route.fullPath, () => { moreOpen.value = false })
</script>

<template>
    <nav
      class="app-bottom-navigation"
      :class="{ 'app-bottom-navigation--hidden': !visible && !moreOpen, [navigationMotionClass('bottom')]: true }"
      aria-label="Основная мобильная навигация"
      :inert="!visible && !moreOpen"
    >
      <template v-for="item in tabs" :key="item.key">
        <button
          v-if="item.key === MORE_DESTINATION_KEY"
          type="button"
          class="app-bottom-navigation__item"
          :class="{ 'app-bottom-navigation__item--active': activeKey === item.key, [navigationMotionClass('item')]: true }"
          :aria-expanded="moreOpen"
          aria-label="Все разделы"
          @click="moreOpen = true"
        >
          <i :class="item.icon" aria-hidden="true" />
          <span>{{ item.label }}</span>
        </button>
        <RouterLink
          v-else
          class="app-bottom-navigation__item"
          :class="{ 'app-bottom-navigation__item--active': activeKey === item.key, [navigationMotionClass('item')]: true }"
          :to="item.route"
          :aria-current="activeKey === item.key ? 'page' : undefined"
        >
          <i :class="item.icon" aria-hidden="true" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </template>
    </nav>
    <NavigationMoreSheet :open="moreOpen" @close="moreOpen = false" />
</template>

<style scoped>
.app-bottom-navigation { position: fixed; z-index: 180; bottom: 0; left: 0; right: 0; min-height: 60px; padding: 5px 5px calc(5px + env(safe-area-inset-bottom)); display: flex; justify-content: space-around; align-items: stretch; gap: 2px; border-top: 1px solid var(--st-shell-border); background: var(--st-shell-bg); box-shadow: 0 -3px 16px rgba(0,0,0,.07); }
.app-bottom-navigation--hidden { transform: translateY(105%); pointer-events: none; }
.app-bottom-navigation__item { flex: 1 1 0; min-width: 0; max-width: 115px; min-height: 52px; padding: 5px 1px; display: flex; flex-direction: column; gap: 4px; justify-content: center; align-items: center; font: inherit; font-size: 10px; font-weight: var(--st-font-weight-medium); line-height: 1.2; text-decoration: none; text-align: center; color: var(--st-shell-text); background: transparent; border: 0; border-radius: 10px; cursor: pointer; }
.app-bottom-navigation__item > i { font-size: 20px; color: var(--st-shell-muted); }
.app-bottom-navigation__item--active { background: var(--st-shell-active); font-weight: var(--st-font-weight-semibold); }
.app-bottom-navigation__item--active > i { color: var(--st-primary); }
.app-bottom-navigation__item:hover, .app-bottom-navigation__item:focus-visible { background: var(--st-shell-hover); }
.app-bottom-navigation__item:focus-visible { outline: 2px solid var(--st-primary); outline-offset: -2px; }
</style>
